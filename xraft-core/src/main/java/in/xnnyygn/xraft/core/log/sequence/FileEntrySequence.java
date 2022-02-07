package in.xnnyygn.xraft.core.log.sequence;

import in.xnnyygn.xraft.core.log.LogDir;
import in.xnnyygn.xraft.core.log.LogException;
import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.EntryFactory;
import in.xnnyygn.xraft.core.log.entry.EntryMeta;
import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@NotThreadSafe
public class FileEntrySequence extends AbstractEntrySequence { // NOTE: htt, 日志条目序列处理，包括文件写入（索引和数据文件），缓存的日志条目等

    private final EntryFactory entryFactory = new EntryFactory();  // NOTE: htt, 工厂方式，根据不同条目类型 生成对应的条目
    private final EntriesFile entriesFile; // NOTE: htt, 条目文件，其中封装了 SeekableFile，基于此进行内容调整
    private final EntryIndexFile entryIndexFile; // NOTE: htt, 日志条目项索引，保留当前日志条目文件的索引信息
    private final LinkedList<Entry> pendingEntries = new LinkedList<>(); // NOTE: htt, 当前缓存的日志条目，尚未写盘
    private int commitIndex; // NOTE: htt, 已提交的index

    public FileEntrySequence(LogDir logDir, int logIndexOffset) {
        super(logIndexOffset); // NOTE: htt, 执行初始化
        try {
            this.entriesFile = new EntriesFile(logDir.getEntriesFile());
            this.entryIndexFile = new EntryIndexFile(logDir.getEntryOffsetIndexFile());
            initialize();
        } catch (IOException e) {
            throw new LogException("failed to open entries file or entry index file", e);
        }
    }

    public FileEntrySequence(EntriesFile entriesFile, EntryIndexFile entryIndexFile, int logIndexOffset) {
        super(logIndexOffset);  // NOTE: htt, 执行初始化
        this.entriesFile = entriesFile;
        this.entryIndexFile = entryIndexFile;
        initialize();
    }

    private void initialize() {
        if (entryIndexFile.isEmpty()) { // NOTE: htt, 空的情况下则直接调整commitIndex
            commitIndex = logIndexOffset - 1;
            return;
        }
        logIndexOffset = entryIndexFile.getMinEntryIndex(); // NOTE: htt, 重新调整 logIndexOffset 为索引中的值
        nextLogIndex = entryIndexFile.getMaxEntryIndex() + 1; // NOTE: htt, 重新调整 nextLogIndex 为索引中的 最大index+1
        commitIndex = entryIndexFile.getMaxEntryIndex(); // TODO: commitIndex 为索引中最大的index，这里必须是master已经允许提交
    }

    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    @Override
    public GroupConfigEntryList buildGroupConfigEntryList() { // NOTE: htt, 获取新增或删除节点的日志条目
        GroupConfigEntryList list = new GroupConfigEntryList();

        // check file
        try {
            int entryKind;
            for (EntryIndexItem indexItem : entryIndexFile) {
                entryKind = indexItem.getKind();
                if (entryKind == Entry.KIND_ADD_NODE || entryKind == Entry.KIND_REMOVE_NODE) { // NOTE: htt, 新增或删除节点
                    list.add((GroupConfigEntry) entriesFile.loadEntry(indexItem.getOffset(), entryFactory)); // NOTE: htt, 获取新增或删除日志条目
                }
            }
        } catch (IOException e) {
            throw new LogException("failed to load entry", e);
        }

        // check pending entries
        for (Entry entry : pendingEntries) {
            if (entry instanceof GroupConfigEntry) {
                list.add((GroupConfigEntry) entry); // NOTE: htt, 获取缓存中的 添加或删除的日志条目
            }
        }
        return list;
    }

    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {
        List<Entry> result = new ArrayList<>();

        // entries from file
        if (!entryIndexFile.isEmpty() && fromIndex <= entryIndexFile.getMaxEntryIndex()) {
            int maxIndex = Math.min(entryIndexFile.getMaxEntryIndex() + 1, toIndex);
            for (int i = fromIndex; i < maxIndex; i++) { // TODO: empe, 需要先判断 fromIndex 和 entryIndexFile.minEntryIndex 大小
                result.add(getEntryInFile(i));
            }
        }

        // entries from pending entries
        if (!pendingEntries.isEmpty() && toIndex > pendingEntries.getFirst().getIndex()) {
            Iterator<Entry> iterator = pendingEntries.iterator();
            Entry entry;
            int index;
            while (iterator.hasNext()) { // NOTE: htt, 获取缓存中的日志项
                entry = iterator.next();
                index = entry.getIndex();
                if (index >= toIndex) {
                    break;
                }
                if (index >= fromIndex) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    @Override
    protected Entry doGetEntry(int index) {
        if (!pendingEntries.isEmpty()) {
            int firstPendingEntryIndex = pendingEntries.getFirst().getIndex();
            if (index >= firstPendingEntryIndex) { // TODO: htt, 可以判断是否在范围内
                return pendingEntries.get(index - firstPendingEntryIndex);
            }
        }

        // pending entries not empty but index < firstPendingEntryIndex => entry in file
        // pending entries empty => entry in file
        assert !entryIndexFile.isEmpty();
        return getEntryInFile(index);
    }

    @Override
    public EntryMeta getEntryMeta(int index) {
        if (!isEntryPresent(index)) {
            return null;
        }
        if (entryIndexFile.isEmpty()) {
            return pendingEntries.get(index - doGetFirstLogIndex()).getMeta(); // TODO: htt, doGetFirstLogIndex()最好用队列头部的index
        }
        return entryIndexFile.get(index).toEntryMeta(); // NOTE: htt, 索引文件获取对应元信息
    }

    private Entry getEntryInFile(int index) { // NOTE: htt, 获取指定索引的日志条目项
        long offset = entryIndexFile.getOffset(index);
        try {
            return entriesFile.loadEntry(offset, entryFactory);
        } catch (IOException e) {
            throw new LogException("failed to load entry " + index, e);
        }
    }

    @Override
    public Entry getLastEntry() { // NOTE: htt, 获取最新的日志条目
        if (isEmpty()) {
            return null;
        }
        if (!pendingEntries.isEmpty()) {
            return pendingEntries.getLast();
        }
        assert !entryIndexFile.isEmpty();
        return getEntryInFile(entryIndexFile.getMaxEntryIndex());
    }

    @Override
    protected void doAppend(Entry entry) {
        pendingEntries.add(entry);
    }

    @Override
    public void commit(int index) { // NOTE: htt, 提交日志，将日志从内存写入到磁盘文件
        if (index < commitIndex) {
            throw new IllegalArgumentException("commit index < " + commitIndex);
        }
        if (index == commitIndex) {
            return;
        }
        if (pendingEntries.isEmpty() || pendingEntries.getLast().getIndex() < index) {
            throw new IllegalArgumentException("no entry to commit or commit index exceed");
        }
        long offset;
        Entry entry = null;
        try {
            for (int i = commitIndex + 1; i <= index; i++) { // TODO: htt, 可以加个校验， entry.getIndex() == i，这两个必须相等
                entry = pendingEntries.removeFirst(); // TODO: htt, 可以在最后删除，如磁盘满等异常
                offset = entriesFile.appendEntry(entry); // NOTE: htt, 日志条目写入到数据文件 TODO:htt, 内部需要校验index和nextIndex一致性？
                entryIndexFile.appendEntryIndex(i, offset, entry.getKind(), entry.getTerm()); // NOTE: htt, 写入日志条目索引文件
                commitIndex = i; // NOTE: htt, 数据写入文件后，会同步递增 commitIndex
            }
        } catch (IOException e) {
            throw new LogException("failed to commit entry " + entry, e);
        }
    }

    @Override
    protected void doRemoveAfter(int index) { // NOTE: htt, 删除 > index, 即删除(index, )的数据
        if (!pendingEntries.isEmpty() && index >= pendingEntries.getFirst().getIndex() - 1) {
            // remove last n entries in pending entries
            for (int i = index + 1; i <= doGetLastLogIndex(); i++) { // NOTE: htt, 删除缓存中 > index 日志条目
                pendingEntries.removeLast();
            }
            nextLogIndex = index + 1;
            return;
        }
        try {
            if (index >= doGetFirstLogIndex()) {
                pendingEntries.clear(); // NOTE: htt, 清空缓存
                // remove entries whose index >= (index + 1)
                entriesFile.truncate(entryIndexFile.getOffset(index + 1)); // NOTE: htt, 数据文件删除记录
                entryIndexFile.removeAfter(index); // NOTE: htt, 索引删除之后数据
                nextLogIndex = index + 1;
                commitIndex = index; // NOTE: htt, 调整commitIndex值， TODO：此时会调整 commitIndex,raft保证一旦commit数据就所有可见状态机都可见
            } else {
                pendingEntries.clear();
                entriesFile.clear();
                entryIndexFile.clear();
                nextLogIndex = logIndexOffset;
                commitIndex = logIndexOffset - 1;
            }
        } catch (IOException e) {
            throw new LogException(e);
        }
    }

    @Override
    public void close() { // NOTE: htt, 关闭
        try {
            entriesFile.close();
            entryIndexFile.close();
        } catch (IOException e) {
            throw new LogException("failed to close", e);
        }
    }

}
