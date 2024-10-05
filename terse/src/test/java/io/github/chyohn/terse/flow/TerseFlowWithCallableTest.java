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

package io.github.chyohn.terse.flow;

import io.github.chyohn.terse.flow.IFlowContext;
import lombok.Getter;
import lombok.Setter;
import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.stream.TerseFlowBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
public class TerseFlowWithCallableTest {


    @Test
    public void testFlow() throws InterruptedException {

        // 构造参数
        FlowContext flowContext = new FlowContext();
        flowContext.setA(1);
        flowContext.setB(2);
        flowContext.setC(3);
        flowContext.setD(4);
        flowContext.setE(5);
        flowContext.setF(6);
        int expect = (flowContext.getA() + flowContext.getB())*(flowContext.getC() + flowContext.getD())/(flowContext.getE()-flowContext.getF());

        TerseFlowBuilder<FlowContext> h4TaskBuilder = Terse.<FlowContext>flow()
                .callable("", context -> context.getA() + context.getB(), (context, v) -> context.setH1(v))
                .callable("", context -> context.getC() + context.getD(), (context, v) -> context.setH2(v))
                .then() // next task
                .callable("", context -> context.getH1() * context.getH2(), (context, v) -> context.setH4(v));

        Terse.<FlowContext>flow()
                .callable("", context -> context.getE() - context.getF(), (context, v) -> context.setH3(v))
                .then().mustRelyOn(h4TaskBuilder) // merge task
                .callable("", (context) -> context.getH4() / context.getH3(), (context, v) -> context.setH5(v))
                .build(context -> { // 汇总结果
                    System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s", context.getA(), context.getB(),
                            context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
                }).execute(flowContext);
        Assertions.assertEquals(expect, flowContext.getH5());

        Terse.<FlowContext>flow()
                // h1
                .callable("", context -> context.getA() + context.getB(), (context, v) -> context.setH1(v))
                // h2
                .callable("", context -> context.getC() + context.getD(), (context, v) -> context.setH2(v))
                .then() // then go h4 task
                // h4
                .callable("", context -> context.getH1() * context.getH2(), (context, v) -> context.setH4(v))
                .then() // then go h5 task
                // rely on h3 task
                .mustRelyOn(Terse.<FlowContext>flow().callable("", context -> context.getE() - context.getF(), (context, v) -> context.setH3(v)))
                // h5
                .callable("", (context) -> context.getH4() / context.getH3(), (context, v) -> context.setH5(v))
                // build flow
                .build(context -> {
                    // summary result
                    System.out.printf("(%s+%s)*(%s+%s)/(%s-%s) = %s", context.getA(), context.getB(),
                            context.getC(), context.getD(), context.getE(), context.getF(), context.getH5());
                })
                // start execute flow
                .execute(flowContext);
        Assertions.assertEquals(expect, flowContext.getH5());

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
