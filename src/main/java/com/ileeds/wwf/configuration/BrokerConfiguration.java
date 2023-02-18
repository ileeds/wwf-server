package com.ileeds.wwf.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class BrokerConfiguration {

  @Value("${broker.hostname}")
  private String hostname;

  @Value("${broker.port}")
  private int port;

  @Value("${broker.secure}")
  private boolean isSecure;
}
