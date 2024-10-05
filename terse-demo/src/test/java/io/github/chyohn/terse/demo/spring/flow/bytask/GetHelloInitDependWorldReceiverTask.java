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

import java.util.List;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.demo.spring.NoRely;
import io.github.chyohn.terse.flow.ITaskSingle;
import io.github.chyohn.terse.command.ICommandX;
import org.junit.jupiter.api.Assertions;
import io.github.chyohn.terse.flow.ITask;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Component
public class GetHelloInitDependWorldReceiverTask implements ITaskSingle<FlowContext, String>, InitializingBean, NoRely<FlowContext> {

    @Override
    public void afterPropertiesSet() throws Exception {
        String value = Terse.commandInvoker().run(new WordReceiverInitDependHelloReceiverHandler.WordRequest(getText()));
        Assertions.assertEquals(getText(), value);
        System.out.println("get command response: "+ value + ", tid: " + Thread.currentThread().getName());
    }

    public String getText() {
        return "hello haha";
    }

    @Override
    public ICommandX<String> getCommand(FlowContext context) {
        return new HelloReceiverDependHellTaskHandler.HelloRequest(getText());
    }

    @Override
    public void handleResult(FlowContext context, String value) {
        Assertions.assertEquals(getText(), value);
        context.setHello(value);
        System.out.println("set hello, tid: " + Thread.currentThread().getName());
    }

//    @Override
//    public ITaskHandler createTaskHandler(FlowContext context) {
//        String text = getText();
//        return TaskHandlerFactory.newTaskHandler(() ->{
//            List<ICommandX<String>> commands = new ArrayList<>();
//            commands.add(new HelloReceiverDependHellTaskHandler.HelloRequest(text));
//            return commands;
//        }, (response, finished) -> {
//            String value = response.getValue();
//            Assertions.assertEquals(text, value);
//            context.setHello(value);
//            System.out.println("set hello, tid: " + Thread.currentThread().getName());
//        });
//    }

    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return null;
    }

}
