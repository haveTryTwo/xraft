package in.xnnyygn.xraft.core.log.sequence;

import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.EntryMeta;

import java.util.List;

public interface EntrySequence { // NOTE: htt, 管理日志条目的列表

    boolean isEmpty();

    int getFirstLogIndex(); // NOTE: htt, 第一条日志条目 index

    int getLastLogIndex(); // NOTE: htt, 最后一条日志条目 index

    int getNextLogIndex(); // NOTE: htt, 下一条日志条目 index

    List<Entry> subView(int fromIndex); // NOTE: htt, 获取从 fromIndex开始的日志

    // [fromIndex, toIndex)
    List<Entry> subList(int fromIndex, int toIndex); // NOTE: htt, 获取从 fromIndex开始的日志，前闭后开

    GroupConfigEntryList buildGroupConfigEntryList(); // NOTE: htt, 构建 GroupConfigEntryList 列表数据

    boolean isEntryPresent(int index); // NOTE: htt, 当前记录是否存在

    EntryMeta getEntryMeta(int index); // NOTE: htt, 获取当前记录的元信息

    Entry getEntry(int index); // NOTE: htt, 获取日志记录

    Entry getLastEntry(); // NOTE: htt, 获取最后的日志记录

    void append(Entry entry); // NOTE: htt, 添加日志条目

    void append(List<Entry> entries); // NOTE: htt, 添加日志条目列表

    void commit(int index); // NOTE: htt, 提交index位置的日志条目

    int getCommitIndex(); // NOTE: htt, 获取当前提交的index位置

    void removeAfter(int index); // NOTE: htt, 删除从 index之后 即 [index+1, ) 范围的数据

    void close();

}
