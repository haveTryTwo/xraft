package in.xnnyygn.xraft.core.node;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import in.xnnyygn.xraft.core.log.FileLog;
import in.xnnyygn.xraft.core.log.Log;
import in.xnnyygn.xraft.core.log.MemoryLog;
import in.xnnyygn.xraft.core.node.config.NodeConfig;
import in.xnnyygn.xraft.core.node.store.FileNodeStore;
import in.xnnyygn.xraft.core.node.store.MemoryNodeStore;
import in.xnnyygn.xraft.core.node.store.NodeStore;
import in.xnnyygn.xraft.core.rpc.Connector;
import in.xnnyygn.xraft.core.rpc.nio.NioConnector;
import in.xnnyygn.xraft.core.schedule.DefaultScheduler;
import in.xnnyygn.xraft.core.schedule.Scheduler;
import in.xnnyygn.xraft.core.support.ListeningTaskExecutor;
import in.xnnyygn.xraft.core.support.TaskExecutor;
import io.netty.channel.nio.NioEventLoopGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;

/**
 * Node builder.
 */
public class NodeBuilder { // NOTE: htt, 生成 NodeContext 信息

    /**
     * Group.
     */
    private final NodeGroup group; // NOTE: htt, 节点列表，记录当前节点，以及当前集群中的节点列表

    /**
     * Self id.
     */
    private final NodeId selfId; // NOTE: htt, 节点id

    /**
     * Event bus, INTERNAL.
     */
    private final EventBus eventBus; // NOTE: htt, 队列发送

    /**
     * Node configuration.
     */
    private NodeConfig config = new NodeConfig(); // NOTE: htt, 节点的相关配置信息，主要是超时信息

    /**
     * Starts as standby or not.
     */
    private boolean standby = false;

    /**
     * Log.
     * If data directory specified, {@link FileLog} will be created.
     * Default to {@link MemoryLog}.
     */
    private Log log = null;

    /**
     * Store for current term and last node id voted for.
     * If data directory specified, {@link FileNodeStore} will be created.
     * Default to {@link MemoryNodeStore}.
     */
    private NodeStore store = null; // NOTE: htt, 节点存储，处理 term以及投票的节点，用于节点异常之后恢复时获取之前投票信息

    /**
     * Scheduler, INTERNAL.
     */
    private Scheduler scheduler = null;  // NOTE: htt, 选举调度，包括选举调度、日志同步调度

    /**
     * Connector, component to communicate between nodes, INTERNAL.
     */
    private Connector connector = null; // NOTE: htt, 连接器，可以用来获取连接并发送请求

    /**
     * Task executor for node, INTERNAL.
     */
    private TaskExecutor taskExecutor = null;

    /**
     * Task executor for group config change task, INTERNAL.
     */
    private TaskExecutor groupConfigChangeTaskExecutor = null;

    /**
     * Event loop group for worker.
     * If specified, reuse. otherwise create one.
     */
    private NioEventLoopGroup workerNioEventLoopGroup = null;

    // TODO add doc
    public NodeBuilder(@Nonnull NodeEndpoint endpoint) {
        this(Collections.singletonList(endpoint), endpoint.getId());
    }

    // TODO add doc
    public NodeBuilder(@Nonnull Collection<NodeEndpoint> endpoints, @Nonnull NodeId selfId) {
        Preconditions.checkNotNull(endpoints);
        Preconditions.checkNotNull(selfId);
        this.group = new NodeGroup(endpoints, selfId);
        this.selfId = selfId;
        this.eventBus = new EventBus(selfId.getValue());
    }

    /**
     * Create.
     *
     * @param selfId self id
     * @param group  group
     */
    @Deprecated
    public NodeBuilder(@Nonnull NodeId selfId, @Nonnull NodeGroup group) {
        Preconditions.checkNotNull(selfId);
        Preconditions.checkNotNull(group);
        this.selfId = selfId;
        this.group = group;
        this.eventBus = new EventBus(selfId.getValue());
    }

    /**
     * Set standby.
     *
     * @param standby standby
     * @return this
     */
    public NodeBuilder setStandby(boolean standby) {
        this.standby = standby;
        return this;
    }

    /**
     * Set configuration.
     *
     * @param config config
     * @return this
     */
    public NodeBuilder setConfig(@Nonnull NodeConfig config) {
        Preconditions.checkNotNull(config);
        this.config = config;
        return this;
    }

    /**
     * Set connector.
     *
     * @param connector connector
     * @return this
     */
    NodeBuilder setConnector(@Nonnull Connector connector) {
        Preconditions.checkNotNull(connector);
        this.connector = connector;
        return this;
    }

    /**
     * Set event loop for worker.
     * If specified, it's caller's responsibility to close worker event loop.
     *
     * @param workerNioEventLoopGroup worker event loop
     * @return this
     */
    public NodeBuilder setWorkerNioEventLoopGroup(@Nonnull NioEventLoopGroup workerNioEventLoopGroup) {
        Preconditions.checkNotNull(workerNioEventLoopGroup);
        this.workerNioEventLoopGroup = workerNioEventLoopGroup;
        return this;
    }

    /**
     * Set scheduler.
     *
     * @param scheduler scheduler
     * @return this
     */
    NodeBuilder setScheduler(@Nonnull Scheduler scheduler) {
        Preconditions.checkNotNull(scheduler);
        this.scheduler = scheduler;
        return this;
    }

    /**
     * Set task executor.
     *
     * @param taskExecutor task executor
     * @return this
     */
    NodeBuilder setTaskExecutor(@Nonnull TaskExecutor taskExecutor) {
        Preconditions.checkNotNull(taskExecutor);
        this.taskExecutor = taskExecutor;
        return this;
    }

    /**
     * Set group config change task executor.
     *
     * @param groupConfigChangeTaskExecutor group config change task executor
     * @return this
     */
    NodeBuilder setGroupConfigChangeTaskExecutor(@Nonnull TaskExecutor groupConfigChangeTaskExecutor) {
        Preconditions.checkNotNull(groupConfigChangeTaskExecutor);
        this.groupConfigChangeTaskExecutor = groupConfigChangeTaskExecutor;
        return this;
    }

    /**
     * Set store.
     *
     * @param store store
     * @return this
     */
    NodeBuilder setStore(@Nonnull NodeStore store) {
        Preconditions.checkNotNull(store);
        this.store = store;
        return this;
    }

    /**
     * Set data directory.
     *
     * @param dataDirPath data directory
     * @return this
     */
    public NodeBuilder setDataDir(@Nullable String dataDirPath) {
        if (dataDirPath == null || dataDirPath.isEmpty()) {
            return this;
        }
        File dataDir = new File(dataDirPath);
        if (!dataDir.isDirectory() || !dataDir.exists()) {
            throw new IllegalArgumentException("[" + dataDirPath + "] not a directory, or not exists");
        }
        log = new FileLog(dataDir, eventBus); // NOTE: htt, 根据指定的根目录 dataDirPath，然后读取 该目录下各个目录以及数据
        store = new FileNodeStore(new File(dataDir, FileNodeStore.FILE_NAME)); // NOTE: htt, 根目录 dataDirPath/node.bin 存储节点信息
        return this;
    }

    /**
     * Build node.
     *
     * @return node
     */
    @Nonnull
    public Node build() {
        return new NodeImpl(buildContext());
    }

    /**
     * Build context for node.
     *
     * @return node context
     */
    @Nonnull
    private NodeContext buildContext() {
        NodeContext context = new NodeContext();
        context.setGroup(group);
        context.setMode(evaluateMode());
        context.setLog(log != null ? log : new MemoryLog(eventBus));
        context.setStore(store != null ? store : new MemoryNodeStore());
        context.setSelfId(selfId);
        context.setConfig(config);
        context.setEventBus(eventBus);
        context.setScheduler(scheduler != null ? scheduler : new DefaultScheduler(config));
        context.setConnector(connector != null ? connector : createNioConnector());
        context.setTaskExecutor(taskExecutor != null ? taskExecutor : new ListeningTaskExecutor(
                Executors.newSingleThreadExecutor(r -> new Thread(r, "node"))
        ));
        // TODO share monitor  // NOTE: htt, 设置成员变化任务，采用监听任务机制
        context.setGroupConfigChangeTaskExecutor(groupConfigChangeTaskExecutor != null ? groupConfigChangeTaskExecutor :
                new ListeningTaskExecutor(Executors.newSingleThreadExecutor(r -> new Thread(r, "group-config-change"))));
        return context;
    }

    /**
     * Create nio connector.
     *
     * @return nio connector
     */
    @Nonnull
    private NioConnector createNioConnector() { // NOTE: htt, 创建 nioConnector
        int port = group.findSelf().getEndpoint().getPort();
        if (workerNioEventLoopGroup != null) {
            return new NioConnector(workerNioEventLoopGroup, selfId, eventBus, port, config.getLogReplicationInterval());
        }
        return new NioConnector(new NioEventLoopGroup(config.getNioWorkerThreads()), false,
                selfId, eventBus, port, config.getLogReplicationInterval());
    }

    /**
     * Evaluate mode.
     *
     * @return mode
     * @see NodeGroup#isStandalone()
     */
    @Nonnull
    private NodeMode evaluateMode() {
        if (standby) {
            return NodeMode.STANDBY; // NOTE: htt, 单机模式
        }
        if (group.isStandalone()) {
            return NodeMode.STANDALONE; // NOTE: htt, 单节点模式
        }
        return NodeMode.GROUP_MEMBER;
    }

}
