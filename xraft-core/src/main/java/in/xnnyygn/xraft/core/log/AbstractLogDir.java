package in.xnnyygn.xraft.core.log;

import in.xnnyygn.xraft.core.support.Files;

import java.io.File;
import java.io.IOException;

abstract class AbstractLogDir implements LogDir { // NOTE: htt, 抽象目录，会创建日志条目文件以及索引文件

    final File dir; // NOTE: htt, 子目录（在baseDir之下的目录）

    AbstractLogDir(File dir) {
        this.dir = dir;
    }

    @Override
    public void initialize() {
        if (!dir.exists() && !dir.mkdir()) {
            throw new LogException("failed to create directory " + dir);
        }
        try {
            Files.touch(getEntriesFile()); // NOTE: htt, 创建或修改日志条目数据文件
            Files.touch(getEntryOffsetIndexFile()); // NOTE: htt, 创建或修改日志条目索引文件
        } catch (IOException e) {
            throw new LogException("failed to create file", e);
        }
    }

    @Override
    public boolean exists() {
        return dir.exists();
    }

    @Override
    public File getSnapshotFile() {
        return new File(dir, RootDir.FILE_NAME_SNAPSHOT); // NOTE: htt, 获取快照文件路径
    }

    @Override
    public File getEntriesFile() {
        return new File(dir, RootDir.FILE_NAME_ENTRIES); // NOTE: htt, 获取 日志条目数据文件路径
    }

    @Override
    public File getEntryOffsetIndexFile() {
        return new File(dir, RootDir.FILE_NAME_ENTRY_OFFSET_INDEX); // NOTE: htt, 日志条目索引文件路径
    }

    @Override
    public File get() {
        return dir;
    }

    @Override
    public boolean renameTo(LogDir logDir) {
        return dir.renameTo(logDir.get()); // NOTE: htt, 获取目录名称
    }

}
