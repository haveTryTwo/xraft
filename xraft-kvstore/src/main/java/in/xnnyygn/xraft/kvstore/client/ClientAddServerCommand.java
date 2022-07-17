package in.xnnyygn.xraft.kvstore.client;

public class ClientAddServerCommand implements Command { // NOTE: htt, 添加节点命令，操作是context中添加节点并构建新的client

    @Override
    public String getName() {
        return "client-add-server";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        // <node-id> <host> <port-service>
        String[] pieces = arguments.split("\\s");
        if (pieces.length != 3) {
            throw new IllegalArgumentException("usage: " + getName() + " <node-id> <host> <port-service>");
        }

        String nodeId = pieces[0];
        String host = pieces[1];
        int port;
        try {
            port = Integer.parseInt(pieces[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("illegal port [" + pieces[2] + "]");
        }

        context.clientAddServer(nodeId, host, port); // NOTE: htt, context添加节点，并生成新的client
        context.printSeverList();
    }

}
