package in.xnnyygn.xraft.core.log;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.log.entry.*;
import in.xnnyygn.xraft.core.log.event.GroupConfigEntryBatchRemovedEvent;
import in.xnnyygn.xraft.core.log.event.GroupConfigEntryCommittedEvent;
import in.xnnyygn.xraft.core.log.event.GroupConfigEntryFromLeaderAppendEvent;
import in.xnnyygn.xraft.core.log.event.SnapshotGenerateEvent;
import in.xnnyygn.xraft.core.log.sequence.EntrySequence;
import in.xnnyygn.xraft.core.log.sequence.GroupConfigEntryList;
import in.xnnyygn.xraft.core.log.snapshot.*;
import in.xnnyygn.xraft.core.log.statemachine.EmptyStateMachine;
import in.xnnyygn.xraft.core.log.statemachine.StateMachine;
import in.xnnyygn.xraft.core.log.statemachine.StateMachineContext;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.message.AppendEntriesRpc;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

abstract class AbstractLog implements Log { // NOTE: htt, 抽象日志处理，包括创建日志添加以及快照请求，以及commit index和处理日志写入

    private static final Logger logger = LoggerFactory.getLogger(AbstractLog.class);

    protected final EventBus eventBus; // NOTE: htt, 消息组件
    protected Snapshot snapshot;  // NOTE: htt, 快照信息，用于保存快照数据
    protected EntrySequence entrySequence;  // NOTE: htt, 管理日志条目的列表，用于处理当前写入到日志条目文件数据（不在快照中的日志数据）

    protected SnapshotBuilder snapshotBuilder = new NullSnapshotBuilder();
    protected GroupConfigEntryList groupConfigEntryList = new GroupConfigEntryList(); // NOTE: htt, 节点成员日志条目列表
    private final StateMachineContext stateMachineContext = new StateMachineContextImpl(); // NOTE: htt, 状态机Conntext，用于将将生成快照的index传递给快照处理
    protected StateMachine stateMachine = new EmptyStateMachine(); // NOTE: htt, 空内容的状态机
    protected int commitIndex = 0;

    AbstractLog(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    @Nonnull
    public EntryMeta getLastEntryMeta() { // NOTE: htt, 获取最后一条日志条目元信息
        if (entrySequence.isEmpty()) {
            return new EntryMeta(Entry.KIND_NO_OP, snapshot.getLastIncludedIndex(), snapshot.getLastIncludedTerm());
        }
        return entrySequence.getLastEntry().getMeta();
    }

    @Override
    public AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries) { // NOTE: htt, 生成日志条目请求
        int nextLogIndex = entrySequence.getNextLogIndex();
        if (nextIndex > nextLogIndex) {
            throw new IllegalArgumentException("illegal next index " + nextIndex);
        }
        if (nextIndex <= snapshot.getLastIncludedIndex()) {
            throw new EntryInSnapshotException(nextIndex);
        }
        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setMessageId(UUID.randomUUID().toString());
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);
        rpc.setLeaderCommit(commitIndex); // NOTE: htt, 随着Append日志条目请求，将commitIndex带上
        if (nextIndex == snapshot.getLastIncludedIndex() + 1) {
            rpc.setPrevLogIndex(snapshot.getLastIncludedIndex());
            rpc.setPrevLogTerm(snapshot.getLastIncludedTerm());
        } else {
            // if entry sequence is empty,
            // snapshot.lastIncludedIndex + 1 == nextLogIndex,
            // so it has been rejected at the first line.
            //
            // if entry sequence is not empty,
            // snapshot.lastIncludedIndex + 1 < nextIndex <= nextLogIndex
            // and snapshot.lastIncludedIndex + 1 = firstLogIndex
            //     nextLogIndex = lastLogIndex + 1
            // then firstLogIndex < nextIndex <= lastLogIndex + 1
            //      firstLogIndex + 1 <= nextIndex <= lastLogIndex + 1
            //      firstLogIndex <= nextIndex - 1 <= lastLogIndex
            // it is ok to get entry without null check
            Entry entry = entrySequence.getEntry(nextIndex - 1);
            assert entry != null;
            rpc.setPrevLogIndex(entry.getIndex()); // NOTE: htt, 添加nextIndex的前一条记录的index和下面的term
            rpc.setPrevLogTerm(entry.getTerm());
        }
        if (!entrySequence.isEmpty()) {
            int maxIndex = (maxEntries == ALL_ENTRIES ? nextLogIndex : Math.min(nextLogIndex, nextIndex + maxEntries));
            rpc.setEntries(entrySequence.subList(nextIndex, maxIndex)); // NOTE: htt, 添加从 [nextIndex, maxIndex)之间日志条目
        }
        return rpc;
    }

    @Override
    public InstallSnapshotRpc createInstallSnapshotRpc(int term, NodeId selfId, int offset, int length) { // NOTE: htt, 生成快照安装请求
        InstallSnapshotRpc rpc = new InstallSnapshotRpc();
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);
        rpc.setLastIndex(snapshot.getLastIncludedIndex());
        rpc.setLastTerm(snapshot.getLastIncludedTerm());
        if (offset == 0) {
            rpc.setLastConfig(snapshot.getLastConfig());
        }
        rpc.setOffset(offset);

        SnapshotChunk chunk = snapshot.readData(offset, length); // NOTE: htt, 读取部分快照数据
        rpc.setData(chunk.toByteArray());
        rpc.setDone(chunk.isLastChunk());
        return rpc;
    }

    @Override
    public GroupConfigEntry getLastUncommittedGroupConfigEntry() { // NOTE: htt, 生成未提交的组成员变更日志条目
        GroupConfigEntry lastEntry = groupConfigEntryList.getLast();
        return (lastEntry != null && lastEntry.getIndex() > commitIndex) ? lastEntry : null;
    }

    @Override
    public int getNextIndex() {
        return entrySequence.getNextLogIndex(); // NOTE: htt, 从日志序列中获取下一个log index
    }

    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    @Override
    public boolean isNewerThan(int lastLogIndex, int lastLogTerm) {
        EntryMeta lastEntryMeta = getLastEntryMeta();
        logger.debug("last entry ({}, {}), candidate ({}, {})", lastEntryMeta.getIndex(), lastEntryMeta.getTerm(), lastLogIndex, lastLogTerm);
        // TODO: htt, 根据raft up-to-date机制 在判断index大小时需要 lastEntryMeta.getTerm() == lastLogTerm
        return lastEntryMeta.getTerm() > lastLogTerm ||
                (lastEntryMeta.getTerm() == lastLogTerm && lastEntryMeta.getIndex() > lastLogIndex); // NOTE: htt, 先判断term，如果大则返回；否则判断index
    }

    @Override
    public NoOpEntry appendEntry(int term) { // NOTE: htt, 添加空日志，以便选举成功后立即同步，保证当前term可以实现多数数据提交
        NoOpEntry entry = new NoOpEntry(entrySequence.getNextLogIndex(), term);
        entrySequence.append(entry); // NOTE: htt, 添加空日志条目
        return entry;
    }

    @Override
    public GeneralEntry appendEntry(int term, byte[] command) {
        GeneralEntry entry = new GeneralEntry(entrySequence.getNextLogIndex(), term, command);
        entrySequence.append(entry); // NOTE: htt, 添加 一般数据的日志条目
        return entry;
    }

    @Override
    public AddNodeEntry appendEntryForAddNode(int term, Set<NodeEndpoint> nodeEndpoints, NodeEndpoint newNodeEndpoint) {
        AddNodeEntry entry = new AddNodeEntry(entrySequence.getNextLogIndex(), term, nodeEndpoints, newNodeEndpoint);
        entrySequence.append(entry); // NOTE: htt, 添加成员增加日志条目
        groupConfigEntryList.add(entry); // NOTE: htt, 添加成员增加日志条目到成员变更列表
        return entry;
    }

    @Override
    public RemoveNodeEntry appendEntryForRemoveNode(int term, Set<NodeEndpoint> nodeEndpoints, NodeId nodeToRemove) {
        RemoveNodeEntry entry = new RemoveNodeEntry(entrySequence.getNextLogIndex(), term, nodeEndpoints, nodeToRemove);
        entrySequence.append(entry); // NOTE: htt, 添加成员减少日志条目
        groupConfigEntryList.add(entry); // NOTE: htt, 添加成员减少日志条目到成员变更列表
        return entry;
    }

    @Override
    public boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> leaderEntries) {
        // check previous log
        if (!checkIfPreviousLogMatches(prevLogIndex, prevLogTerm)) { // NOTE: htt, 如果leader中的 <preLogIndex, prevLogTerm> 和当前不相同，则返回出错
            return false;
        }
        // heartbeat
        if (leaderEntries.isEmpty()) {
            return true;
        }
        assert prevLogIndex + 1 == leaderEntries.get(0).getIndex(); // NOTE: htt, prevLogIndex即leaderEntries中前一个entry index
        EntrySequenceView newEntries = removeUnmatchedLog(new EntrySequenceView(leaderEntries)); // NOTE: htt, 获取当前需要处理的日志条目
        appendEntriesFromLeader(newEntries); // NOTE: 添加当前新的日志条目，但是未 commit
        return true;
    }

    private void appendEntriesFromLeader(EntrySequenceView leaderEntries) {
        if (leaderEntries.isEmpty()) {
            return;
        }
        logger.debug("append entries from leader from {} to {}", leaderEntries.getFirstLogIndex(), leaderEntries.getLastLogIndex());
        for (Entry leaderEntry : leaderEntries) {
            appendEntryFromLeader(leaderEntry); // NOTE: htt, 添加日志条目
        }
    }

    private void appendEntryFromLeader(Entry leaderEntry) {
        entrySequence.append(leaderEntry); // NOTE: htt, 添加日志请求（此时未提交）
        if (leaderEntry instanceof GroupConfigEntry) {
            eventBus.post(new GroupConfigEntryFromLeaderAppendEvent( // NOTE: htt, 添加日志请求如果是 成员变更请求，则执行相应节点添加或删除
                    (GroupConfigEntry) leaderEntry)
            );
        }
    }

    private EntrySequenceView removeUnmatchedLog(EntrySequenceView leaderEntries) {
        assert !leaderEntries.isEmpty();
        int firstUnmatched = findFirstUnmatchedLog(leaderEntries);

        // NOTE: htt, 注意删除节点超时，队友leader节点是不会有的，因为leader节点数据是比如不会回滚
        removeEntriesAfter(firstUnmatched - 1); // NOTE: htt, 删除原有日志条目中 firstUnmatched - 1 之后数据
        return leaderEntries.subView(firstUnmatched); // NOTE: htt, 返回leader中不一致的日志条目，然后后续添加到当前日志条目
    }

    private int findFirstUnmatchedLog(EntrySequenceView leaderEntries) {
        assert !leaderEntries.isEmpty();
        int logIndex;
        EntryMeta followerEntryMeta;
        for (Entry leaderEntry : leaderEntries) {
            logIndex = leaderEntry.getIndex();
            followerEntryMeta = entrySequence.getEntryMeta(logIndex);
            if (followerEntryMeta == null || followerEntryMeta.getTerm() != leaderEntry.getTerm()) { // NOTE: htt, 如果index不一致，或term不一致
                return logIndex;
            }
        }
        return leaderEntries.getLastLogIndex() + 1;
    }

    private boolean checkIfPreviousLogMatches(int prevLogIndex, int prevLogTerm) { // NOTE: htt, 判断 prevLogIndex和prevLogTerm在有效范围内
        int lastIncludedIndex = snapshot.getLastIncludedIndex();
        if (prevLogIndex < lastIncludedIndex) { // NOTE: htt, 比快照中包含的index小则返回false
            logger.debug("previous log index {} < snapshot's last included index {}", prevLogIndex, lastIncludedIndex);
            return false;
        }
        if (prevLogIndex == lastIncludedIndex) {
            int lastIncludedTerm = snapshot.getLastIncludedTerm();
            if (prevLogTerm != lastIncludedTerm) {
                logger.debug("previous log index matches snapshot's last included index, " +
                        "but term not (expected {}, actual {})", lastIncludedTerm, prevLogTerm);
                return false;
            }
            return true;
        }
        Entry entry = entrySequence.getEntry(prevLogIndex);
        if (entry == null) {
            logger.debug("previous log {} not found", prevLogIndex);
            return false;
        }
        int term = entry.getTerm();
        if (term != prevLogTerm) {
            logger.debug("different term of previous log, local {}, remote {}", term, prevLogTerm);
            return false;
        }
        return true;
    }

    private void removeEntriesAfter(int index) { // NOTE: htt, 删除 (index, )之后的数据，这里只考虑 日志条目数据，未考虑快照数据不一致
        if (entrySequence.isEmpty() || index >= entrySequence.getLastLogIndex()) {
            return;
        }
        int lastApplied = stateMachine.getLastApplied();
        if (index < lastApplied && entrySequence.subList(index + 1, lastApplied + 1).stream().anyMatch(this::isApplicable)) {
            logger.warn("applied log removed, reapply from start");
            applySnapshot(snapshot); // NOTE: htt, 状态机从快照全部恢复
            logger.debug("apply log from {} to {}", entrySequence.getFirstLogIndex(), index);
            entrySequence.subList(entrySequence.getFirstLogIndex(), index + 1).forEach(this::applyEntry); // NOTE: htt, 状态机恢复日志条目部分数据，恢复到删除的下一条
        }
        logger.debug("remove entries after {}", index);
        entrySequence.removeAfter(index); // NOTE: htt, 日志条目删除index之后数据
        if (index < commitIndex) {
            commitIndex = index; // NOTE: htt, 调整commit index，TODO：实际上不应该调整提交日志条目位置？
        }
        GroupConfigEntry firstRemovedEntry = groupConfigEntryList.removeAfter(index);
        if (firstRemovedEntry != null) {
            logger.info("group config removed");

            //  NOTE: htt, 恢复的列表使用 FirstRemovedEntry，是因为 GroupConfigEntry.nodeEndpoints 为删除前的列表集合，
            //  剩下如果新增节点则保存为 AddNodeEntry.newNodeEndpoint 或者 RemoveNodeEntry.nodeToRemove
            //  这里使用上有些取巧，容易造成误解
            eventBus.post(new GroupConfigEntryBatchRemovedEvent(firstRemovedEntry)); // NOTE: htt, 恢复成员变更请求
        }
    }

    @Override
    public void advanceCommitIndex(int newCommitIndex, int currentTerm) {
        if (!validateNewCommitIndex(newCommitIndex, currentTerm)) { // NOTE: htt, 检查commit index对应的term是在当前的 term，保证master在当前term下更新index
            return;
        }
        logger.debug("advance commit index from {} to {}", commitIndex, newCommitIndex);
        entrySequence.commit(newCommitIndex); // NOTE: htt, 日志条目更新commit内容，包括刷盘到commit文件以及更新该值
        groupConfigsCommitted(newCommitIndex); // NOTE: htt, 处理成员变更commit，包括新增或删除节点
        commitIndex = newCommitIndex; // NOTE: htt, 更新commit index

        advanceApplyIndex(); // NOTE: htt, 推进状态机中的内容
    }

    @Override
    public void generateSnapshot(int lastIncludedIndex, Set<NodeEndpoint> groupConfig) {
        logger.info("generate snapshot, last included index {}", lastIncludedIndex);
        EntryMeta lastAppliedEntryMeta = entrySequence.getEntryMeta(lastIncludedIndex);
        replaceSnapshot(generateSnapshot(lastAppliedEntryMeta, groupConfig)); // NOTE: htt, 生成 baseDir/generating 日志快照， 并替换为 baseDir/log-xxx
    }

    private void advanceApplyIndex() { // NOTE: htt, 推进状态机中的index
        // start up and snapshot exists
        int lastApplied = stateMachine.getLastApplied();
        int lastIncludedIndex = snapshot.getLastIncludedIndex();
        if (lastApplied == 0 && lastIncludedIndex > 0) {
            assert commitIndex >= lastIncludedIndex;
            applySnapshot(snapshot);
            lastApplied = lastIncludedIndex;
        }
        for (Entry entry : entrySequence.subList(lastApplied + 1, commitIndex + 1)) {
            applyEntry(entry); // NOTE: htt, state machine推进数据至当前的 commitIndex
        }
    }

    private void applySnapshot(Snapshot snapshot) {
        logger.debug("apply snapshot, last included index {}", snapshot.getLastIncludedIndex());
        try {
            stateMachine.applySnapshot(snapshot); // NOTE: htt, 应用日志到状态机
        } catch (IOException e) {
            throw new LogException("failed to apply snapshot", e);
        }
    }

    private void applyEntry(Entry entry) { // NOTE: htt, 应用日志条目到状态机
        // skip no-op entry and membership-change entry
        if (isApplicable(entry)) { // NOTE: htt, 状态机仅添加 数据请求日志
            stateMachine.applyLog(stateMachineContext, entry.getIndex(), entry.getCommandBytes(), entrySequence.getFirstLogIndex());
        }
    }

    private boolean isApplicable(Entry entry) {
        return entry.getKind() == Entry.KIND_GENERAL;
    }

    private void groupConfigsCommitted(int newCommitIndex) {
        for (GroupConfigEntry groupConfigEntry : groupConfigEntryList.subList(commitIndex + 1, newCommitIndex + 1)) {

            // NOTE: htt, follower由于没有实际的groupConfigChangeTaskHolder，所以也不会进行相应操作，
            // 但是leader会根据groupConfigEntry 执行相应节点添加或删除后的commit操作 GroupConfigChangeTaskHolder.onLogCommitted
            // leader在删除节点为自己的时候，会将自己将为不带选主的follower，这样不会干扰集群，集群可以继续选主
            eventBus.post(new GroupConfigEntryCommittedEvent(groupConfigEntry)); // NOTE: htt, 处理成员变更commit事件，
        }
    }

    private boolean validateNewCommitIndex(int newCommitIndex, int currentTerm) {
        if (newCommitIndex <= commitIndex) {
            return false;
        }
        Entry entry = entrySequence.getEntry(newCommitIndex);
        if (entry == null) {
            logger.debug("log of new commit index {} not found", newCommitIndex);
            return false;
        }
        if (entry.getTerm() != currentTerm) { // NOTE: htt, 如果不是当前term，则不允许推进commit，保证数据不会回滚
            logger.debug("log term of new commit index != current term ({} != {})", entry.getTerm(), currentTerm);
            return false;
        }
        return true;
    }

    protected abstract Snapshot generateSnapshot(EntryMeta lastAppliedEntryMeta, Set<NodeEndpoint> groupConfig); // NOTE: htt, 生成本地快照

    @Override
    public InstallSnapshotState installSnapshot(InstallSnapshotRpc rpc) {
        if (rpc.getLastIndex() <= snapshot.getLastIncludedIndex()) {
            logger.debug("snapshot's last included index from rpc <= current one ({} <= {}), ignore",
                    rpc.getLastIndex(), snapshot.getLastIncludedIndex());
            return new InstallSnapshotState(InstallSnapshotState.StateName.ILLEGAL_INSTALL_SNAPSHOT_RPC);
        }
        if (rpc.getOffset() == 0) { // NOTE: htt, 生成新的快照请求
            assert rpc.getLastConfig() != null;
            snapshotBuilder.close();
            snapshotBuilder = newSnapshotBuilder(rpc);
        } else {
            snapshotBuilder.append(rpc); // NOTE: htt, 继续添加快照安装请求
        }
        if (!rpc.isDone()) {
            return new InstallSnapshotState(InstallSnapshotState.StateName.INSTALLING); // NOTE: htt, 回复安装中
        }
        Snapshot newSnapshot = snapshotBuilder.build();
        applySnapshot(newSnapshot); // NOTE: htt, 应用快照到状态机中
        replaceSnapshot(newSnapshot); // NOTE: htt, 替换 baseDir/installing 为 baseDir/log-xxx
        int lastIncludedIndex = snapshot.getLastIncludedIndex();
        if (commitIndex < lastIncludedIndex) {
            commitIndex = lastIncludedIndex; // NOTE: htt, 更新commitIndex, TODO: htt, master必须确保快照中的 lastIncludeIndex <= commitIndex
        }
        return new InstallSnapshotState(InstallSnapshotState.StateName.INSTALLED, newSnapshot.getLastConfig()); // NOTE: htt, 快照以安装完
    }

    protected abstract SnapshotBuilder newSnapshotBuilder(InstallSnapshotRpc firstRpc);

    protected abstract void replaceSnapshot(Snapshot newSnapshot); // NOTE: htt, 替换快照

    @Override
    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Override
    public void close() { // NOTE: htt, 关闭日志
        snapshot.close();
        entrySequence.close();
        snapshotBuilder.close();
        stateMachine.shutdown();
    }

    private class StateMachineContextImpl implements StateMachineContext { // NOTE: htt, 状态机Conntext，用于将将生成快照的index传递给快照处理

        @Override
        public void generateSnapshot(int lastIncludedIndex) { // NOTE: htt, 状态机Conntext，用于将将生成快照的index传递给快照处理
            eventBus.post(new SnapshotGenerateEvent(lastIncludedIndex)); // NOTE: htt, 通过EventBus发送快照生成消息
        }

    }

    private static class EntrySequenceView implements Iterable<Entry> { // NOTE: htt, 日志条目序列视图

        private final List<Entry> entries; // NOTE: htt, 日志条目视图中条目列表
        private int firstLogIndex; // NOTE: htt, 第一条请求
        private int lastLogIndex; // NOTE: htt, 最后一条请求

        EntrySequenceView(List<Entry> entries) {
            this.entries = entries;
            if (!entries.isEmpty()) {
                firstLogIndex = entries.get(0).getIndex();
                lastLogIndex = entries.get(entries.size() - 1).getIndex();
            }
        }

        Entry get(int index) {
            if (entries.isEmpty() || index < firstLogIndex || index > lastLogIndex) {
                return null;
            }
            return entries.get(index - firstLogIndex);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        int getFirstLogIndex() {
            return firstLogIndex;
        }

        int getLastLogIndex() {
            return lastLogIndex;
        }

        EntrySequenceView subView(int fromIndex) {
            if (entries.isEmpty() || fromIndex > lastLogIndex) {
                return new EntrySequenceView(Collections.emptyList());
            }
            return new EntrySequenceView(
                    entries.subList(fromIndex - firstLogIndex, entries.size())
            );
        }

        @Override
        @Nonnull
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }

    }

}
