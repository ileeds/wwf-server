package com.ileeds.wwf.service;

import com.ileeds.wwf.aop.DistributedLock;
import com.ileeds.wwf.aop.DistributedLockKey;
import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.post.PlayerPatch;
import com.ileeds.wwf.model.post.PlayerPost;
import com.ileeds.wwf.repository.PlayerRepository;
import com.ileeds.wwf.repository.RoomRepository;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

  public static class InvalidPlayerKeyException extends Exception {
  }

  public static class PlayerExistsException extends Exception {
  }

  public static class PlayerDoesNotExistException extends Exception {
  }

  public static class RoomDoesNotExistException extends Exception {
  }

  public static final class RoomFullException extends Exception {
  }

  public static final class ColorSelectedException extends Exception {
  }

  @Autowired
  private PlayerRepository playerRepository;

  @Autowired
  private RoomRepository roomRepository;

  private static final Map<Integer, List<Point>> POINTS_BY_PLAYER_COUNT = Map.of(
      1, new ArrayList<>() {{
        add(new Point(24, 24));
      }},
      2, new ArrayList<>() {{
        add(new Point(24, 12));
        add(new Point(24, 36));
      }},
      3, new ArrayList<>() {{
        add(new Point(12, 12));
        add(new Point(36, 12));
        add(new Point(24, 36));
      }},
      4, new ArrayList<>() {{
        add(new Point(12, 12));
        add(new Point(36, 12));
        add(new Point(12, 36));
        add(new Point(36, 36));
      }});

  @DistributedLock
  public List<PlayerCached> findAllByRoomKey(@DistributedLockKey String roomKey) {
    assert roomKey != null;

    return this.playerRepository.findAllByRoomKey(roomKey);
  }

  @DistributedLock
  public PlayerCached addPlayerToRoom(@DistributedLockKey String roomKey, PlayerPost playerPost)
      throws RoomFullException, InvalidPlayerKeyException, RoomDoesNotExistException,
      PlayerExistsException {
    assert roomKey != null;
    assert playerPost != null;

    if (playerPost.key().equals(PlayerPost.COLLISION_KEY)) {
      throw new InvalidPlayerKeyException();
    }

    final var room = this.roomRepository.findById(roomKey);
    if (room.isEmpty()) {
      throw new RoomDoesNotExistException();
    }

    final var existing = this.playerRepository.findById(playerPost.key());
    if (existing.isPresent()) {
      throw new PlayerExistsException();
    }

    final var roomPlayers = this.playerRepository.findAllByRoomKey(roomKey);
    final var selectedColors = roomPlayers.stream().map(PlayerCached::getColor).toList();
    final var color =
        RoomService.ALL_COLORS.stream().filter(elem -> !selectedColors.contains(elem)).findFirst()
            .orElseThrow(RoomFullException::new);
    final var newPlayer = PlayerCached.builder()
        .key(playerPost.key())
        .roomKey(roomKey)
        .color(color)
        .score(0)
        .build();
    roomPlayers.add(newPlayer);

    this.setPlayerPositionsAndSave(roomPlayers);
    return newPlayer;
  }

  @DistributedLock
  public PlayerCached updatePlayer(@DistributedLockKey String roomKey,
                                   String playerKey,
                                   PlayerPatch playerPatch)
      throws PlayerService.PlayerDoesNotExistException, ColorSelectedException {
    assert roomKey != null;
    assert playerKey != null;
    assert playerPatch != null;

    final var player =
        this.playerRepository.findById(playerKey).orElseThrow(
            PlayerService.PlayerDoesNotExistException::new);

    final var existingPlayers = this.playerRepository.findAllByRoomKey(player.getRoomKey());
    final var selectedColors =
        existingPlayers.stream().filter(p -> !p.getKey().equals(player.getKey()))
            .map(PlayerCached::getColor).toList();

    if (selectedColors.contains(playerPatch.color())) {
      throw new ColorSelectedException();
    }

    player.setColor(playerPatch.color());
    this.playerRepository.save(player);
    return player;
  }

  @DistributedLock
  public void playerWon(@DistributedLockKey String roomKey, String winnerKey)
      throws PlayerService.PlayerDoesNotExistException {
    assert roomKey != null;
    assert winnerKey != null;

    final var player =
        this.playerRepository.findById(winnerKey).orElseThrow(
            PlayerService.PlayerDoesNotExistException::new);

    player.setScore(player.getScore() + 1);
    this.playerRepository.save(player);
  }

  private void setPlayerPositionsAndSave(List<PlayerCached> players) {
    assert players != null;

    final var points = PlayerService.POINTS_BY_PLAYER_COUNT.get(players.size());
    Collections.shuffle(points);

    for (int i = 0; i < players.size(); i++) {
      players.get(i).setPosition(points.get(i));
    }
    this.playerRepository.saveAll(players);
  }
}
