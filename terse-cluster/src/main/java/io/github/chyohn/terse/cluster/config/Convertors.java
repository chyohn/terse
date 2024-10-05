package io.github.chyohn.terse.cluster.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Convertors {
    private static final Map<ConvertiblePair, Convertor<?, ?>> convertorMap = new ConcurrentHashMap<>();

    static {
        addConvertor(String.class, Integer.class, Integer::parseInt);
        addConvertor(String.class, Long.class, Long::parseLong);
        addConvertor(String.class, Double.class, Double::parseDouble);
    }

    public static <S, T> void addConvertor(Class<S> sourceType, Class<T> targetType, Convertor<S, T> convertor) {
        ConvertiblePair pair = new ConvertiblePair(sourceType, targetType);
        convertorMap.put(pair, convertor);
    }

    public static <S, T> Convertor<S, T> findConvertor(Class<S> sourceType, Class<T> targetType) {
        ConvertiblePair pair = new ConvertiblePair(sourceType, targetType);
        return (Convertor<S, T>) convertorMap.get(pair);
    }

    /**
     * Holder for a source-to-target class pair.
     */
    static final class ConvertiblePair {

        private final Class<?> sourceType;

        private final Class<?> targetType;

        /**
         * Create a new source-to-target pair.
         *
         * @param sourceType the source type
         * @param targetType the target type
         */
        public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || other.getClass() != ConvertiblePair.class) {
                return false;
            }
            ConvertiblePair otherPair = (ConvertiblePair) other;
            return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
        }

        @Override
        public int hashCode() {
            return (this.sourceType.hashCode() * 31 + this.targetType.hashCode() * 11);
        }

        @Override
        public String toString() {
            return (this.sourceType.getName() + " -> " + this.targetType.getName());
        }
    }

}
