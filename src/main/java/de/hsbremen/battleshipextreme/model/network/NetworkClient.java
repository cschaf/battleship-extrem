package de.hsbremen.battleshipextreme.model.network;

import de.hsbremen.battleshipextreme.network.IDisposable;
import de.hsbremen.battleshipextreme.network.ITransferable;
import de.hsbremen.battleshipextreme.network.TransferableObjectFactory;
import de.hsbremen.battleshipextreme.network.eventhandling.ErrorHandler;
import de.hsbremen.battleshipextreme.network.eventhandling.EventArgs;
import de.hsbremen.battleshipextreme.network.eventhandling.listener.IErrorListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class NetworkClient implements IDisposable {
    private String serverIp;
    private int serverPort;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    private Sender sender;
    private Listener listener;
    //private String username;
    private ErrorHandler errorHandler;
    private ArrayList<IServerObjectReceivedListener> tempServerObjectReceivedListeners;
    private boolean isConnected;

    public NetworkClient() {
        serverIp = "localhost";
        //username = "Player";
        serverPort = 1337;
        this.errorHandler = new ErrorHandler();
        this.tempServerObjectReceivedListeners = new ArrayList<IServerObjectReceivedListener>();
    }

    public NetworkClient(String serverIp, int serverPort, String username) {
        this();
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        //this.username = username;
    }

    public void addErrorListener(IErrorListener listener) {
        this.errorHandler.addErrorListener(listener);
    }

    public void removeErrorListener(IErrorListener listener) {
        this.errorHandler.removeErrorListener(listener);
    }

    public void addServerObjectReceivedListener(IServerObjectReceivedListener listener) {
        if (this.listener == null) {
            tempServerObjectReceivedListeners.add(listener);
        } else {
            this.listener.addServerObjectReceivedListener(listener);
        }
    }

    public void removeServerObjectReceivedListener(IServerObjectReceivedListener listener) {
        this.listener.removeServerObjectReceivedListener(listener);
    }

    /**
     * Connect to Server
     */
    public void connect() {
        try {
            socket = new Socket(serverIp, serverPort);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            isConnected = true;
        } catch (Exception e) {
            isConnected = false;
            errorHandler.errorHasOccurred(new EventArgs<ITransferable>(this, TransferableObjectFactory.CreateMessage("Can not establish connection to " + serverIp + ":" + serverPort)));
        }

        // Create and start Sender thread
        this.sender = new Sender(socket, out);
        this.sender.start();

        this.listener = new Listener(in, this.errorHandler);
        for (IServerObjectReceivedListener tempListener : this.tempServerObjectReceivedListeners) {
            this.listener.addServerObjectReceivedListener(tempListener);
        }
        this.tempServerObjectReceivedListeners.clear();
        this.listener.start();
    }

    public void dispose() {
        try {
            if (this.listener != null) {
                this.listener.dispose();
                this.listener = null;
            }
            if (this.sender != null) {
                this.sender.dispose();
                this.sender = null;
            }
            if (this.out != null) {
                this.out.close();
                this.out = null;
            }
            if (this.in != null) {
                this.in.close();
                this.in = null;
            }
            if (this.socket != null) {
                this.socket.close();
                this.socket = null;
            }
            isConnected = false;

        } catch (IOException e) {
            errorHandler.errorHasOccurred(new EventArgs<ITransferable>(this, TransferableObjectFactory.CreateMessage("Could not dispose clientobject")));
        }
    }

    public void setIp(String ip) {
        this.serverIp = ip;
    }

    public void setPort(int port) {
        this.serverPort = port;
    }

//    public void setUsername(String username) {
//        this.username = username;
//    }

    public boolean isConnected() {
        return isConnected;
    }

    public Sender getSender() {
        return sender;
    }

    public void join(String id) {
        this.sender.sendJoin(id);
    }
}
