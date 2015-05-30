package de.hsbremen.battleshipextreme.server;

import de.hsbremen.battleshipextreme.network.IDisposable;
import de.hsbremen.battleshipextreme.network.ITransferable;
import de.hsbremen.battleshipextreme.network.TransferableObjectFactory;
import de.hsbremen.battleshipextreme.network.eventhandling.ErrorHandler;
import de.hsbremen.battleshipextreme.network.eventhandling.EventArgs;
import de.hsbremen.battleshipextreme.network.eventhandling.listener.IErrorListener;
import de.hsbremen.battleshipextreme.server.listener.IClientConnectionListener;
import de.hsbremen.battleshipextreme.server.listener.IClientObjectReceivedListener;
import de.hsbremen.battleshipextreme.server.listener.IServerListener;

import javax.swing.event.EventListenerList;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

/**
 * Created by cschaf on 26.04.2015.
 */
public class Server implements IDisposable {

    public int port;
    protected EventListenerList listeners;
    private ServerSocket serverSocket;
    private ServerDispatcher serverDispatcher;
    private ClientAccepter clientAccepter;
    private ArrayList<IClientConnectionListener> tempClientConnectionListeners;
    private ArrayList<IClientObjectReceivedListener> tempClientObjectReceivedListeners;
    private ArrayList<IServerListener> tempServerListeners;
    private ErrorHandler errorHandler;
    private boolean isRunning;

    public Server(int port) {
        this.listeners = new EventListenerList();
        this.errorHandler = new ErrorHandler();
        this.tempClientObjectReceivedListeners = new ArrayList<IClientObjectReceivedListener>();
        this.tempClientConnectionListeners = new ArrayList<IClientConnectionListener>();
        this.tempServerListeners = new ArrayList<IServerListener>();
        this.port = port;
        this.isRunning = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            this.isRunning = true;
        } catch (IOException e) {
            errorHandler.errorHasOccurred(new EventArgs<ITransferable>(this, TransferableObjectFactory.CreateMessage("Can not start listening on port " + port)));
            this.dispose();
            this.isRunning = false;
            System.exit(-1);
        }
        // Start ServerDispatcher thread
        this.serverDispatcher = new ServerDispatcher(this.errorHandler);
        for (IClientConnectionListener listener : this.tempClientConnectionListeners) {
            this.serverDispatcher.addClientConnectionListener(listener);
        }
        for (IClientObjectReceivedListener listener : this.tempClientObjectReceivedListeners) {
            this.serverDispatcher.addClientObjectReceivedListener(listener);
        }
        for (IServerListener listener : this.tempServerListeners) {
            this.serverDispatcher.addServerListener(listener);
        }

        this.tempClientConnectionListeners.clear();
        this.serverDispatcher.start();
        // Accept and handle client connections
        this.clientAccepter = new ClientAccepter(serverSocket, serverDispatcher);
        this.clientAccepter.start();

        this.serverDispatcher.printInfo(new EventArgs<ITransferable>(this, TransferableObjectFactory.CreateMessage("Server started on port " + port)));
    }

    public void stop() {
        this.dispose();
        this.isRunning = false;
    }

    public void unicast(){
    }

    public void broadcast(ITransferable object){
        this.serverDispatcher.broadcast(object, null);
    }

    public void addClientObjectReceivedListener(IClientObjectReceivedListener listener) {
        if (this.serverDispatcher == null) {
            tempClientObjectReceivedListeners.add(listener);
        } else {
            this.serverDispatcher.addClientObjectReceivedListener(listener);
        }
    }

    public void removeClientObjectReceivedListener(IClientObjectReceivedListener listener) {
        this.listeners.remove(IClientObjectReceivedListener.class, listener);
    }

    public void addClientConnectionListener(IClientConnectionListener listener) {
        if (this.serverDispatcher == null) {
            tempClientConnectionListeners.add(listener);
        } else {
            this.serverDispatcher.addClientConnectionListener(listener);
        }
    }

    public void removeClientConnectionListener(IClientConnectionListener listener) {
        this.serverDispatcher.removeClientConnectionListener(listener);
    }

    public void addServerListener(IServerListener listener) {
        if (this.serverDispatcher == null) {
            tempServerListeners.add(listener);
        } else {
            this.serverDispatcher.addServerListener(listener);
        }
    }

    public void removeServerListener(IServerListener listener) {
        this.removeServerListener(listener);
    }

    public void addErrorListener(IErrorListener listener) {
        this.errorHandler.addErrorListener(listener);
    }

    public void removeErrorListener(IErrorListener listener) {
        this.errorHandler.removeErrorListener(listener);
    }


    public void dispose() {
        try {
            if (this.clientAccepter != null) {
                this.clientAccepter.dispose();
            }
            if (this.serverDispatcher != null) {
                this.serverDispatcher.dispose();
            }
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            errorHandler.errorHasOccurred(new EventArgs<ITransferable>(this, TransferableObjectFactory.CreateMessage("Can not dispose Server")));
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
