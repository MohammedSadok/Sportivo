package com.sadok.sportivo;

import org.springframework.boot.SpringApplication;

public class TestSportivoApplication {

    public static void main(String[] args) {
        SpringApplication.from(SportivoApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
