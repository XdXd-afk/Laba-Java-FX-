import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;

public class ClientApplication extends Application {

    // Элементы GUI
    private ProgressBar progressBar;
    private Button startButton;
    private Button pauseResumeButton;
    private Button stopButton;
    private Label statusLabel;
    private Label connectionLabel;

    // Сетевые компоненты
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread serverListener;

    // Состояние
    private boolean isPaused = false;
    private boolean isConnected = false;

    @Override
    public void start(Stage primaryStage) {
        // Создание элементов интерфейса
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(30);

        startButton = new Button("Старт");
        startButton.setPrefWidth(100);

        pauseResumeButton = new Button("Пауза");
        pauseResumeButton.setPrefWidth(100);
        pauseResumeButton.setDisable(true);

        stopButton = new Button("Стоп");
        stopButton.setPrefWidth(100);
        stopButton.setDisable(true);

        statusLabel = new Label("Статус: Отключен");
        connectionLabel = new Label("Подключение: Не подключено");

        // Панель кнопок
        VBox buttonBox = new VBox(10, startButton, pauseResumeButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);

        // Основной layout
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(
                progressBar,
                statusLabel,
                connectionLabel,
                buttonBox
        );

        // Обработчики кнопок
        startButton.setOnAction(e -> handleStart());
        pauseResumeButton.setOnAction(e -> handlePauseResume());
        stopButton.setOnAction(e -> handleStop());

        // Настройка сцены
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Клиент ProgressBar");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Подключение к серверу
        connectToServer();

        // Обработка закрытия окна
        primaryStage.setOnCloseRequest(e -> {
            disconnectFromServer();
        });
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5555);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isConnected = true;

            // Запуск потока для прослушивания сервера
            serverListener = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        processServerMessage(message);
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        connectionLabel.setText("Подключение: Разорвано");
                        statusLabel.setText("Статус: Отключен");
                        resetButtons();
                    });
                    isConnected = false;
                }
            });
            serverListener.setDaemon(true);
            serverListener.start();

            Platform.runLater(() -> {
                connectionLabel.setText("Подключение: Установлено");
                startButton.setDisable(false);
            });

        } catch (IOException e) {
            Platform.runLater(() -> {
                connectionLabel.setText("Подключение: Ошибка. Запустите сервер!");
                showErrorDialog("Не удалось подключиться к серверу");
            });
        }
    }

    private void disconnectFromServer() {
        if (isConnected && out != null) {
            out.println("DISCONNECT");
            try {
                if (socket != null) socket.close();
                if (serverListener != null) serverListener.interrupt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processServerMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("PROGRESS:")) {
                // Обновление прогресса
                String progressStr = message.substring(9);
                double progress = Double.parseDouble(progressStr);
                progressBar.setProgress(progress);
                statusLabel.setText(String.format("Статус: Выполнено %.1f%%", progress * 100));

            } else if (message.equals("COMPLETE")) {
                // Завершение работы
                progressBar.setProgress(1.0);
                statusLabel.setText("Статус: Завершено");
                startButton.setDisable(false);
                pauseResumeButton.setDisable(true);
                stopButton.setDisable(true);
                pauseResumeButton.setText("Пауза");
                isPaused = false;

            } else if (message.equals("STOPPED")) {
                // Остановка
                progressBar.setProgress(0);
                statusLabel.setText("Статус: Остановлен");
                startButton.setDisable(false);
                pauseResumeButton.setDisable(true);
                stopButton.setDisable(true);
                pauseResumeButton.setText("Пауза");
                isPaused = false;
            }
        });
    }

    private void handleStart() {
        if (!isConnected) {
            showErrorDialog("Нет соединения с сервером");
            return;
        }

        out.println("START");
        startButton.setDisable(true);
        pauseResumeButton.setDisable(false);
        stopButton.setDisable(false);
        statusLabel.setText("Статус: Запущен");
    }

    private void handlePauseResume() {
        if (!isConnected) return;

        if (isPaused) {
            // Возобновление
            out.println("RESUME");
            pauseResumeButton.setText("Пауза");
            statusLabel.setText("Статус: Возобновлен");
            isPaused = false;
        } else {
            // Пауза
            out.println("PAUSE");
            pauseResumeButton.setText("Продолжить");
            statusLabel.setText("Статус: На паузе");
            isPaused = true;
        }
    }

    private void handleStop() {
        if (!isConnected) return;

        out.println("STOP");
        resetButtons();
        statusLabel.setText("Статус: Остановка...");
    }

    private void resetButtons() {
        startButton.setDisable(false);
        pauseResumeButton.setDisable(true);
        stopButton.setDisable(true);
        pauseResumeButton.setText("Пауза");
        isPaused = false;
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