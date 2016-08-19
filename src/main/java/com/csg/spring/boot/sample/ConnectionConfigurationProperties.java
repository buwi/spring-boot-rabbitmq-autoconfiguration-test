package com.csg.spring.boot.sample;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties("csg.rabbitmq")
public class ConnectionConfigurationProperties {

	private String host = "localhost";
	private int port = 5672;
	private String username = "guest";
	private String password = "guest";
}
