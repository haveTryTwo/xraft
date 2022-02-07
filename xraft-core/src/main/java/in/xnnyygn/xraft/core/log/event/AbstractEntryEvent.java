package in.xnnyygn.xraft.core.log.event;

import in.xnnyygn.xraft.core.log.entry.Entry;

abstract class AbstractEntryEvent<T extends Entry> { // NOTE: htt, 抽象日志条目，获取日志信息

    protected final T entry; // NOTE: htt, 日志条目

    AbstractEntryEvent(T entry) {
        this.entry = entry;
    }

    public T getEntry() {
        return entry;
    }

}
