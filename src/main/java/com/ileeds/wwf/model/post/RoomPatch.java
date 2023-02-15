package com.ileeds.wwf.model.post;

import com.ileeds.wwf.service.RoomService;
import java.util.Optional;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record RoomPatch(Optional<RoomService.RoomStatus> status) {
}
