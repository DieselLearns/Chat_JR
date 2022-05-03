package task3008.client;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import com.javarush.task.task30.task3008.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {

    protected Connection connection;

    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера");

        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите ваше имя");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Ошибка отправки сообщения!");
            clientConnected = false;
        }

    }

    public void run() {
        Thread st = getSocketThread();
        st.setDaemon(true);
        st.start();
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage(" Ошибка подключения!");
            }
        }
        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено.\n" +
                    "Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        while (clientConnected){
             String text = ConsoleHelper.readString();
            if (text.equals("exit")) {
                clientConnected = false;
            }
            if (shouldSendTextFromConsole()){
                sendTextMessage(text);
            }

        }

    }

    public class SocketThread extends Thread {

        @Override
        public void run(){
            String adress = getServerAddress();
            int port = getServerPort();
            try {
                Socket socket = new Socket(adress,port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if (message.getType()==null) throw new IOException("Unexpected MessageType");
                switch (message.getType()){
                    case NAME_REQUEST: {
                        connection.send(new Message(MessageType.USER_NAME,getUserName()));
                        break;
                    }

                    case NAME_ACCEPTED:{
                        notifyConnectionStatusChanged(true);
                        return;
                    }
                    default: throw new IOException("Unexpected MessageType");
                }

            }
        }
        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if (message.getType()==null) throw new IOException("Unexpected MessageType");
                switch (message.getType()){

                    case TEXT:{
                        processIncomingMessage(message.getData());
                        break;
                    }
                    case USER_ADDED:{
                        informAboutAddingNewUser(message.getData());
                        break;
                    }
                    case USER_REMOVED:{
                        informAboutDeletingNewUser(message.getData());
                        break;
                    }
                    default: throw new IOException("Unexpected MessageType");

                }
            }
        }


        protected void processIncomingMessage(String message){
            System.out.println(message);
        }

        protected void informAboutAddingNewUser(String userName){
            System.out.println(userName + " подключился к чату!");
        }

        protected void informAboutDeletingNewUser(String userName) {
            System.out.println(userName + " покинул чат!");
        }
        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }
    }
}
