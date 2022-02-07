package in.xnnyygn.xraft.core.log;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.EntryMeta;
import in.xnnyygn.xraft.core.log.sequence.EntrySequence;
import in.xnnyygn.xraft.core.log.sequence.MemoryEntrySequence;
import in.xnnyygn.xraft.core.log.snapshot.*;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@NotThreadSafe
public class MemoryLog extends AbstractLog { // NOTE: htt, 内存日志

    private static final Logger logger = LoggerFactory.getLogger(MemoryLog.class);

    public MemoryLog() {
        this(new EventBus());
    }

    public MemoryLog(EventBus eventBus) {
        this(new EmptySnapshot(), new MemoryEntrySequence(), eventBus);
    }

    public MemoryLog(Snapshot snapshot, EntrySequence entrySequence, EventBus eventBus) {
        super(eventBus);
        this.snapshot = snapshot;
        this.entrySequence = entrySequence;
    }

    @Override
    protected Snapshot generateSnapshot(EntryMeta lastAppliedEntryMeta, Set<NodeEndpoint> groupConfig) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            stateMachine.generateSnapshot(output); // NOTE: htt, 默认的EmptyStateMachine，即没有处理，TODO:可以考虑添加个stateMachine
        } catch (IOException e) {
            throw new LogException("failed to generate snapshot", e);
        }
        return new MemorySnapshot(lastAppliedEntryMeta.getIndex(), lastAppliedEntryMeta.getTerm(), output.toByteArray(), groupConfig);
    }

    @Override
    protected SnapshotBuilder newSnapshotBuilder(InstallSnapshotRpc firstRpc) {
        return new MemorySnapshotBuilder(firstRpc); // NOTE: htt, 生成快照builder
    }

    @Override
    protected void replaceSnapshot(Snapshot newSnapshot) { // NOTE: htt, 替换快照，并替换内存中日志序列
        int logIndexOffset = newSnapshot.getLastIncludedIndex() + 1; // NOTE: htt, 获取比快照新的index offset
        EntrySequence newEntrySequence = new MemoryEntrySequence(logIndexOffset);
        List<Entry> remainingEntries = entrySequence.subView(logIndexOffset);
        newEntrySequence.append(remainingEntries);
        logger.debug("snapshot -> {}", newSnapshot);
        snapshot = newSnapshot; // NOTE: htt, 已有的快照替换为新的快照
        logger.debug("entry sequence -> {}", newEntrySequence);
        entrySequence = newEntrySequence; // NOTE: htt, 替换当前的 日志条目序列（之前的数据已经保存在快照中）
    }

}
