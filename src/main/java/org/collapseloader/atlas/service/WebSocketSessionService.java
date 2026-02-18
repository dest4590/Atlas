package org.collapseloader.atlas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketSessionService {
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, Boolean> activeSessions = new ConcurrentHashMap<>();
    private final AtomicInteger userCount = new AtomicInteger(0);
    private final AtomicInteger guestCount = new AtomicInteger(0);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        boolean isGuest = headerAccessor.getUser() == null;

        if (sessionId != null && !activeSessions.containsKey(sessionId)) {
            activeSessions.put(sessionId, isGuest);
            if (isGuest) {
                guestCount.incrementAndGet();
            } else {
                userCount.incrementAndGet();
            }
            broadcastCount();
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId != null) {
            Boolean isGuest = activeSessions.remove(sessionId);
            if (isGuest != null) {
                if (isGuest) {
                    guestCount.decrementAndGet();
                } else {
                    userCount.decrementAndGet();
                }
                broadcastCount();
            }
        }
    }

    public int getUserCount() {
        return userCount.get();
    }

    public int getGuestCount() {
        return guestCount.get();
    }

    public int getTotalCount() {
        return userCount.get() + guestCount.get();
    }

    private void broadcastCount() {
        Map<String, Integer> counts = Map.of(
                "users", userCount.get(),
                "guests", guestCount.get(),
                "total", getTotalCount()
        );
        messagingTemplate.convertAndSend("/topic/online-count", counts);
    }
}
