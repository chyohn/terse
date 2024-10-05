package io.github.chyohn.terse.example.cal;

import io.github.chyohn.terse.command.IReceiverBuilder;
import io.github.chyohn.terse.command.IReceiverFactory;

import java.util.concurrent.Executors;

public class CalculateReceiverFactory implements IReceiverFactory<CalculateCommand> {
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
}
