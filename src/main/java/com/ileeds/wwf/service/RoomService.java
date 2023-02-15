package com.ileeds.wwf.service;

import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.cache.RoomCached;
import com.ileeds.wwf.model.post.RoomPatch;
import com.ileeds.wwf.model.post.RoomPost;
import com.ileeds.wwf.model.socket.RoomSocket;
import com.ileeds.wwf.repository.PlayerRepository;
import com.ileeds.wwf.repository.RoomRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  public enum RoomStatus {
    PENDING,
    STARTED,
    ENDED
  }

  public static class RoomExistsException extends Exception {
  }

  public static class RoomNotFoundException extends Exception {
  }

  public static class RoomCannotStartException extends Exception {
  }

  public static final List<PlayerCached.PlayerColor> ALL_COLORS =
      Arrays.asList(PlayerCached.PlayerColor.values());

  @Autowired
  @Lazy
  private SimpMessagingTemplate simpMessagingTemplate;

  @Autowired
  private RoomRepository roomRepository;

  @Autowired
  private PlayerRepository playerRepository;

  public RoomCached createRoom(RoomPost roomPost) throws RoomExistsException {
    assert roomPost != null;

    final var existing = this.roomRepository.findById(roomPost.key());
    if (existing.isPresent()) {
      throw new RoomExistsException();
    }

    return this.roomRepository.save(
        RoomCached.builder().key(roomPost.key()).status(RoomService.RoomStatus.PENDING).build());
  }

  public RoomCached updateRoom(String roomKey, RoomPatch roomPatch)
      throws RoomNotFoundException, RoomCannotStartException {
    assert roomKey != null;
    assert roomPatch != null;

    final var room = this.roomRepository.findById(roomKey).orElseThrow(RoomNotFoundException::new);

    if (roomPatch.status().isPresent()) {
      final var status = roomPatch.status().get();
      if (status.equals(RoomStatus.STARTED)) {
        final var players = this.playerRepository.findAllByRoomKey(roomKey);
        if (players.size() < 2) {
          throw new RoomCannotStartException();
        }
        room.setStatus(status);
      }
    }

    return this.roomRepository.save(room);
  }

  public void deleteRoom(String roomKey) {
    assert roomKey != null;

    this.roomRepository.deleteById(roomKey);
  }

  public Optional<RoomCached> findRoom(String roomKey) {
    assert roomKey != null;

    return this.roomRepository.findById(roomKey);
  }

  public void publish(String roomKey) {
    assert roomKey != null;

    final var players = this.playerRepository.findAllByRoomKey(roomKey);
    final var roomSocket = RoomSocket.builder().key(roomKey)
        .players(players.stream().map(RoomSocket.PlayerSocket::fromPlayerCached).toList())
        .colors(RoomService.ALL_COLORS)
        .build();
    this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms/%s", roomKey),
        roomSocket);
  }
}
