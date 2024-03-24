package com.welcome.tteoksang.socket.interceptor;

import com.welcome.tteoksang.auth.exception.TokenInvalidException;
import com.welcome.tteoksang.auth.jwt.JWTUtil;
import com.welcome.tteoksang.game.service.RedisGameInfoService;
import com.welcome.tteoksang.redis.RedisPrefix;
import com.welcome.tteoksang.redis.RedisService;
import com.welcome.tteoksang.user.dto.User;
import com.welcome.tteoksang.user.exception.UserNotExistException;
import com.welcome.tteoksang.user.repository.UserRepository;
import com.welcome.tteoksang.user.service.GameInfoService;
import com.welcome.tteoksang.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Component
@Slf4j
public class InGameChannelInterceptor implements ChannelInterceptor {

    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final RedisGameInfoService redisGameInfoService;
    private final GameInfoService gameInfoService;

    private final Set<String> subscribedTopics = new HashSet<>();

    /**
     * 메시지를 보내기 전에 실행되는 인터셉터 메소드
     *
     * @param message 전송될 메시지. 이 메시지의 헤더에는 JWT 토큰이 포함되어 있어야 함
     * @param channel 메시지가 전송될 채널
     * @return 수정된 메시지를 반환(사용자 인증 정보가 추가된 메시지)
     */


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        // 핸드 셰이크에서 등록한 유저 정보 가져오기
        String userId = null;
        if (accessor != null) {
            // simpSessionAttributes에서 userId 속성 가져오기
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                userId = (String) sessionAttributes.get("userId");
                log.debug("핸드 셰이크 유저 아이디 : {}", userId);
            }
        }

        // CONNECT 요청 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("CONNECT MESSAGE START");
            /*
             웹에서 ws 테스트 시 유저 정보가 없기 때문에 자체적으로 Authorization Bearer로 설정해서 확인한다.
            */
            String authToken = accessor.getFirstNativeHeader("Authorization");

            if (authToken != null && authToken.startsWith("Bearer ")) {
                String jwtToken = authToken.split(" ")[1];
                try {
                    // 토큰 유효성 검사
                    if (!jwtUtil.isValid(jwtToken)) {
                        throw new JwtException("토큰이 만료되었습니다.");
                    }
                    //토큰에서 userId, role 획득
                    userId = jwtUtil.getUserId(jwtToken);
                    log.debug("유저 아이디 : {}", userId);
                } catch (JwtException e) {
                    e.printStackTrace();
                    throw new TokenInvalidException(e);
                }
            }
            //user를 생성하여 값 set
            User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new UserNotExistException("해당하는 유저가 없습니다."));

            //스프링 시큐리티 인증 토큰 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, null);
            // 사용자 정보 저장
            accessor.setUser(authentication);

            // 레디스에 유저 정보 저장
            userService.saveUserInfo(user);
            // DB에 있는 게임 데이터 불러오기
            gameInfoService.saveGameInfo(user.getUserId());
        }

        // 클라이언트 연결 해제 시
        else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            log.debug("DISCONNECT MESSAGE START");

            // 사용자 인증 정보 추출
            Authentication authentication = (Authentication) accessor.getUser();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {

                // 레디스에 있는 게임 데이터 저장
                redisGameInfoService.saveRedisGameInfo(user.getUserId());

                // 레디스에 있는 유저 정보 지우기
                String userInfoKey = RedisPrefix.USERINFO.prefix() + user.getUserId();
                if (redisService.hasKey(userInfoKey)) {
                    redisService.deleteValues(userInfoKey);
                    log.debug("유저 정보 제거");
                }
            }
        }
        // 구독시
        else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // 메시지의 목적지(destination)을 가져와 private시 webSocketId가 유효성 검사 실시
            // 사용자 인증 정보 추출
            Authentication authentication = (Authentication) accessor.getUser();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                String destination = accessor.getDestination();
                log.debug("-----{}",accessor.getDestination());
                if (destination != null && destination.startsWith("/topic/private/")) {
                    // WebSocket ID 추출
                    String webSocketId = destination.substring("/topic/private/".length());

                    // Redis에서 webSocketId의 유효성 검증
                    String webSocketKey = RedisPrefix.WEBSOCKET.prefix() + user.getUserId();

                    // 레디스에서 webSocketId가 있는지 확인
                    if (!redisService.hasKey(webSocketKey)) {
                        log.debug("webSocketId 없음");
                        // 에러 처리
                    }
                    // 클라이언트가 이미 해당 토픽을 구독한 경우에는 처리하지 않음
                    if (subscribedTopics.contains(webSocketId)) {
                        log.debug("이미 구독된 토픽입니다.");
                        // 중복 구독 처리 또는 에러 처리
                        return null; // 구독을 거부하고 메시지 처리를 종료
                    }

                    // 구독 상태 갱신
                    subscribedTopics.add(webSocketId);
                }
            }
        }
        log.debug("{} MESSAGE END", accessor.getCommand());
        return message;
    }
}
