package in.xnnyygn.xraft.kvstore.client;

import in.xnnyygn.xraft.core.node.NodeId;

public class ClientSetLeaderCommand implements Command { // NOTE: htt, 设置路由中 leaderId

    @Override
    public String getName() {
        return "client-set-leader";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage: " + getName() + " <node-id>");
        }

        NodeId nodeId = new NodeId(arguments);
        try {
            context.setClientLeader(nodeId); // NOTE: htt, 设置leaderid
            System.out.println(nodeId);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
        }
    }

}
