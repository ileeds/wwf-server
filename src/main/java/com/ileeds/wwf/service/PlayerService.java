package com.ileeds.wwf.service;

import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.post.PlayerPatch;
import com.ileeds.wwf.model.post.PlayerPost;
import com.ileeds.wwf.repository.PlayerRepository;
import java.util.List;
import java.util.Optional;
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

  @Autowired
  private PlayerRepository playerRepository;

  @Autowired
  private RoomService roomService;

  @Autowired
  private SynchronizedPlayerService synchronizedPlayerService;

  public PlayerCached createPlayer(String roomKey, PlayerPost playerPost)
      throws PlayerExistsException, RoomDoesNotExistException,
      SynchronizedPlayerService.RoomFullException, InvalidPlayerKeyException {
    assert roomKey != null;
    assert playerPost != null;

    if (playerPost.key().equals(PlayerPost.COLLISION_KEY)) {
      throw new InvalidPlayerKeyException();
    }

    final var room = this.roomService.findRoom(roomKey);
    if (room.isEmpty()) {
      throw new RoomDoesNotExistException();
    }

    final var existing = this.playerRepository.findById(playerPost.key());
    if (existing.isPresent()) {
      throw new PlayerExistsException();
    }

    final var player = this.synchronizedPlayerService.addPlayerToRoom(roomKey, playerPost);
    this.roomService.publish(player.getRoomKey());
    return player;
  }

  public PlayerCached updatePlayer(String roomKey, String playerKey, PlayerPatch playerPatch)
      throws PlayerDoesNotExistException, SynchronizedPlayerService.ColorSelectedException {
    assert roomKey != null;
    assert playerKey != null;
    assert playerPatch != null;

    final var player = this.synchronizedPlayerService.updatePlayer(roomKey, playerKey, playerPatch);
    this.roomService.publish(player.getRoomKey());
    return player;
  }

  public void deletePlayer(PlayerCached player) {
    assert player != null;

    this.synchronizedPlayerService.deletePlayer(player.getRoomKey(), player);
    this.roomService.publish(player.getRoomKey());
  }

  public Optional<PlayerCached> findPlayer(String playerKey) {
    assert playerKey != null;

    return this.playerRepository.findById(playerKey);
  }

  public List<PlayerCached> findAllByRoomKey(String roomKey) {
    assert roomKey != null;

    return this.playerRepository.findAllByRoomKey(roomKey);
  }
}
