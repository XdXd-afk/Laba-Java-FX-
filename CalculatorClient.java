import java.io.*;
import java.net.*;
import java.util.Scanner;

public class CalculatorClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Подключено к серверу калькулятора");
            System.out.println("===================================");
            System.out.println("Инструкция по использованию:");
            System.out.println("  • Введите выражение в формате: число оператор число");
            System.out.println("  • Поддерживаемые операторы: +, -, *, /");
            System.out.println("  • Примеры: 5.5 + 3.2  или  -2.5 * 4  или  10 / -2");
            System.out.println("  • Для выхода введите 'exit'");
            System.out.println("===================================\n");

            String userInput;
            while (true) {
                // Получаем ввод от пользователя
                System.out.print("Введите выражение: ");
                userInput = scanner.nextLine();

                // Проверяем на команду выхода
                if (userInput.equalsIgnoreCase("exit")) {
                    out.println("exit");
                    break;
                }

                // Отправляем выражение на сервер
                out.println(userInput);

                // Получаем ответ от сервера
                String response = in.readLine();
                System.out.println("Результат: " + response + "\n");
            }

            System.out.println("Соединение закрыто");

        } catch (UnknownHostException e) {
            System.err.println("Неизвестный хост: " + SERVER_ADDRESS);
        } catch (ConnectException e) {
            System.err.println("Не удалось подключиться к серверу. Убедитесь, что сервер запущен.");
        } catch (IOException e) {
            System.err.println("Ошибка ввода/вывода: " + e.getMessage());
        }
    }
}