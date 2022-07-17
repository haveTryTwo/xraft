package in.xnnyygn.xraft.kvstore.client;

public interface Command { // NOTE: htt, 操作命令

    String getName();

    void execute(String arguments, CommandContext context);

}
