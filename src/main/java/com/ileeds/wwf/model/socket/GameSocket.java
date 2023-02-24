package com.ileeds.wwf.model.socket;

import java.util.Collection;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record GameSocket(String[][] board,
                         int countdown,
                         GameState gameState,
                         String winnerKey,
                         Collection<GamePlayerSocket> players) {
  @Builder
  public GameSocket {
  }

  public enum GameState {
    GOING,
    DONE
  }
}
