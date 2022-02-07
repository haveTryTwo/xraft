package in.xnnyygn.xraft.core.log;

import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.log.entry.Entry;
import in.xnnyygn.xraft.core.log.entry.EntryMeta;
import in.xnnyygn.xraft.core.log.sequence.EntrySequence;
import in.xnnyygn.xraft.core.log.sequence.FileEntrySequence;
import in.xnnyygn.xraft.core.log.snapshot.*;
import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@NotThreadSafe
public class FileLog extends AbstractLog { // NOTE: htt, 日志条目数据管理，包括快照+日志条目数据（日志数据+日志索引），以及数据应用到状态机，并支持目录轮换 baseDir/log-xxx

    private final RootDir rootDir; // NOTE: htt, 日志的根目录

    public FileLog(File baseDir, EventBus eventBus) { // NOTE: htt, 如果 baseDir/log-xxx存在，则加载 快照/日志数据文件/日志条目索引文件； 如果不存在，则创建 baseDir/log-0 并创建 日志数据文件和日志条目索引文件
        super(eventBus);
        rootDir = new RootDir(baseDir);

        LogGeneration latestGeneration = rootDir.getLatestGeneration(); // NOTE: htt, 获取最新的日志分代 baseDir/log-xxx
        snapshot = new EmptySnapshot(); // NOTE: htt, 空的快照信息
        // TODO add log
        if (latestGeneration != null) { // NOTE: htt, log-xxx 目录存在，此时使用的最新的log-xxx目录
            if (latestGeneration.getSnapshotFile().exists()) {
                snapshot = new FileSnapshot(latestGeneration); // NOTE: htt, 生成快照文件的内存对象
            }
            FileEntrySequence fileEntrySequence = new FileEntrySequence(latestGeneration, snapshot.getLastIncludedIndex() + 1); // NOTE: htt, 文件日志读取序列
            commitIndex = fileEntrySequence.getCommitIndex(); // NOTE: htt, 提交index
            entrySequence = fileEntrySequence; // NOTE: htt, 日志条目序列的内存对象
            // TODO apply last group config entry
            groupConfigEntryList = entrySequence.buildGroupConfigEntryList(); // NOTE: htt, 构建配置变化list（增加或减少节点的配置列表）
        } else {
            LogGeneration firstGeneration = rootDir.createFirstGeneration(); // NOTE: htt, 创建第一个 logBase/log-0 目录，以及 baseDir/log-0/entries.bin  baseDir/log-0/entries.idx
            entrySequence = new FileEntrySequence(firstGeneration, 1); // NOTE: htt, 当前目录下 第一个日志序列
        }
    }

    @Override
    protected Snapshot generateSnapshot(EntryMeta lastAppliedEntryMeta, Set<NodeEndpoint> groupConfig) {
        LogDir logDir = rootDir.getLogDirForGenerating(); // NOTE: htt, baseDir/generating/
        try (FileSnapshotWriter snapshotWriter = new FileSnapshotWriter( // NOTE: htt, 写入快照文件头，即baseDir/generaing/service.ss 文件
                logDir.getSnapshotFile(), lastAppliedEntryMeta.getIndex(), lastAppliedEntryMeta.getTerm(), groupConfig)) {
            stateMachine.generateSnapshot(snapshotWriter.getOutput()); // NOTE: htt, 通过状态机来生成快照，即从状态机获取当前最新的快照数据, TODO: htt, 锁的问题
        } catch (IOException e) {
            throw new LogException("failed to generate snapshot", e);
        }
        return new FileSnapshot(logDir); // NOTE: htt, 读取快照，即 baseDir/generaing/service.ss 文件
    }

    @Override
    protected SnapshotBuilder newSnapshotBuilder(InstallSnapshotRpc firstRpc) { // NOTE: htt, 生成快照builder，并写入install请求中数据
        return new FileSnapshotBuilder(firstRpc, rootDir.getLogDirForInstalling()); // NOTE: htt, 生成快照安装baseDir/installing/services.ss，并写入安装请求中的快照数据
    }

    @Override
    protected void replaceSnapshot(Snapshot newSnapshot) { // NOTE: htt, 置换快照目录，将 baseDir/generating 命名为新的 baseDir/log-xxx，并重新打开
        FileSnapshot fileSnapshot = (FileSnapshot) newSnapshot;
        int lastIncludedIndex = fileSnapshot.getLastIncludedIndex();
        int logIndexOffset = lastIncludedIndex + 1;

        List<Entry> remainingEntries = entrySequence.subView(logIndexOffset);
        EntrySequence newEntrySequence = new FileEntrySequence(fileSnapshot.getLogDir(), logIndexOffset);
        newEntrySequence.append(remainingEntries); // NOTE: htt, 追加原有 baseDir/log-xxx 目录下日志数据
        newEntrySequence.commit(Math.max(commitIndex, lastIncludedIndex)); // NOTE: htt, 推进新的 baseDir/generating 目录commitIndex至当前的位置
        newEntrySequence.close(); // NOTE: htt, 关闭日志目录描述符（包括数据和索引）

        snapshot.close();
        entrySequence.close(); // NOTE: htt, 关闭当前 baseDir/log-xxx 目录的快照和日志（数据和index）文件
        newSnapshot.close(); // NOTE: htt, 关闭新的快照目录下 快照文件

        LogDir generation = rootDir.rename(fileSnapshot.getLogDir(), lastIncludedIndex); // NOTE: htt, 重命名baseDir/generating 到 baseDir/log-xxx 目录
        snapshot = new FileSnapshot(generation); // NOTE: htt, 打开 baseDir/log-xxx/service.ss 快照文件
        entrySequence = new FileEntrySequence(generation, logIndexOffset); // NOTE: htt, 打开 baseDir/log-xxx/entries.bin 和 entries.idx数据文件
        groupConfigEntryList = entrySequence.buildGroupConfigEntryList(); // NOTE: htt, 获取成员操作的相关日志
        commitIndex = entrySequence.getCommitIndex(); // NOTE: htt, 变更当前的 commitIndex
    }

}
