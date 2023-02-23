package com.ileeds.wwf.model.post;

import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record PlayerPost(String key) {
  public static final String COLLISION_KEY = "COLLISION";
}
