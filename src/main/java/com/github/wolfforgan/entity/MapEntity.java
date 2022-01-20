package com.github.wolfforgan.entity;

import java.io.Serializable;

public class MapEntity implements Serializable {
    public String Key;
    public String Value;
    public Long Timestamp;

    public MapEntity(String Key, String Value, Long Timestamp){
        this.Key = Key;
        this.Value = Value;
        this.Timestamp = Timestamp;
    }
    public String getKey() {
        return Key;
    }

    public void setKey(String key) {
        Key = key;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }

    public Long getTimestamp() {
        return Timestamp;
    }

    public void setTimestamp(Long timestamp) {
        Timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MapEntity{" +
                "Key='" + Key + '\'' +
                ", Value='" + Value + '\'' +
                ", Timestamp=" + Timestamp +
                '}';
    }
}
