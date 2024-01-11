package Cli;

/**
 * ---------------------------------------------------------------------------------------------------------------------
 * ArgumentsParser
 * ---------------------------------------------------------------------------------------------------------------------
 * Parses the CLI arguments into an Arguments object
 */
public class ArgumentsParser {
    /**
     * @param args CLI arguments
     * @return Parsed arguments
     */
    public static Arguments parseArgs(String[] args) {
        Arguments arguments = new Arguments();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-tcpport":
                    arguments.setPort(args[++i]);
                    break;
                case "-record":
                    arguments.setRecord(args[++i]);
                    break;
                case "-connect":
                    arguments.setConnect(args[++i]);
                    break;
                default:
                    System.out.printf("Invalid Argument: %s \n", args[i]);
                    System.exit(1);
            }
        }

        if (!arguments.getRecord().isSet()) {
            System.out.println("Record has not been set");
            System.exit(2);
        }

        if (arguments.getPort() == 0) {
            System.out.println("Port was not provided");
            System.exit(3);
        }

        return arguments;
    }
}
