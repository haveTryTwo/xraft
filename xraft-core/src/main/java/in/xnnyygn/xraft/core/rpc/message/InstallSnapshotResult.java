package in.xnnyygn.xraft.core.rpc.message;

public class InstallSnapshotResult { // NOTE: htt, 快照安装的回包，TODO: htt，这里只有term，安装的结果

    private final int term; // NOTE: htt, 当前所在的term

    public InstallSnapshotResult(int term) {
        this.term = term;
    }

    public int getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return "InstallSnapshotResult{" +
                "term=" + term +
                '}';
    }

}
