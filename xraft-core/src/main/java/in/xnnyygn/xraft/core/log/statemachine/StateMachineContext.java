package in.xnnyygn.xraft.core.log.statemachine;


public interface StateMachineContext { // NOTE: htt, 状态机内容

    void generateSnapshot(int lastIncludedIndex); // NOTE: htt, 产生快照，包含最后index

}
