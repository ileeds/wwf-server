package com.ileeds.wwf.configuration;

import com.ileeds.wwf.service.PlayerService;
import com.ileeds.wwf.service.PlayerSessionService;
import com.ileeds.wwf.service.RoomService;
import java.util.Map;
import java.util.Objects;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

  private static final String REMOTE_ADDRESS_ATTRIBUTE = "remoteAddress";

  private static class IpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
      attributes.put(WebSocketConfiguration.REMOTE_ADDRESS_ATTRIBUTE,
          request.getRemoteAddress().getAddress().getHostAddress());
      return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
  }

  @Value
  private static class CustomChannelInterceptor implements ChannelInterceptor {

    PlayerService playerService;
    PlayerSessionService playerSessionService;
    RoomService roomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
      StompHeaderAccessor accessor =
          MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      assert accessor != null;

      final var remoteAddress = (String) Objects.requireNonNull(accessor.getSessionAttributes())
          .get(WebSocketConfiguration.REMOTE_ADDRESS_ATTRIBUTE);

      if (CustomChannelInterceptor.isServerConnection(remoteAddress)) {
        return message;
      }

      if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        final var playerKey = accessor.getFirstNativeHeader("playerKey");
        final var roomKey = accessor.getFirstNativeHeader("roomKey");
        final var simpSessionId = (String) accessor.getHeader("simpSessionId");

        final var player = this.playerService.findPlayer(playerKey);
        if (player.isEmpty()) {
          return null;
        }

        if (!player.get().getRoomKey().equals(roomKey)) {
          return null;
        }

        try {
          this.playerSessionService.setPlayerSession(simpSessionId, player.get());
        } catch (PlayerSessionService.SessionExistsException e) {
          return null;
        }
      } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
        final var simpSessionId = (String) accessor.getHeader("simpSessionId");
        this.playerSessionService.findPlayerSession(simpSessionId).ifPresent(session -> {
          this.playerSessionService.deletePlayerSession(session);
          final var player = this.playerService.findPlayer(session.getPlayerKey());
          player.ifPresent(p -> {
            this.playerService.deletePlayer(p);
            final var roomKey = p.getRoomKey();
            final var remainingPlayers = this.playerService.findAllByRoomKey(roomKey);
            if (remainingPlayers.isEmpty()) {
              this.roomService.deleteRoom(roomKey);
            }
          });
        });
      }
      return message;
    }

    private static boolean isServerConnection(String remoteAddress) {
      return remoteAddress.equals("127.0.0.1");
    }
  }

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PlayerSessionService playerSessionService;

  @Autowired
  private RoomService roomService;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry
        .setApplicationDestinationPrefixes("/app")
        .enableStompBrokerRelay("/topic")
        .setRelayHost("localhost")
        .setRelayPort(61613)
        .setClientLogin("guest")
        .setClientPasscode("guest");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").addInterceptors(new IpHandshakeInterceptor())
        .setAllowedOriginPatterns("http://localhost:3000");
    registry.addEndpoint("/ws").addInterceptors(new IpHandshakeInterceptor())
        .setAllowedOriginPatterns("http://localhost:3000").withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new CustomChannelInterceptor(this.playerService, this.playerSessionService,
            this.roomService));
  }

  @Bean
  public ApplicationListener<SessionSubscribeEvent> webSocketEventListener(
      final AbstractSubscribableChannel clientOutboundChannel) {
    return event -> {
      final var stompHeaderAccessor = StompHeaderAccessor.wrap(event.getMessage());
      if (stompHeaderAccessor.getReceipt() != null) {
        stompHeaderAccessor.setHeader("stompCommand", StompCommand.RECEIPT);
        stompHeaderAccessor.setReceiptId(stompHeaderAccessor.getReceipt());
        clientOutboundChannel.send(
            MessageBuilder.createMessage(new byte[0], stompHeaderAccessor.getMessageHeaders()));
      }
    };
  }
}
