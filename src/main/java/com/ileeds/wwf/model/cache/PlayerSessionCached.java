package com.ileeds.wwf.model.cache;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("session")
@Jacksonized
public record PlayerSessionCached(@Id String id, String playerKey) {
  @Builder public PlayerSessionCached {}
}
