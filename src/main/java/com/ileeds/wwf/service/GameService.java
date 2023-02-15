package com.ileeds.wwf.service;

import com.ileeds.wwf.model.socket.GameSocket;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Service
public class GameService {

  private static final class GameSessionHandler extends StompSessionHandlerAdapter {
    private final String roomKey;

    public GameSessionHandler(String roomKey) {
      this.roomKey = roomKey;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
      session.subscribe(String.format("/topic/rooms/%s/game/actions", this.roomKey), this);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      var todo = 1;
    }
  }

  private static final WebSocketStompClient STOMP_CLIENT =
      new WebSocketStompClient(new StandardWebSocketClient());

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

  public void publish(String roomKey) {
    assert roomKey != null;

    final var gameSocket = GameSocket.builder()
        .build();
    this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms/%s/game", roomKey),
        gameSocket);
  }
}
