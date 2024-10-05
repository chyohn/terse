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

package io.github.chyohn.terse.demo.spring.flow.bytask;

import io.github.chyohn.terse.demo.spring.flow.bytask.HelloReceiverDependHellTaskHandler.HelloRequest;
import io.github.chyohn.terse.command.ICommandX;
import io.github.chyohn.terse.command.IReceiverBuilder;
import io.github.chyohn.terse.command.IReceiverFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Component
public class HelloReceiverDependHellTaskHandler implements IReceiverFactory<HelloRequest>, InitializingBean{

    @Autowired
    GetHelloInitDependWorldReceiverTask getHelloInitDependWorldReceiverTask;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(getHelloInitDependWorldReceiverTask);
    }



    @Override
    public void buildReceiver(IReceiverBuilder<HelloRequest> builder) {
        builder.onReceive(HelloRequest.class, (request) -> {
            System.out.println("receive hello: " + request.word + ", tid: " + Thread.currentThread().getName());
            return request.word;
        });
    }

    public static class HelloRequest implements ICommandX<String> {
        private final String word;

        public HelloRequest(String word) {
            this.word = word;
        }


        private int id;
        @Override
        public int getId() {
            return id;
        }

        @Override
        public void setId(int id) {
            this.id = id;
        }
    }
}
