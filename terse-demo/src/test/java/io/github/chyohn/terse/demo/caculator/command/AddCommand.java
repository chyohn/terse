package io.github.chyohn.terse.demo.caculator.command;

// 加法命令
public class AddCommand extends CalculateCommand {
    public AddCommand(int x, int y) {
        super(x, y);
    }
}
