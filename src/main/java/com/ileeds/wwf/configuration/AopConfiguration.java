package com.ileeds.wwf.configuration;

import com.ileeds.wwf.aop.DistributedLock;
import com.ileeds.wwf.aop.DistributedLockAdvice;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AopConfiguration {

  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static PointcutAdvisor distributedLock(RedisLockRegistry redisLockRegistry) {
    return new DefaultPointcutAdvisor(
        new AnnotationMatchingPointcut(null, DistributedLock.class, false),
        new DistributedLockAdvice(redisLockRegistry));
  }
}
