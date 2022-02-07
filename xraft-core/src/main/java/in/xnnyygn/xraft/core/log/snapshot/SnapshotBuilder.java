package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;

public interface SnapshotBuilder<T extends Snapshot> { // NOTE: htt, 构建快照

    void append(InstallSnapshotRpc rpc); // NOTE: htt, 添加快照安装请求

    T build(); // NOTE: htt, 构建快照

    void close();

}
