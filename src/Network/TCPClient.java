package Network;

import Utils.ClientResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPClient implements Runnable {
    public interface MessageHandler {
        void handle(ClientResponse response, TCPClient client);
    }

    private final Socket clientSocket;
    private MessageHandler handler;

    public TCPClient(Socket clientSocket, MessageHandler handler) {
        this.clientSocket = clientSocket;
        this.setMessageHandler(handler);
    }

    public void run() {
        this.receive();
    }

    public void receive() {
        while (true) {
            this.handler.handle(this.readLine(), this);
        }
    }

    /**
     * Sends a message to the client
     *
     * @param message The message to send
     */
    public synchronized void send(String message) {
        try {
            PrintWriter out = new PrintWriter(this.clientSocket.getOutputStream(), true);
            out.println(message);
            System.out.println(String.format("Sent: %s", message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads messages from the client
     */
    public ClientResponse readLine() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            return new ClientResponse(in.readLine(), this.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Closes the client socket
     */
    public void close() {
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the message handler
     */
    public void setMessageHandler(MessageHandler handler) {
        this.handler = handler;
    }

    /**
     * @return The port the client is connected to
     */
    public int getPort() {
        return this.clientSocket.getPort();
    }
}
