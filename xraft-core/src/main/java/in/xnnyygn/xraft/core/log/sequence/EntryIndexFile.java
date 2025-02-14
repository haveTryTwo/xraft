package in.xnnyygn.xraft.core.log.sequence;

import in.xnnyygn.xraft.core.support.RandomAccessFileAdapter;
import in.xnnyygn.xraft.core.support.SeekableFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EntryIndexFile implements Iterable<EntryIndexItem> { // NOTE: htt, 日志条目项索引，保留当前日志条目文件的索引信息

    private static final long OFFSET_MAX_ENTRY_INDEX = Integer.BYTES; // NOTE: htt, maxEntryIndex 在文件中偏移，对应为4bytes
    private static final int LENGTH_ENTRY_INDEX_ITEM = 16; // NOTE: htt, 单个日志条目索引的大小，16bytes，offset占8bytes + kind 占4bytes + term占4bytes
    private final SeekableFile seekableFile;  // NOTE: htt, 封装文件读写相关操作
    private int entryIndexCount; // NOTE: htt, 日志条目个数
    private int minEntryIndex; // NOTE: htt, 最小日志条目index
    private int maxEntryIndex; // NOTE: htt, 最大日志条目index
    private Map<Integer, EntryIndexItem> entryIndexMap = new HashMap<>(); // NOTE: htt, 内存中保存日志条目的索引信息 TODO: 内存可能会不足

    public EntryIndexFile(File file) throws IOException {
        this(new RandomAccessFileAdapter(file));
    }

    public EntryIndexFile(SeekableFile seekableFile) throws IOException {
        this.seekableFile = seekableFile;
        load();
    }

    private void load() throws IOException { // NOTE: htt, 加载索引文件至内存
        if (seekableFile.size() == 0L) {
            entryIndexCount = 0;
            return;
        }
        minEntryIndex = seekableFile.readInt(); // NOTE: htt, minEntryIndex 内容
        maxEntryIndex = seekableFile.readInt(); // NOTE: htt, maxEntryIndex 内容
        updateEntryIndexCount();
        long offset;
        int kind;
        int term;
        for (int i = minEntryIndex; i <= maxEntryIndex; i++) {
            offset = seekableFile.readLong(); // NOTE: htt, 单条索引中的 offset
            kind = seekableFile.readInt(); // NOTE: htt, 单条索引中 kind
            term = seekableFile.readInt(); // NOTE: htt, 单条索引中 term
            entryIndexMap.put(i, new EntryIndexItem(i, offset, kind, term));
        }
    }

    private void updateEntryIndexCount() {
        entryIndexCount = maxEntryIndex - minEntryIndex + 1; // NOTE: htt, 得到日志条目个数
    }

    public boolean isEmpty() {
        return entryIndexCount == 0;
    }

    public int getMinEntryIndex() {
        checkEmpty();
        return minEntryIndex;
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw new IllegalStateException("no entry index");
        }
    }

    public int getMaxEntryIndex() {
        checkEmpty();
        return maxEntryIndex;
    }

    public int getEntryIndexCount() {
        return entryIndexCount;
    }

    public void appendEntryIndex(int index, long offset, int kind, int term) throws IOException {
        if (seekableFile.size() == 0L) { // NOTE: htt, 空文件
            seekableFile.writeInt(index); // NOTE: htt, 空文件写入第一个index为minEntryIndex
            minEntryIndex = index;
        } else {
            if (index != maxEntryIndex + 1) { // NOTE: htt, 日志索引文件校验index和maxEntryIndex+1, 但是数据文件写入时没有校验
                throw new IllegalArgumentException("index must be " + (maxEntryIndex + 1) + ", but was " + index);
            }
            seekableFile.seek(OFFSET_MAX_ENTRY_INDEX); // skip min entry index
        }

        // write max entry index
        seekableFile.writeInt(index); // NOTE: htt, 写入 maxEntryIndex
        maxEntryIndex = index;
        updateEntryIndexCount();

        // move to position after last entry offset
        seekableFile.seek(getOffsetOfEntryIndexItem(index));
        seekableFile.writeLong(offset); // NOTE: htt, 写入offset
        seekableFile.writeInt(kind); // NOTE: htt, 写入kind
        seekableFile.writeInt(term); // NOTE: htt, 写入term

        entryIndexMap.put(index, new EntryIndexItem(index, offset, kind, term));
    }

    private long getOffsetOfEntryIndexItem(int index) {
        return (index - minEntryIndex) * LENGTH_ENTRY_INDEX_ITEM + Integer.BYTES * 2; // NOTE: htt, index number * INDEX_ITEM + min/max index 头部元信息长度
    }

    public void clear() throws IOException {
        seekableFile.truncate(0L);
        entryIndexCount = 0;
        entryIndexMap.clear();
    }

    public void removeAfter(int newMaxEntryIndex) throws IOException {
        if (isEmpty() || newMaxEntryIndex >= maxEntryIndex) {
            return;
        }
        if (newMaxEntryIndex < minEntryIndex) {
            clear();
            return;
        }
        seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        seekableFile.writeInt(newMaxEntryIndex);
        seekableFile.truncate(getOffsetOfEntryIndexItem(newMaxEntryIndex + 1)); // NOTE: htt, 删除索引 newMaxEntryIndex+1 之后的内容
        for (int i = newMaxEntryIndex + 1; i <= maxEntryIndex; i++) {
            entryIndexMap.remove(i); // NOTE: htt 删除内容中信息
        }
        maxEntryIndex = newMaxEntryIndex;
        entryIndexCount = newMaxEntryIndex - minEntryIndex + 1; // NOTE: htt, 复用updateEntryIndexCount()
    }

    public long getOffset(int entryIndex) {
        return get(entryIndex).getOffset();
    }

    @Nonnull
    public EntryIndexItem get(int entryIndex) { // NOTE: htt, 返回在范围之内的 EntryIndexItem（日志条目项）
        checkEmpty();
        if (entryIndex < minEntryIndex || entryIndex > maxEntryIndex) {
            throw new IllegalArgumentException("index < min or index > max");
        }
        return entryIndexMap.get(entryIndex);
    }

    @Override
    @Nonnull
    public Iterator<EntryIndexItem> iterator() {
        if (isEmpty()) {
            return Collections.emptyIterator();
        }
        return new EntryIndexIterator(entryIndexCount, minEntryIndex);
    }

    public void close() throws IOException {
        seekableFile.close();
    }

    private class EntryIndexIterator implements Iterator<EntryIndexItem> { // NOTE: htt, 日志条目项迭代，从 entryIndexMap 获取

        private final int entryIndexCount; // NOTE: htt, 日志条目个数
        private int currentEntryIndex; // NOTE: htt, 当前index

        EntryIndexIterator(int entryIndexCount, int minEntryIndex) {
            this.entryIndexCount = entryIndexCount;
            this.currentEntryIndex = minEntryIndex;
        }

        @Override
        public boolean hasNext() {
            checkModification();
            return currentEntryIndex <= maxEntryIndex;
        }

        private void checkModification() { // NOTE: htt, 检测日志条目个数数据发生变化 TODO:是否考虑锁等，为什么会变化
            if (this.entryIndexCount != EntryIndexFile.this.entryIndexCount) {
                throw new IllegalStateException("entry index count changed");
            }
        }

        @Override
        public EntryIndexItem next() {
            checkModification();
            return entryIndexMap.get(currentEntryIndex++);
        }
    }

}
