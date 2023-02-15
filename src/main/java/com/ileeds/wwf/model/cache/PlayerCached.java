package com.ileeds.wwf.model.cache;

import java.awt.Point;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash(value = "player", timeToLive = 86400)
@Jacksonized
@Getter
@Setter
@Builder
public class PlayerCached {

  @Id
  private String key;
  @Indexed
  private String roomKey;
  private PlayerColor color;
  private Point position;

  public enum PlayerColor {
    RED,
    BLUE,
    FUCHSIA,
    PURPLE,
  }
}
