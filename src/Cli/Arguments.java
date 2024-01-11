package Cli;

import Utils.NodeInfo;
import Utils.NodeRecord;

import java.util.ArrayList;

/**
 * Represents the arguments passed to the program
 */
public class Arguments {
    private int port;
    private NodeRecord record;


    private ArrayList<NodeInfo> connect;

    public Arguments() {
        this.connect = new ArrayList<>();
        this.record = new NodeRecord();
    }

    /**
     * @param port Port value
     */
    public void setPort(String port) {
        this.setPort(Integer.parseInt(port));
    }

    /**
     * @param port Port value
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return Port value
     */
    public int getPort() {
        return this.port;
    }

    /**
     * @param connect Node to connect to - address:port
     */
    public void setConnect(String connect) {
        String[] parts = connect.split(":");
        this.connect.add(new NodeInfo(parts[0], Integer.parseInt(parts[1])));
    }

    /**
     * @return Nodes to connect to
     */
    public ArrayList<NodeInfo> getConnect() {
        return this.connect;
    }

    /**
     * @param record Record to store - key:value
     */
    public void setRecord(String record) {
        String[] parts = record.split(":");
        this.record.setValue(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * @return Stored record
     */
    public NodeRecord getRecord() {
        return this.record;
    }
}
