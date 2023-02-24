package com.ileeds.wwf.model.socket;

import com.ileeds.wwf.model.cache.PlayerCached;
import java.awt.Point;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Getter
@Setter
public class GamePlayerSocket {

  public enum PlayerState {
    VROOM,
    CRASHED
  }

  private final String key;
  private Point position;
  private GameAction.Direction direction;
  private PlayerState state;

  public static GamePlayerSocket fromPlayerCached(PlayerCached playerCached) {
    assert playerCached != null;

    return GamePlayerSocket.builder()
        .key(playerCached.getKey())
        .position(playerCached.getPosition())
        .direction(GameAction.Direction.RIGHT)
        .state(PlayerState.VROOM)
        .build();
  }
}
