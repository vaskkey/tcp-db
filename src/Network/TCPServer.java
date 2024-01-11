package Network;

import java.net.*;
import java.io.*;

public class TCPServer {
    private final int port;
    private ServerSocket serverSocket;


    /**
     * @param port The port to bind to
     */
    public TCPServer(int port) {
        this.port = port;
    }

    /**
     * Starts the server and waits for a client to connect
     */
    public void start() {
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void accept(TCPClient.MessageHandler messageHandler) {
        try {
            Socket clientSocket = this.serverSocket.accept();
            TCPClient handler = new TCPClient(clientSocket, messageHandler);
            Thread thread = new Thread(handler);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return The port the server is bound to
     */
    public String getPort() {
        return String.valueOf(this.port);
    }

    /**
     * @return Server Data <address>:<port>
     */
    public String toString() {
        return String.format("localhost:%s", this.port);
    }
}
