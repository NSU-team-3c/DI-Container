    package ru.nsu;
    import java.io.IOException;

    @Scan(packageToScan = "ru.nsu.test")
    public class App {
        public static void main(String[] args) {
            try {
                Application.run(App.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
