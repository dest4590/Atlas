package org.collapseloader.atlas.domain.irc;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Getter
public class IrcSettings {

    @Value("${irc.enabled:true}")
    private boolean enabled;

    @Value("${irc.host:0.0.0.0}")
    private String host;

    @Value("${irc.port:1338}")
    private int port;

    @Value("${irc.boss-threads:1}")
    private int bossThreads;

    @Value("${irc.worker-threads:0}")
    private int workerThreads;

    @Value("${irc.max-frame-length:16384}")
    private int maxFrameLength;

    @Value("${irc.history-limit:50}")
    private int historyLimit;

    @Value("${irc.room-state-tick-seconds:15}")
    private int roomStateTickSeconds;

    @Value("${irc.auth-timeout-seconds:30}")
    private int authTimeoutSeconds;

    @Value("${irc.read-timeout-seconds:120}")
    private int readTimeoutSeconds;

    @Value("${irc.data-dir:.}")
    private String dataDir;

    public Duration authTimeout() {
        return Duration.ofSeconds(Math.max(1, authTimeoutSeconds));
    }

    public Duration readTimeout() {
        return Duration.ofSeconds(Math.max(1, readTimeoutSeconds));
    }

    public Duration roomStateTick() {
        return Duration.ofSeconds(Math.max(5, roomStateTickSeconds));
    }
}
