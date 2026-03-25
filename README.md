# Config Viewer (Java Desktop)

Десктоп-приложение на Java (Swing) для работы с JSON-конфигурациями вида SUP/параметры:
- импорт JSON в локальную базу;
- удобный просмотр в таблице;
- быстрый поиск по всем полям;
- сравнение двух конфигураций и поиск отличий.

Приложение ориентировано на ключевые поля параметра:
- имя параметра;
- пояснение (description);
- тип (dataType);
- значение (values);
- путь (bundles.path).

## Что внутри

- Язык: Java 21
- GUI: Swing
- Парсинг JSON: Gson
- Локальная база: SQLite (`configurations.db`)
- Сборка: Maven

## Структура данных из JSON

Ожидается массив объектов. Для каждого элемента используются:
- `key.tenant`, `key.scope`, `key.location`
- `parameter.name`, `parameter.description`, `parameter.dataType`
- `parameter.bundles[].path`
- `parameter.bundles[].values[]`

Если у параметра несколько `bundles` и/или несколько `values`, каждая комбинация сохраняется в БД.
Для сравнения используется составной ключ:
- `tenant|scope|location|path|name`

Это позволяет корректно сравнивать параметры с одинаковым именем, но разным путём/контекстом.

## Быстрый старт (локально, без контейнера)

### 1) Сборка

```bash
mvn -DskipTests package
```

### 2) Запуск

```bash
java -jar target/config-viewer-1.0.0-jar-with-dependencies.jar
```

### 3) Как пользоваться

1. Нажать `Импорт JSON` и выбрать файл.
2. Задать имя конфигурации (снимка).
3. Выбрать конфигурацию слева в `База конфигураций`.
4. Во вкладке `Просмотр и поиск`:
   - смотреть таблицу;
   - искать по любому тексту через поле `Поиск`.
5. Во вкладке `Сравнение`:
   - выбрать `Конфигурация A` и `Конфигурация B`;
   - нажать `Сравнить`;
   - анализировать статусы:
     - `ADDED` — параметр добавлен;
     - `REMOVED` — параметр удалён;
     - `CHANGED` — параметр изменён.

## Где хранятся данные

- SQLite база: `configurations.db`
- В контейнерном режиме рекомендуется хранить в `./data/configurations.db`

## Git-репозиторий

Репозиторий инициализирован в корне проекта.
Базовые команды:

```bash
git status
git add .
git commit -m "Initial config viewer"
```

## Запуск в Docker/Podman (Linux, удобно и без лишних шагов)

Добавлены:
- `Dockerfile`
- `Containerfile` (для Podman)
- `compose.yaml`
- скрипт управления: `scripts/cfgviewctl.sh`

Скрипт поддерживает обе runtime-среды:
- Docker по умолчанию
- Podman через `CFGVIEW_RUNTIME=podman`

### Одной командой: собрать и запустить

Docker:

```bash
./scripts/cfgviewctl.sh build && ./scripts/cfgviewctl.sh up
```

Podman:

```bash
CFGVIEW_RUNTIME=podman ./scripts/cfgviewctl.sh build && CFGVIEW_RUNTIME=podman ./scripts/cfgviewctl.sh up
```

### Остановить контейнер (одна команда)

Docker:

```bash
./scripts/cfgviewctl.sh stop
```

Podman:

```bash
CFGVIEW_RUNTIME=podman ./scripts/cfgviewctl.sh stop
```

### Удалить контейнер (одна команда)

Docker:

```bash
./scripts/cfgviewctl.sh rm
```

Podman:

```bash
CFGVIEW_RUNTIME=podman ./scripts/cfgviewctl.sh rm
```

### Остановить и удалить сразу

```bash
./scripts/cfgviewctl.sh down
```

или для Podman:

```bash
CFGVIEW_RUNTIME=podman ./scripts/cfgviewctl.sh down
```

## Важно для Linux GUI (X11)

Приложение десктопное (Swing), поэтому контейнеру нужен доступ к X-серверу:
- должен быть установлен `xhost`;
- переменная `DISPLAY` должна быть задана;
- скрипт автоматически выполняет безопасное разрешение:
  - `xhost +SI:localuser:$(id -un)`

Если у вас Wayland, обычно нужен XWayland (в большинстве дистрибутивов уже есть).

## Рекомендованный alias (без конфликта с Unix-командами)

Чтобы не конфликтовать со стандартными командами (`cat`, `top`, `service` и т.д.),
используйте уникальный префикс, например `cfgv`.

Добавьте в `~/.bashrc` или `~/.zshrc`:

```bash
alias cfgv='$PWD/scripts/cfgviewctl.sh'
```

Лучше с абсолютным путём:

```bash
alias cfgv='/absolute/path/to/pacman-python-script/scripts/cfgviewctl.sh'
```

После этого:

```bash
cfgv build
cfgv up
cfgv status
cfgv stop
cfgv rm
```

## Альтернатива через docker compose

Сборка и запуск:

```bash
docker compose up -d --build
```

Остановка и удаление:

```bash
docker compose down
```

## Команды для диагностики

Логи:

```bash
./scripts/cfgviewctl.sh logs
```

Статус контейнера:

```bash
./scripts/cfgviewctl.sh status
```

## Что можно добавить дальше

- экспорт результатов сравнения в CSV/Excel;
- фильтр по типу параметра;
- подсветку изменённых полей в сравнении;
- запуск как systemd user service.
