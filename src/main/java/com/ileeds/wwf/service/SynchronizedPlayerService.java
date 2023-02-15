package com.ileeds.wwf.service;

import com.ileeds.wwf.aop.DistributedLock;
import com.ileeds.wwf.aop.DistributedLockKey;
import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.post.PlayerPatch;
import com.ileeds.wwf.model.post.PlayerPost;
import com.ileeds.wwf.repository.PlayerRepository;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SynchronizedPlayerService {

  public static final class RoomFullException extends Exception {
  }

  public static final class ColorSelectedException extends Exception {
  }

  @Autowired
  private PlayerRepository playerRepository;

  private static final Map<Integer, List<Point>> POINTS_BY_PLAYER_COUNT = Map.of(
      1, new ArrayList<Point>() {{
        add(new Point(24, 24));
      }},
      2, new ArrayList<Point>() {{
        add(new Point(24, 12));
        add(new Point(24, 36));
      }},
      3, new ArrayList<Point>() {{
        add(new Point(12, 12));
        add(new Point(36, 12));
        add(new Point(24, 36));
      }},
      4, new ArrayList<Point>() {{
        add(new Point(12, 12));
        add(new Point(36, 12));
        add(new Point(12, 36));
        add(new Point(36, 36));
      }});

  @DistributedLock
  public PlayerCached addPlayerToRoom(@DistributedLockKey String roomKey, PlayerPost playerPost)
      throws RoomFullException {
    assert roomKey != null;
    assert playerPost != null;

    final var roomPlayers = this.playerRepository.findAllByRoomKey(roomKey);
    final var selectedColors = roomPlayers.stream().map(PlayerCached::getColor).toList();
    final var color =
        RoomService.ALL_COLORS.stream().filter(elem -> !selectedColors.contains(elem)).findFirst()
            .orElseThrow(RoomFullException::new);
    final var newPlayer = PlayerCached.builder()
        .key(playerPost.key())
        .roomKey(roomKey)
        .color(color)
        .build();
    roomPlayers.add(newPlayer);

    this.setPlayerPositions(roomPlayers);
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
  public void deletePlayer(@DistributedLockKey String roomKey, PlayerCached player) {
    this.playerRepository.delete(player);
    final var players = this.playerRepository.findAllByRoomKey(roomKey);
    if (players.size() > 0) {
      this.setPlayerPositions(players);
    }
  }

  private void setPlayerPositions(List<PlayerCached> players) {
    final var points = SynchronizedPlayerService.POINTS_BY_PLAYER_COUNT.get(players.size());
    Collections.shuffle(points);

    for (int i = 0; i < players.size(); i++) {
      players.get(i).setPosition(points.get(i));
    }
    this.playerRepository.saveAll(players);
  }
}
