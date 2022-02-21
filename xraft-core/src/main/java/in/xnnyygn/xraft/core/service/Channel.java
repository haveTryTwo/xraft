package in.xnnyygn.xraft.core.service;

public interface Channel { // NOTE: htt, 发送消息管道

    Object send(Object payload);

}
