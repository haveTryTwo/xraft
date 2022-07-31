package in.xnnyygn.xraft.kvstore.server;

import in.xnnyygn.xraft.core.node.Node;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server { // NOTE: htt, 启动服务，包括service和node，执行 状态机以及xraft后台服务

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final Node node; // NOTE: htt, xraft节点服务
    private final int port; // NOTE: htt, 状态机端口
    private final Service service; // NOTE: htt, service状态机服务
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(4);

    public Server(Node node, int port) {
        this.node = node;
        this.service = new Service(node);
        this.port = port;
    }

    public void start() throws Exception {
        this.node.start();

        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Encoder());
                        pipeline.addLast(new Decoder());
                        pipeline.addLast(new ServiceHandler(service));
                    }
                });
        logger.info("server started at port {}", this.port);
        serverBootstrap.bind(this.port);
    }

    public void stop() throws Exception {
        logger.info("stopping server");
        this.node.stop();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();
    }

}
