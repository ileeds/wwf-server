package com.ileeds.wwf.model.response;

import com.ileeds.wwf.model.cache.PlayerCached;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record PlayerResponse(String key, PlayerCached.PlayerColor color) {
  public static PlayerResponse fromPlayerCached(PlayerCached playerCached) {
    assert playerCached != null;

    return new PlayerResponse(playerCached.getKey(), playerCached.getColor());
  }
}
