package io.github.chyohn.terse.demo.caculator.command;

import io.github.chyohn.terse.command.IReceiverBuilder;
import io.github.chyohn.terse.command.IReceiverFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 计算命令接收器
 */
public class CalculateReceiverFactory implements IReceiverFactory<CalculateCommand> {
    // 默认命令线程池
    final static Executor DEFAULT_EXECUTOR = Executors.newFixedThreadPool(2);
    // 加法命令线程池
    final static Executor ADD_EXECUTOR = Executors.newFixedThreadPool(2);
    // 乘法命令线程池
    final static Executor MUL_EXECUTOR = Executors.newFixedThreadPool(2);
    // 除法命令线程池
    final static Executor DIV_EXECUTOR = Executors.newFixedThreadPool(2);

    @Override
    public void buildReceiver(IReceiverBuilder<CalculateCommand> builder) {
        builder
                .defaultExecutor(DEFAULT_EXECUTOR)
                .executor(AddCommand.class, command -> { // 为加法命令指定线程池
                    return ADD_EXECUTOR;
                })
                .onReceive(AddCommand.class, command -> { // 注册加法命令处理器
                    return add(command);
                })
                .onReceive(SubCommand.class, command -> { // 注册减法命令处理器
                    return command.getX() - command.getY();
                })
                .onReceive(MulCommand.class, command -> { // 注册乘法命令处理器
                    return mul(command);
                }, command -> { // 为乘法命令指定线程池
                    return MUL_EXECUTOR;
                })
                .onReceive(DivCommand.class, (command, callback) -> { // 注册除法命令处理器，直接使用除法的异步方法
                    asyncDivCommand(command, result -> {
                        callback.accept(result);
                    });
                });
    }

    Integer add(AddCommand command) {
        return command.getX() + command.getY();
    }

    Integer mul(MulCommand command) {
        return command.getX() * command.getY();
    }

    // 异步方法处理除法命令
    void asyncDivCommand(DivCommand command, Consumer<Object> callback) {
        CompletableFuture.runAsync(() -> {
            callback.accept(command.getX() / command.getY());
        }, DIV_EXECUTOR);
    }
}
