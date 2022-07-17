package in.xnnyygn.xraft.kvstore.client;

import in.xnnyygn.xraft.core.service.NoAvailableServerException;

public class RaftRemoveNodeCommand implements Command { // NOTE: htt, 节点删除命令

    @Override
    public String getName() {
        return "raft-remove-node";
    } // NOTE: htt, 节点删除命令

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage " + getName() + " <node-id>");
        }

        try {
            context.getClient().removeNode(arguments);
        } catch (NoAvailableServerException e) {
            System.err.println(e.getMessage());
        }
    }

}
