package com.ileeds.wwf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.socket.GameAction;
import com.ileeds.wwf.model.socket.GamePlayerSocket;
import com.ileeds.wwf.model.socket.GameSocket;
import java.awt.Point;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final class GameStateEmitter extends TimerTask {

    private final Timer timer;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PlayerService playerService;
    private final String roomKey;
    private final Date start;
    private final Map<String, GamePlayerSocket> playerByKey;
    private final String[][] board;
    private StompSession session;

    private GameStateEmitter(Timer timer,
                             SimpMessagingTemplate simpMessagingTemplate,
                             PlayerService playerService,
                             String roomKey,
                             List<PlayerCached> players) {
      this.timer = timer;
      this.simpMessagingTemplate = simpMessagingTemplate;
      this.playerService = playerService;
      this.roomKey = roomKey;
      this.start = new Date();

      this.playerByKey = players.stream()
          .map(player -> Map.entry(player.getKey(), GamePlayerSocket.fromPlayerCached(player)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      this.board = new String[50][50];
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
        final var playerKeyByPosition = this.playerByKey.values().stream()
            .filter(player -> player.getState().equals(GamePlayerSocket.PlayerState.VROOM))
            .map(player -> {
              final var position = player.getPosition();
              final var direction = player.getDirection();

              final Point newPosition;
              switch (direction) {
                case UP -> newPosition = new Point(position.x, position.y - 1);
                case DOWN -> newPosition = new Point(position.x, position.y + 1);
                case LEFT -> newPosition = new Point(position.x - 1, position.y);
                case RIGHT -> newPosition = new Point(position.x + 1, position.y);
                default -> throw new AssertionError();
              }
              player.setPosition(newPosition);

              if (newPosition.x < 0 || newPosition.y < 0
                  || newPosition.x >= 50 || newPosition.y >= 50) {
                player.setState(GamePlayerSocket.PlayerState.CRASHED);
              } else {
                if (this.board[newPosition.y][newPosition.x] != null) {
                  player.setState(GamePlayerSocket.PlayerState.CRASHED);
                } else {
                  this.board[newPosition.y][newPosition.x] = player.getKey();
                }
              }
              return Map.entry(String.format("%d_%d", newPosition.x, newPosition.y),
                  player.getKey());
            }).collect(Collectors.groupingBy(Map.Entry::getKey));

        playerKeyByPosition.entrySet().stream().filter(entry -> entry.getValue().size() > 1)
            .forEach(entry -> entry.getValue().forEach(playerKeyEntry -> {
              final var player = this.playerByKey.get(playerKeyEntry.getValue());
              player.setState(GamePlayerSocket.PlayerState.CRASHED);
            }));
      }

      final var livePlayers = this.playerByKey.values().stream()
          .filter(player -> player.getState().equals(GamePlayerSocket.PlayerState.VROOM)).toList();
      final var gameState =
          livePlayers.size() <= 1 ? GameSocket.GameState.DONE : GameSocket.GameState.GOING;
      final var winnerKey = livePlayers.size() == 1 ? livePlayers.get(0).getKey() : null;

      this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms.%s.game", this.roomKey),
          GameSocket.builder()
              .board(this.board)
              .countdown(countdown)
              .gameState(gameState)
              .winnerKey(winnerKey)
              .players(this.playerByKey.values())
              .build());

      if (gameState.equals(GameSocket.GameState.DONE)) {
        this.session.disconnect();
        this.timer.cancel();
        if (winnerKey != null) {
          try {
            this.playerService.playerWon(roomKey, winnerKey);
          } catch (PlayerService.PlayerDoesNotExistException e) {
            throw new RuntimeException("Winner does not exist");
          }
        }
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
                              PlayerService playerService,
                              String roomKey,
                              List<PlayerCached> players,
                              ObjectMapper objectMapper) {
      this.timer = new Timer();
      this.roomKey = roomKey;
      this.gameStateEmitter =
          new GameStateEmitter(timer, simpMessagingTemplate, playerService,
              this.roomKey, players);
      this.objectMapper = objectMapper;
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
      this.gameStateEmitter.playerByKey.get(gameAction.playerKey())
          .setDirection(gameAction.direction());
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
  private PlayerService playerService;

  @Value("${server.port}")
  private String serverPort;

  @Async
  public void startGame(String roomKey) {
    final var players = this.playerService.findAllByRoomKey(roomKey);

    final StompSession stompSession;
    try {
      stompSession =
          GameService.STOMP_CLIENT.connectAsync(
                  String.format("ws://127.0.0.1:%s/ws", this.serverPort),
                  new GameSessionHandler(
                      this.simpMessagingTemplate,
                      this.playerService,
                      roomKey,
                      players,
                      GameService.MAPPER))
              .get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    //noinspection StatementWithEmptyBody
    while (stompSession.isConnected()) {
    }
  }
}
