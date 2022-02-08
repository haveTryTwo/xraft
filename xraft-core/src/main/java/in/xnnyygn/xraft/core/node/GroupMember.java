package in.xnnyygn.xraft.core.node;

/**
 * State of group member.
 *
 * @see ReplicatingState
 */
class GroupMember { // NOTE: htt, group成员，包括节点信息，复制状态

    private final NodeEndpoint endpoint; // NOTE: htt, 节点信息， 包括 <id, <host, port> >
    private ReplicatingState replicatingState; // NOTE: htt, 复制状态，包括 next index 和 match index
    private boolean major; // NOTE: htt, 当前是为系统内可用，即已上线，不过才该字段这里是命名是否合适，是否可以使用状态上线来标记？
    private boolean removing = false;

    GroupMember(NodeEndpoint endpoint) {
        this(endpoint, null, true);
    }

    GroupMember(NodeEndpoint endpoint, ReplicatingState replicatingState, boolean major) {
        this.endpoint = endpoint;
        this.replicatingState = replicatingState;
        this.major = major;
    }

    NodeEndpoint getEndpoint() {
        return endpoint;
    }

    NodeId getId() {
        return endpoint.getId();
    }

    boolean idEquals(NodeId id) {
        return endpoint.getId().equals(id);
    }

    void setReplicatingState(ReplicatingState replicatingState) {
        this.replicatingState = replicatingState;
    }

    boolean isReplicationStateSet() {
        return replicatingState != null;
    }

    private ReplicatingState ensureReplicatingState() {
        if (replicatingState == null) {
            throw new IllegalStateException("replication state not set");
        }
        return replicatingState;
    }

    boolean isMajor() {
        return major;
    }

    void setMajor(boolean major) {
        this.major = major;
    }

    boolean isRemoving() {
        return removing;
    }

    void setRemoving() {
        removing = true;
    }

    int getNextIndex() {
        return ensureReplicatingState().getNextIndex();
    }

    int getMatchIndex() {
        return ensureReplicatingState().getMatchIndex();
    }

    boolean advanceReplicatingState(int lastEntryIndex) {
        return ensureReplicatingState().advance(lastEntryIndex);
    }

    boolean backOffNextIndex() {
        return ensureReplicatingState().backOffNextIndex();
    }

    void replicateNow() {
        replicateAt(System.currentTimeMillis());
    }

    void replicateAt(long replicatedAt) { // NOTE: htt, 设置复制时间
        ReplicatingState replicatingState = ensureReplicatingState();
        replicatingState.setReplicating(true);
        replicatingState.setLastReplicatedAt(replicatedAt);
    }

    boolean isReplicating() {
        return ensureReplicatingState().isReplicating();
    }

    void stopReplicating() {
        ensureReplicatingState().setReplicating(false);
    }

    /**
     * Test if should replicate.
     * <p>
     * Return true if
     * <ol>
     * <li>not replicating</li>
     * <li>replicated but no response in specified timeout</li>
     * </ol>
     * </p>
     *
     * @param readTimeout read timeout
     * @return true if should, otherwise false
     */
    boolean shouldReplicate(long readTimeout) { // NOTE: htt, 当前不在复制状态中 或者 复制的时间已经超过 readTimeout 则 可以启动复制
        ReplicatingState replicatingState = ensureReplicatingState();
        return !replicatingState.isReplicating() ||
                System.currentTimeMillis() - replicatingState.getLastReplicatedAt() >= readTimeout; // NOTE: htt, 读超时时间范围内，继续等待
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "endpoint=" + endpoint +
                ", major=" + major +
                ", removing=" + removing +
                ", replicatingState=" + replicatingState +
                '}';
    }

}
