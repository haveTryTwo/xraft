package in.xnnyygn.xraft.kvstore.client;

public class ExitCommand implements Command { // NOTE: htt, 退出命令

    @Override
    public String getName() {
        return "exit";
    } // NOTE: htt, 退出

    @Override
    public void execute(String arguments, CommandContext context) {
        System.out.println("bye");
        context.setRunning(false);
    }

}
