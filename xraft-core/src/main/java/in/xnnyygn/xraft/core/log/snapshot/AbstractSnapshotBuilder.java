package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.log.LogException;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;

import java.io.IOException;
import java.util.Set;

abstract class AbstractSnapshotBuilder<T extends Snapshot> implements SnapshotBuilder<T> { // NOTE: htt, 抽象快照生成，用于添加 拆分的日志安装请求

    int lastIncludedIndex; // NOTE: htt, 包含的最新的index，对于当前快照的多个安装请求，这里的值是不变的，即最终的快照所包含最新的index
    int lastIncludedTerm; // NOTE: htt, 包含的最新的term，对于当前快照的多个安装请求，这里的值是不变的，即最终的快照所包含最新的term
    Set<NodeEndpoint> lastConfig; // NOTE: htt, 节点列表
    private int offset; // NOTE: htt, 偏移，默认为快照安装请求中数据长度

    AbstractSnapshotBuilder(InstallSnapshotRpc firstRpc) {
        assert firstRpc.getOffset() == 0; // NOTE: 强制要求初始化的offset为0
        lastIncludedIndex = firstRpc.getLastIndex();
        lastIncludedTerm = firstRpc.getLastTerm();
        lastConfig = firstRpc.getLastConfig();
        offset = firstRpc.getDataLength(); // NOTE: htt, 偏移为安装快照请求（主同步）的数据长度，在进行初始化处理，随即数据都会写入，可以认为这里是提前处理
    }

    protected void write(byte[] data) { // NOTE: htt, 写数据
        try {
            doWrite(data);
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    protected abstract void doWrite(byte[] data) throws IOException;

    @Override
    public void append(InstallSnapshotRpc rpc) {
        if (rpc.getOffset() != offset) { // NOTE: htt, 新的安装请求，对应的偏移位置必须为上次偏移位置，即接着上次请求继续处理
            throw new IllegalArgumentException("unexpected offset, expected " + offset + ", but was " + rpc.getOffset());
        }
        // NOTE: htt, 对于一次快照安装处理，lastIncludedIndex和lastIncludedTerm 是不变的，即最终的值，所以对于分块的值这里是一致
        if (rpc.getLastIndex() != lastIncludedIndex || rpc.getLastTerm() != lastIncludedTerm) { // NOTE: htt, 检查index和term
            throw new IllegalArgumentException("unexpected last included index or term");
        }
        write(rpc.getData());
        offset += rpc.getDataLength(); // NOTE: htt, 偏移增加数据长度
    }

}
