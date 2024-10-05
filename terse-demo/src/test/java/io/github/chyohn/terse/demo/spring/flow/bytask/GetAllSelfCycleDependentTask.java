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
import io.github.chyohn.terse.flow.factory.TaskHandlerFactory;
import io.github.chyohn.terse.command.ICommandX;
import io.github.chyohn.terse.command.IReceiverBuilder;
import io.github.chyohn.terse.command.IReceiverFactory;
import org.junit.jupiter.api.Assertions;
import io.github.chyohn.terse.demo.spring.flow.bytask.GetAllSelfCycleDependentTask.GetAllRequest;
import io.github.chyohn.terse.flow.ITask;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 自依赖，执行初始化方法时依赖一个依赖环中还未初始化的bean
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@Component
public class GetAllSelfCycleDependentTask implements ITaskSingle<FlowContext, String>, InitializingBean
    , IReceiverFactory<GetAllRequest>, NoRely<FlowContext> {

    @Override
    public void buildReceiver(IReceiverBuilder<GetAllRequest> builder) {
        builder.onReceive(GetAllRequest.class, (request) -> {
            System.out.println("receive getAll command tid: " + Thread.currentThread().getName());
            return request.text;
        });
    }


    public static class GetAllRequest implements ICommandX<String> {

        private int id;

        final String text;

        public GetAllRequest(String text) {
            this.text = text;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public void setId(int id) {
            this.id = id;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String value = Terse.commandInvoker().run(new GetAllRequest(getText()));
        Assertions.assertEquals(getText(), value);
        System.out.println("get command response: " + value);
    }

    public String getText() {
        return "Hello world, Mr. Right Haha";
    }

    @Override
    public ICommandX<String> getCommand(FlowContext context) {
        return new HelloReceiverDependHellTaskHandler.HelloRequest(getText());
    }

    @Override
    public void handleResult(FlowContext context, String value) {
        Assertions.assertEquals(value, getText());
        context.setAll(value);
        System.out.println("set all, tid: " + Thread.currentThread().getName());

        System.out.printf("任务A在线程[%s]发起请求 \n", Thread.currentThread().getName());
        TaskHandlerFactory.newTaskHandler("", () -> {
            System.out.printf("任务A在线程[%s]执行请求 \n", Thread.currentThread().getName());
            return "任务A";
        }, v -> {
            System.out.printf("任务A在线程[%s]收到结果: %s \n", Thread.currentThread().getName(), v);
        });
    }

//    @Override
//    public ITaskHandler createTaskHandler(FlowContext context) {
//        String text = getText();
//        return TaskHandlerFactory.newTaskHandler(() -> {
//            List<ICommandX<String>> commands = new ArrayList<>();
//            commands.add(new HelloReceiverDependHellTaskHandler.HelloRequest(text));
//            return commands;
//        }, (response, finished) -> {
//            String value = response.getValue();
//            Assertions.assertEquals(value, text);
//            context.setAll(value);
//            System.out.println("set all, tid: " + Thread.currentThread().getName());
//        });
//    }

    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        return null;
    }

}
