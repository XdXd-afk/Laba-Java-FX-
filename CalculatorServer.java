import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class CalculatorServer {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер калькулятора запущен на порту " + PORT);
            System.out.println("Ожидание подключений...");

            // Бесконечный цикл для обработки подключений
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Клиент подключен: " + clientSocket.getInetAddress());

                // Создаем новый поток для обработки клиента
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        }
    }

    // Класс для обработки клиентов в отдельных потоках
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(
                            clientSocket.getOutputStream(), true)
            ) {
                String inputLine;

                // Читаем запросы от клиента
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Получено от клиента: " + inputLine);

                    if (inputLine.equalsIgnoreCase("exit")) {
                        System.out.println("Клиент отключился: " + clientSocket.getInetAddress());
                        break;
                    }

                    // Обрабатываем математическое выражение
                    String result = calculateExpression(inputLine);
                    out.println(result);
                }
            } catch (IOException e) {
                System.err.println("Ошибка обработки клиента: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Ошибка закрытия сокета: " + e.getMessage());
                }
            }
        }

        // Метод для вычисления математического выражения
        private String calculateExpression(String expression) {
            try {
                // Используем StringTokenizer для разбора выражения
                StringTokenizer tokenizer = new StringTokenizer(expression);

                if (tokenizer.countTokens() != 3) {
                    return "ОШИБКА: Неверный формат. Используйте: число оператор число";
                }

                // Парсим операнды и оператор
                double num1 = Double.parseDouble(tokenizer.nextToken());
                String operator = tokenizer.nextToken();
                double num2 = Double.parseDouble(tokenizer.nextToken());

                // Выполняем операцию
                double result;
                switch (operator) {
                    case "+":
                        result = num1 + num2;
                        break;
                    case "-":
                        result = num1 - num2;
                        break;
                    case "*":
                        result = num1 * num2;
                        break;
                    case "/":
                        if (num2 == 0) {
                            return "ОШИБКА: Деление на ноль недопустимо!";
                        }
                        result = num1 / num2;
                        break;
                    default:
                        return "ОШИБКА: Неподдерживаемая операция. Используйте +, -, *, /";
                }

                // Форматируем результат
                return String.format("%.4f", result);

            } catch (NumberFormatException e) {
                return "ОШИБКА: Неверный формат чисел!";
            } catch (Exception e) {
                return "ОШИБКА: " + e.getMessage();
            }
        }
    }
}