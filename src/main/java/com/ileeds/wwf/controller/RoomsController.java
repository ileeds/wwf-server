package com.ileeds.wwf.controller;

import com.ileeds.wwf.model.cache.RoomCached;
import com.ileeds.wwf.model.post.RoomPatch;
import com.ileeds.wwf.model.post.RoomPost;
import com.ileeds.wwf.model.response.RestResponse;
import com.ileeds.wwf.model.response.RoomResponse;
import com.ileeds.wwf.service.RoomPublisher;
import com.ileeds.wwf.service.RoomService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rooms")
@Validated
public class RoomsController {

  @Autowired
  private RoomService roomService;

  @Autowired
  private RoomPublisher roomPublisher;

  @GetMapping("/{roomKey}")
  public RestResponse<RoomResponse> getRoom(@PathVariable String roomKey) {
    final var room = this.roomService.findRoom(roomKey);
    if (room.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room does not exist");
    }
    this.roomPublisher.publish(roomKey);
    return new RestResponse<>(new RoomResponse(roomKey));
  }

  @PostMapping
  public RestResponse<RoomResponse> postRoom(@Valid @RequestBody RoomPost roomPost) {
    final RoomCached roomCached;
    try {
      roomCached = this.roomService.createRoom(roomPost);
    } catch (RoomService.RoomExistsException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room already exists");
    }
    return new RestResponse<>(new RoomResponse(roomCached.getKey()));
  }

  @PatchMapping("/{roomKey}")
  public RestResponse<RoomResponse> patchRoom(
      @PathVariable String roomKey,
      @Valid @RequestBody RoomPatch roomPatch) {
    final RoomCached roomCached;
    try {
      roomCached = this.roomService.updateRoom(roomKey, roomPatch);
    } catch (RoomService.RoomNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
    } catch (RoomService.RoomCannotStartException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room cannot be started");
    }
    return new RestResponse<>(new RoomResponse(roomCached.getKey()));
  }
}
