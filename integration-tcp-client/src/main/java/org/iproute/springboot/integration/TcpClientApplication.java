package org.iproute.springboot.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcpClientApplication.class, args);
    }

}