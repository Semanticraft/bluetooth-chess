package ui.connectionacceptance;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.bluetoothchess.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import connectionengine.ConnectionFacade;

/**
 * This Activity class is used to receive game and accept game requests send by another player.
 */
public class ConnectionAcceptanceActivity extends AppCompatActivity implements PropertyChangeListener {
    private ConnectionAcceptanceModel connectionAcceptanceModel;
    private ConnectionFacade connectionFacade;
    private ConnectionAcceptanceAdapter connectionAcceptanceAdapter;
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
        setContentView(R.layout.activity_connection_acceptance);
        TextView ownIDTextView = findViewById(R.id.txt_own_id_ca);
        ownID = getIntent().getStringExtra("OWN_ID");
        ownIDTextView.setText(ownID);
        findViewById(R.id.btn_back_ca).setOnClickListener(this::onButtonBackClick);
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
            connectionAcceptanceAdapter = new ConnectionAcceptanceAdapter(this, connectionFacade);
            connectionAcceptanceModel = new ViewModelProvider(this).get(ConnectionAcceptanceModel.class);
            connectionAcceptanceModel.getUiState().observe(this, connectionAcceptanceAdapter::updateData);
        } else {
            Toast.makeText(this, "Please accept Permissions!", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }


    /**
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("Start")) {
            connectionAcceptanceModel.addConnectionRequest(new ConnectionRequest((long) evt.getOldValue(), (int[][]) evt.getNewValue()));
        }
    }
}
