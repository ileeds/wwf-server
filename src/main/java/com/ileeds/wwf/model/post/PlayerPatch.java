package com.ileeds.wwf.model.post;

import com.ileeds.wwf.model.cache.PlayerCached;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record PlayerPatch(PlayerCached.PlayerColor color) {
}
