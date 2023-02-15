package com.ileeds.wwf.model.cache;

import com.ileeds.wwf.service.RoomService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "room", timeToLive = 86400)
@Jacksonized
@Getter
@Setter
@Builder
public class RoomCached {

  @Id
  private String key;
  private RoomService.RoomStatus status;
}
