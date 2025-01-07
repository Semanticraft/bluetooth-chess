package ui.connectionmenu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import gamelogic.FENGenerator;
import ui.game.GameActivity;

/**
 * This Activity class is used to display possible enemies in Bluetooth range and connect to them to
 * play Chess.
 */
public class ConnectionActivity extends AppCompatActivity implements PropertyChangeListener {
    private ConnectionModel connectionModel;
    private ConnectionFacade connectionFacade;
    private String ownID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        TextView ownIDTextView = findViewById(R.id.txt_own_id_c);
        ownID = getIntent().getStringExtra("OWN_ID");
        ownIDTextView.setText(ownID);
        connectionFacade = ConnectionFacade.getInstance(Long.parseLong(ownID.replaceAll("\\D", "")), this);
        connectionFacade.startEngine(false, this);
        ConnectionAdapter connectionAdapter = new ConnectionAdapter(this, connectionFacade);
        connectionModel = new ViewModelProvider(this).get(ConnectionModel.class);
        connectionModel.getUiState().observe(this, connectionAdapter::updateData);
        findViewById(R.id.btn_back_c).setOnClickListener(this::onButtonBackClick);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_c);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(connectionAdapter);
    }

    private void onButtonBackClick(View view) {
        connectionFacade.stopEngine(this);
        connectionFacade.unregisterReceiver(this);
        finish();
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
        if (evt.getPropertyName().equals("Start")) {
            connectionFacade.unregisterReceiver(this);
            Intent intent = new Intent(ConnectionActivity.this, GameActivity.class);
            intent.putExtra("PLAYER_COLOR", "white");
            intent.putExtra("ENEMY_ID", String.valueOf(evt.getOldValue()));
            intent.putExtra("OWN_ID", Long.parseLong(ownID.replaceAll("\\D", "")));
            startActivity(intent);
        }
    }
}
