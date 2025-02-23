package ru.nsu.test;

import ru.nsu.Bean;

public class AppConfig {

    @Bean
    public UserRepository userRepository() {
        return new UserRepository();
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }
}
