package Network;

import Cli.Arguments;
import Utils.ClientResponse;
import Utils.NodeInfo;
import Utils.NodeRecord;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**---------------------------------------------------------------------------------------------------------------------
 *                                                    Node
 * ---------------------------------------------------------------------------------------------------------------------
 * Represents a node in the network
 * Example request string: "get-value 17
 * Available commands:
 * - get-value <key> - returns key:value
 * - set-value <key>:<value> - sets key to value
 * - find-key <key> - returns the node that contains the key address:port
 * - get-max-key - returns the maximum key:value in the network
 * - get-min-key - returns the minimum key:value in the network
 * - new-record <key>:<value> - replaces the current record with a new one
 * - terminate - terminates the Node
 */
public class Node {
    private final NodeRecord record;
    private final TCPServer server;

    private final Map<Integer, TCPClient> clients;
    private Map<String, String> responseCache;
    private Map<String, TCPClient> clientsToRespond;
    private Set<String> IDsOriginatedFromThisNode;


    public Node(Arguments arguments) {
        this.responseCache = new HashMap<>();
        this.server = new TCPServer(arguments.getPort());
        this.record = arguments.getRecord();
        this.clients = new HashMap<>();
        this.clientsToRespond = new HashMap<>();
        this.IDsOriginatedFromThisNode = new HashSet<>();

        if (arguments.getConnect() != null) {
            this.connect(arguments.getConnect());
        }
    }

    /**
     * Connects to all the nodes in the list
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
        System.out.printf("Received message from client: %s, %s%n",message.getPort(), message.getMessage());
        if (this.clients.containsKey(message.getPort())) {
            this.handleNodeMessage(message.getMessage(), client);
        } else {
            this.handleClientMessage(message, client);
        }
    }

    private synchronized void handleNodeMessage(String message, TCPClient tcpClient) {
        System.out.printf("Received message from node: %s%n", message);
        String[] parts = message.split(" ");
        String verb = parts[0];
        String ID = parts[1];
        String msg = parts[2];
        try {
            switch (verb) {
                case "GET":
                    this.clientsToRespond.put(ID, tcpClient);
                    this.getValue(Integer.parseInt(msg), ID);
                    break;
                case "RETURN":
                    this.respond(ID, msg);
                    break;
                default:
                    System.out.println(msg);
                    tcpClient.send("ERROR: Invalid arguments");
                    System.exit(69);
            }
        } catch (Exception e) {
            e.printStackTrace();
            tcpClient.send("ERROR: Invalid arguments");
            System.exit(69);
        }
    }

    private synchronized void handleClientMessage(ClientResponse message, TCPClient client) {
        System.out.printf("Received message from client: %s%n", message.getMessage());
        String[] parts = message.getMessage().split(" ");
        try {
           switch (parts[0]) {
               case "HELLO-NODE":
                   if (!this.clients.containsKey(message.getPort())){
                    this.clients.put(message.getPort(), client);
                    System.out.println(this.clients);
                   }
                   break;
               case "get-value":
                   String ID = this.getRandomID();
                   this.clientsToRespond.put(ID, client);
                   this.IDsOriginatedFromThisNode.add(ID);
                   this.getValue(Integer.parseInt(parts[1]), ID);
                   break;
               case "set-value":
                   client.send(this.setValue(parts[1]));
                   break;
//                   case "find-key":
//                       return this.findKey(Integer.parseInt(parts[++i]));
//                   case "get-max-key":
//                       return this.getMaxKey();
//                   case "get-min-key":
//                       return this.getMinKey();
//                   case "new-record":
//                       return this.newRecord(parts[++i]);
               case "terminate":
                   System.out.println("Terminating");
                   System.exit(0);
               default:
                   client.send("ERROR: Invalid arguments");
           }
        } catch (Exception e) {
            client.send("ERROR: Invalid arguments");
        }
    }

    /**
     * return <ID> <key>:<value>
     * @param ID - ID
     * @param response - key:value
     * @return ID:key:value
     */
    private String returnResponse(String ID, String response) {
        System.out.printf("Returning response for ID: %s%n", ID);
        String result;

        if (this.IDsOriginatedFromThisNode.contains(ID)) {
            result = response;
        } else {
            result = String.format("RETURN %s %s", ID, response);
        }

        this.responseCache.put(ID, response);
        return result;
    }

    /**
     * get-value <key>
     * @param key - key to get
     * @param ID - ID of request used to cache the response
     * @return key:value
     */
    private void getValue(int key, String ID) {
        System.out.printf("Searching for key: %s. ID %s%n", key, ID);
        if(this.record.has(key)) {
            System.out.printf("Found key: %s%n", key);
            this.respond(ID, this.returnResponse(ID, this.record.toString()));
            return;
        }

        if (this.responseCache.containsKey(ID)) {
            System.out.printf("Found key: %s in cache%n", key);
            this.respond(ID, this.returnResponse(ID, this.responseCache.get(ID)));
            return;
        }

        for (TCPClient client : this.clients.values()) {
            client.send(String.format("GET %s %s", ID, key));
        }
    }

    /**
     * set-value <key>:<value>
     * @param keyValue - key:value to set
     * @return OK response
     */
    private String setValue(String keyValue) {
        this.record.setValue(keyValue);
        return "OK";
    }

    /**
     * Generates a random ID
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
     * @param ID - ID of request
     * @param response - response to send
     */
    private void respond(String ID, String response) {
        System.out.printf("Responding to client with ID: %s%n", ID);
        this.clientsToRespond.get(ID).send(response);
        this.responseCache.put(ID, response);
        this.clientsToRespond.remove(ID);
    }
}
