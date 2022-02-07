package in.xnnyygn.xraft.core.log.snapshot;

public class SnapshotChunk { // NOTE: htt, 每次读取的快照块

    private final byte[] bytes; // NOTE: htt, 快照块
    private final boolean lastChunk; // NOTE: htt, 是否为最后读取的块

    SnapshotChunk(byte[] bytes, boolean lastChunk) {
        this.bytes = bytes;
        this.lastChunk = lastChunk;
    }

    public boolean isLastChunk() {
        return lastChunk;
    }

    public byte[] toByteArray() {
        return bytes;
    }

}
