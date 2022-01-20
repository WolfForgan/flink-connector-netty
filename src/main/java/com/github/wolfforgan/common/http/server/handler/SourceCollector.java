package com.github.wolfforgan.common.http.server.handler;

public interface SourceCollector<T> {
    void collect(T element);
}
