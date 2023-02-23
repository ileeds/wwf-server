package com.ileeds.wwf.service;

import com.ileeds.wwf.model.socket.RoomSocket;
import com.ileeds.wwf.repository.PlayerRepository;
import java.awt.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomPublisher {

  @Autowired
  private PlayerRepository playerRepository;

  @Autowired
  @Lazy
  private SimpMessagingTemplate simpMessagingTemplate;

  public void publish(String roomKey) {
    assert roomKey != null;

    final var players = this.playerRepository.findAllByRoomKey(roomKey);
    final var roomSocket = RoomSocket.builder()
        .key(roomKey)
        .players(players.stream().map(RoomSocket.PlayerSocket::fromPlayerCached).toList())
        .colors(RoomService.ALL_COLORS)
        .dimensions(new Point(50, 50))
        .build();
    this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms.%s", roomKey),
        roomSocket);
  }
}
