package ru.nsu.test;

import ru.nsu.annotations.Bean;

public class AppConfig {

    public UserRepository userRepository() {
        return new UserRepository();
    }

    public UserService userService() {
        return new UserService();
    }
}
