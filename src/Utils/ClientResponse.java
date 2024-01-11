package Utils;

/**
 * ---------------------------------------------------------------------------------------------------------------------
 * ClientResponse
 * ---------------------------------------------------------------------------------------------------------------------
 * Represents a response from client
 */
public class ClientResponse {
    String message;
    int port;

    public ClientResponse(String message, int port) {
        this.message = message;
        this.port = port;
    }

    public String getMessage() {
        return this.message;
    }

    public int getPort() {
        return this.port;
    }
}
