package com.ileeds.wwf.model.cache;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "session", timeToLive = 86400)
@Jacksonized
@Getter
@Setter
@Builder
public class PlayerSessionCached {

  @Id
  private String id;
  private String playerKey;
}
