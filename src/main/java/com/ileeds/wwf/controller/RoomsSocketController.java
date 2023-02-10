package com.ileeds.wwf.controller;

import com.ileeds.wwf.model.response.RoomResponse;
import com.ileeds.wwf.model.response.SocketResponse;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

//@RestController
//@RequestMapping("/rooms")
@Controller
public class RoomsSocketController {

//  @Autowired
//  private RoomService roomService;
//
//  @PostMapping
//  public RestResponse<RoomResponse> postRoom(@RequestBody RoomPost roomPost) {
//    final RoomCached roomCached;
//    try {
//      roomCached = this.roomService.createRoom(roomPost);
//    } catch (RoomService.RoomExistsException e) {
//      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room already exists");
//    }
//    return new RestResponse<>(new RoomResponse(roomCached.key()));
//  }

  @SubscribeMapping("/a")
  public SocketResponse<RoomResponse> subscribeToRoom(SimpMessageHeaderAccessor headerAccessor) {
    return new SocketResponse<>(new RoomResponse("a"));
  }
}
