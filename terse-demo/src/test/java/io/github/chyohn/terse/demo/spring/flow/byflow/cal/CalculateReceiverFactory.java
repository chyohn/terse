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

package io.github.chyohn.terse.demo.spring.flow.byflow.cal;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.stream.TerseFlow;
import io.github.chyohn.terse.demo.spring.flow.byflow.SpringCalculatorTest;
import io.github.chyohn.terse.command.IReceiverBuilder;
import io.github.chyohn.terse.command.IReceiverFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class CalculateReceiverFactory implements IReceiverFactory<CalculateCommand>, InitializingBean {
    TerseFlow<SpringCalculatorTest.FlowContext> flow = Terse.<SpringCalculatorTest.FlowContext>flow()
            // h1
            .runCommand(context -> new CalculateCommand(context.getA(), Op.ADD, context.getB()), (context, v) -> context.setH1(v))
            // build flow
            .build(context -> {
                // summary result
            });

    @Override
    public void buildReceiver(IReceiverBuilder<CalculateCommand> builder) {
        builder.onReceive(CalculateCommand.class, command -> {
            Thread.sleep(200);
            switch (command.getOp()) {
                case ADD:
                    return command.getX() + command.getY();
                case SUBTRACT:
                    return command.getX() - command.getY();
                case MULTIPLY:
                    return command.getX() * command.getY();
                case DIVIDE:
                    return command.getX() / command.getY();
                default:
                    throw new IllegalArgumentException("不支持的操作");
            }
        })
        // 这里设置线程池执行来执行上面命令
        .defaultExecutor(Executors.newFixedThreadPool(4));
    }

//    @Autowired
//    IBatchExecutor batchExecutor;
    @Override
    public void afterPropertiesSet() throws Exception {
        SpringCalculatorTest.FlowContext context = new SpringCalculatorTest.FlowContext();
        context.setA(1);
        context.setB(2);
        flow.execute(context);
        System.out.println("bean load: " + context.getH1());
    }
}
