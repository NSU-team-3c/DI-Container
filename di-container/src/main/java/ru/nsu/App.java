    package ru.nsu;

    import ru.nsu.annotations.Scan;
    import ru.nsu.context.ContextInitializer;

    import javax.xml.stream.XMLStreamException;
    import java.io.IOException;

    @Scan(packageToScan = "ru.nsu.test")
    public class App {
        public static void main(String[] args) {
            try {
                Application.run(App.class);

                var init = new ContextInitializer();
                init.init();

//                for (int i = 0; i < 5; i++) {
//                    UserService userService = Application.getContext().getBean(UserService.class, ScopeType.PROTOTYPE);
//                    userService.getUserInfo();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }
    }
