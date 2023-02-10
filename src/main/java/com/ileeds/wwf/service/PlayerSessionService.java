package com.ileeds.wwf.service;

import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.cache.PlayerSessionCached;
import com.ileeds.wwf.repository.PlayerSessionRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlayerSessionService {

  public static final class SessionExistsException extends Exception {
  }

  @Autowired
  private PlayerSessionRepository playerSessionRepository;

  public PlayerSessionCached setPlayerSession(String sessionId, PlayerCached playerCached)
      throws SessionExistsException {
    assert sessionId != null;
    assert playerCached != null;

    final var existing = this.playerSessionRepository.findById(sessionId);
    if (existing.isPresent()) {
      throw new SessionExistsException();
    }

    return this.playerSessionRepository.save(
        PlayerSessionCached.builder().id(sessionId).playerKey(playerCached.key()).build());
  }

  public Optional<PlayerSessionCached> findPlayerSession(String sessionId) {
    assert sessionId != null;

    return this.playerSessionRepository.findById(sessionId);
  }

  public void deletePlayerSession(PlayerSessionCached playerSessionCached) {
    assert playerSessionCached != null;

    this.playerSessionRepository.delete(playerSessionCached);
  }
}
