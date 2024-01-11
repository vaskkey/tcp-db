package Utils;

/**
 * Represents data that node stores - key - int, value - int
 */
public class NodeRecord {
    private int key;
    private int value;
    private boolean isNull;

    public NodeRecord() {
        this.isNull = true;
    }

    /**
     * @param key   Key value
     * @param value Value value
     */
    public NodeRecord(int key, int value) {
        this.key = key;
        this.value = value;
        this.isNull = false;
    }

    /**
     * @return Whether the key is present
     */
    public boolean has(int key) {
        return this.key == key;
    }

    /**
     * @return Value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * @param key   Key to assign value to
     * @param value Value to be assigned
     */
    public void setValue(int key, int value) {
        this.key = key;
        this.value = value;
        this.isNull = false;
    }

    /**
     * @param keyValuePair Key and value pair to be assigned key:value
     */
    public void setValue(String keyValuePair) {
        String[] parts = keyValuePair.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid key:value pair");
        }
        this.setValue(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * @return Whether the record was set
     */
    public boolean isSet() {
        return !this.isNull;
    }

    /**
     * @return key:value
     */
    @Override
    public String toString() {
        return String.format("%d:%d", this.key, this.value);
    }
}
