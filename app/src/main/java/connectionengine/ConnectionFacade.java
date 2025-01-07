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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used to build a connection, send and receive PDUs and observe the connection status.
 */
public class ConnectionFacade {
    private static ConnectionFacade instance;
    public static final int REQUEST_ENABLE_BT = 1;
    private long playerID;
    private static final ConcurrentHashMap<Long, BluetoothDevice> devices = new ConcurrentHashMap<>();
    private static ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean isDiscoverer = false;
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothDevice currentDevice;
    private final Object lock = new Object();
    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
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

    private Handler handler;
    private boolean isDiscoveryRunning = false;
    private static AcceptThread acceptThread;
    private ConnectThread connectThread;
    private static PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(ConnectionFacade.class);

    public ConnectionFacade(long playerID, Context context) throws UnsupportedOperationException {
        initializeBluetoothAdapter(context);
        this.handler = new Handler();
        this.playerID = playerID;
    }

    public static synchronized ConnectionFacade getInstance(long playerID, Context context) {
        if (instance == null) {
            instance = new ConnectionFacade(playerID, context);
        }
        for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
        if (bluetoothAdapter == null) initializeBluetoothAdapter(context);
        startAcceptThread();
        propertyChangeSupport.addPropertyChangeListener((PropertyChangeListener) context);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, filter);
        return instance;
    }

    private static void initializeBluetoothAdapter(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            throw new UnsupportedOperationException("Bluetooth is not supported on this device.");
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            throw new UnsupportedOperationException("Bluetooth is not supported on this device.");
        }
    }

    private static void startAcceptThread() {
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
    public void startEngine(boolean discoverable, Context context) {
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
            setDiscoverable(context);
        } else {
            startDiscovery(context);
        }
    }


    @SuppressLint("MissingPermission")
    private void setDiscoverable(Context context) {
        isDiscoverer = false;
        int requestCode = 1;
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        ((Activity) context).startActivityForResult(discoverableIntent, requestCode);
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery(Context context) {
        if (!bluetoothAdapter.startDiscovery()) {
            Log.e("BluetoothDiscovery", "Failed to start Bluetooth discovery.");
            Toast.makeText(context, "Unable to start Bluetooth discovery. Please try again.", Toast.LENGTH_SHORT).show();
        }
        isDiscoveryRunning = true;
        isDiscoverer = true;

        // Set a timeout to ensure discovery stops after a certain period.
        handler.postDelayed(() -> {
            if (isDiscoveryRunning) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (!pairedDevices.isEmpty()) {
                    for (BluetoothDevice device : pairedDevices) {

                        Log.e(device.getName(), device.getName());
                        synchronized (lock) {
                            connectThread = new ConnectThread(device);
                            connectThread.start();
                            try {
                                lock.wait();
                            } catch (InterruptedException ignored) {}
                        }
                        try {
                            if (out != null) {
                                serializePlayerIDPDU(playerID);
                                synchronized (lock) {
                                    lock.wait();
                                }
                            }
                        } catch (IOException | InterruptedException ignored) {}
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
    public void stopEngine(Context context) {
        stopDiscovery();
        acceptThread.cancel();
        if (connectThread != null) connectThread.cancel();
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(receiver);
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
        stopDiscovery();
    }

    private static void addDevice(long playerID, BluetoothDevice device) {
        if (playerID == 0) {
            throw new IllegalArgumentException("Player ID cannot be 0");
        }
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }
        devices.put(playerID, device);
        propertyChangeSupport.firePropertyChange("New Device", 0, playerID);
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

    public Object getLock() {
        return lock;
    }

    private static void handleInputStream() throws IOException, ClassNotFoundException, InterruptedException {
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
            if (!instance.isDiscoverer) {
                synchronized (instance.lock) {
                    instance.connectTo(((PlayerIDPDU) received).getPlayerID());
                    instance.lock.wait();
                }
                instance.serializePlayerIDPDU(instance.playerID);
            } else {
                synchronized (instance.lock) {
                    instance.connectThread.cancel();
                    instance.lock.notify();
                }
            }
        }
        if (received instanceof StartPDU) {
            propertyChangeSupport.firePropertyChange("Start", ((StartPDU) received).getEnemyID(), ((StartPDU) received).getSavedState());
        }
    }

    private static void setConnectionStatus(int connectionStatus) {
        propertyChangeSupport.firePropertyChange("Connection", 0, connectionStatus);
    }

    public void setCurrentDevice(BluetoothDevice bluetoothDevice) {
        currentDevice = bluetoothDevice;
    }

    private static class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private boolean isInterrupted = false;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
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
            isInterrupted = false;
            while (!isInterrupted) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("Server", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    try {
                        in = new ObjectInputStream(socket.getInputStream());
                        instance.setCurrentDevice(socket.getRemoteDevice());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        handleInputStream();
                    } catch (IOException | ClassNotFoundException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public void cancel() {
            try {
                isInterrupted = true;
                mmServerSocket.close();
                in = null;
            } catch (IOException e) {
                Log.e("Server", "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            setCurrentDevice(device);

            try {
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
            synchronized (lock) {
                try {
                    mmSocket.connect();
                } catch (IOException connectException) {
                    try {
                        mmSocket.close();
                        lock.notify();
                        Log.e("FATAL", "Could not form connection!", connectException);
                    } catch (IOException closeException) {
                        Log.e("Client", "Could not close the client socket", closeException);
                    }
                    return;
                }

                try {
                    out = new ObjectOutputStream(mmSocket.getOutputStream());
                    lock.notify();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                out = null;
            } catch (IOException e) {
                Log.e("Client", "Could not close the client socket", e);
            }
        }
    }
}
