package io.github.chyohn.terse.cluster.config;

import java.util.Map;

public interface Environment {

    default  <T> T getProperty(String key, Class<T> targetType) {
        return getProperty(key, targetType, null);
    }
    default String getProperty(String key) {
        return getProperty(key, (String) null);
    }

    void setProperty(String key, String value);

    void setProperties(Map<String, String> properties);

    <T> T getProperty(String key, Class<T> targetType, T defaultValue);

    String getProperty(String key, String defaultVal);

}
