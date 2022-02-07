package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.log.LogException;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MemorySnapshotBuilder extends AbstractSnapshotBuilder<MemorySnapshot> { // NOTE: htt, 内存快照生成

    private final ByteArrayOutputStream output; // NOTE: htt, 写入到内存数组

    public MemorySnapshotBuilder(InstallSnapshotRpc firstRpc) {
        super(firstRpc);
        output = new ByteArrayOutputStream();

        try {
            output.write(firstRpc.getData()); // NOTE: htt, 写入数据，后续状态改变在主进行
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    @Override
    protected void doWrite(byte[] data) throws IOException {
        output.write(data);
    }

    @Override
    public MemorySnapshot build() { // NOTE: htt, 构建内存快照
        return new MemorySnapshot(lastIncludedIndex, lastIncludedTerm, output.toByteArray(), lastConfig);
    }

    @Override
    public void close() {
    }

}
