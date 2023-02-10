package com.ileeds.wwf.service;

import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.post.PlayerPost;
import com.ileeds.wwf.repository.PlayerRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

  public static class PlayerExistsException extends Exception {
  }

  public static class RoomDoesNotExistException extends Exception {
  }

  @Autowired
  private PlayerRepository playerRepository;

  @Autowired
  private RoomService roomService;

  public PlayerCached createPlayer(String roomKey, PlayerPost playerPost)
      throws PlayerExistsException, RoomDoesNotExistException {
    assert roomKey != null;
    assert playerPost != null;

    final var room = this.roomService.findRoom(roomKey);
    if (room.isEmpty()) {
      throw new RoomDoesNotExistException();
    }

    final var existing = this.playerRepository.findById(playerPost.key());
    if (existing.isPresent()) {
      throw new PlayerExistsException();
    }

    final var player = this.playerRepository.save(
        PlayerCached.builder().key(playerPost.key()).roomKey(roomKey).build());
    this.roomService.publish(player.roomKey());
    return player;
  }

  public void deletePlayer(PlayerCached player) {
    assert player != null;

    this.playerRepository.delete(player);
    this.roomService.publish(player.roomKey());
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
