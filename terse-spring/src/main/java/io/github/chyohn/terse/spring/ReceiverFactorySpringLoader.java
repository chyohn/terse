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

package io.github.chyohn.terse.spring;

import java.util.ArrayList;
import java.util.List;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.IClusterClient;
import io.github.chyohn.terse.cluster.config.DefaultEnvironment;
import io.github.chyohn.terse.cluster.support.ClusterSupport;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IReceiverFactory;
import io.github.chyohn.terse.command.IReceiverFactoryLoader;
import io.github.chyohn.terse.spi.ISpiFactory;
import io.github.chyohn.terse.utils.ObjectUtils;
import io.github.chyohn.terse.utils.ResolvableType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * 加载受spring容器管理的IReceiverFactory
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public class ReceiverFactorySpringLoader implements IReceiverFactoryLoader, ApplicationContextAware, BeanPostProcessor {

    private ApplicationContext applicationContext;

    @Override
    public  List<IReceiverFactory<ICommand>> load(ICommand command) {

        String[] beanNames = applicationContext.getBeanNamesForType(IReceiverFactory.class,
            true, false);
        if (ObjectUtils.isEmpty(beanNames)) {
            return null;
        }

        List<IReceiverFactory<ICommand>> factories = new ArrayList<>();
        for (String beanName : beanNames) {
            Class<?> type = applicationContext.getType(beanName, false);
            if (ResolvableType.hasGeneric(type, command.getClass())) {
                IReceiverFactory<ICommand> factory = (IReceiverFactory<ICommand>) applicationContext.getBean(type);
                factories.add(factory);
            }
        }

        return factories;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

        initCluster();
    }

    private static final String CLUSTER_CLIENT = "io.github.chyohn.terse.cluster.support.ClusterSupport";

    private void initCluster() {
        if (!ClassUtils.isPresent(CLUSTER_CLIENT, ClassUtils.getDefaultClassLoader())) {
            return;
        }

        ClusterSupport clusterClient = (ClusterSupport)ISpiFactory.get(IClusterClient.class);
        Cluster cluster = clusterClient.getCluster();
        cluster.setEnvironment(new ClusterWithSpringEnvironment(applicationContext.getEnvironment()));
        Terse.initCluster(null);
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent event) {
        Terse.readyCluster();
    }

    private static class ClusterWithSpringEnvironment extends DefaultEnvironment {

        private final Environment springEnv;

        private ClusterWithSpringEnvironment(Environment springEnv) {
            this.springEnv = springEnv;
        }

        @Override
        public String getProperty(String key, String defaultVal) {

            String val = config.get(key);
            if (val != null) {
                return val;
            }

            return springEnv.getProperty(key, defaultVal);
        }
    }
}
