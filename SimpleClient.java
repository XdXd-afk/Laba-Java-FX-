import java.io.*;
import java.net.*;

public class SimpleClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true);
                BufferedReader consoleInput = new BufferedReader(
                        new InputStreamReader(System.in))
        ) {
            System.out.println("Клиент калькулятора запущен");
            System.out.println("Введите выражения (формат: число оператор число)");
            System.out.println("Пример: 5.5 + 3.2");

            String userInput;
            while ((userInput = consoleInput.readLine()) != null) {
                if (userInput.equalsIgnoreCase("exit")) {
                    out.println("exit");
                    break;
                }

                out.println(userInput);
                String response = in.readLine();
                System.out.println("Ответ сервера: " + response);
            }

        } catch (IOException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        }
    }
}