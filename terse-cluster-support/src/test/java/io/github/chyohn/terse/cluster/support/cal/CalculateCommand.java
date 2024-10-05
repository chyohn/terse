package io.github.chyohn.terse.cluster.support.cal;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.command.ICommandX;
import lombok.Getter;

@Getter
public class CalculateCommand implements ICommandX<Integer> {
    static {
        // 注册命令接收者
        Terse.registerReceiverFactory(new CalculateReceiverFactory());
    }

    private int id;
    private final Op op;
    private final int x;
    private final int y;
    public CalculateCommand(int x, Op op, int y) {
        this.x = x;
        this.op = op;
        this.y = y;
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
