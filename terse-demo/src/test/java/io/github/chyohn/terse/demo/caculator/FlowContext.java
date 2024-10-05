package io.github.chyohn.terse.demo.caculator;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.flow.IFlowContext;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Executors;

@Setter
@Getter
public class FlowContext implements IFlowContext {

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

    @Override
    public long timeout() {
        String MY_POOL = "my_pool";
        Terse.registerExecutor(MY_POOL, Executors.newFixedThreadPool(2));
        return 0;
    }

}
