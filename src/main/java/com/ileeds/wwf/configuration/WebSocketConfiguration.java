package com.ileeds.wwf.configuration;

import com.ileeds.wwf.repository.PlayerRepository;
import com.ileeds.wwf.service.PlayerService;
import com.ileeds.wwf.service.PlayerSessionService;
import com.ileeds.wwf.service.RoomService;
import java.util.Map;
import java.util.Objects;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.server.HandshakeInterceptor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketConfiguration.class);

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

  private record CustomChannelInterceptor(PlayerService playerService,
                                          PlayerSessionService playerSessionService,
                                          PlayerRepository playerRepository,
                                          RoomService roomService) implements ChannelInterceptor {

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

        if (playerKey == null) {
          logger.warn("Reject connection - no player key");
          return null;
        }

        final var player = this.playerRepository.findById(playerKey);
        if (player.isEmpty()) {
          logger.warn("Reject connection - player not found");
          return null;
        }

        if (!player.get().getRoomKey().equals(roomKey)) {
          logger.warn("Reject connection - player room key mismatch");
          return null;
        }

        try {
          this.playerSessionService.setPlayerSession(simpSessionId, player.get());
        } catch (PlayerSessionService.SessionExistsException e) {
          logger.warn("Reject connection - session already exists");
          return null;
        }
      } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
        final var simpSessionId = (String) accessor.getHeader("simpSessionId");
        this.playerSessionService.findPlayerSession(simpSessionId).ifPresent(session -> {
          this.playerSessionService.deletePlayerSession(session);
          final var player = this.playerRepository.findById(session.getPlayerKey());
          player.ifPresent(p -> {
            final var roomKey = p.getRoomKey();
            this.playerRepository.delete(p);
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
  private PlayerRepository playerRepository;

  @Autowired
  private RoomService roomService;

  @Autowired
  private BrokerConfiguration brokerConfiguration;

  @Value("${spring.profiles.active}")
  private String activeProfile;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    final var tcpClient = new ReactorNettyTcpClient<>(configurer -> {
      if (this.brokerConfiguration.isSecure()) {
        configurer = configurer.secure();
      }
      return configurer.host(this.brokerConfiguration.getHostname())
          .port(this.brokerConfiguration.getPort());
    }, new StompReactorNettyCodec());

    final var stompBrokerRelay = registry
        .setApplicationDestinationPrefixes("/app")
        .enableStompBrokerRelay("/topic")
        .setAutoStartup(true)
        .setTcpClient(tcpClient);

    if (!this.activeProfile.equals("local") && !this.activeProfile.equals("test")) {
      final var secretsClient = SecretsManagerClient.builder()
          .credentialsProvider(DefaultCredentialsProvider.create())
          .build();

      final var valueRequest = GetSecretValueRequest.builder()
          .secretId("BrokerSecret")
          .build();
      final var valueResponse = secretsClient.getSecretValue(valueRequest);
      final var secret = valueResponse.secretString();

      final var jsonSecret = new JSONObject(secret);
      final var username = jsonSecret.getString("username");
      final var password = jsonSecret.getString("password");
      stompBrokerRelay.setClientLogin(username).setClientPasscode(password)
          .setSystemLogin(username).setSystemPasscode(password);

      secretsClient.close();
    }
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").addInterceptors(new IpHandshakeInterceptor())
        .setAllowedOriginPatterns("http://localhost:3000", "https://wormswithfriends.com",
            "https://www.wormswithfriends.com");
    registry.addEndpoint("/ws").addInterceptors(new IpHandshakeInterceptor())
        .setAllowedOriginPatterns("http://localhost:3000", "https://wormswithfriends.com",
            "https://www.wormswithfriends.com")
        .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new CustomChannelInterceptor(this.playerService,
            this.playerSessionService,
            this.playerRepository,
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
