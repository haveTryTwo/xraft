package in.xnnyygn.xraft.core.node.config;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public interface NodeConfigLoader { // NOTE: htt, 从文件中加载节点的配置信息

    @Nonnull
    NodeConfig load(@Nonnull InputStream input) throws IOException;

}
