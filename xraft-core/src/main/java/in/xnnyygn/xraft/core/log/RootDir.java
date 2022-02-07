package in.xnnyygn.xraft.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * NOTE: htt, 根目录，相应目录结构
 *    baseDir
 *       |- log-0
 *             |- entries.bin
 *             |- entries.idx
 *             |- service.ss
 *       |- log-111
 *             |- entries.bin
 *             |- entries.idx
 *             |- service.ss
 *       |- generating  （本地进程生成快照)
 *             |- entries.bin
 *             |- entries.idx
 *             |- service.ss
 *       |- installing （从主获取快照）
 *             |- entries.bin
 *             |- entries.idx
 *             |- service.ss
 */
class RootDir { // NOTE: htt, 根目录，以及整体的目录结构

    static final String FILE_NAME_SNAPSHOT = "service.ss"; // NOTE: htt, 快照文件
    static final String FILE_NAME_ENTRIES = "entries.bin"; // NOTE: htt, 日志条目数据文件
    static final String FILE_NAME_ENTRY_OFFSET_INDEX = "entries.idx"; // NOTE: htt, 日志条目索引文件
    private static final String DIR_NAME_GENERATING = "generating"; // NOTE: htt,
    private static final String DIR_NAME_INSTALLING = "installing";

    private static final Logger logger = LoggerFactory.getLogger(RootDir.class);
    private final File baseDir; // NOTE: htt, 根目录

    RootDir(File baseDir) {
        if (!baseDir.exists()) { // NOTE: htt, baseDir目录必须存在
            throw new IllegalArgumentException("dir " + baseDir + " not exists");
        }
        this.baseDir = baseDir;
    }

    LogDir getLogDirForGenerating() {
        return getOrCreateNormalLogDir(DIR_NAME_GENERATING); // NOTE: htt, 在根目录下创建 generating 快照目录
    }

    LogDir getLogDirForInstalling() {
        return getOrCreateNormalLogDir(DIR_NAME_INSTALLING); // NOTE: htt, 在根目录下创建 installing 快照目录
    }

    private NormalLogDir getOrCreateNormalLogDir(String name) {
        NormalLogDir logDir = new NormalLogDir(new File(baseDir, name));
        if (!logDir.exists()) {
            logDir.initialize(); // NOTE: htt, 不存在则初始化
        }
        return logDir;
    }

    LogDir rename(LogDir dir, int lastIncludedIndex) { // NOTE: htt,将dir重命名为 baseDir/log-xxxx (其中xxxx为最新index)目录
        LogGeneration destDir = new LogGeneration(baseDir, lastIncludedIndex);
        if (destDir.exists()) {
            throw new IllegalStateException("failed to rename, dest dir " + destDir + " exists");
        }

        logger.info("rename dir {} to {}", dir, destDir);
        if (!dir.renameTo(destDir)) {
            throw new IllegalStateException("failed to rename " + dir + " to " + destDir);
        }
        return destDir;
    }

    LogGeneration createFirstGeneration() { // NOTE: htt, 创建第一个generation目录， baseDir/log-0
        LogGeneration generation = new LogGeneration(baseDir, 0); // NOTE: htt, baseDir/log-0
        generation.initialize(); // NOTE: htt, baseDir/log-0/entries.bin  baseDir/log-0/entries.idx
        return generation;
    }

    LogGeneration getLatestGeneration() { // NOTE: htt, 获取xxx最大的 baseDir/log-xxx 目录
        File[] files = baseDir.listFiles();
        if (files == null) {
            return null;
        }
        LogGeneration latest = null;
        String fileName;
        LogGeneration generation;
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            fileName = file.getName();
            if (DIR_NAME_GENERATING.equals(fileName) || DIR_NAME_INSTALLING.equals(fileName) ||
                    !LogGeneration.isValidDirName(fileName)) { // NOTE: htt, generating/installing/ 非 log-xxx 目录，则继续
                continue;
            }
            generation = new LogGeneration(file); // NOTE: htt, log-xxx 目录
            if (latest == null || generation.compareTo(latest) > 0) {
                latest = generation;
            }
        }
        return latest;
    }

}
