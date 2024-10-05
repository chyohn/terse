package io.github.chyohn.terse.flow;

import io.github.chyohn.terse.example.cal.CalculateCommand;
import io.github.chyohn.terse.example.cal.Op;
import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.exception.FlowConditionNotMatchException;
import io.github.chyohn.terse.stream.TerseFlow;
import io.github.chyohn.terse.stream.TerseFlowBuilder;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * (A+B)*(E/(C-D))
 * H1 = A + B
 * H2 = C - D
 * H3 = E/H2 WHEN H2 != 0
 * H4 = H1 * H3 when h2 > 0 or h1 > 0
 */
public class FlowMustAndAnyConditionTest {

    TerseFlowBuilder<Context> h1 = Terse.<Context>flow().runCommand(c -> new CalculateCommand(c.getA(), Op.ADD, c.getB()), Context::setH1);
    TerseFlowBuilder<Context> h2 = Terse.<Context>flow().runCommand(c -> new CalculateCommand(c.getC(), Op.SUBTRACT, c.getD()), Context::setH2);
    TerseFlow<Context> flow = Terse.<Context>flow()
            .mustCondition(Terse.<Context>condition(c -> c.getH2() != 0).mustRelyOn(h2))
            .runCommand(c -> new CalculateCommand(c.getE(), Op.DIVIDE, c.getH2()), Context::setH3)
            .then()
            .mustRelyOn(h1)
            .randomCondition(Terse.<Context>condition(c -> c.getH2() > 0).mustRelyOn(h2))
            .randomCondition(Terse.<Context>condition(c -> c.getH1() > 0).mustRelyOn(h1))
            .runCommand(c -> new CalculateCommand(c.getH1(), Op.MULTIPLY, c.getH3()), Context::setH4)
            .then()
            .build(context -> {
                System.out.println("H1：" + context.getH1());
                System.out.println("H2：" + context.getH2());
                System.out.println("H3：" + context.getH3());
                System.out.println("H4：" + context.getH4());
            });

    @Test
    void testMustAndAnyCondition() {
        // must condition H3 = E/H2 WHEN H2 != 0
        Context c = new Context();
        c.setA(1);
        c.setB(2);
        c.setC(3);
        c.setD(3);
        c.setE(6);
        Assertions.assertThrows(FlowConditionNotMatchException.class, () -> {
            flow.execute(c);
        });

        // H4 = H1 * H3 when h2 > 0 or h1 > 0
        // 两个条件都不满足
        c.setB(-2);
        c.setD(4);
        c.clear();
        Assertions.assertThrows(FlowConditionNotMatchException.class, () -> {
            flow.execute(c);
        });
        // 都满足
        c.setB(2);
        c.setD(2);
        c.clear();
        flow.execute(c);

        // 满足H2>0
        c.setB(-2);
        c.setD(2);
        c.clear();
        flow.execute(c);
        // 满足H1>0
        c.setB(2);
        c.setD(4);
        c.clear();
        flow.execute(c);
    }

}

@Getter
@Setter
class Context implements IFlowContext {
    int a;
    int b;
    int c;
    int d;
    int e;

    Integer h1;
    Integer h2;
    Integer h3;
    Integer h4;
    void clear() {
        h1 = h2 = h3 = h4 = null;
    }
}