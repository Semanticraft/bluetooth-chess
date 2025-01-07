package ui.connectionacceptance;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_acceptance);
        TextView ownIDTextView = findViewById(R.id.txt_own_id_ca);
        String ownID = getIntent().getStringExtra("OWN_ID");
        ownIDTextView.setText(ownID);
        findViewById(R.id.btn_back_ca).setOnClickListener(this::onButtonBackClick);
        connectionFacade = ConnectionFacade.getInstance(Long.parseLong(ownID.replaceAll("\\D", "")), this);
        connectionFacade.startEngine(true, this);
        ConnectionAcceptanceAdapter connectionAcceptanceAdapter = new ConnectionAcceptanceAdapter(this, connectionFacade);
        connectionAcceptanceModel = new ViewModelProvider(this).get(ConnectionAcceptanceModel.class);
        connectionAcceptanceModel.getUiState().observe(this, connectionAcceptanceAdapter::updateData);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_ca);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(connectionAcceptanceAdapter);
    }

    private void onButtonBackClick(View view) {
        connectionFacade.unregisterReceiver(this);
        connectionFacade.stopEngine(this);
        finish();
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
