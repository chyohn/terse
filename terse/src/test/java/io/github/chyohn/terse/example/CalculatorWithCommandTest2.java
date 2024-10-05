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

package io.github.chyohn.terse.example;

import io.github.chyohn.terse.example.cal.CalculateCommand;
import io.github.chyohn.terse.example.cal.Op;
import lombok.Getter;
import lombok.Setter;
import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.stream.TerseFlow;
import io.github.chyohn.terse.stream.TerseFlowBuilder;
import io.github.chyohn.terse.flow.IFlowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.function.Function;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
public class CalculatorWithCommandTest2 {


    // 定义流程
    TerseFlow<FlowContext> flow = Terse.<FlowContext>flow()
            // h1
            .runCommand(context -> new CalculateCommand(context.getA(), Op.ADD, context.getB()), (context, v) -> context.setH1(v))
            // h2
            .runCommand(context -> new CalculateCommand(context.getC(), Op.ADD, context.getD()), (context, v) -> context.setH2(v))
            .then() // then go h4 task
            // h4
            .runCommand(context -> new CalculateCommand(context.getH1(), Op.MULTIPLY, context.getH2()), (context, v) -> context.setH4(v))
            .then() // then go h5 task
            // rely on h3 task
            .mustRelyOn(Terse.<FlowContext>flow().runCommand(context -> new CalculateCommand(context.getE(), Op.SUBTRACT, context.getF()), (context, v) -> context.setH3(v)))
            // h5
            .runCommand((context) -> new CalculateCommand(context.getH4(), Op.DIVIDE, context.getH3()), (context, v) -> context.setH5(v))
            // build flow
            .build(context -> {
                // summary result
//                System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s \n", context.getA(), context.getB(),
//                        context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
            });


    TerseFlowBuilder<FlowContext> h2 = Terse.<FlowContext>flow()
            // h2
            .runCommand(context -> new CalculateCommand(context.getC(), Op.ADD, context.getD()), (context, v) -> context.setH2(v));

    TerseFlowBuilder<FlowContext> h4 = Terse.<FlowContext>flow()
            // h1
            .runCommand(context -> new CalculateCommand(context.getA(), Op.ADD, context.getB()), (context, v) -> context.setH1(v))
            // 合并h2
            .merge(h2)
            .then() // then go h4 task
            // h4
            .runCommand(context -> new CalculateCommand(context.getH1(), Op.MULTIPLY, context.getH2()), (context, v) -> context.setH4(v));
    TerseFlowBuilder<FlowContext> h5 = Terse.<FlowContext>flow()
            // h3
            .runCommand(context -> new CalculateCommand(context.getE(), Op.SUBTRACT, context.getF()), (context, v) -> context.setH3(v))
            // 合并h2
            .merge(h2)
            .then()
            // h5
            .runCommand((context) -> new CalculateCommand(context.getH2(), Op.DIVIDE, context.getH3()), (context, v) -> context.setH5(v));

    TerseFlow<FlowContext> flow3 = Terse.<FlowContext>flow()
            .merge(h4)
            .merge(h5)
            .then()
            .runCommand(context -> new CalculateCommand(context.getH4(), Op.ADD, context.getH5()), (context, v) -> context.setH5(v))
            // build flow
            .build(context -> {
                // summary result
                System.out.printf("(%s+%s)*(%s+%s)+(%s+%s)/(%s-%s) = %s \n", context.getA(), context.getB(),
                        context.getC(), context.getD(),
                        context.getC(), context.getD(), context.getE(), context.getF(),
                        context.getH5());
            });


    TerseFlow<FlowContext> serialFlow = Terse.<FlowContext>flow()
            // h1
            .runCommand(context -> new CalculateCommand(context.getA(), Op.ADD, context.getB()), (context, v) -> context.setH1(v))
            .then()
            // h2
            .runCommand(context -> new CalculateCommand(context.getC(), Op.ADD, context.getD()), (context, v) -> context.setH2(v))
            .then() // then go h4 task
            // h4
            .runCommand(context -> new CalculateCommand(context.getH1(), Op.MULTIPLY, context.getH2()), (context, v) -> context.setH4(v))
            .then() // then go h5 task
            // rely on h3 task
            .runCommand(context -> new CalculateCommand(context.getE(), Op.SUBTRACT, context.getF()), (context, v) -> context.setH3(v))
            .then()
            // h5
            .runCommand((context) -> new CalculateCommand(context.getH4(), Op.DIVIDE, context.getH3()), (context, v) -> context.setH5(v))
            // build flow
            .build(context -> {
                // summary result
//                System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s \n", context.getA(), context.getB(),
//                        context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
            });



    @Test
    public void testFlow1() throws InterruptedException {

        Function<FlowContext, Integer> cal = context -> {
            return (context.getA() + context.getB()) * (context.getC() + context.getD()) / (context.getE() - context.getF());
        };

        test(1, cal, context -> {
            flow.execute(context);
            return context.getH5();
        });
        test(1, cal, context -> {
            serialFlow.execute(context);
            return context.getH5();
        });

        Function<FlowContext, Integer> calFlow3 = context -> {
            return (context.getA() + context.getB()) * (context.getC() + context.getD())
                    + (context.getC() + context.getD()) / (context.getE() - context.getF());
        };
        test(1, calFlow3, context -> {
            flow3.execute(context);
            return context.getH5();
        });
    }

    @Test
    public void testBenchmarking() throws InterruptedException {


        int count = 50;
        Function<FlowContext, Integer> cal = context -> {
            return (context.getA() + context.getB()) * (context.getC() + context.getD()) / (context.getE() - context.getF());
        };

        long p = test(count, cal, context -> {
            flow.execute(context);
            return context.getH5();
        });
        long s = test(count, cal, context -> {
            serialFlow.execute(context);
            return context.getH5();
        });
        long p3 = test(count, cal, context -> {
            flow.serialExecute(context);
            return context.getH5();
        });
        System.out.println(String.format("parallel: %s, serial: %s, p3: %s", p, s, p3));


    }

    private long test(int count, Function<FlowContext, Integer> cal, Function<FlowContext, Integer> function) {
        Random random = new Random();
        FlowContext context = new FlowContext();
        long total = 0;
        for (int i = 0; i < count; i++) {
            context.setA(random.nextInt());
            context.setA(random.nextInt());
            context.setB(random.nextInt());
            context.setC(random.nextInt());
            context.setD(random.nextInt());
            context.setE(random.nextInt());
            context.setF(random.nextInt());
            if (context.getE() - context.getF() == 0) {
                continue;
            }
            int expect = cal.apply(context);
            long start = System.currentTimeMillis();
            // 执行流程并等待结果
            int result = function.apply(context);
            total += System.currentTimeMillis() - start;
//            System.out.printf("执行结果：%s \n", context.getH5());
            Assertions.assertEquals(expect, result);
        }
        return total;
    }


    @Setter
    @Getter
    private static class FlowContext implements IFlowContext {

        private int a;
        private int b;
        private int c;
        private int d;
        private int e;
        private int f;

        private int h1;
        private int h2;
        private int h3;
        private int h4;
        private int h5;
    }

}