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

import io.github.chyohn.terse.command.IReceiverFactory;
import io.github.chyohn.terse.command.IReceiverFactoryLoader;
import io.github.chyohn.terse.spi.ISpiFactory;

import java.util.List;

/**
 * Terse's bean factory for spring.
 * <ol>
 * <li>if use spring xml, should configure in xml as below:
 * <pre>
 * &lt;bean id="receiverFactorySpringLoader" factory-method="getReceiverFactorySpringLoader" class="io.github.chyohn.terse.spring.TerseSpringFactory" /&gt;
 * </pre>
 * </li>
 *
 * <li>if use spring @Configuration annotation, should configure bean in class as below:
 * <pre>
 * &#64;Bean
 * public ReceiverFactorySpringLoader receiverFactorySpringLoader() {
 *     return TerseSpringFactory.getReceiverFactorySpringLoader();
 * }
 * </pre>
 * </li>
 *</ol>
 *
 * @author qiang.shao
 * @since 1.0.0
 */
public class TerseSpringFactory {

    /**
     * Get a receiver factory loader that can load {@link IReceiverFactory} in Spring Container.
     *
     * @return receiver factory loader
     */
    public static ReceiverFactorySpringLoader getReceiverFactorySpringLoader() {
        List<IReceiverFactoryLoader> loaders = ISpiFactory.getAll(IReceiverFactoryLoader.class);
        for (IReceiverFactoryLoader loader : loaders) {
            if (loader instanceof ReceiverFactorySpringLoader) {
                return (ReceiverFactorySpringLoader) loader;
            }
        }
        throw new RuntimeException(
                String.format("bean[%s] not found", ReceiverFactorySpringLoader.class.getName()));
    }

}
