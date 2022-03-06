package in.xnnyygn.xraft.kvstore.message;

import in.xnnyygn.xraft.core.node.NodeId;

public class Redirect { // NOTE: htt, 提供重定向leaderid信息

    private final String leaderId; // NOTE: htt, leaderid，重定向的

    public Redirect(NodeId leaderId) {
        this(leaderId != null ? leaderId.getValue() : null);
    }

    public Redirect(String leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        return "Redirect{" + "leaderId=" + leaderId + '}';
    }

}
