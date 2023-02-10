package com.ileeds.wwf.model.cache;

import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("room")
@Jacksonized
public record RoomCached(@Id String key) {
}
