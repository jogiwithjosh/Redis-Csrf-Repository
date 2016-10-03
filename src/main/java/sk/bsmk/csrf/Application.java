package sk.bsmk.csrf;

import org.springframework.boot.SpringApplication;

public class Application {

    public static void main(String[] args) throws Throwable {
        SpringApplication.run(WebSecurityConfig.class, args);
    }

}