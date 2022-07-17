package in.xnnyygn.xraft.kvstore.client;

public class KVStoreSetCommand implements Command { // NOTE: htt, set命令

    @Override
    public String getName() {
        return "kvstore-set";
    } // NOTE: htt, set命令

    @Override
    public void execute(String arguments, CommandContext context) {
        int index = arguments.indexOf(' '); // NOTE: htt, set命令以 空格分开
        if (index <= 0 || index == arguments.length() - 1) {
            throw new IllegalArgumentException("usage: " + getName() + " <key> <value>");
        }
        context.getClient().set(arguments.substring(0, index), arguments.substring(index + 1).getBytes()); // NOTE: htt, 发送写命令
    }

}
