package com.ileeds.wwf.service;

import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Service
public class GameService {

  private static final Logger logger = LoggerFactory.getLogger(GameService.class);

  private static final class GameSessionHandler extends StompSessionHandlerAdapter {
    private final String roomKey;

    public GameSessionHandler(String roomKey) {
      this.roomKey = roomKey;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
      session.subscribe(String.format("/topic/rooms.%s.game.actions", this.roomKey), this);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      assert payload != null;

      logger.info(payload.toString());
//      this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms.%s.game", roomKey),
//          gameSocket);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
      logger.error("handleTransportError", exception);
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                byte[] payload, Throwable exception) {
      logger.error("handleException", exception);
    }
  }

  private static final WebSocketStompClient STOMP_CLIENT;

  static {
    final var stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    stompClient.setMessageConverter(new StringMessageConverter());
    STOMP_CLIENT = stompClient;
  }

  @Autowired
  @Lazy
  private SimpMessagingTemplate simpMessagingTemplate;

  @Async
  public void startGame(String roomKey) {
    final StompSession stompSession;
    try {
      stompSession =
          GameService.STOMP_CLIENT.connectAsync("ws://localhost:8080/ws",
                  new GameSessionHandler(roomKey))
              .get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    //noinspection StatementWithEmptyBody
    while (stompSession.isConnected()) {
    }
  }
}
