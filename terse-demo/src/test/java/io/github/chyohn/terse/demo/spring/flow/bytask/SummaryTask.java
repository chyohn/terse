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

import java.util.ArrayList;
import java.util.List;

import io.github.chyohn.terse.demo.spring.NoRely;
import org.junit.jupiter.api.Assertions;
import io.github.chyohn.terse.flow.ISummaryTask;
import io.github.chyohn.terse.flow.ITask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Component
public class SummaryTask implements ISummaryTask<FlowContext>, NoRely<FlowContext> {

    @Autowired
    GetHelloInitDependWorldReceiverTask getHelloInitDependWorldReceiverTask;
    @Autowired
    GetWorldTask getWorldTask;
    @Autowired
    GetAllSelfCycleDependentTask getAllSelfCycleDependentTask;

    @Override
    public void summary(FlowContext context) {
        Assertions.assertEquals(context.getHello(), getHelloInitDependWorldReceiverTask.getText());
        Assertions.assertEquals(context.getWorld(), getWorldTask.getText());
        Assertions.assertEquals(context.getAll(), getAllSelfCycleDependentTask.getText());
        System.out.println(context.getHello() + " " + context.getWorld()  + ", tid: " + Thread.currentThread().getName());
    }

    @Override
    public List<ITask<FlowContext>> mustRelyOnTasks() {
        List<ITask<FlowContext>> list = new ArrayList<>();
        list.add(getHelloInitDependWorldReceiverTask);
        list.add(getWorldTask);
        list.add(getAllSelfCycleDependentTask);
        return list;
    }
}
