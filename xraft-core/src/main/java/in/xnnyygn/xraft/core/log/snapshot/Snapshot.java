package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Set;

// TODO add doc
public interface Snapshot { // NOTE: htt, 快照信息

    int getLastIncludedIndex(); // NOTE: htt, 最后include的index

    int getLastIncludedTerm(); // NOTE: htt, 最后include的term

    @Nonnull
    Set<NodeEndpoint> getLastConfig(); // NOTE: htt, 快照时刻的节点集合

    long getDataSize();

    @Nonnull
    SnapshotChunk readData(int offset, int length); // NOTE: htt, 每次读取的快照块

    @Nonnull
    InputStream getDataStream();

    void close();

}
