package com.example.configviewer.service;

import com.example.configviewer.model.ParamDiffRecord;
import com.example.configviewer.model.ParamRecord;

import java.util.*;

public class CompareService {

    public List<ParamDiffRecord> compare(List<ParamRecord> paramsA, List<ParamRecord> paramsB) {
        Map<String, ParamRecord> mapA = toUniqueMap(paramsA);
        Map<String, ParamRecord> mapB = toUniqueMap(paramsB);

        Set<String> keys = new TreeSet<>();
        keys.addAll(mapA.keySet());
        keys.addAll(mapB.keySet());

        List<ParamDiffRecord> diffs = new ArrayList<>();
        for (String key : keys) {
            ParamRecord left = mapA.get(key);
            ParamRecord right = mapB.get(key);

            if (left == null && right != null) {
                diffs.add(new ParamDiffRecord(
                        "ADDED",
                        key,
                        right.name(),
                        right.path(),
                        "",
                        "",
                        right.dataType(),
                        right.value(),
                        right.description()
                ));
                continue;
            }

            if (left != null && right == null) {
                diffs.add(new ParamDiffRecord(
                        "REMOVED",
                        key,
                        left.name(),
                        left.path(),
                        left.dataType(),
                        left.value(),
                        "",
                        "",
                        left.description()
                ));
                continue;
            }

            if (left != null && right != null && !left.isEquivalent(right)) {
                diffs.add(new ParamDiffRecord(
                        "CHANGED",
                        key,
                        left.name(),
                        left.path(),
                        left.dataType(),
                        left.value(),
                        right.dataType(),
                        right.value(),
                        chooseDescription(left.description(), right.description())
                ));
            }
        }

        return diffs;
    }

    private Map<String, ParamRecord> toUniqueMap(List<ParamRecord> records) {
        Map<String, ParamRecord> map = new LinkedHashMap<>();

        for (ParamRecord record : records) {
            String key = record.compositeKey();
            if (!map.containsKey(key)) {
                map.put(key, record);
                continue;
            }

            ParamRecord prev = map.get(key);
            map.put(key, prev.withValue(mergeValues(prev.value(), record.value())));
        }

        return map;
    }

    private String mergeValues(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;

        if (a.isBlank()) {
            return b;
        }
        if (b.isBlank()) {
            return a;
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String item : a.split(";\\s*")) {
            if (!item.isBlank()) {
                unique.add(item);
            }
        }
        for (String item : b.split(";\\s*")) {
            if (!item.isBlank()) {
                unique.add(item);
            }
        }

        return String.join("; ", unique);
    }

    private String chooseDescription(String left, String right) {
        if (left != null && !left.isBlank()) {
            return left;
        }
        if (right != null && !right.isBlank()) {
            return right;
        }
        return "";
    }
}
