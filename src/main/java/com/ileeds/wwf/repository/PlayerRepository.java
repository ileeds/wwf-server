package com.ileeds.wwf.repository;

import com.ileeds.wwf.model.cache.PlayerCached;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends CrudRepository<PlayerCached, String> {
  List<PlayerCached> findAllByRoomKey(String roomKey);
}
