package com.example.configviewer.model;

public record ParamDiffRecord(String status,
                              String key,
                              String name,
                              String path,
                              String dataTypeA,
                              String valueA,
                              String dataTypeB,
                              String valueB,
                              String description) {
}
