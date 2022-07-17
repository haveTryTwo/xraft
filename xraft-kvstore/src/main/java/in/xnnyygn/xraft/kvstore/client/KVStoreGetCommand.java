package in.xnnyygn.xraft.kvstore.client;

import in.xnnyygn.xraft.core.service.NoAvailableServerException;

public class KVStoreGetCommand implements Command { // NOTE: htt, GET key 命令

    @Override
    public String getName() {
        return "kvstore-get";
    } // NOTE: htt, get命令

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage: " + getName() + " <key>");
        }

        byte[] valueBytes;
        try {
            valueBytes = context.getClient().get(arguments); // NOTE: htt, get arguments对应的value
        } catch (NoAvailableServerException e) {
            System.err.println(e.getMessage());
            return;
        }

        if (valueBytes == null) {
            System.out.println("null");
        } else {
            System.out.println(new String(valueBytes));
        }
    }

}
