package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;

public class NullSnapshotBuilder implements SnapshotBuilder { // NOTE: htt, 空的快照构建，不产生实际快照

    @Override
    public void append(InstallSnapshotRpc rpc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Snapshot build() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }

}
