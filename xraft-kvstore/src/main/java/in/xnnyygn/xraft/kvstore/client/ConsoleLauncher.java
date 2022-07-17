package in.xnnyygn.xraft.kvstore.client;

import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.Address;
import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;

public class ConsoleLauncher { // NOTE: htt, 客户端终端launcher启动

    private static class ServerConfig { // NOTE: htt, 服务端配置

        private final String nodeId;
        private final String host;
        private final int port;

        ServerConfig(String nodeId, String host, int port) {
            this.nodeId = nodeId;
            this.host = host;
            this.port = port;
        }

        String getNodeId() {
            return nodeId;
        }

        String getHost() {
            return host;
        }

        int getPort() {
            return port;
        }

    }

    private void execute(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("gc")
                .hasArgs()
                .argName("server-config")
                .required()
                .desc("group config, required. format: <server-config> <server-config>. " +
                        "format of server config: <node-id>,<host>,<port-service>. e.g A,localhost,8001 B,localhost,8011")
                .build());
        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("xraft-kvstore-client [OPTION]...", options);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        Map<NodeId, Address> serverMap;
        try {
            CommandLine commandLine = parser.parse(options, args);
            serverMap = parseGroupConfig(commandLine.getOptionValues("gc"));
        } catch (ParseException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        Console console = new Console(serverMap); // NOTE: htt, 构建终端
        console.start(); // NOTE: htt 启动终端
    }

    private Map<NodeId, Address> parseGroupConfig(String[] rawGroupConfig) { // NOTE: htt, 解析服务列表
        Map<NodeId, Address> serverMap = new HashMap<>();
        for (String rawServerConfig : rawGroupConfig) {
            ServerConfig serverConfig = parseServerConfig(rawServerConfig);
            serverMap.put(new NodeId(serverConfig.getNodeId()), new Address(serverConfig.getHost(), serverConfig.getPort()));
        }
        return serverMap;
    }

    private ServerConfig parseServerConfig(String rawServerConfig) { // NOTE: htt, 获取 <nodeId, host, port>
        String[] pieces = rawServerConfig.split(",");
        if (pieces.length != 3) {
            throw new IllegalArgumentException("illegal server config [" + rawServerConfig + "]");
        }
        String nodeId = pieces[0];
        String host = pieces[1];
        int port;
        try {
            port = Integer.parseInt(pieces[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("illegal port [" + pieces[2] + "]");
        }
        return new ServerConfig(nodeId, host, port);
    }


    public static void main(String[] args) throws Exception {
        ConsoleLauncher launcher = new ConsoleLauncher();
        launcher.execute(args);
    }

}
