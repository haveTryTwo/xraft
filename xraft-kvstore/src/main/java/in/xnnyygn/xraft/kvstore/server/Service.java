package in.xnnyygn.xraft.kvstore.server;

import com.google.protobuf.ByteString;
import in.xnnyygn.xraft.core.log.statemachine.AbstractSingleThreadStateMachine;
import in.xnnyygn.xraft.core.node.task.GroupConfigChangeTaskReference;
import in.xnnyygn.xraft.core.node.Node;
import in.xnnyygn.xraft.core.node.role.RoleName;
import in.xnnyygn.xraft.core.node.role.RoleNameAndLeaderId;
import in.xnnyygn.xraft.core.service.AddNodeCommand;
import in.xnnyygn.xraft.core.service.RemoveNodeCommand;
import in.xnnyygn.xraft.kvstore.Protos;
import in.xnnyygn.xraft.kvstore.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

public class Service { // NOTE: htt, 状态机服务，用于实现状态机数据存储，并且调用node节点执行节点添加和删除，以及数据写入和读取

    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    private final Node node; // NOTE: htt, xraft节点，实现raft功能，包括选主
    private final ConcurrentMap<String, CommandRequest<?>> pendingCommands = new ConcurrentHashMap<>(); // NOTE: htt, <requestId, CommandRequest> 对应
    private Map<String, byte[]> map = new HashMap<>(); // NOTE: htt, 状态机，存储<key, value>数据，为业务提供服务

    public Service(Node node) {
        this.node = node;
        this.node.registerStateMachine(new StateMachineImpl()); // NOTE: htt, 注册状态机
    }

    public void addNode(CommandRequest<AddNodeCommand> commandRequest) {
        Redirect redirect = checkLeadership();
        if (redirect != null) {
            commandRequest.reply(redirect); // NOTE: htt, 如果重定向不为空，则返回重定向请求
            return;
        }

        AddNodeCommand command = commandRequest.getCommand();
        GroupConfigChangeTaskReference taskReference = this.node.addNode(command.toNodeEndpoint());
        awaitResult(taskReference, commandRequest);
    }

    private <T> void awaitResult(GroupConfigChangeTaskReference taskReference, CommandRequest<T> commandRequest) {
        try {
            switch (taskReference.getResult(3000L)) { // NOTE: htt, 等3s，获取节点添加情况
                case OK:
                    commandRequest.reply(Success.INSTANCE); // NOTE: htt, 回复成功
                    break;
                case TIMEOUT:
                    commandRequest.reply(new Failure(101, "timeout"));  // NOTE: htt, 回复超时
                    break;
                default:
                    commandRequest.reply(new Failure(100, "error")); // NOTE: htt, 回复错误
            }
        } catch (TimeoutException e) {
            commandRequest.reply(new Failure(101, "timeout"));
        } catch (InterruptedException ignored) {
            commandRequest.reply(new Failure(100, "error"));
        }
    }

    public void removeNode(CommandRequest<RemoveNodeCommand> commandRequest) {
        Redirect redirect = checkLeadership();
        if (redirect != null) {
            commandRequest.reply(redirect);
            return;
        }

        RemoveNodeCommand command = commandRequest.getCommand();
        GroupConfigChangeTaskReference taskReference = node.removeNode(command.getNodeId()); // NOTE: htt, 删除节点，
        awaitResult(taskReference, commandRequest); // NOTE: htt, 等待删除
    }

    public void set(CommandRequest<SetCommand> commandRequest) {
        Redirect redirect = checkLeadership();
        if (redirect != null) {
            commandRequest.reply(redirect);
            return;
        }

        SetCommand command = commandRequest.getCommand();
        logger.debug("set {}", command.getKey());
        this.pendingCommands.put(command.getRequestId(), commandRequest); // NOTE: htt, 将 set命令 添加到 pendingCommands，等commit后再回包
        commandRequest.addCloseListener(() -> pendingCommands.remove(command.getRequestId()));
        this.node.appendLog(command.toBytes()); // NOTE: htt, 主节点添加日志
    }

    public void get(CommandRequest<GetCommand> commandRequest) {
        String key = commandRequest.getCommand().getKey();
        logger.debug("get {}", key);
        byte[] value = this.map.get(key); // NOTE: htt, 直接从状态机提供服务
        // TODO view from node state machine
        commandRequest.reply(new GetCommandResponse(value));
    }

    private Redirect checkLeadership() { // NOTE: htt, 如果当前节点不是leader，则会生成一个重定向请求
        RoleNameAndLeaderId state = node.getRoleNameAndLeaderId(); // NOTE: htt, 获取leader信息
        if (state.getRoleName() != RoleName.LEADER) {
            return new Redirect(state.getLeaderId());
        }
        return null;
    }

    static void toSnapshot(Map<String, byte[]> map, OutputStream output) throws IOException { // NOTE: htt, 日志写入到快照中
        Protos.EntryList.Builder entryList = Protos.EntryList.newBuilder();
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            entryList.addEntries(
                    Protos.EntryList.Entry.newBuilder()
                            .setKey(entry.getKey())
                            .setValue(ByteString.copyFrom(entry.getValue())).build()
            );
        }
        entryList.build().writeTo(output); // NOTE: htt, 日志写入到快照中
        entryList.build().getSerializedSize(); // TODO: htt, no used?
    }

    static Map<String, byte[]> fromSnapshot(InputStream input) throws IOException { // NOTE: htt, 从快照中恢复 <key, value>
        Map<String, byte[]> map = new HashMap<>();
        Protos.EntryList entryList = Protos.EntryList.parseFrom(input); // NOTE: htt, 快照记录的直接就是<key,value>序列化后的二进制
        for (Protos.EntryList.Entry entry : entryList.getEntriesList()) {
            map.put(entry.getKey(), entry.getValue().toByteArray());
        }
        return map;
    }

    private class StateMachineImpl extends AbstractSingleThreadStateMachine { // NOTE: htt, 状态机，包括将commit日志写入到状态机map，已经快照到状态机，和通过状态即生成快照

        @Override
        protected void applyCommand(@Nonnull byte[] commandBytes) {
            SetCommand command = SetCommand.fromBytes(commandBytes);
            map.put(command.getKey(), command.getValue()); // NOTE: htt, 更新状态机
            CommandRequest<?> commandRequest = pendingCommands.remove(command.getRequestId());
            if (commandRequest != null) { // NOTE: htt, follower这里为空
                commandRequest.reply(Success.INSTANCE); // NOTE: htt, 主节点在commit后会执行给客户端回包
            }
        }

        @Override
        protected void doApplySnapshot(@Nonnull InputStream input) throws IOException {
            map = fromSnapshot(input); // NOTE: htt, 从快照中恢复状态机
        }

        @Override
        public boolean shouldGenerateSnapshot(int firstLogIndex, int lastApplied) {
            return lastApplied - firstLogIndex > 1; // NOTE: htt, lastApplied比日志中第一条日志大于1则需要产生快照， TODO：htt, 个数值可配置
        }

        @Override
        public void generateSnapshot(@Nonnull OutputStream output) throws IOException { // NOTE: htt, xraft生成快照时，会调用这里来生成对应的snapshot
            toSnapshot(map, output); // TODO: htt, map使用的竞争问题，是否要锁
        }

    }

}
