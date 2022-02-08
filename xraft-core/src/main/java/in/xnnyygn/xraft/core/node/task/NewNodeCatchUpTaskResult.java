package in.xnnyygn.xraft.core.node.task;

import org.omg.CORBA.TIMEOUT;

public class NewNodeCatchUpTaskResult { // NOTE: htt, 新node在 cache up 任务结果

    public static enum State { // NOTE: htt, 新node在 cache up 任务结果状态
        OK,
        TIMEOUT,
        REPLICATION_FAILED
    }

    private final State state; // NOTE: htt, catch up 状态
    private final int nextIndex;
    private final int matchIndex;

    public NewNodeCatchUpTaskResult(State state) {
        this.state = state;
        this.nextIndex = 0;
        this.matchIndex = 0;
    }

    public NewNodeCatchUpTaskResult(int nextIndex, int matchIndex) {
        this.state = State.OK;
        this.nextIndex = nextIndex;
        this.matchIndex = matchIndex;
    }

    public State getState() {
        return state;
    }

    public int getNextIndex() {
        return nextIndex;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

}