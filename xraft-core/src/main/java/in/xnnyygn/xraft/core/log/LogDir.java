package in.xnnyygn.xraft.core.log;

import java.io.File;

public interface LogDir { // NOTE: htt, 日志条目目录

    void initialize(); // NOTE: htt, 初始化目录

    boolean exists(); // NOTE: htt, 日志条目目录是否存在

    File getSnapshotFile(); // NOTE: htt, 日志快照

    File getEntriesFile(); // NOTE: htt, 日志条目数据文件

    File getEntryOffsetIndexFile(); // NOTE: htt, 日志条目索引文件

    File get();

    boolean renameTo(LogDir logDir);

}
