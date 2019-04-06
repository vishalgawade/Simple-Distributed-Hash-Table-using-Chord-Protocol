package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static String [] remotePort = {"11108","11112","11116","11120","11124"};
    private Node myNode;
    private String leaderPort=remotePort[0];
    //keep track of all nodes in ring.We used linked list to form ring so it is not good idea to traverse linked list
    //to just find every time if node is present in ring or not
    private Set<String> allNodesInRing=new HashSet<String>();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        //setting node itself as its predecessor and successor
        myNode=new Node(Integer.parseInt(myPort),myPort,myPort);

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            //we assumed that port 5504(avd0) will be the leader always if it comes online
            // therefore no need to send msg to anyone
            if(!myPort.equals(leaderPort)){
                //telling everyone hey, i am joining the network
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "JOIN", myPort);
            }
            //if current port is not avd0 then we have to every other nodes in network i am joining the network
            else{
                //add node to ring
                allNodesInRing.add(leaderPort);
            }
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return true;
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
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

    public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket client;
            InputStream is;
            BufferedInputStream br;
            ByteArrayOutputStream ret;
            BufferedReader bf;
            String msg = "";
            DataInputStream di = null;
            DataOutputStream ds = null;
            //reference:https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
            //reference:https://docs.oracle.com/javase/7/docs/api/java/io/BufferedReader.html
            while (!serverSocket.isClosed()) {
                try {
                    // serverSocket.setSoTimeout(5000);
                    //server accepts from client
                    client = serverSocket.accept();
                    is = client.getInputStream();
                    // bf = new BufferedReader(new InputStreamReader(is));
                    String s = null;
                    di = new DataInputStream(is);
                    msg = di.readUTF();
                    //split message to check msg type
                    String [] msgSplit=msg.split(",");
                    //if node add to ring request
                    //we already checked for leader avd0,so don't worry about it.
                    if(msgSplit.length==2&&msgSplit[0].equals("ADD")&& myNode.getPort().equals(leaderPort)){
                        if(allNodesInRing.size()==1){
                            myNode.setPredecessor(msgSplit[1]);
                            myNode.setSuccessor(msgSplit[1]);
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Can't listen to client/issue with connection");
                }
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        Socket socket;
        DataOutputStream ds=null;
        DataInputStream di=null;
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                if (msgs[0].equals("JOIN")) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(msgs[1]));
                    OutputStream os = socket.getOutputStream();
                    ds = new DataOutputStream(os);
                    //tell leader to add this node to his ring
                    ds.writeUTF("ADD,"+msgs[0]);

                }
            }
            catch (Exception e){
                Log.e(TAG, "ClientTask socket Exception for client "+msgs[1]);
            }
            return null;
        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

//    private boolean is
}
