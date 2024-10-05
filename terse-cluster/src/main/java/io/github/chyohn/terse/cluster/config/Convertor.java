package io.github.chyohn.terse.cluster.config;

import java.util.function.Function;

public interface Convertor<T, R> extends Function<T, R> {
}
