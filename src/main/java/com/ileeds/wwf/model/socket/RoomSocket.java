package com.ileeds.wwf.model.socket;

import com.ileeds.wwf.model.cache.PlayerCached;
import java.awt.Point;
import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record RoomSocket(String key,
                         List<PlayerSocket> players,
                         List<PlayerCached.PlayerColor> colors) {
  @Builder
  public RoomSocket {
  }

  public record PlayerSocket(String key, PlayerCached.PlayerColor color, Point position) {
    public static PlayerSocket fromPlayerCached(PlayerCached playerCached) {
      assert playerCached != null;

      return new PlayerSocket(
          playerCached.getKey(),
          playerCached.getColor(),
          playerCached.getPosition());
    }
  }
}
