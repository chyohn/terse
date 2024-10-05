package io.github.chyohn.terse.cluster.support;

import io.github.chyohn.terse.cluster.service.ServiceProcessor;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IReceiver;
import io.github.chyohn.terse.command.IReceiverRegistry;
import io.github.chyohn.terse.spi.ISpiFactory;

import java.util.concurrent.CompletableFuture;

public class CommandRequestProcessor implements ServiceProcessor<CommandRequest, CompletableFuture<CommandResponse>> {

    private final IReceiverRegistry receiverRegistry = ISpiFactory.get(IReceiverRegistry.class);
    @Override
    public CompletableFuture<CommandResponse> process(CommandRequest request) {
        CompletableFuture<CommandResponse> future = new CompletableFuture<>();

        ICommand command = request.getCommand();
        IReceiver<ICommand> receiver = receiverRegistry.getAsyncReceiver(command);
        receiver.async(command, r -> {
            CommandResponse response = new CommandResponse();
            response.setResult(r);
            future.complete(response);
        });
        return future;
    }
}
