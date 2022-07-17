package in.xnnyygn.xraft.kvstore.client;

public class ClientRemoveServerCommand implements Command { // NOTE: htt, 添加xraft节点命令

    @Override
    public String getName() {
        return "client-remove-server";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage: " + getName() + " <node-id>");
        }

        if (context.clientRemoveServer(arguments)) { // NOTE: htt, 删除节点并重新构建client
            context.printSeverList();
        } else {
            System.err.println("no such server [" + arguments + "]");
        }
    }

}
