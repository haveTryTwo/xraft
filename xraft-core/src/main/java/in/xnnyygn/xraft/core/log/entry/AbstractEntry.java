package in.xnnyygn.xraft.core.log.entry;

abstract class AbstractEntry implements Entry { // NOTE: htt, 日志条码的抽象实现，即返回了 kind/index/term 信息

    private final int kind;
    protected final int index;
    protected final int term;

    AbstractEntry(int kind, int index, int term) {
        this.kind = kind;
        this.index = index;
        this.term = term;
    }

    @Override
    public int getKind() {
        return this.kind;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public EntryMeta getMeta() {
        return new EntryMeta(kind, index, term);
    }

}
