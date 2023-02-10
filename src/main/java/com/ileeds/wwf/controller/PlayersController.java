package com.ileeds.wwf.controller;

import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.post.PlayerPost;
import com.ileeds.wwf.model.response.PlayerResponse;
import com.ileeds.wwf.model.response.RestResponse;
import com.ileeds.wwf.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rooms/{roomKey}/players")
public class PlayersController {

  @Autowired
  private PlayerService playerService;

  @PostMapping
  public RestResponse<PlayerResponse> postPlayer(@PathVariable String roomKey, @RequestBody PlayerPost playerPost) {
    final PlayerCached playerCached;
    try {
      playerCached = this.playerService.createPlayer(roomKey, playerPost);
    } catch (PlayerService.PlayerExistsException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player already exists");
    } catch (PlayerService.RoomDoesNotExistException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room does not exist");
    }
    return new RestResponse<>(new PlayerResponse(playerCached.key()));
  }
}
