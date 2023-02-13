package com.ileeds.wwf.model.socket;

import com.ileeds.wwf.model.cache.PlayerCached;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record GameSocket(Board board) {
  @Builder
  public GameSocket {
  }

  public enum State {
    PENDING,
    STARTED,
    ENDED
  }

  // 50x50
  public record Board(String key, PlayerCached.PlayerColor color) {
  }
}
