# MVP Architecture

Проект переведен на MVP (Model-View-Presenter), чтобы код было проще поддерживать, тестировать и расширять.

## Слои

### 1) Model
- `AppUiSettings` — runtime-настройки UI.
- `ConfigRecord` — запись конфигурации (snapshot).
- `ParamRecord` — параметр конфигурации.
- `ParamDiffRecord` — строка отличия при сравнении.

### 2) View
- `MainView` — контракт интерфейса (что Presenter может попросить показать).
- `SwingMainView` — Swing-реализация UI.

View отвечает только за:
- отображение;
- UI-события (кнопки, выбор, поле поиска);
- диалоги (info/warn/error).

View не содержит SQL и не парсит JSON.

### 3) Presenter
- `MainPresenter` — orchestration между View и бизнес-логикой.
- `MainViewListener` — контракт событий от View к Presenter.

Presenter отвечает за:
- инициализацию;
- обработку импорт/refresh/selection/compare;
- валидацию пользовательских сценариев;
- показ ошибок/уведомлений через View.

### 4) Service
- `ConfigService` — импорт и чтение конфигураций.
- `CompareService` — логика сравнения параметров.

### 5) Repository
- `ConfigRepository` — SQL/SQLite, хранение и выборки.

### 6) Parser
- `ConfigJsonParser` — парсинг входного JSON в `ParamRecord`.

### 7) Config/bootstrap
- `UiSettingsLoader` — чтение настроек UI.
- `UiScaleApplier` — применение масштабирования/шрифтов.
- `ConfigViewerApp` — точка входа, wiring зависимостей.

## Поток данных

1. Пользователь нажимает `Импорт JSON`.
2. `SwingMainView` отправляет событие в `MainPresenter`.
3. `MainPresenter` вызывает `ConfigService.importConfig(...)`.
4. `ConfigService` использует `ConfigJsonParser` и `ConfigRepository`.
5. После сохранения Presenter обновляет список через View.

Сценарий сравнения:
1. View отправляет выбранные `ConfigRecord A/B`.
2. Presenter читает параметры через `ConfigService`.
3. Presenter вызывает `CompareService.compare(...)`.
4. View получает и отображает diff-таблицу.

## Почему стало лучше

- Меньше связанности: UI отделен от SQL и парсинга.
- Проще изменения: можно менять View или storage отдельно.
- Ясные зоны ответственности по пакетам.
- Удобнее писать unit-тесты для `CompareService`, `ConfigJsonParser`, `ConfigService`.
