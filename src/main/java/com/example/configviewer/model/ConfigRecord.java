package com.example.configviewer.model;

public record ConfigRecord(int id, String name, String fileName, String importedAt) {
    @Override
    public String toString() {
        return name + "  [" + importedAt + "]";
    }
}
