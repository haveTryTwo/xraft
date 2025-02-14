package in.xnnyygn.xraft.kvstore.client;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Address;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.*;

public class Console { // NOTE: htt, 终端执行xraft相关命令

    private static final String PROMPT = "kvstore-client " + Client.VERSION + "> ";
    private final Map<String, Command> commandMap; // NOTE: htt, 命令列表，对应为 <commandName, Command>
    private final CommandContext commandContext; // NOTE: htt, Command内容，包括 服务列表，已经构建对应的 channel
    private final LineReader reader; // NOTE: htt, 命令读取

    public Console(Map<NodeId, Address> serverMap) {
        commandMap = buildCommandMap(Arrays.asList(
                new ExitCommand(),
                new ClientAddServerCommand(),
                new ClientRemoveServerCommand(),
                new ClientListServerCommand(),
                new ClientGetLeaderCommand(),
                new ClientSetLeaderCommand(),
                new RaftAddNodeCommand(),
                new RaftRemoveNodeCommand(),
                new KVStoreGetCommand(),
                new KVStoreSetCommand()
        ));
        commandContext = new CommandContext(serverMap);

        ArgumentCompleter completer = new ArgumentCompleter(
                new StringsCompleter(commandMap.keySet()),
                new NullCompleter()
        );
        reader = LineReaderBuilder.builder()
                .completer(completer)
                .build();
    }

    private Map<String, Command> buildCommandMap(Collection<Command> commands) {
        Map<String, Command> commandMap = new HashMap<>();
        for (Command cmd : commands) {
            commandMap.put(cmd.getName(), cmd);
        }
        return commandMap;
    }

    void start() { // NOTE: htt, 启动，循环执行
        commandContext.setRunning(true);
        showInfo(); // NOTE: htt, 显示列表
        String line;
        while (commandContext.isRunning()) {
            try {
                line = reader.readLine(PROMPT);
                if (line.trim().isEmpty())
                    continue;
                dispatchCommand(line);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            } catch (EndOfFileException ignored) {
                break;
            }
        }
    }

    private void showInfo() {
        System.out.println("Welcome to XRaft KVStore Shell\n");
        System.out.println("***********************************************");
        System.out.println("current server list: \n");
        commandContext.printSeverList();
        System.out.println("***********************************************");
    }

    private void dispatchCommand(String line) {
        String[] commandNameAndArguments = line.split("\\s+", 2);
        String commandName = commandNameAndArguments[0];
        Command command = commandMap.get(commandName);
        if (command == null) {
            throw new IllegalArgumentException("no such command [" + commandName + "]");
        }
        command.execute(commandNameAndArguments.length > 1 ? commandNameAndArguments[1] : "", commandContext); // NOTE: htt, 执行命令
    }

}
