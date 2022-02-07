package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.log.LogException;

public class EntryInSnapshotException extends LogException { // NOTE: htt, 快照中日志条目异常

    private final int index; // NOTE: htt, 异常的index

    public EntryInSnapshotException(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

}
