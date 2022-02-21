package in.xnnyygn.xraft.core.service;

import in.xnnyygn.xraft.core.node.NodeId;

public class RedirectException extends ChannelException { // NOTE: htt, 重定向信息

    private final NodeId leaderId; // NOTE: htt, 重定向信息，重定向的leaderId

    public RedirectException(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

}
