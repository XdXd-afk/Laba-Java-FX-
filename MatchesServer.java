import java.io.*;
import java.net.*;
import java.util.*;

public class MatchesServer {
    private static final int PORT = 8888;
    private static final int TOTAL_MATCHES = 37;
    private static final int MAX_TAKE = 5;

    private static List<ClientHandler> players = new ArrayList<>();
    private static int currentMatches = TOTAL_MATCHES;
    private static int currentPlayerIndex = 0;
    private static boolean gameActive = false;

    public static void main(String[] args) {
        System.out.println("Сервер игры 'Спички' запущен на порту " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                players.add(client);
                new Thread(client).start();

                System.out.println("Игрок подключен. Всего игроков: " + players.size());

                if (players.size() == 2 && !gameActive) {
                    startGame();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void startGame() {
        gameActive = true;
        currentMatches = TOTAL_MATCHES;
        currentPlayerIndex = 0;

        System.out.println("\n=== Игра началась! ===");
        System.out.println("Спичек: " + currentMatches);

        // Уведомляем всех игроков
        broadcast("GAME_START:" + TOTAL_MATCHES);
        broadcast("PLAYER_TURN:1"); // Ход первого игрока
    }

    private static synchronized void processMove(int playerId, int matchesTaken) {
        if (!gameActive || playerId != currentPlayerIndex + 1) {
            return;
        }

        currentMatches -= matchesTaken;
        System.out.println("Игрок " + playerId + " взял " + matchesTaken + " спичек");
        System.out.println("Осталось спичек: " + currentMatches);

        // Отправляем обновление всем игрокам
        broadcast("MATCHES_UPDATE:" + currentMatches);
        broadcast("LAST_MOVE:Игрок " + playerId + " взял " + matchesTaken + " спичек");

        // Проверяем конец игры
        if (currentMatches <= 0) {
            endGame(playerId);
            return;
        }

        // Передаем ход следующему игроку
        currentPlayerIndex = (currentPlayerIndex + 1) % 2;
        broadcast("PLAYER_TURN:" + (currentPlayerIndex + 1));
    }

    private static synchronized void endGame(int winnerId) {
        gameActive = false;
        System.out.println("\n=== Игра окончена! ===");
        System.out.println("Победитель: Игрок " + winnerId);

        // Отправляем результат игрокам
        for (int i = 0; i < players.size(); i++) {
            if (i == winnerId - 1) {
                players.get(i).sendMessage("GAME_END:WIN:Вы победили!");
            } else {
                players.get(i).sendMessage("GAME_END:LOSE:Вы проиграли!");
            }
        }

        // Очищаем игроков для новой игры
        players.clear();
    }

    private static void broadcast(String message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int playerId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.playerId = players.size() + 1;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Отправляем ID игроку
                sendMessage("PLAYER_ID:" + playerId);
                System.out.println("Игроку назначен ID: " + playerId);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Игрок " + playerId + ": " + message);

                    if (message.startsWith("TAKE:")) {
                        int matches = Integer.parseInt(message.substring(5));
                        if (matches >= 1 && matches <= MAX_TAKE) {
                            processMove(playerId, matches);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Игрок " + playerId + " отключился");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}