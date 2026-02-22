package com.sadok.sportivo.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String host, int port, String username, String password, String from) {
}
