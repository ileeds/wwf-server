package com.ileeds.wwf.model.post;

import com.ileeds.wwf.model.cache.PlayerCached;
import java.util.Optional;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record PlayerPatch(Optional<PlayerCached.PlayerColor> color, Optional<String> name) {
}
