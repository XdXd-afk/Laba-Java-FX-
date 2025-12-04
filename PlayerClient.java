import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;

public class PlayerClient extends Application {

    // Элементы GUI
    private Label titleLabel;
    private Label matchesLabel;
    private Label statusLabel;
    private Label lastMoveLabel;
    private VBox matchesContainer;
    private HBox buttonsContainer;
    private Button take1Btn, take2Btn, take3Btn, take4Btn, take5Btn;
    private Button restartBtn;

    // Сетевые компоненты
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread serverListener;

    // Игровое состояние
    private int playerId;
    private int currentMatches = 37;
    private boolean isMyTurn = false;
    private boolean gameActive = false;

    @Override
    public void start(Stage primaryStage) {
        // Создание элементов интерфейса
        titleLabel = new Label("ИГРА 'СПИЧКИ'");
        titleLabel.setFont(Font.font("Arial", 24));
        titleLabel.setTextFill(Color.DARKBLUE);

        matchesLabel = new Label("37");
        matchesLabel.setFont(Font.font("Arial", 48));
        matchesLabel.setTextFill(Color.BROWN);

        statusLabel = new Label("Ожидание подключения...");
        statusLabel.setFont(Font.font("Arial", 16));

        lastMoveLabel = new Label("");
        lastMoveLabel.setFont(Font.font("Arial", 14));
        lastMoveLabel.setTextFill(Color.GRAY);

        // Контейнер для визуализации спичек
        matchesContainer = new VBox(5);
        matchesContainer.setAlignment(Pos.CENTER);
        matchesContainer.setPadding(new Insets(20));
        updateMatchesDisplay();

        // Кнопки для взятия спичек
        take1Btn = createTakeButton("1");
        take2Btn = createTakeButton("2");
        take3Btn = createTakeButton("3");
        take4Btn = createTakeButton("4");
        take5Btn = createTakeButton("5");

        buttonsContainer = new HBox(10, take1Btn, take2Btn, take3Btn, take4Btn, take5Btn);
        buttonsContainer.setAlignment(Pos.CENTER);
        buttonsContainer.setPadding(new Insets(20));

        restartBtn = new Button("Новая игра");
        restartBtn.setPrefSize(120, 40);
        restartBtn.setDisable(true);
        restartBtn.setOnAction(e -> reconnectToServer());

        // Основной layout
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.getChildren().addAll(
                titleLabel,
                createPlayerInfoPanel(),
                matchesLabel,
                matchesContainer,
                statusLabel,
                lastMoveLabel,
                buttonsContainer,
                restartBtn
        );

        // Настройка сцены
        Scene scene = new Scene(root, 600, 700);
        primaryStage.setTitle("Игра 'Спички' - Игрок");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Подключение к серверу
        connectToServer();

        // Обработка закрытия окна
        primaryStage.setOnCloseRequest(e -> disconnectFromServer());
    }

    private HBox createPlayerInfoPanel() {
        Label playerLabel = new Label("Игрок: ");
        playerLabel.setFont(Font.font("Arial", 16));

        Label playerIdLabel = new Label("-");
        playerIdLabel.setFont(Font.font("Arial", 20));
        playerIdLabel.setTextFill(Color.RED);
        playerIdLabel.setStyle("-fx-font-weight: bold");

        // Обновление ID игрока при получении с сервера
        Platform.runLater(() -> {
            playerIdLabel.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.equals("-")) {
                    playerIdLabel.setTextFill(playerId == 1 ? Color.RED : Color.BLUE);
                }
            });
        });

        HBox panel = new HBox(10, playerLabel, playerIdLabel);
        panel.setAlignment(Pos.CENTER);

        // Сохраняем ссылку для обновления
        new Thread(() -> {
            try {
                Thread.sleep(100);
                Platform.runLater(() -> playerIdLabel.setText(String.valueOf(playerId)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        return panel;
    }

    private Button createTakeButton(String text) {
        Button btn = new Button("Взять " + text);
        btn.setPrefSize(80, 50);
        btn.setDisable(true);
        btn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        btn.setOnAction(e -> {
            if (isMyTurn && gameActive) {
                int matches = Integer.parseInt(text);
                if (matches <= currentMatches) {
                    sendToServer("TAKE:" + matches);
                    disableButtons();
                    statusLabel.setText("Ход отправлен...");
                }
            }
        });

        return btn;
    }

    private void updateMatchesDisplay() {
        matchesContainer.getChildren().clear();

        // Показываем текущее количество спичек визуально
        int matchesToShow = Math.min(currentMatches, 20); // Ограничиваем для отображения

        int rows = (int) Math.ceil(matchesToShow / 5.0);
        for (int i = 0; i < rows; i++) {
            HBox row = new HBox(5);
            row.setAlignment(Pos.CENTER);

            int matchesInRow = Math.min(5, matchesToShow - i * 5);
            for (int j = 0; j < matchesInRow; j++) {
                Rectangle match = new Rectangle(30, 100);
                match.setFill(Color.SADDLEBROWN);
                match.setStroke(Color.BLACK);
                match.setStrokeWidth(2);
                row.getChildren().add(match);
            }

            matchesContainer.getChildren().add(row);
        }

        // Обновляем цифровое отображение
        matchesLabel.setText(String.valueOf(currentMatches));
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8888);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Запускаем поток для прослушивания сервера
            serverListener = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        processServerMessage(message);
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Соединение с сервером разорвано");
                        disableButtons();
                        restartBtn.setDisable(false);
                    });
                }
            });
            serverListener.setDaemon(true);
            serverListener.start();

            Platform.runLater(() -> {
                statusLabel.setText("Подключено к серверу. Ожидание второго игрока...");
            });

        } catch (IOException e) {
            Platform.runLater(() -> {
                statusLabel.setText("Не удалось подключиться к серверу");
                showErrorDialog("Запустите сервер игры!");
                restartBtn.setDisable(false);
            });
        }
    }

    private void processServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("PLAYER_ID:")) {
                playerId = Integer.parseInt(message.substring(10));
                statusLabel.setText("Вы Игрок " + playerId + ". Ожидание начала игры...");

            } else if (message.startsWith("GAME_START:")) {
                gameActive = true;
                int total = Integer.parseInt(message.substring(11));
                currentMatches = total;
                updateMatchesDisplay();
                statusLabel.setText("Игра началась! Спичек: " + total);

            } else if (message.startsWith("PLAYER_TURN:")) {
                int turnPlayerId = Integer.parseInt(message.substring(12));
                isMyTurn = (turnPlayerId == playerId);

                if (isMyTurn) {
                    statusLabel.setText("ВАШ ХОД! Возьмите от 1 до 5 спичек");
                    enableButtons();
                } else {
                    statusLabel.setText("Ход противника (Игрок " + turnPlayerId + ")");
                    disableButtons();
                }

            } else if (message.startsWith("MATCHES_UPDATE:")) {
                currentMatches = Integer.parseInt(message.substring(15));
                updateMatchesDisplay();

            } else if (message.startsWith("LAST_MOVE:")) {
                lastMoveLabel.setText(message.substring(10));

            } else if (message.startsWith("GAME_END:")) {
                gameActive = false;
                String[] parts = message.split(":", 3);
                String result = parts[1];
                String text = parts[2];

                if (result.equals("WIN")) {
                    statusLabel.setText("🎉 " + text + " 🎉");
                    statusLabel.setTextFill(Color.GREEN);
                    statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
                } else {
                    statusLabel.setText("😔 " + text);
                    statusLabel.setTextFill(Color.RED);
                }

                disableButtons();
                restartBtn.setDisable(false);
            }
        });
    }

    private void sendToServer(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void enableButtons() {
        take1Btn.setDisable(false);
        take2Btn.setDisable(false);
        take3Btn.setDisable(false);
        take4Btn.setDisable(false);
        take5Btn.setDisable(false);

        // Отключаем кнопки, если нельзя взять столько спичек
        take1Btn.setDisable(currentMatches < 1);
        take2Btn.setDisable(currentMatches < 2);
        take3Btn.setDisable(currentMatches < 3);
        take4Btn.setDisable(currentMatches < 4);
        take5Btn.setDisable(currentMatches < 5);
    }

    private void disableButtons() {
        take1Btn.setDisable(true);
        take2Btn.setDisable(true);
        take3Btn.setDisable(true);
        take4Btn.setDisable(true);
        take5Btn.setDisable(true);
    }

    private void reconnectToServer() {
        disconnectFromServer();
        connectToServer();
        restartBtn.setDisable(true);
        statusLabel.setTextFill(Color.BLACK);
        statusLabel.setStyle("");
        lastMoveLabel.setText("");
    }

    private void disconnectFromServer() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (serverListener != null) serverListener.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}