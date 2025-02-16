package ru.nsu;

import ru.nsu.Scan;
import java.io.IOException;

@Scan(packageToScan = "ru.nsu")
public class App {
    public static void main(String[] args) {
        try {
            Application.run(App.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
