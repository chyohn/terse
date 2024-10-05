package io.github.chyohn.terse.cluster.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultEnvironment implements Environment {

    protected final Map<String, String> config = new ConcurrentHashMap<>();

    public void setProperty(String key, String value) {
        config.put(key, value);
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        config.putAll(properties);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String val = getProperty(key);
        if (val == null) {
            return defaultValue;
        }

        Convertor<String, T> convertor = Convertors.findConvertor(String.class, targetType);
        return convertor.apply(val);
    }

    public String getProperty(String key, String defaultVal) {

        String val = config.get(key);
        if (val != null) {
            return val;
        }

        val = System.getProperty(key, System.getenv(key));
        if (val != null) {
            return val;
        }
        return defaultVal;
    }

}
