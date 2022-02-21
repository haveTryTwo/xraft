package in.xnnyygn.xraft.core.service;

import in.xnnyygn.xraft.core.node.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ServerRouter { // NOTE: htt, 服务端路由

    private static Logger logger = LoggerFactory.getLogger(ServerRouter.class);
    private final Map<NodeId, Channel> availableServers = new HashMap<>(); // NOTE: htt, <节点， 管道> map
    private NodeId leaderId; // NOTE: htt, leaderid信息

    public Object send(Object payload) { // NOTE: htt, 发送 payload信息到leader
        for (NodeId nodeId : getCandidateNodeIds()) {
            try {
                Object result = doSend(nodeId, payload);
                this.leaderId = nodeId;
                return result;
            } catch (RedirectException e) {
                logger.debug("not a leader server, redirect to server {}", e.getLeaderId());
                this.leaderId = e.getLeaderId(); // NOTE: htt, 获取重定向异常，则再次发送信息到对应leader上
                return doSend(e.getLeaderId(), payload);
            } catch (Exception e) {
                logger.debug("failed to process with server " + nodeId + ", cause " + e.getMessage());
            }
        }
        throw new NoAvailableServerException("no available server");
    }

    private Collection<NodeId> getCandidateNodeIds() { // NOTE: htt, 获取 candidate 节点id信息
        if (availableServers.isEmpty()) {
            throw new NoAvailableServerException("no available server");
        }

        if (leaderId != null) {
            List<NodeId> nodeIds = new ArrayList<>();
            nodeIds.add(leaderId);
            for (NodeId nodeId : availableServers.keySet()) {
                if (!nodeId.equals(leaderId)) {
                    nodeIds.add(nodeId);
                }
            }
            return nodeIds;
        }

        return availableServers.keySet();
    }

    private Object doSend(NodeId id, Object payload) { // NOTE: htt, 向指定的id 发送payload消息
        Channel channel = this.availableServers.get(id);
        if (channel == null) {
            throw new IllegalStateException("no such channel to server " + id);
        }
        logger.debug("send request to server {}", id);
        return channel.send(payload);
    }

    public void add(NodeId id, Channel channel) {
        this.availableServers.put(id, channel); // NOTE: htt, 添加<id, channel> 到map中
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(NodeId leaderId) {
        if (!availableServers.containsKey(leaderId)) {
            throw new IllegalStateException("no such server [" + leaderId + "] in list");
        }
        this.leaderId = leaderId;
    }

}
