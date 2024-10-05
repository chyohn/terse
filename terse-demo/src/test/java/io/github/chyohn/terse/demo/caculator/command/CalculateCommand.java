package io.github.chyohn.terse.demo.caculator.command;

import io.github.chyohn.terse.command.ICommandX;
import lombok.Getter;

@Getter
public abstract class CalculateCommand implements ICommandX<Integer> {
    // id用于标识批量请求中命令顺序
    private int id;
    private final int x;
    private final int y;

    public CalculateCommand(int x, int y) {
        this.x = x;
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
