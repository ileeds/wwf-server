package com.ileeds.wwf.service;

import com.ileeds.wwf.model.socket.GameSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameService {

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

  public void publish(String roomKey) {
    assert roomKey != null;

    final var gameSocket = GameSocket.builder()
        .build();
    this.simpMessagingTemplate.convertAndSend(String.format("/topic/rooms/%s/game", roomKey),
        gameSocket);
  }
}
