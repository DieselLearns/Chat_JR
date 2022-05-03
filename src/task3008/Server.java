package task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> pair : connectionMap.entrySet()) {
            try {
                pair.getValue().send(message);
            } catch (IOException e) {
                System.out.println("Не смогли отправить сообщение " + pair.getKey());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Введите порт сервера");
        int port = ConsoleHelper.readInt();
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            System.out.println("Server started");

            while (true) {
                Socket socket = ss.accept();
                Thread handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ss != null)
                ss.close();
        }
    }

    private static class Handler extends Thread {
        Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run(){
            ConsoleHelper.writeMessage("Установслено соединение " + socket.getRemoteSocketAddress());
            Connection connection = null;
            String userName = "";
            try {
                connection = new Connection(socket);
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection,userName);
                serverMainLoop(connection,userName);
            } catch (IOException | ClassNotFoundException e) {
               ConsoleHelper.writeMessage("Соединение прервано... (0)");
            }
            connectionMap.remove(userName);
            sendBroadcastMessage(new Message(MessageType.USER_REMOVED,userName));


        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {

            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message introduced = connection.receive();
                if (introduced.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage(" Некорректное собщение от " + socket.getRemoteSocketAddress() + " , ещё раз!");
                    continue;
                }

                String userName = introduced.getData();

                if (userName.isEmpty()) {
                    ConsoleHelper.writeMessage(" Пустое имя от " + socket.getRemoteSocketAddress() + "  , ещё раз!");
                    continue;
                }
                if (connectionMap.containsKey(introduced.getData())) {
                    ConsoleHelper.writeMessage("Попытка подключения к серверу с уже используемым именем от " + socket.getRemoteSocketAddress());
                    continue;
                }
                connectionMap.put(userName, connection);

                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String,Connection> pair:connectionMap.entrySet()) {

                if(!userName.equals(pair.getKey())){
                    connection.send(new Message(MessageType.USER_ADDED, pair.getKey()));
                }

            }
        }


        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{

            for(;;) {
                Message message = connection.receive();

                if (message.getType() == MessageType.TEXT) {
                    String text = userName + ": " + message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, text));
                } else {
                    ConsoleHelper.writeMessage("Ошибка отправки сообщения");
                }
            }

        }




    }
}
