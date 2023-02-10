package com.ileeds.wwf.model.socket;

import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record RoomSocket(String key, List<PlayerSocket> players) {
  @Builder
  public RoomSocket {}

  public record PlayerSocket(String key) {}
}
