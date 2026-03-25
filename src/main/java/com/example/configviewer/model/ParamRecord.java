package com.example.configviewer.model;

import java.util.Objects;

public record ParamRecord(String compositeKey,
                          String name,
                          String description,
                          String dataType,
                          String value,
                          String path,
                          String tenant,
                          String scope,
                          String location) {

    public boolean isEquivalent(ParamRecord other) {
        return Objects.equals(name, other.name)
                && Objects.equals(description, other.description)
                && Objects.equals(dataType, other.dataType)
                && Objects.equals(value, other.value)
                && Objects.equals(path, other.path)
                && Objects.equals(tenant, other.tenant)
                && Objects.equals(scope, other.scope)
                && Objects.equals(location, other.location);
    }

    public ParamRecord withValue(String nextValue) {
        return new ParamRecord(compositeKey, name, description, dataType, nextValue, path, tenant, scope, location);
    }
}
