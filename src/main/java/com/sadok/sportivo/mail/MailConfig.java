package com.sadok.sportivo.mail;

import java.util.Properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

  @Bean
  public JavaMailSender javaMailSender(MailProperties props) {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(props.host());
    mailSender.setPort(props.port());

    boolean hasAuth = props.username() != null && !props.username().isBlank();
    if (hasAuth) {
      mailSender.setUsername(props.username());
      mailSender.setPassword(props.password());
    }

    Properties javaMailProps = mailSender.getJavaMailProperties();
    javaMailProps.put("mail.transport.protocol", "smtp");
    javaMailProps.put("mail.smtp.auth", String.valueOf(hasAuth));
    javaMailProps.put("mail.smtp.starttls.enable", "false");
    return mailSender;
  }
}
