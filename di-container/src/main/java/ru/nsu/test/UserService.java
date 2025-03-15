package ru.nsu.test;


import ru.nsu.enums.ScopeType;
import ru.nsu.annotations.Wired;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;


@Named("userService")
public class UserService {

//    @Wired
//    private UserRepository userRepositorySingleton;

    @Inject
    @Scope(ScopeType.SINGLETON)
    private UserRepository userRepositoryPrototype;

    public void getUserInfo() {
//        System.out.println("User Info (Singleton): " + userRepositorySingleton.getUserName());
        System.out.println("User Info (Prototype): " + userRepositoryPrototype.getUserName());
    }

}
