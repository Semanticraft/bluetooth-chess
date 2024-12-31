package ui.connectionmenu;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.bluetoothchess.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import connectionengine.ConnectionFacade;

/**
 * This Activity class is used to display possible enemies in Bluetooth range and connect to them to
 * play Chess.
 */
public class ConnectionActivity extends AppCompatActivity implements PropertyChangeListener {
    private final ConnectionAdapter connectionAdapter = new ConnectionAdapter();
    private ConnectionModel connectionModel;
    private ConnectionFacade connectionFacade;
    private String ownID;
    private final String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        TextView ownIDTextView = findViewById(R.id.txt_own_id_c);
        ownID = getIntent().getStringExtra("OWN_ID");
        ownIDTextView.setText(ownID);
        connectionModel = new ViewModelProvider(this).get(ConnectionModel.class);
        connectionModel.getUiState().observe(this, connectionAdapter::updateData);
        findViewById(R.id.btn_back_c).setOnClickListener(this::onButtonBackClick);
        ActivityCompat.requestPermissions(this, permissions, 1);
    }

    private void onButtonBackClick(View view) {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            connectionFacade = (ConnectionFacade) getIntent().getSerializableExtra("CONNECTION_FACADE");
            connectionFacade.setContext(this);
            connectionFacade.initializeBluetoothAdapter(this);
            connectionFacade.initializeBroadcastReceiver();
            connectionFacade.startAcceptThread();
            connectionFacade.addPropertyChangeListener(this);
            connectionFacade.startEngine(true);
            connectionFacade.addPropertyChangeListener(this);
            connectionFacade.startEngine(false);
        } else {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    /**
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("New Device")) {
            connectionModel.addID((long) evt.getNewValue());
        }
    }
}
