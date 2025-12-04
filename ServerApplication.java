import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ServerApplication {
    private static final int PORT = 5555;
    private static Map<Integer, ClientHandler> clients = new HashMap<>();
    private static int clientIdCounter = 1;

    public static void main(String[] args) {
        System.out.println("Сервер ProgressBar запущен...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientIdCounter++;
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                clients.put(clientId, clientHandler);
                new Thread(clientHandler).start();
                System.out.println("Клиент #" + clientId + " подключен");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(int clientId) {
        clients.remove(clientId);
        System.out.println("Клиент #" + clientId + " отключен");
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private int clientId;
        private PrintWriter out;
        private BufferedReader in;
        private WorkerThread workerThread;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Клиент #" + clientId + ": " + message);
                    processCommand(message);
                }
            } catch (IOException e) {
                System.out.println("Ошибка соединения с клиентом #" + clientId);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ServerApplication.removeClient(clientId);
            }
        }

        private void processCommand(String command) {
            switch (command) {
                case "START":
                    startWorker();
                    break;
                case "PAUSE":
                    pauseWorker();
                    break;
                case "RESUME":
                    resumeWorker();
                    break;
                case "STOP":
                    stopWorker();
                    break;
                case "DISCONNECT":
                    stopWorker();
                    break;
            }
        }

        private void startWorker() {
            if (workerThread != null && workerThread.isAlive()) {
                workerThread.interrupt();
            }
            workerThread = new WorkerThread(out);
            workerThread.start();
            System.out.println("Поток запущен для клиента #" + clientId);
        }

        private void pauseWorker() {
            if (workerThread != null) {
                workerThread.setPaused(true);
                System.out.println("Поток приостановлен для клиента #" + clientId);
            }
        }

        private void resumeWorker() {
            if (workerThread != null) {
                workerThread.setPaused(false);
                synchronized (workerThread) {
                    workerThread.notify();
                }
                System.out.println("Поток возобновлен для клиента #" + clientId);
            }
        }

        private void stopWorker() {
            if (workerThread != null) {
                workerThread.interrupt();
                workerThread = null;
                System.out.println("Поток остановлен для клиента #" + clientId);
            }
        }
    }

    static class WorkerThread extends Thread {
        private PrintWriter out;
        private volatile boolean paused = false;
        private volatile boolean stopped = false;

        public WorkerThread(PrintWriter out) {
            this.out = out;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i <= 1000; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }

                    // Обработка паузы
                    synchronized (this) {
                        while (paused) {
                            wait();
                        }
                    }

                    // Отправка прогресса клиенту
                    double progress = (double) i / 1000;
                    out.println("PROGRESS:" + progress);

                    // Имитация работы
                    Thread.sleep(20);

                    if (i == 1000) {
                        out.println("COMPLETE");
                    }
                }
            } catch (InterruptedException e) {
                // Поток был остановлен
                out.println("STOPPED");
            }
        }
    }
}