package com.ileeds.wwf.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameService {

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

//  public void publish(String roomKey) {
//    assert roomKey != null;
//
//    final var players = this.playerRepository.findAllByRoomKey(roomKey);
//    final var roomSocket = RoomSocket.builder().key(roomKey)
//        .players(players.stream().map(RoomSocket.PlayerSocket::fromPlayerCached).toList())
//        .colors(RoomService.ALL_COLORS)
//        .build();
//    this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms/%s", roomKey),
//        roomSocket);
//  }
}
