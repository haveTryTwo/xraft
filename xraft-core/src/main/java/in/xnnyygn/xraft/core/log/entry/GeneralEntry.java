package in.xnnyygn.xraft.core.log.entry;

public class GeneralEntry extends AbstractEntry { // NOTE: htt, 正常的日志条目内容，用于正常的日志写入

    private final byte[] commandBytes; // NOTE: htt, 日志条目内容，正常的写日志操作，对应上层为 <requestId, key, value> 序列化后的二进制

    public GeneralEntry(int index, int term, byte[] commandBytes) {
        super(KIND_GENERAL, index, term);
        this.commandBytes = commandBytes;
    }

    @Override
    public byte[] getCommandBytes() {
        return this.commandBytes;
    }

    @Override
    public String toString() {
        return "GeneralEntry{" +
                "index=" + index +
                ", term=" + term +
                '}';
    }

}
