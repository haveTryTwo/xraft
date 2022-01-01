package in.xnnyygn.xraft.core.log.entry;

public class EntryMeta { // NOTE: htt, 日志条目的元信息

    private final int kind; // NOTE: htt, 类型
    private final int index; // NOTE: htt, 日志对应index
    private final int term; // NOTE: htt, 日志对应term

    public EntryMeta(int kind, int index, int term) {
        this.kind = kind;
        this.index = index;
        this.term = term;
    }

    public int getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

    public int getTerm() {
        return term;
    }

}
