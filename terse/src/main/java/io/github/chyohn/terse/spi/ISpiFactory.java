/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chyohn.terse.spi;

import io.github.chyohn.terse.anotations.Internal;

import java.util.List;

/**
 * a simple Ioc
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@Internal
public interface ISpiFactory {


    static <T> T get(Class<T> clazz) {
        return get(clazz, true);
    }

    static <T> T get(Class<T> clazz, boolean required) {
        T service = DefaultSpiFactory.INSTANCE.getService(clazz);
        if (service == null && required) {
            throw new RuntimeException("service is required, but not found: " + clazz.getName());
        }
        return service;
    }

    static <T> List<T> getAll(Class<T> clazz) {
        return DefaultSpiFactory.INSTANCE.getServices(clazz);
    }

    <T> T getService(Class<T> clazz);


    <T> List<T> getServices(Class<T> clazz);

}
