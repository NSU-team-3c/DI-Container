    package ru.nsu;

    import ru.nsu.test.UserService;
    import ru.nsu.testInject.Car;

    import java.io.IOException;

    @Scan(packageToScan = "ru.nsu.testInject")
    public class App {
        public static void main(String[] args) {
            try {
                Application.run(App.class);

                Car car = Application.getContext().getBean(Car.class);
                car.drive();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
