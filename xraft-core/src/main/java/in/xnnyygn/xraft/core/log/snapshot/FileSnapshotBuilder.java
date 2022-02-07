package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.log.LogDir;
import in.xnnyygn.xraft.core.log.LogException;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;

import java.io.IOException;

public class FileSnapshotBuilder extends AbstractSnapshotBuilder<FileSnapshot> { // NOTE: htt, 构建快照文件build，用于写入快照头部以及快照数据

    private final LogDir logDir; // NOTE: htt, 数据目录
    private FileSnapshotWriter writer; // NOTE: htt, 快照文件内容写入

    public FileSnapshotBuilder(InstallSnapshotRpc firstRpc, LogDir logDir) {
        super(firstRpc);
        this.logDir = logDir;

        try {
            writer = new FileSnapshotWriter(logDir.getSnapshotFile(), firstRpc.getLastIndex(), firstRpc.getLastTerm(), firstRpc.getLastConfig());
            writer.write(firstRpc.getData()); // NOTE: htt, 写入快照数据
        } catch (IOException e) {
            throw new LogException("failed to write snapshot data to file", e);
        }
    }

    @Override
    protected void doWrite(byte[] data) throws IOException {
        writer.write(data); // NOTE: htt, 写入数据
    }

    @Override
    public FileSnapshot build() {
        close();
        return new FileSnapshot(logDir); // NOTE: htt, 生成新的快照文件
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new LogException("failed to close writer", e);
        }
    }

}
