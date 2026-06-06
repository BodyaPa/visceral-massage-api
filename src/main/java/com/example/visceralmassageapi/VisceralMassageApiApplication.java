package com.example.visceralmassageapi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@RestController
public class VisceralMassageApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisceralMassageApiApplication.class, args);
    }
    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "VML") String name) {
        return String.format("Hello I'm %s!", name);
    }
}
