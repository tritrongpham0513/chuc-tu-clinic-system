package com.chuctupk.config;

import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Thiếu token đăng nhập");
            }
            String token = header.substring(7);
            if (!jwtTokenProvider.validateToken(token)) {
                throw new MessagingException("Phiên đăng nhập đã hết hạn");
            }
            String username = jwtTokenProvider.getUsernameFromJwt(token);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new MessagingException("Tài khoản không tồn tại"));
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new MessagingException("Tài khoản đã bị khoá");
            }
            if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF && user.getRole() != Role.DOCTOR) {
                throw new MessagingException("Bạn không có quyền kết nối kênh thông báo nội bộ");
            }
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            accessor.setUser(new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/staff/")) {
                if (!(accessor.getUser() instanceof Authentication auth) || auth.getAuthorities().stream()
                        .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_STAFF".equals(a.getAuthority()))) {
                    throw new MessagingException("Bạn không có quyền theo dõi kênh thông báo này");
                }
            }
        }
        return message;
    }
}
