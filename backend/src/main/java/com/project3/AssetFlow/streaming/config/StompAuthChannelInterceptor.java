package com.project3.AssetFlow.streaming.config;

import com.project3.AssetFlow.identity.UserRepository;
import com.project3.AssetFlow.identity.securityConfig.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    static final String SESSION_USERNAME = "username";
    static final String SESSION_USER_ID  = "userId";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("STOMP CONNECT rejected: missing or malformed Authorization header");
                throw new MessageDeliveryException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            String username;
            try {
                // extractUsername uses JJWT's parseClaimsJws which validates expiry by default;
                // expired tokens throw ExpiredJwtException and are caught here
                username = jwtService.extractUsername(token);
            } catch (Exception e) {
                log.warn("STOMP CONNECT rejected: invalid token — {}", e.getMessage());
                throw new MessageDeliveryException("Invalid token");
            }

            UUID userId = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("STOMP CONNECT rejected: user '{}' not found", username);
                        return new MessageDeliveryException("User not found");
                    })
                    .getId();

            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            if (sessionAttrs == null) {
                log.warn("STOMP CONNECT: session attributes map is null — cannot store auth");
                throw new MessageDeliveryException("Session not initialized");
            }
            sessionAttrs.put(SESSION_USERNAME, username);
            sessionAttrs.put(SESSION_USER_ID, userId);

            // Attach a Principal so convertAndSendToUser can route to this session.
            // The name must match the first arg passed to convertAndSendToUser (String.valueOf(userId)).
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    String.valueOf(userId), null, Collections.emptyList()));

        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/alerts/")) {
                String pathSegment = destination.substring("/topic/alerts/".length());
                UUID subscribedUserId;
                try {
                    subscribedUserId = UUID.fromString(pathSegment);
                } catch (IllegalArgumentException e) {
                    log.warn("STOMP SUBSCRIBE rejected: invalid alert destination '{}'", destination);
                    throw new MessageDeliveryException("Invalid alert destination");
                }

                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                UUID sessionUserId = sessionAttrs != null ? (UUID) sessionAttrs.get(SESSION_USER_ID) : null;
                if (!subscribedUserId.equals(sessionUserId)) {
                    log.warn("STOMP SUBSCRIBE rejected: userId mismatch for destination '{}'", destination);
                    throw new MessageDeliveryException("Unauthorized: userId mismatch");
                }
            }
        }

        return message;
    }
}
