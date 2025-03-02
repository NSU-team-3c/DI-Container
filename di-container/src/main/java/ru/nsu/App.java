    package ru.nsu;

    import ru.nsu.test.UserService;

    import java.io.IOException;

    @Scan(packageToScan = "ru.nsu.test")
    public class App {
        public static void main(String[] args) {
            try {
                Application.run(App.class);

                for (int i = 0; i < 5; i++) {
                    UserService userService = Application.getContext().getBean(UserService.class, ScopeType.PROTOTYPE);
                    userService.getUserInfo();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
