package in.xnnyygn.xraft.core.log.sequence;

import in.xnnyygn.xraft.core.log.entry.GroupConfigEntry;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@NotThreadSafe
public class GroupConfigEntryList implements Iterable<GroupConfigEntry> { // NOTE: htt, 包装 GroupConfigEntry 列表，支持迭代

    private final LinkedList<GroupConfigEntry> entries = new LinkedList<>(); // NOTE: htt, 保存一系列的 组成员变更日志 列表

    public GroupConfigEntry getLast() {
        return entries.isEmpty() ? null : entries.getLast();
    }

    public void add(GroupConfigEntry entry) {
        entries.add(entry);
    }

    /**
     * Remove entries whose index is greater than {@code entryIndex}.
     *
     * @param entryIndex entry index
     * @return first removed entry, {@code null} if no entry removed
     */
    public GroupConfigEntry removeAfter(int entryIndex) { // NOTE: htt, 是大于 entryIndex 的日志条目，可以用于回滚不一致数据
        Iterator<GroupConfigEntry> iterator = entries.iterator();
        GroupConfigEntry firstRemovedEntry = null;
        while (iterator.hasNext()) {
            GroupConfigEntry entry = iterator.next();
            if (entry.getIndex() > entryIndex) { // NOTE: htt, 是大于 entryIndex 的日志条目，可以用于回滚不一致数据
                if (firstRemovedEntry == null) {
                    firstRemovedEntry = entry;
                }
                iterator.remove();
            }
        }
        return firstRemovedEntry; // NOTE: htt, 返回第一条记录
    }

    public List<GroupConfigEntry> subList(int fromIndex, int toIndex) { // NOTE: htt,返回 [fromIndex, toIndex) 范围的日志记录
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("from index > to index");
        }
        return entries.stream()
                .filter(e -> e.getIndex() >= fromIndex && e.getIndex() < toIndex)
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Iterator<GroupConfigEntry> iterator() {
        return entries.iterator();
    } // NOTE: htt, 返回迭代

    @Override
    public String toString() {
        return "GroupConfigEntryList{" + entries + '}';
    }

}
