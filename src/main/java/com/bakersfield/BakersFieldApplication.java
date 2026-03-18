package com.bakersfield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BakersFieldApplication {
  public static void main(String[] args) {
    SpringApplication.run(BakersFieldApplication.class, args);
  }
}
