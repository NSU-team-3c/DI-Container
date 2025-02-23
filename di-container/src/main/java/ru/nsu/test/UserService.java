package ru.nsu.test;


import ru.nsu.ScopeType;
import ru.nsu.Wired;


public class UserService {

//    @Wired
//    private UserRepository userRepositorySingleton;

    @Wired(scope = ScopeType.PROTOTYPE)
    private UserRepository userRepositoryPrototype;

    public void getUserInfo() {
//        System.out.println("User Info (Singleton): " + userRepositorySingleton.getUserName());
        System.out.println("User Info (Prototype): " + userRepositoryPrototype.getUserName());
    }

}
