package in.xnnyygn.xraft.core.log.sequence;

import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.EntryMeta;

import java.util.Collections;
import java.util.List;

abstract class AbstractEntrySequence implements EntrySequence { // NOTE: htt, 基于 日志序列 抽象实现

    int logIndexOffset; // NOTE: htt, 日志 index，即第一个index
    int nextLogIndex; // NOTE: htt, 下一个新的日志 next index， lastLogIndex = nextLogIndex - 1

    AbstractEntrySequence(int logIndexOffset) { // NOTE: htt, 初始化时 logIndexOffset 和 nextLogIndex 一致，同时代表此时没有数据
        this.logIndexOffset = logIndexOffset;
        this.nextLogIndex = logIndexOffset;
    }

    @Override
    public boolean isEmpty() {
        return logIndexOffset == nextLogIndex;
    }

    @Override
    public int getFirstLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        return doGetFirstLogIndex();
    }

    int doGetFirstLogIndex() {
        return logIndexOffset;
    }

    @Override
    public int getLastLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        return doGetLastLogIndex();
    }

    int doGetLastLogIndex() {
        return nextLogIndex - 1;
    }

    @Override
    public boolean isEntryPresent(int index) { // NOTE: htt, 日志在 [firstLogindex, lastLogindex] 范围内才有效
        return !isEmpty() && index >= doGetFirstLogIndex() && index <= doGetLastLogIndex();
    }

    @Override
    public Entry getEntry(int index) {
        if (!isEntryPresent(index)) {
            return null;
        }
        return doGetEntry(index);
    }

    @Override
    public EntryMeta getEntryMeta(int index) {
        Entry entry = getEntry(index);
        return entry != null ? entry.getMeta() : null;
    }

    protected abstract Entry doGetEntry(int index);  // NOTE: htt, 返回指定位置的日志

    @Override
    public Entry getLastEntry() {
        return isEmpty() ? null : doGetEntry(doGetLastLogIndex());
    }

    @Override
    public List<Entry> subView(int fromIndex) { // NOTE: htt, 返回 [fromIndex, )数据，其中起始位置关注和 firstLogIndex 大小
        if (isEmpty() || fromIndex > doGetLastLogIndex()) {
            return Collections.emptyList();
        }
        return subList(Math.max(fromIndex, doGetFirstLogIndex()), nextLogIndex);
    }

    // [fromIndex, toIndex)
    @Override
    public List<Entry> subList(int fromIndex, int toIndex) {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }
        if (fromIndex < doGetFirstLogIndex() || toIndex > doGetLastLogIndex() + 1 || fromIndex > toIndex) {
            throw new IllegalArgumentException("illegal from index " + fromIndex + " or to index " + toIndex);
        }
        return doSubList(fromIndex, toIndex);
    }

    protected abstract List<Entry> doSubList(int fromIndex, int toIndex);

    @Override
    public int getNextLogIndex() {
        return nextLogIndex;
    }

    @Override
    public void append(List<Entry> entries) {
        for (Entry entry : entries) {
            append(entry);
        }
    }

    @Override
    public void append(Entry entry) {
        if (entry.getIndex() != nextLogIndex) { // NOTE: htt, 添加的日志的index 必须是系统中下一条要写入的index
            throw new IllegalArgumentException("entry index must be " + nextLogIndex);
        }
        doAppend(entry);
        nextLogIndex++; // NOTE: htt, 增加next log index
    }

    protected abstract void doAppend(Entry entry);

    @Override
    public void removeAfter(int index) { // NOTE: htt, 删除 (index, ) 的数据
        if (isEmpty() || index >= doGetLastLogIndex()) {
            return;
        }
        doRemoveAfter(index);
    }

    protected abstract void doRemoveAfter(int index);

}
