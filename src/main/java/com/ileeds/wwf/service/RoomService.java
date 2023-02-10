package com.ileeds.wwf.service;

import com.ileeds.wwf.model.cache.RoomCached;
import com.ileeds.wwf.model.post.RoomPost;
import com.ileeds.wwf.model.socket.RoomSocket;
import com.ileeds.wwf.repository.PlayerRepository;
import com.ileeds.wwf.repository.RoomRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  public static class RoomExistsException extends Exception {
  }

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

    return this.roomRepository.save(new RoomCached(roomPost.key()));
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
        .players(players.stream().map(player -> new RoomSocket.PlayerSocket(player.key())).toList())
        .build();
    this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms/%s", roomKey),
        roomSocket);
  }
}
