package in.xnnyygn.xraft.core.rpc;

import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.message.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MockConnector extends ConnectorAdapter { // NOTE: htt, mock connector，采用内存messages封装对应的消息，用于测试，TODO:可以移到测试

    private LinkedList<Message> messages = new LinkedList<>();

    @Override
    public void sendRequestVote(@Nonnull RequestVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints) {
        Message m = new Message();
        m.rpc = rpc;
        messages.add(m);
    }

    @Override
    public void replyRequestVote(@Nonnull RequestVoteResult result, @Nonnull RequestVoteRpcMessage rpcMessage) {
        Message m = new Message();
        m.result = result;
        m.destinationNodeId = rpcMessage.getSourceNodeId();
        messages.add(m);
    }

    @Override
    public void sendAppendEntries(@Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
        Message m = new Message();
        m.rpc = rpc;
        m.destinationNodeId = destinationEndpoint.getId();
        messages.add(m);
    }

    @Override
    public void replyAppendEntries(@Nonnull AppendEntriesResult result, @Nonnull AppendEntriesRpcMessage rpcMessage) {
        Message m = new Message();
        m.result = result;
        m.destinationNodeId = rpcMessage.getSourceNodeId();
        messages.add(m);
    }

    @Override
    public void sendInstallSnapshot(@Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {
        Message m = new Message();
        m.rpc = rpc;
        m.destinationNodeId = destinationEndpoint.getId();
        messages.add(m);
    }

    @Override
    public void replyInstallSnapshot(@Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage) {
        Message m = new Message();
        m.result = result;
        m.destinationNodeId = rpcMessage.getSourceNodeId();
        messages.add(m);
    }

    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    } // NOTE: htt, 最后一条消息

    private Message getLastMessageOrDefault() {
        return messages.isEmpty() ? new Message() : messages.getLast();
    }

    public Object getRpc() {
        return getLastMessageOrDefault().rpc;
    }

    public Object getResult() {
        return getLastMessageOrDefault().result;
    }

    public NodeId getDestinationNodeId() {
        return getLastMessageOrDefault().destinationNodeId;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void clearMessage() {
        messages.clear();
    }

    public static class Message { // NOTE: 请求消息，可以使用泛型

        private Object rpc; // NOTE: htt, raft 的rpc请求
        private NodeId destinationNodeId; // NOTE: htt, 指定的单个目标节点id
        private Object result; // NOTE: htt, 结果

        public Object getRpc() {
            return rpc;
        }

        public NodeId getDestinationNodeId() {
            return destinationNodeId;
        }

        public Object getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "destinationNodeId=" + destinationNodeId +
                    ", rpc=" + rpc +
                    ", result=" + result +
                    '}';
        }

    }

}
