package org.collapseloader.atlas.domain.irc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class IrcNettyServer implements SmartLifecycle {

    private final ObjectMapper objectMapper;
    private final IrcSettings settings;
    private final IrcServerState state;
    private final IrcAuthService authService;
    private final IrcCommandService commandService;
    private final IrcModerationService moderationService;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private ScheduledExecutorService roomStateScheduler;
    private volatile boolean running;

    public IrcNettyServer(
            ObjectMapper objectMapper,
            IrcSettings settings,
            IrcServerState state,
            IrcAuthService authService,
            IrcCommandService commandService,
            IrcModerationService moderationService) {
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.state = state;
        this.authService = authService;
        this.commandService = commandService;
        this.moderationService = moderationService;
    }

    @Override
    public synchronized void start() {
        if (!settings.isEnabled() || running) {
            return;
        }

        bossGroup = new MultiThreadIoEventLoopGroup(Math.max(1, settings.getBossThreads()), NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(Math.max(0, settings.getWorkerThreads()),
                NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 8192)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ReadTimeoutHandler((int) settings.readTimeout().toSeconds()));
                            ch.pipeline().addLast(new LineBasedFrameDecoder(settings.getMaxFrameLength()));
                            ch.pipeline().addLast(new StringDecoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
                            ch.pipeline().addLast(
                                    new IrcChannelHandler(objectMapper, settings, state, authService, commandService, moderationService));
                        }
                    });

            serverChannel = bootstrap.bind(settings.getHost(), settings.getPort()).sync().channel();

            roomStateScheduler = Executors.newSingleThreadScheduledExecutor();
            roomStateScheduler.scheduleAtFixedRate(
                    state::broadcastRoomState,
                    settings.roomStateTick().toSeconds(),
                    settings.roomStateTick().toSeconds(),
                    TimeUnit.SECONDS);

            running = true;
            log.info("[STARTUP] IRC Netty server started on {}:{}", settings.getHost(), settings.getPort());
        } catch (Exception ex) {
            log.error("[FATAL] Error starting IRC Netty server", ex);
            stop();
            throw new IllegalStateException("Failed to start IRC Netty server", ex);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;

        if (roomStateScheduler != null) {
            roomStateScheduler.shutdownNow();
            roomStateScheduler = null;
        }

        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
