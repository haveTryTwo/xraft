package in.xnnyygn.xraft.core.rpc;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Address { // NOTE: htt,  host 和 port信息

    private final String host; // NOTE: htt, host 即ip
    private final int port; // NOTE: htt, 端口

    /**
     * Create.
     *
     * @param host host
     * @param port port
     */
    public Address(@Nonnull String host, int port) {
        Preconditions.checkNotNull(host);
        this.host = host;
        this.port = port;
    }

    /**
     * Get host.
     *
     * @return host
     */
    @Nonnull
    public String getHost() {
        return host;
    }

    /**
     * Get port.
     *
     * @return port
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Address{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

}
