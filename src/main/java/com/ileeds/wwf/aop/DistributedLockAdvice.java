package com.ileeds.wwf.aop;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.integration.redis.util.RedisLockRegistry;

public class DistributedLockAdvice implements MethodInterceptor {

  private final RedisLockRegistry redisLockRegistry;

  public DistributedLockAdvice(RedisLockRegistry redisLockRegistry) {
    this.redisLockRegistry = redisLockRegistry;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    assert invocation != null;

    final var parameters = invocation.getMethod().getParameters();
    final var lockKeyParameterIndexes = IntStream.range(0, parameters.length)
        .filter(idx -> parameters[idx].getAnnotation((DistributedLockKey.class)) != null)
        .toArray();

    assert lockKeyParameterIndexes.length == 1;
    final var lockKey = invocation.getArguments()[lockKeyParameterIndexes[0]];

    final var lock = this.redisLockRegistry.obtain(lockKey);
    try {
      if (lock.tryLock(1, TimeUnit.SECONDS)) {
        return invocation.proceed();
      } else {
        throw new RuntimeException("Could not acquire lock");
      }
    } finally {
      lock.unlock();
    }
  }
}
