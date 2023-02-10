package com.ileeds.wwf.model.cache;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("player")
@Jacksonized
public record PlayerCached(@Id String key, @Indexed String roomKey) {
  @Builder public PlayerCached {}
}
