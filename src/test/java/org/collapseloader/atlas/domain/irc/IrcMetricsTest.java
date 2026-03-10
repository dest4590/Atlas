package org.collapseloader.atlas.domain.irc;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IrcMetricsTest {
    private MeterRegistry registry;
    private IrcServerState state;
    private IrcMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        IrcSettings settings = mock(IrcSettings.class);
        when(settings.getHistoryLimit()).thenReturn(50);
        IrcMetrics stubMetrics = mock(IrcMetrics.class);
        state = new IrcServerState(settings, stubMetrics);
        metrics = new IrcMetrics(registry, state);
    }

    @Test
    void gaugeReflectsActiveSessions() {
        assertEquals(0.0, registry.get("atlas.irc.sessions.active").gauge().value());

        io.netty.channel.Channel fakeChannel = mock(io.netty.channel.Channel.class);
        IrcSession session = new IrcSession(fakeChannel, p -> {
        }, "127.0.0.1", "uid", "", "client", "name", "user", true, "foo", false, false);
        state.register(session);
        assertEquals(1.0, registry.get("atlas.irc.sessions.active").gauge().value());

        state.unregister(session.getChannel());
        assertEquals(0.0, registry.get("atlas.irc.sessions.active").gauge().value());
    }

    @Test
    void broadcastInvokesMetricsForChat() {
        MeterRegistry localRegistry = new SimpleMeterRegistry();
        IrcMetrics stubMetrics = mock(IrcMetrics.class);
        IrcServerState localState = new IrcServerState(mock(IrcSettings.class), stubMetrics);
        new IrcMetrics(localRegistry, localState);

        IrcPackets.OutgoingPacket packet = IrcPackets.OutgoingPacket.builder()
                .type("chat")
                .content("hello")
                .sender(IrcPackets.SenderInfo.builder().role("user").build())
                .build();

        localState.broadcast(packet);
        Mockito.verify(stubMetrics).recordChatMessage("user", 5);
    }

    @Test
    void recordChatMessageAddsCountersAndSummaries() {
        metrics.recordChatMessage("user", 7);
        Counter c = registry.get("atlas.irc.messages.total").tag("role", "user").counter();
        assertEquals(1.0, c.count());

        DistributionSummary sum = registry.get("atlas.irc.message.length").tag("role", "user").summary();
        assertEquals(7.0, sum.totalAmount());
    }
}
