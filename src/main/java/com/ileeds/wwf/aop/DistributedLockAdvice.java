package com.ileeds.wwf.aop;

import com.ileeds.wwf.service.RoomPublisher;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.integration.redis.util.RedisLockRegistry;

public class DistributedLockAdvice implements MethodInterceptor {

  private final RedisLockRegistry redisLockRegistry;
  private final RoomPublisher roomPublisher;

  public DistributedLockAdvice(RedisLockRegistry redisLockRegistry, RoomPublisher roomPublisher) {
    this.redisLockRegistry = redisLockRegistry;
    this.roomPublisher = roomPublisher;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    assert invocation != null;

    final var parameters = invocation.getMethod().getParameters();
    final var lockKeyParameterIndexes = IntStream.range(0, parameters.length)
        .filter(idx -> parameters[idx].getAnnotation((DistributedLockKey.class)) != null)
        .toArray();

    assert lockKeyParameterIndexes.length == 1;
    final var lockKey = (String) invocation.getArguments()[lockKeyParameterIndexes[0]];

    final var lock = this.redisLockRegistry.obtain(lockKey);
    try {
      if (lock.tryLock(1, TimeUnit.SECONDS)) {
        return invocation.proceed();
      } else {
        throw new RuntimeException("Could not acquire lock");
      }
    } finally {
      this.roomPublisher.publish(lockKey);
      lock.unlock();
    }
  }
}
