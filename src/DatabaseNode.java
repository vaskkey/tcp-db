import Cli.Arguments;
import Cli.ArgumentsParser;
import Network.Node;

/**---------------------------------------------------------------------------------------------------------------------
 *                                             DatabaseNode
 * ---------------------------------------------------------------------------------------------------------------------
 * Represents the entry point of the application
 */
public class DatabaseNode {
    public static void main(String[] args) {
        Arguments arguments = ArgumentsParser.parseArgs(args);
        Node node = new Node(arguments);
        node.start();
    }
}