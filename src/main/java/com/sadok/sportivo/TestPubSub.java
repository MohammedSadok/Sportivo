package com.sadok.sportivo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sadok.sportivo.mail.RabbitMqProducer;

@Component
public class TestPubSub implements CommandLineRunner {

  @Autowired
  private RabbitMqProducer rabbitMqProducer;

  @Override
  public void run(String... args) throws Exception {
    rabbitMqProducer.sendMessage("this  is a test message 1");
  }

}
