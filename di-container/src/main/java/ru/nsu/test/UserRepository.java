package ru.nsu.test;

import java.util.Random;

public class UserRepository {

    private final String userName;

    public UserRepository() {
        Random random = new Random();
        this.userName = "User" + random.nextInt(1000);
    }

    public String getUserName() {
        return userName;
    }
}


