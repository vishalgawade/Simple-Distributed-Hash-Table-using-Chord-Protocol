package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Node {
    private String id;

    public void setSuccessor(String successor) {
        this.successor = successor;
    }

    public void setPredecessor(String predecessor) {
        this.predecessor = predecessor;
    }

    private Integer port;
    private String successor;
    private String predecessor;

    public Node(Integer port, String successor, String predecessor) {
        this.id = getNodeHash(port);
        this.port = port;
        this.successor = successor;
        this.predecessor = predecessor;
    }

    @Override
    public String toString() {
        return "port=" + port +
                ", successor=" + successor +
                ", predecessor=" + predecessor;
    }

    public String getId() {
        return id;
    }

    public Integer getPort() {
        return port;
    }

    public String getSuccessor() {
        return successor;
    }

    public String getPredecessor() {
        return predecessor;
    }

    public String getNodeHash(Integer port){
        try {
            return genHash(port.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e("Node Hash","Unable to get node Hash");
        }
        finally {
            return null;
        }
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
