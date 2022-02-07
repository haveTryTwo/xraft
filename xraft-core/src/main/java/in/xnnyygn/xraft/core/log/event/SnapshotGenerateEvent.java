package in.xnnyygn.xraft.core.log.event;

public class SnapshotGenerateEvent { // NOTE: htt, 快照生成事件

    private final int lastIncludedIndex; // NOTE: htt, 快照最后包括索引

    public SnapshotGenerateEvent(int lastIncludedIndex) {
        this.lastIncludedIndex = lastIncludedIndex;
    }

    public int getLastIncludedIndex() {
        return lastIncludedIndex;
    }

}
