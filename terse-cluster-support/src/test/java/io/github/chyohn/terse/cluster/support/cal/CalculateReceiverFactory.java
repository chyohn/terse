package io.github.chyohn.terse.cluster.support.cal;

import io.github.chyohn.terse.command.IReceiverBuilder;
import io.github.chyohn.terse.command.IReceiverFactory;

import java.util.concurrent.Executors;

public class CalculateReceiverFactory implements IReceiverFactory<CalculateCommand> {
    @Override
    public void buildReceiver(IReceiverBuilder<CalculateCommand> builder) {
        builder.onReceive(CalculateCommand.class, command -> {
                    int r = b(command);
                    System.out.printf("calculate: %s %s %s = %s %n", command.getX(), command.getOp().name(), command.getY(), r);
                    return r;
                })
                // 这里设置线程池执行来执行上面命令
                .defaultExecutor(Executors.newFixedThreadPool(4));
    }

    int b(CalculateCommand command) throws Exception {
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
    }
}
