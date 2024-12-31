package connectionengine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Used to build a connection, send and receive PDUs and observe the connection status.
 */
public class ConnectionFacade implements Serializable {
    public static final int REQUEST_ENABLE_BT = 1;
    private long playerID;
    private transient HashMap<Long, BluetoothDevice> devices = new HashMap<>();
    private transient ObjectInputStream in;
    private transient ObjectOutputStream out;
    private int connectionStatus;
    private transient BluetoothAdapter bluetoothAdapter;
    private transient BluetoothDevice currentDevice;
    private transient BroadcastReceiver receiver;

    private transient Context context;
    private transient Handler handler;
    private boolean isDiscoveryRunning = false;
    private transient AcceptThread acceptThread;
    private transient ConnectThread connectThread;
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public ConnectionFacade(long playerID, Context context) throws UnsupportedOperationException {
        initializeBluetoothAdapter(context);
        initializeBroadcastReceiver();
        acceptThread = new AcceptThread();
        acceptThread.start();
        this.handler = new Handler();
        this.playerID = playerID;
        this.context = context;
    }

    @Serial
    private void writeObject(@NotNull ObjectOutputStream out) throws IOException {
        out.writeLong(playerID);
        out.writeInt(connectionStatus);
        out.writeBoolean(isDiscoveryRunning);
        out.writeObject(propertyChangeSupport);
    }

    @Serial
    private void readObject(@NotNull ObjectInputStream in) throws IOException, ClassNotFoundException {
        playerID = in.readLong();
        connectionStatus = in.readInt();
        isDiscoveryRunning = in.readBoolean();
        propertyChangeSupport = (PropertyChangeSupport) in.readObject();

        handler = new Handler();
    }

    public void initializeBluetoothAdapter(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            throw new UnsupportedOperationException("Bluetooth is not supported on this device.");
        }
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        if (this.bluetoothAdapter == null) {
            throw new UnsupportedOperationException("Bluetooth is not supported on this device.");
        }
    }

    public void initializeBroadcastReceiver() {
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                    int connectionState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);

                    switch (connectionState) {
                        case BluetoothAdapter.STATE_CONNECTED:
                            setConnectionStatus(BluetoothAdapter.STATE_CONNECTED);
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTED:
                            setConnectionStatus(BluetoothAdapter.STATE_DISCONNECTED);
                            break;
                        case BluetoothAdapter.STATE_CONNECTING:
                            setConnectionStatus(BluetoothAdapter.STATE_CONNECTING);
                            break;
                        case BluetoothAdapter.STATE_DISCONNECTING:
                            setConnectionStatus(BluetoothAdapter.STATE_DISCONNECTING);
                            break;
                        default:
                            Log.d("Bluetooth", "Unknown connection state.");
                            break;
                    }
                }
            }
        };
    }

    public void startAcceptThread() {
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    /**
     * Sets preconditions for the connection of devices.
     * @param discoverable If true, preconditions for the visibility of the BT-Central get set, otherwise
     *                     BT-Discovery will be started and the devices list of the engine gets filled.
     * @throws IllegalStateException if another discovery or BT operation is already running.
     */
    @SuppressLint("MissingPermission")
    public void startEngine(boolean discoverable) {
        // I'll try to make a better strategy for denial, but for now this will work
        // It'll just continuously ask to enable Bluetooth until the user accepts
        while (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) context).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (isDiscoveryRunning) {
            throw new IllegalStateException("Another Bluetooth operation is already running.");
        }

        if (discoverable) {
            setDiscoverable();
        } else {
            startDiscovery();
        }
    }


    @SuppressLint("MissingPermission")
    private void setDiscoverable() {
        int requestCode = 1;
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        ((Activity) context).startActivityForResult(discoverableIntent, requestCode);
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (!bluetoothAdapter.startDiscovery()) {
            Log.e("BluetoothDiscovery", "Failed to start Bluetooth discovery.");
            Toast.makeText(context, "Unable to start Bluetooth discovery. Please try again.", Toast.LENGTH_SHORT).show();
        }
        isDiscoveryRunning = true;

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, filter);

        // Set a timeout to ensure discovery stops after a certain period.
        handler.postDelayed(() -> {
            if (isDiscoveryRunning) {
                stopDiscovery();
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (!pairedDevices.isEmpty()) {
                    for (BluetoothDevice device : pairedDevices) {
                        connectThread = new ConnectThread(device);
                        connectThread.start();
                        try {
                            serializePlayerIDPDU(playerID);
                        } catch (IOException ignored) {}
                        connectThread.cancel();
                    }
                }
            }
        }, 12000); // Stop discovery after 12 seconds
    }

    @SuppressLint("MissingPermission")
    private void stopDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        isDiscoveryRunning = false;
    }

    /**
     * For an ordinary connection release for navigation to another Activity, in which the connection
     * is not useful anymore or if the game is aborted.
     */
    public void stopEngine() {
        context.unregisterReceiver(receiver);
        stopDiscovery();
        acceptThread.cancel();
        connectThread.cancel();
    }

    /**
     * To build a connection to the BluetoothDevice with the given playerID.
     * @param playerID player ID
     * @throws IllegalArgumentException if playerID is not in devices map.
     */
    public void connectTo(long playerID)  {
        if (devices.get(playerID) == null) throw new IllegalArgumentException("Gerät mit gegebener ID wurde nicht gefunden.");
        connectThread = new ConnectThread(Objects.requireNonNull(devices.get(playerID)));
        connectThread.start();
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeSupport.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        propertyChangeSupport.removePropertyChangeListener(pcl);
    }

    public HashMap<Long, BluetoothDevice> getDevices() {
        return devices;
    }

    private void addDevice(long playerID, BluetoothDevice device) {
        devices.put(playerID, device);
        propertyChangeSupport.firePropertyChange("New Device", 0, playerID);
    }

    private void removeDevice(BluetoothDevice device) {
        if (getKeyByValue(devices, device) == null) {
            return;
        }
        long ID = getKeyByValue(devices, device);
        devices.remove(ID);
        propertyChangeSupport.firePropertyChange("Removed Device", 0, ID);
    }

    private static <K, V> K getKeyByValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void serializeDrawPDU(boolean drawFlag) throws IOException {
        out.writeObject(new DrawPDU(drawFlag));
        out.flush();
    }

    public void serializePlayerIDPDU(long ID) throws IOException {
        out.writeObject(new PlayerIDPDU(ID));
        out.flush();
    }

    public void serializeStartPDU(int[][] savedState) throws IOException {
        out.writeObject(new StartPDU(savedState, playerID));
        out.flush();
    }

    public void serializeMovePDU(int[][] newState, int endCondition) throws IOException {
        out.writeObject(new MovePDU(newState, endCondition));
        out.flush();
    }

    public void serializeErrorPDU(String type, String message) throws IOException {
        out.writeObject(new ErrorPDU(type, message));
        out.flush();
    }

    private void handleInputStream() throws IOException, ClassNotFoundException {
        Object received = in.readObject();
        if (received instanceof DrawPDU) {
            propertyChangeSupport.firePropertyChange("Draw", false, ((DrawPDU) received).isDrawFlagActive());
        }
        if (received instanceof ErrorPDU) {
            throw new IOException(((ErrorPDU) received).getType() + ": " + ((ErrorPDU) received).getMessage());
        }
        if (received instanceof MovePDU) {
            propertyChangeSupport.firePropertyChange("Move", ((MovePDU) received).getEndCondition(), ((MovePDU) received).getNewState());
        }
        if (received instanceof PlayerIDPDU) {
            addDevice(((PlayerIDPDU) received).getPlayerID(), currentDevice);
        }
        if (received instanceof StartPDU) {
            propertyChangeSupport.firePropertyChange("Start", ((StartPDU) received).getEnemyID(), ((StartPDU) received).getSavedState());
        }
    }

    private void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
        propertyChangeSupport.firePropertyChange("Connection", 0, connectionStatus);
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public BluetoothDevice getCurrentDevice() {
        return currentDevice;
    }

    public void setCurrentDevice(BluetoothDevice bluetoothDevice) {
        currentDevice = bluetoothDevice;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("btchess", UUID.fromString("baa6cde2-3025-4aea-b078-db83988c812a"));
            } catch (IOException e) {
                Log.e("Server", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("Server", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    try {
                        in = new ObjectInputStream(socket.getInputStream());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        handleInputStream();
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("Server", "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            setCurrentDevice(device);

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("baa6cde2-3025-4aea-b078-db83988c812a"));
            } catch (IOException e) {
                Log.e("Client", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("Client", "Could not close the client socket", closeException);
                }
                return;
            }

            try {
                out = new ObjectOutputStream(mmSocket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Client", "Could not close the client socket", e);
            }
        }
    }
}
