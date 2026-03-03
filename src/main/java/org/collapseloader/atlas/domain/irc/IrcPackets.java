package org.collapseloader.atlas.domain.irc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class IrcPackets {
    private IrcPackets() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IncomingPacket {
        private String op;
        private String token;
        private String type;
        private String client;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OutgoingPacket {
        private String type;
        private String id;
        private String time;
        private SenderInfo sender;
        private String content;
        private Boolean history;
        @JsonProperty("room_state")
        private RoomState roomState;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SenderInfo {
        private String username;
        private String role;
        @JsonProperty("user_id")
        private String userId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomState {
        @JsonProperty("online_users")
        private int onlineUsers;
        @JsonProperty("online_guests")
        private int onlineGuests;
    }
}
