package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

@Immutable
public class MemorySnapshot implements Snapshot { // NOTE: htt, 内存快照，其中数据在内存中，并包括包含进来的最新的 index 和 term

    private final int lastIncludedIndex; // NOTE: htt, 最后include的 index
    private final int lastIncludedTerm; // NOTE: htt, 最后include 的 term
    private final byte[] data; // NOTE: htt, 快照数据
    private final Set<NodeEndpoint> lastConfig; // NOTE: htt, 快照节点

    public MemorySnapshot(int lastIncludedIndex, int lastIncludedTerm) {
        this(lastIncludedIndex, lastIncludedTerm, new byte[0], Collections.emptySet());
    }

    public MemorySnapshot(int lastIncludedIndex, int lastIncludedTerm, byte[] data, Set<NodeEndpoint> lastConfig) {
        this.lastIncludedIndex = lastIncludedIndex;
        this.lastIncludedTerm = lastIncludedTerm;
        this.data = data;
        this.lastConfig = lastConfig;
    }

    @Override
    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    @Override
    public int getLastIncludedTerm() {
        return lastIncludedTerm;
    }

    @Nonnull
    @Override
    public Set<NodeEndpoint> getLastConfig() {
        return lastConfig;
    }

    @Override
    public long getDataSize() {
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    @Nonnull
    public SnapshotChunk readData(int offset, int length) { // NOTE: htt, 读数据到快照chunk中，注意 offset 为快照数据的偏移
        if (offset < 0 || offset > data.length) {
            throw new IndexOutOfBoundsException("offset " + offset + " out of bound");
        }

        int bufferLength = Math.min(data.length - offset, length);
        byte[] buffer = new byte[bufferLength];
        System.arraycopy(data, offset, buffer, 0, bufferLength);
        return new SnapshotChunk(buffer, offset + length >= this.data.length); // TODO: empe, 这里length应换成bufferLength，因为这个是实际的值
    }

    @Override
    @Nonnull
    public InputStream getDataStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "MemorySnapshot{" +
                "lastIncludedIndex=" + lastIncludedIndex +
                ", lastIncludedTerm=" + lastIncludedTerm +
                ", data.size=" + data.length +
                '}';
    }

}
