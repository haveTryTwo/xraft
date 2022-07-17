package in.xnnyygn.xraft.kvstore.client;

public class ClientListServerCommand implements Command { // NOTE: htt, 打印xraft的列表

    @Override
    public String getName() {
        return "client-list-server";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        context.printSeverList();
    }

}
