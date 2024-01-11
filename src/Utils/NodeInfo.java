package Utils;

/**
 * A simple tuple representing the information of a node
 */
public class NodeInfo {
    private final String address;
    private final int port;

    /**
     * @param address Address value
     * @param port    Port value
     */
    public NodeInfo(String address, int port) {
        this.address = address;
        this.port = port;
    }

    /**
     * @return Address value
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * @return Port value
     */
    public int getPort() {
        return this.port;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", this.address, this.port);
    }
}
