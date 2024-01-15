# TCPDatabase

-----

## Implemented functions

- [X] Creating nodes that connect to each other
- [X] The network is ready to receive connections from the client and execute commands
- [X] Each node is capable of serving multiple clients simultaneously

-----

## Description of the implementation

The beginning of the application is in the file `DatabaseNode.java`, it starts by passing arguments to `Cli/ArgumentsParser`

This class parses the arguments and creates an instance of the `Cli/Arguments` class from them.
It saves there information about the port on which the node is to be launched, what it is to connect to and what data it is to save

The most important logic takes place in the `Node.java` file in the `Network` package.
It takes `Cli/Arguments` and starts `TCPServer` which handles incoming connections

It also creates an instance of `TCPClient` for each node to connect to and stores them in `this.clients` for further use.

On each connection, a node sends a `HELLO-NODE` message, and each node receives such a message,
saves the client from which it received it in `this.clients` and treats it the same as the nodes it itself connected to.

For example: We create node 1, then create node 2 and tell it to connect to node 1. Node 1 receives the message `HELLO-NODE`
and saves it in `this.clients`. Now node 1 can always send a message to node 2 and vice versa.

When receiving a message such as `get-value`, `set-value` and `find-key`, the node:

1. Checks whether it can handle this query itself.
2. If so, returns the value/message "OK"
3. If not, the node generates an ID, saves it to remember that it created it, and sends a query along with the ID to all clients in `this.clients`

This query has the format `VERB ID VALUE`, e.g. `SET 123445 14:42`

When receiving such a message, each node:

1. Checks whether a query with a given ID has already been handled
2. If so, it returns whatever is in the cache
3. If not, checks whether it can handle this query itself
4. If it can, it returns the value/message "OK" with the verb `RETURN` and ID, e.g. `RETURN 123445 14:42`
5. In this case, it saves the response in its cache using the ID as the key
6. If he is unable to handle it, he sends a request to all his clients, except those who asked him about it themselves

Each node monitors currently supported tasks in `this.clientsToRespond` and `this.waitingForResponseFrom`

- `this.clientsToRespond` - Monitors what other clients (another node or database client) sent requests with a given ID
- `this.waitingForResponseFrom` - Monitors to whom a given node sent a request with a given ID

Each time another node responds, it is removed from `this.waitingForResponseFrom`.
Then the value of `this.waitingForResponseFrom` is checked and if it is empty, we send `ERROR Not found` to the client who asked us about it (it may be another node).
The exception is the response with the value `RETURN` which means success, it does not wait for `this.waitingForResponseFrom` and is immediately forwarded

When receiving a message such as `get-min`, `get-max`, the node:

1. Checks its value and stores it in `this.minMaxCache`
2. Sends `GET-MIN ID` or `GET-MAX ID` to each node
3. Each node does the same, after receiving responses from its clients, it compares them with its value and returns the smallest/largest, e.g. `RETURN 1234345 12`
4. The master node receives all the values, compares them with its own and returns the smallest one to the client

When receiving a message such as `new-record`, `terminate`, the node itself handles these queries. Sets a value, or ends its existence and disconnects from clients
It is then removed from `this.clients` on other nodes.
