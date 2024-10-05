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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import io.github.chyohn.terse.utils.ObjectUtils;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class DefaultSpiFactory implements ISpiFactory {

    private static final String FACTORY_FILE = "META-INF/parallel.factories";

    static final DefaultSpiFactory INSTANCE = new DefaultSpiFactory();

    private final Map<ClassLoader, Map<Class<?>, List<?>>> cache = new HashMap<>();

    private final Map<ClassLoader, Map<Class<?>, Set<String>>> cacheNames = new HashMap<>();
    private final ReentrantLock loadLock = new ReentrantLock();


    @Override
    public <T> T getService(Class<T> clazz) {
        List<T> services = getServices(clazz);
        if (ObjectUtils.isEmpty(services)) {
            return null;
        }
        return services.get(0);
    }

    @Override
    public <T> List<T> getServices(Class<T> clazz) {
        ClassLoader loader = getClassLoader();
        Map<Class<?>, List<?>> beanOfService = cache.get(loader);
        if (beanOfService != null && beanOfService.containsKey(clazz)) {
            return (List<T>) beanOfService.get(clazz);
        }

        SPI spi = clazz.getAnnotation(SPI.class);
        if (spi == null) {
            throw new RuntimeException("service must be annotated by @SPI: " + clazz.getName());
        }

        Set<String> serviceNames = loadServices(clazz.getClassLoader()).get(clazz);
        if (ObjectUtils.isEmpty(serviceNames)) {
            return null;
        }

        if (serviceNames.size() > 1 && !spi.allowMultiInstance()) {
            throw new RuntimeException(String.format("service[%s] only required one implementation, but find %s. %s",
                clazz.getName(), serviceNames.size(), serviceNames));
        }

        try {
            loadLock.lock();
            beanOfService = cache.computeIfAbsent(loader, k -> new HashMap<>());
            if (beanOfService.containsKey(clazz)) {
                return (List<T>) beanOfService.get(clazz);
            }
            List<T> services = new ArrayList<>();
            for (String serviceName : serviceNames) {
                services.add(newService(clazz, serviceName, loader));
            }
            beanOfService.put(clazz, services);
            return services;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            loadLock.unlock();
        }
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = DefaultSpiFactory.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }

    private Map<Class<?>, Set<String>> loadServices(ClassLoader loader) {
        Map<Class<?>, Set<String>> result = cacheNames.get(loader);
        if (result != null) {
            return result;
        }

        try {
            loadLock.lock();
            result = cacheNames.get(loader);
            if (result != null) {
                return result;
            }
            Enumeration<URL> configs = loader.getResources(FACTORY_FILE);
            result = new HashMap<>();
            while (configs.hasMoreElements()) {
                URL url = configs.nextElement();
                Properties properties = new Properties();
                properties.load(url.openStream());
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    Class<?> serviceInterface = Class.forName(entry.getKey().toString().trim(), false, loader);
                    String[] serviceNames = ObjectUtils.split(entry.getValue().toString(), ",");
                    for (String serviceName : serviceNames) {
                        result.computeIfAbsent(serviceInterface, k -> new HashSet<>()).add(serviceName);
                    }
                }
            }
            cacheNames.put(loader, result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            loadLock.unlock();
        }
    }

    private static <T> T newService(Class<T> serviceInterface, String serviceImplName, ClassLoader loader)
        throws Exception {
        Class<?> clazz = Class.forName(serviceImplName, false, loader);
        if (!serviceInterface.isAssignableFrom(clazz)) {
            throw new ClassCastException(serviceImplName + " cannot be cast to " + serviceInterface.getName());
        }

        return (T) makeAccessibleConstructor(clazz).newInstance();
    }

    private static <T> Constructor<T> makeAccessibleConstructor(Class<T> clazz) throws NoSuchMethodException {
        Constructor<T> ctor = clazz.getDeclaredConstructor();
        if ((!Modifier.isPublic(ctor.getModifiers()) ||
            !Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) && !ctor.isAccessible()) {
            ctor.setAccessible(true);
        }
        return ctor;
    }

}
