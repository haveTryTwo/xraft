package in.xnnyygn.xraft.core.log.sequence;

import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;

@NotThreadSafe
public class MemoryEntrySequence extends AbstractEntrySequence { // NOTE: htt, 内存保存日志条目，用于功能测试

    private final List<Entry> entries = new ArrayList<>();

    public MemoryEntrySequence() {
        this(1);
    } // NOTE: htt, 日志序号默认长1开始

    public MemoryEntrySequence(int logIndexOffset) {
        super(logIndexOffset);
    }

    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) { // NOTE: htt, 返回内存中 [fromIndex-firstIndex, toIndex-firstIndex) 数据
        return entries.subList(fromIndex - logIndexOffset, toIndex - logIndexOffset);
    }

    @Override
    protected Entry doGetEntry(int index) {
        return entries.get(index - logIndexOffset);
    }

    @Override
    protected void doAppend(Entry entry) {
        entries.add(entry);
    } // NOTE: htt, 添加日志

    @Override
    public void commit(int index) {
    }

    @Override
    public int getCommitIndex() {
        // TODO implement me
        throw new UnsupportedOperationException();
    }

    @Override
    public GroupConfigEntryList buildGroupConfigEntryList() {
        GroupConfigEntryList list = new GroupConfigEntryList();
        for (Entry entry : entries) {
            if (entry instanceof GroupConfigEntry) {
                list.add((GroupConfigEntry) entry); // NOTE: htt, 如果是成员变化日志，则添加
            }
        }
        return list;
    }

    @Override
    protected void doRemoveAfter(int index) {
        if (index < doGetFirstLogIndex()) {
            entries.clear();
            nextLogIndex = logIndexOffset; // NOTE: htt, 清空并复原
        } else {
            entries.subList(index - logIndexOffset + 1, entries.size()).clear(); // NOTE: htt, 删除 [index-firstIndex+1, ) 数据
            nextLogIndex = index + 1; // NOTE: htt, 设置 next log index 位置
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "MemoryEntrySequence{" +
                "logIndexOffset=" + logIndexOffset +
                ", nextLogIndex=" + nextLogIndex +
                ", entries.size=" + entries.size() +
                '}';
    }

}
