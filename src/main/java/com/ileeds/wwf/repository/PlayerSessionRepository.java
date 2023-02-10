package com.ileeds.wwf.repository;

import com.ileeds.wwf.model.cache.PlayerSessionCached;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerSessionRepository extends CrudRepository<PlayerSessionCached, String> {}
