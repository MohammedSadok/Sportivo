package com.sadok.sportivo.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;

  public void sendWelcomeEmail(String to, String username, String password) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(mailProperties.from());
      message.setTo(to);
      message.setSubject("Welcome to Sportivo \u2013 Your Login Credentials");
      message.setText("""
          Welcome to Sportivo!

          Your account has been created. Here are your login credentials:

            Username : %s
            Email    : %s
            Password : %s

          This is a temporary password. You will be required to change it on your first login.

          Best regards,
          The Sportivo Team
          """.formatted(username, to, password));

      mailSender.send(message);
      log.info("Welcome email sent to [{}]", to);
    } catch (Exception ex) {
      log.error("Failed to send welcome email to [{}]", to, ex);
    }
  }
}
