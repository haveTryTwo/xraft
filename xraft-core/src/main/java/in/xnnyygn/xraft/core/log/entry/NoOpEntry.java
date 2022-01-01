package in.xnnyygn.xraft.core.log.entry;

public class NoOpEntry extends AbstractEntry { // NOTE: htt, 空操作，用于candidate选为master后会首先发送一条消息

    public NoOpEntry(int index, int term) {
        super(KIND_NO_OP, index, term);
    }

    @Override
    public byte[] getCommandBytes() {
        return new byte[0];
    }

    @Override
    public String toString() {
        return "NoOpEntry{" +
                "index=" + index +
                ", term=" + term +
                '}';
    }

}
