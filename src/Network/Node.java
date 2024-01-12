package Network;

import Cli.Arguments;
import Utils.ClientResponse;
import Utils.NodeInfo;
import Utils.NodeRecord;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * ---------------------------------------------------------------------------------------------------------------------
 * Node
 * ---------------------------------------------------------------------------------------------------------------------
 * Represents a node in the network
 * Example request string: "get-value 17
 * Available commands:
 * - get-value <key> - returns key:value from whole network
 * - set-value <key>:<value> - sets key to value in the network
 * - find-key <key> - returns the node that contains the key address:port
 * - get-max-key - returns the maximum key:value in the network
 * - get-min-key - returns the minimum key:value in the network
 * - new-record <key>:<value> - replaces the current record with a new one on this node
 * - terminate - terminates the Node
 */
public class Node {
    private final NodeRecord record;
    private final TCPServer server;

    private final Map<Integer, TCPClient> clients;
    private Map<String, String> responseCache;
    private Map<String, TCPClient> requestOrigin;
    private Map<String, Set<TCPClient>> clientsToRespond;
    private Map<String, Set<TCPClient>> waitingForResponseFrom;
    private Set<String> IDsOriginatedFromThisNode;
    private Map<String, Integer> minMaxCache;


    public Node(Arguments arguments) {
        this.responseCache = new HashMap<>();
        this.server = new TCPServer(arguments.getPort());
        this.record = arguments.getRecord();
        this.clients = new HashMap<>();
        this.clientsToRespond = new HashMap<>();
        this.IDsOriginatedFromThisNode = new HashSet<>();
        this.requestOrigin = new HashMap<>();
        this.waitingForResponseFrom = new HashMap<>();
        this.minMaxCache = new HashMap<>();

        if (arguments.getConnect() != null) {
            this.connect(arguments.getConnect());
        }
    }

    /**
     * Connects to all the nodes in the list
     *
     * @param connect Node to connect to
     */
    private void connect(ArrayList<NodeInfo> connect) {
        for (NodeInfo nodeInfo : connect) {
            try {
                Socket socket = new Socket(nodeInfo.getAddress(), nodeInfo.getPort());
                TCPClient client = new TCPClient(socket, this::handleMessage);
                System.out.printf("Connected to %s%n", nodeInfo);
                new Thread(client).start();
                this.clients.put(client.getPort(), client);
                client.send("HELLO-NODE");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Starts the TCP server and accept connections
     */
    public void start() {
        this.server.start();
        System.out.printf("Listening on port %s%n", this.server.getPort());
        while (true) {
            this.server.accept(this::handleMessage);
        }
    }

    private synchronized void handleMessage(ClientResponse message, TCPClient client) {
        System.out.printf("Received message from client: %s, %s%n", message.getPort(), message.getMessage());
        if (this.clients.containsKey(message.getPort())) {
            this.handleNodeMessage(message.getMessage(), client);
        } else {
            this.handleClientMessage(message, client);
        }
    }

    private synchronized void handleNodeMessage(String message, TCPClient tcpClient) {
        System.out.printf("Received message from node: %s%n", message);
        if (message == null) {
            System.out.println("Node disconnected");
            this.clients.remove(tcpClient.getPort());
            tcpClient.close();
            return;
        }

        String[] parts = message.split(" ");
        String verb = parts[0];
        String ID = parts[1];
        // rest of the message
        String msg = message.substring(verb.length() + ID.length() + 2);

        if (this.responseCache.containsKey(ID) && !verb.startsWith("RETURN")) {
            System.out.printf("Found key: %s in cache%n", this.responseCache.get(ID));
            this.respond(ID, this.returnResponse(ID, this.responseCache.get(ID)));
            return;
        }

        if (!this.IDsOriginatedFromThisNode.contains(ID) && !this.requestOrigin.containsKey(ID)) {
            this.requestOrigin.put(ID, tcpClient);
        }

        this.markAsResponded(ID, verb, msg, tcpClient);
        try {
            switch (verb) {
                case "GET":
                    this.addClientToRespond(ID, tcpClient);
                    this.getValue(Integer.parseInt(msg), ID);
                    break;
                case "GET-MAX":
                    this.handleMinMax(ID, tcpClient, "MAX");
                    break;
                case "GET-MIN":
                    this.handleMinMax(ID, tcpClient, "MIN");
                    break;
                case "SET":
                    this.addClientToRespond(ID, tcpClient);
                    this.setValue(msg, ID);
                    break;
                case "FIND":
                    this.addClientToRespond(ID, tcpClient);
                    this.findKey(Integer.parseInt(msg), ID);
                    break;
                case "RETURN":
                    this.waitingForResponseFrom.get(ID).clear();
                    this.respond(ID, this.returnResponse(ID, msg));
                    break;
                case "RETURN-MAX":
                    this.handleMax(ID, msg);
                    break;
                case "RETURN-MIN":
                    this.handleMin(ID, msg);
                    break;
                case "ERROR":
                    if (this.waitingForResponseFrom.containsKey(ID) && !this.waitingForResponseFrom.get(ID).isEmpty()) return;
                    this.respond(ID, this.returnResponse(ID, msg, "ERROR"));
                    break;
                default:
                    tcpClient.send("ERROR Invalid arguments");
            }
        } catch (Exception e) {
            tcpClient.send("ERROR Invalid arguments");
        }
    }

    private void addClientToRespond(String id, TCPClient tcpClient) {
        if (!this.clientsToRespond.containsKey(id)) {
            this.clientsToRespond.put(id, new HashSet<>());
        }

        this.clientsToRespond.get(id).add(tcpClient);
    }

    private void handleMinMax(String ID, TCPClient tcpClient, String type) {
        String returnMsg = String.format("RETURN-%s", type);
        if (this.IDsOriginatedFromThisNode.contains(ID)) {
            tcpClient.send(this.returnResponse(ID, String.valueOf(this.record.getValue()), returnMsg));
        }

        this.addClientToRespond(ID, tcpClient);
        this.getMinMax(ID, type);
    }

    private synchronized void handleClientMessage(ClientResponse message, TCPClient client) {
        System.out.printf("Received message from client: %s%n", message.getMessage());

        if (message.getMessage() == null) {
            System.out.println("Client disconnected");
            client.close();
            this.clients.remove(message.getPort());
            return;
        }

        String[] parts = message.getMessage().split(" ");
        String ID;
        try {
            switch (parts[0]) {
                case "HELLO-NODE":
                    if (!this.clients.containsKey(message.getPort())) {
                        this.clients.put(message.getPort(), client);
                        System.out.println(this.clients);
                    }
                    break;
                case "get-value":
                    ID = this.getRootID(client);
                    this.getValue(Integer.parseInt(parts[1]), ID);
                    break;
                case "new-record":
                    client.send(this.newRecord(parts[1]));
                    client.close();
                    break;
                case "set-value":
                    ID = this.getRootID(client);
                    this.setValue(parts[1], ID);
                    break;
                case "find-key":
                    ID = this.getRootID(client);
                    this.findKey(Integer.parseInt(parts[1]), ID);
                    break;
                case "get-max":
                    ID = this.getRootID(client);
                    this.getMinMax(ID, "MAX");
                    break;
               case "get-min":
                   ID = this.getRootID(client);
                   this.getMinMax(ID, "MIN");
                   break;
                case "terminate":
                    System.out.println("Terminating");
                    System.exit(0);
                default:
                    client.send("ERROR Invalid arguments");
                    client.close();
            }
        } catch (Exception e) {
            client.send("ERROR Invalid arguments");
            client.close();
        }
    }

    /**
     * @param ID  - ID of request
     * @param msg - node max value
     */
    private void handleMax(String ID, String msg) {
        System.out.printf("Received max value from node: %s%n", msg);
        int max = Integer.parseInt(msg);
        if (this.minMaxCache.get(ID) < max) {
            this.minMaxCache.put(ID, max);
        }

        if (this.waitingForResponseFrom.get(ID).isEmpty()) {
            this.respond(ID, this.returnResponse(ID, String.valueOf(this.minMaxCache.get(ID)), "RETURN-MAX"));
        }
    }

    /**
     * @param ID  - ID of request
     * @param msg - node max value
     */
    private void handleMin(String ID, String msg) {
        System.out.printf("Received min value from node: %s%n", msg);
        int min = Integer.parseInt(msg);
        if (this.minMaxCache.get(ID) > min) {
            this.minMaxCache.put(ID, min);
        }

        if (this.waitingForResponseFrom.get(ID).isEmpty()) {
            this.respond(ID, this.returnResponse(ID, String.valueOf(this.minMaxCache.get(ID)), "RETURN-MIN"));
        }
    }

    /**
     * get-max-key - returns the maximum value in the network
     * get-min-key - returns the minimum value in the network
     *
     * @param ID - ID of request used to cache the response
     */
    private void getMinMax(String ID, String type) {
        System.out.printf("Searching for %s key. ID %s%n", type, ID);
        if (!this.minMaxCache.containsKey(ID)) {
            this.minMaxCache.put(ID, this.record.getValue());
        }

        this.poll(ID, String.format("GET-%s", type), "");

        String returnMsg = String.format("RETURN-%s", type);

        if (this.waitingForResponseFrom.get(ID).isEmpty()) {
            this.respond(ID, this.returnResponse(ID, String.valueOf(this.minMaxCache.get(ID)), returnMsg));
        }
    }

    private String getRootID(TCPClient client) {
        String ID = this.getRandomID();
        this.addClientToRespond(ID, client);

        this.IDsOriginatedFromThisNode.add(ID);
        return ID;
    }

    /**
     * return <ID> <key>:<value>
     *
     * @param ID       - ID
     * @param response - key:value
     * @return ID:key:value
     */
    private String returnResponse(String ID, String response) {
        return this.returnResponse(ID, response, "RETURN");
    }

    private String returnResponse(String ID, String response, String verb) {
        System.out.printf("Returning response for ID: %s%n", ID);
        String result;

        if (this.IDsOriginatedFromThisNode.contains(ID)) {
            result = response;
        } else {
            result = String.format("%s %s %s", verb, ID, response);
        }

        return result;
    }

    /**
     * find-key <key>
     *
     * @param key - key to find
     * @param ID  - ID of request used to cache the response
     */
    private void findKey(int key, String ID) {
        System.out.printf("Searching for key: %s%n", key);
        if (this.record.has(key)) {
            System.out.printf("Found key: %s%n", key);
            this.respond(ID, this.returnResponse(ID, this.server.toString()));
            return;
        }

        this.poll(ID, "FIND", String.valueOf(key));

        if (this.waitingForResponseFrom.get(ID).isEmpty()) {
            this.respond(ID, this.returnResponse(ID, "ERROR: Not found", "ERROR"));
        }
    }

    /**
     * set-value <key>:<value>
     *
     * @param part - key:value
     * @param ID   - ID of request used to cache the response
     */
    private void setValue(String part, String ID) {
        String[] parts = part.split(":");
        int key = Integer.parseInt(parts[0]);
        int value = Integer.parseInt(parts[1]);
        System.out.printf("Setting key: %d to value %d. ID %s%n", key, value, ID);

        if (this.record.has(key)) {
            this.record.setValue(part);
            this.respond(ID, this.returnResponse(ID, "OK"));
            return;
        }

        this.poll(ID, "SET", part);

        if (this.waitingForResponseFrom.get(ID).isEmpty()) {
            this.respond(ID, this.returnResponse(ID, "ERROR: Not found", "ERROR"));
        }
    }

    /**
     * get-value <key>
     *
     * @param key - key to get
     * @param ID  - ID of request used to cache the response
     */
    private void getValue(int key, String ID) {
        System.out.printf("Searching for key: %s. ID %s%n", key, ID);
        if (this.record.has(key)) {
            System.out.printf("Found key: %s%n", key);
            this.respond(ID, this.returnResponse(ID, this.record.toString()));
            return;
        }

        this.poll(ID, "GET", String.valueOf(key));

        if (this.waitingForResponseFrom.get(ID).isEmpty()) {
            this.respond(ID, this.returnResponse(ID, "ERROR: Not found", "ERROR"));
        }
    }

    /**
     * set-value <key>:<value>
     *
     * @param keyValue - key:value to set
     * @return OK response
     */
    private String newRecord(String keyValue) {
        this.record.setValue(keyValue);
        return "OK";
    }

    /**
     * Generates a random ID
     *
     * @return Random ID
     */
    private String getRandomID() {
        Random random = new Random();
        return random.ints(48, 123)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Respond to client with ID
     *
     * @param ID       - ID of request
     * @param response - response to send
     */
    private void respond(String ID, String response) {
        System.out.printf("Responding to client with ID: %s; With message: %s%n", ID, response);

        if (!this.clientsToRespond.containsKey(ID)) {
            System.out.printf("Clients with ID: %s not found%n", ID);
            return;
        }

        for (TCPClient client : this.clientsToRespond.get(ID)) {
            client.send(response);
            if (!response.contains(ID)) {
                client.close();
            }

            this.clientsToRespond.remove(ID);
        }
    }

    /**
     * Polls the clients for a response
     *
     * @param ID      - ID of request
     * @param verb    - verb of request
     * @param message - message of request
     */
    private void poll(String ID, String verb, String message) {
        System.out.printf("Polling for response with ID: %s%n", ID);
        if (!this.waitingForResponseFrom.containsKey(ID)) {
            this.waitingForResponseFrom.put(ID, new HashSet<>());
        }

        for (TCPClient client : this.clients.values()) {
            if (this.clientsToRespond.containsKey(ID) && this.clientsToRespond.get(ID).contains(client)) continue;
            if (this.waitingForResponseFrom.get(ID).contains(client)) continue;
            if (this.requestOrigin.get(ID) == client) continue;

            System.out.printf("Polling client: %s%n", client.getPort());

            client.send(String.format("%s %s %s", verb, ID, message));
            this.waitingForResponseFrom.get(ID).add(client);
        }
    }

    /**
     * Marks the client as responded
     *
     * @param ID      - ID of request
     * @param verb - verb of request
     * @param msg - message of request
     * @param client  - client that responded
     */
    private void markAsResponded(String ID, String verb, String msg, TCPClient client) {
        if (!this.waitingForResponseFrom.containsKey(ID)) return;

        System.out.printf("Marking client as responded with ID: %s%n", ID);

        if (verb.startsWith("RETURN") || (!this.responseCache.containsKey(ID) && verb.startsWith("ERROR"))) {
            this.responseCache.put(ID, msg);
        }

        this.waitingForResponseFrom.get(ID).remove(client);
    }
}
