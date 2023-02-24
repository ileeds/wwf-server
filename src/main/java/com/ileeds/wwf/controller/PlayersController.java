package com.ileeds.wwf.controller;

import com.ileeds.wwf.model.cache.PlayerCached;
import com.ileeds.wwf.model.post.PlayerPatch;
import com.ileeds.wwf.model.post.PlayerPost;
import com.ileeds.wwf.model.response.PlayerResponse;
import com.ileeds.wwf.model.response.RestResponse;
import com.ileeds.wwf.service.PlayerService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rooms/{roomKey}/players")
@Validated
public class PlayersController {

  @Autowired
  private PlayerService playerService;

  @PostMapping
  public RestResponse<PlayerResponse> postPlayer(@PathVariable String roomKey,
                                                 @Valid @RequestBody PlayerPost playerPost) {
    final PlayerCached playerCached;
    try {
      playerCached = this.playerService.addPlayerToRoom(roomKey, playerPost);
    } catch (PlayerService.PlayerExistsException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player already exists");
    } catch (PlayerService.RoomDoesNotExistException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room does not exist");
    } catch (PlayerService.RoomFullException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is full");
    }
    return new RestResponse<>(PlayerResponse.fromPlayerCached(playerCached));
  }

  @PatchMapping("/{playerKey}")
  public RestResponse<PlayerResponse> updatePlayer(@PathVariable String roomKey,
                                                   @PathVariable String playerKey,
                                                   @Valid @RequestBody PlayerPatch playerPatch) {
    final PlayerCached playerCached;
    try {
      playerCached = this.playerService.updatePlayer(roomKey, playerKey, playerPatch);
    } catch (PlayerService.PlayerDoesNotExistException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player does not exist");
    } catch (PlayerService.ColorSelectedException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Color is not available");
    }
    return new RestResponse<>(PlayerResponse.fromPlayerCached(playerCached));
  }
}
