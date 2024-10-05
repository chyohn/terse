package io.github.chyohn.terse.cluster.support;

import io.github.chyohn.terse.cluster.Cluster;
import io.github.chyohn.terse.cluster.IClusterClient;
import io.github.chyohn.terse.cluster.broadcast.IBroadcaster;
import io.github.chyohn.terse.cluster.member.Member;
import io.github.chyohn.terse.cluster.remote.client.RequestCallBack;
import io.github.chyohn.terse.cluster.remote.client.RpcClientProxy;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IReceiver;
import io.github.chyohn.terse.command.IReceiverRegistry;
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.function.Callback2;
import io.github.chyohn.terse.spi.ISpiFactory;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClusterSupport implements IClusterClient {

    @Getter
    private final Cluster cluster = Cluster.defaultCluster();
    private final IReceiverRegistry receiverRegistry = ISpiFactory.get(IReceiverRegistry.class);
    public ClusterSupport() {
    }

    public Map<String, Object> getClusterInfo() {
        Map<String, Object> info = new HashMap<>();
        for (Member member : cluster.getMemberManager().allMembers()) {
            info.put(member.getAddress(), member.getState().name());
        }
        return info;
    }

    @Override
    public void onInit(Map<String, Object> config) {
        if (isInit()) {
            return;
        }
        if (config != null && !config.isEmpty()) {
            Map<String, String> resolved = new HashMap<>(config.size());
            config.forEach((k, v) -> {
                resolved.put(k, v.toString());
            });
            cluster.getEnvironment().setProperties(resolved);
        }
        cluster.prepare();
        cluster.registerProcessor(CommandRequest.class, new CommandRequestProcessor());
    }

    @Override
    public boolean isInit() {
        return cluster.isPrepared();
    }

    @Override
    public void onReady() {
        if (isReady()) {
            return;
        }
        cluster.ready();
    }

    @Override
    public boolean isReady() {
        return cluster.isReady();
    }

    private Member randomTarget() {
        if (!isInit()) {
            return null;
        }
        List<Member> members = cluster.getMemberManager().allUpMembers();
        if (members == null || members.isEmpty()) {
            return null;
        }
        Random random = new Random();
        int index = random.nextInt(members.size() * 100) % members.size();
        return members.get(index);
    }

    @Override
    public void request(ICommand command, Callback2<IResult<?>, Throwable> callable) {
        Member member = randomTarget();
        if (member == null || cluster.getMemberManager().isSelf(member)) {
            IReceiver<ICommand> receiver = receiverRegistry.getAsyncReceiver(command);
            receiver.async(command, r -> callable.apply(r, null));
        } else {
            RpcClientProxy clientProxy = cluster.getRpcClientProxy();
            CommandRequest request = new CommandRequest();
            request.setCommand(command);
            clientProxy.request(member, request, new RequestCallBack<CommandResponse>() {
                @Override
                public void onResponse(CommandResponse response) {
                    callable.apply(response.getResult(), null);
                }

                @Override
                public void onException(Throwable e) {
                    callable.apply(null, e);
                }
            });

        }
    }

    @Override
    public IBroadcaster getBroadcaster() {
        return null;
    }
}
