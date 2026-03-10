package org.collapseloader.atlas.domain.irc;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class IrcMetrics {
    private final MeterRegistry meterRegistry;

    public IrcMetrics(MeterRegistry meterRegistry, IrcServerState state) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("atlas.irc.sessions.active", state, s -> s.snapshotUsers().size())
                .description("Current number of active IRC sessions")
                .register(meterRegistry);
    }

    public void recordChatMessage(String role, int length) {
        Tags tags = Tags.of("role", (role == null || role.isBlank()) ? "unknown" : role);
        meterRegistry.counter("atlas.irc.messages.total", tags).increment();
        DistributionSummary.builder("atlas.irc.message.length")
                .description("Length of IRC chat messages")
                .tags(tags)
                .register(meterRegistry)
                .record(length);
    }
}
