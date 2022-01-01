package in.xnnyygn.xraft.core.log.entry;

import in.xnnyygn.xraft.core.node.NodeEndpoint;

import java.util.Set;

public abstract class GroupConfigEntry extends AbstractEntry { // NOTE: htt, 成员组日志条目，用于增、减节点时使用

    private final Set<NodeEndpoint> nodeEndpoints; // NOTE: htt, 集群中节点列表信息

    protected GroupConfigEntry(int kind, int index, int term, Set<NodeEndpoint> nodeEndpoints) {
        super(kind, index, term);
        this.nodeEndpoints = nodeEndpoints;
    }

    public Set<NodeEndpoint> getNodeEndpoints() {
        return nodeEndpoints;
    }

    public abstract Set<NodeEndpoint> getResultNodeEndpoints();

}
