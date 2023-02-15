package com.ileeds.wwf.model.socket;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record GameSocket(Board board) {
  @Builder
  public GameSocket {
  }

  // 50x50
  public record Board() {
  }
}
