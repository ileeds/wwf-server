package com.ileeds.wwf.repository;

import com.ileeds.wwf.model.cache.RoomCached;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends CrudRepository<RoomCached, String> {}
