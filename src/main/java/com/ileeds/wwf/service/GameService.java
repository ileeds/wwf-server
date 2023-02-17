package com.ileeds.wwf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.socket.GameAction;
import com.ileeds.wwf.model.socket.GameSocket;
import com.ileeds.wwf.repository.PlayerRepository;
import java.awt.Point;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.Builder;
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

  private enum PlayerState {
    VROOM,
    CRASHED
  }

  @Builder
  private static final class PlayerSocketState {
    private final String key;
    private Point position;
    private GameAction.Direction direction;
    private PlayerState state;

    public static PlayerSocketState fromPlayerCached(PlayerCached playerCached) {
      assert playerCached != null;

      return PlayerSocketState.builder()
          .key(playerCached.getKey())
          .position(playerCached.getPosition())
          .direction(GameAction.Direction.RIGHT)
          .state(PlayerState.VROOM)
          .build();
    }
  }

  private static final class GameStateEmitter extends TimerTask {

    private static final int DIMENSION = 50;

    private final Timer timer;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final String roomKey;
    private final Date start;
    private final Map<String, PlayerSocketState> playerByKey;
    private final String[][] board;
    private StompSession session;

    private GameStateEmitter(Timer timer,
                             SimpMessagingTemplate simpMessagingTemplate,
                             String roomKey,
                             List<PlayerCached> players) {
      this.timer = timer;
      this.simpMessagingTemplate = simpMessagingTemplate;
      this.roomKey = roomKey;
      this.start = new Date();

      this.playerByKey = players.stream()
          .map(player -> Map.entry(player.getKey(), PlayerSocketState.fromPlayerCached(player)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      this.board = new String[DIMENSION][DIMENSION];
      players.forEach(player -> {
        final var position = player.getPosition();
        this.board[position.y][position.x] = player.getKey();
      });
    }

    @Override
    public void run() {
      assert this.session != null;

      final var countdown =
          (int) (3 - Math.min(3, (new Date().getTime() - this.start.getTime()) / 1000));

      if (countdown == 0) {
        this.playerByKey.values().stream()
            .filter(player -> player.state.equals(PlayerState.VROOM))
            .forEach(player -> {
              final var position = player.position;
              final var direction = player.direction;

              final Point newPosition;
              switch (direction) {
                case UP -> newPosition = new Point(position.x, position.y - 1);
                case DOWN -> newPosition = new Point(position.x, position.y + 1);
                case LEFT -> newPosition = new Point(position.x - 1, position.y);
                case RIGHT -> newPosition = new Point(position.x + 1, position.y);
                default -> throw new AssertionError();
              }
              player.position = newPosition;

              if (player.position.x < 0 || player.position.y < 0 || player.position.x >= DIMENSION
                  || player.position.y >= DIMENSION) {
                player.state = PlayerState.CRASHED;
              } else {
                this.board[player.position.y][player.position.x] = player.key;
              }
            });
      }

      final var livePlayers = this.playerByKey.values().stream()
          .filter(player -> player.state.equals(PlayerState.VROOM)).toList();
      final var gameState =
          livePlayers.size() <= 1 ? GameSocket.GameState.DONE : GameSocket.GameState.GOING;

      this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms.%s.game", this.roomKey),
          GameSocket.builder()
              .board(this.board)
              .countdown(countdown)
              .gameState(gameState)
              .winnerKey(livePlayers.size() == 1 ? livePlayers.get(0).key : null)
              .build());

      if (gameState.equals(GameSocket.GameState.DONE)) {
        this.session.disconnect();
        this.timer.cancel();
      }
    }

    public void setSession(StompSession session) {
      this.session = session;
    }
  }

  private static final class GameSessionHandler extends StompSessionHandlerAdapter {
    private final Timer timer;
    private final String roomKey;
    private final GameStateEmitter gameStateEmitter;
    private final ObjectMapper objectMapper;

    public GameSessionHandler(SimpMessagingTemplate simpMessagingTemplate,
                              String roomKey,
                              List<PlayerCached> players) {
      this.timer = new Timer();
      this.roomKey = roomKey;
      this.gameStateEmitter =
          new GameStateEmitter(timer, simpMessagingTemplate, this.roomKey, players);
      this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
      session.subscribe(String.format("/topic/rooms.%s.game.actions", this.roomKey), this);

      this.gameStateEmitter.setSession(session);
      this.timer.schedule(this.gameStateEmitter, 0, 50);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      assert payload != null;

      final GameAction gameAction;
      try {
        gameAction = this.objectMapper.readValue((String) payload, GameAction.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Invalid game action");
      }
      this.gameStateEmitter.playerByKey.get(gameAction.playerKey()).direction =
          gameAction.direction();
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
    STOMP_CLIENT = new WebSocketStompClient(new StandardWebSocketClient());
    STOMP_CLIENT.setMessageConverter(new StringMessageConverter());
  }

  @Autowired
  @Lazy
  private SimpMessagingTemplate simpMessagingTemplate;

  @Autowired
  private PlayerRepository playerRepository;

  @Async
  public void startGame(String roomKey) {
    final var players = this.playerRepository.findAllByRoomKey(roomKey);

    final StompSession stompSession;
    try {
      stompSession =
          GameService.STOMP_CLIENT.connectAsync("ws://localhost:8080/ws",
                  new GameSessionHandler(this.simpMessagingTemplate, roomKey, players))
              .get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    //noinspection StatementWithEmptyBody
    while (stompSession.isConnected()) {
    }
  }
}
