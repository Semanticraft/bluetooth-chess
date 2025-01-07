package ui.savestateforenemy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothchess.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import connectionengine.ConnectionFacade;
import persistence.Reader;
import ui.game.GameActivity;

public class SaveStateForEnemyActivity extends AppCompatActivity implements PropertyChangeListener {

    private long ownID;
    private ConnectionFacade connectionFacade;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_state_for_enemy);
        TextView enemyID = findViewById(R.id.txt_id_ssfe);
        Intent intent = getIntent();
        String enemyIDString = intent.getStringExtra("ENEMY_ID");
        ownID = intent.getLongExtra("OWN_ID", 0);
        enemyID.setText(enemyIDString);

        connectionFacade = ConnectionFacade.getInstance(ownID, this);

        SaveStateForEnemyAdapter saveStateForEnemyAdapter = new SaveStateForEnemyAdapter(this, connectionFacade);

        ImageButton btnBack = findViewById(R.id.btn_back_ssfe);
        btnBack.setOnClickListener(v -> finish());

        SaveStateForEnemyModel saveStateForEnemyModel = new ViewModelProvider(this).get(SaveStateForEnemyModel.class);
        saveStateForEnemyModel.getUiState().observe(this, saveStateForEnemyAdapter::updateData);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_ssfe);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(saveStateForEnemyAdapter);

        File savedStatesFile = new File(getFilesDir(), "saved_states.txt");
        String pathOfSavedStates = savedStatesFile.getAbsolutePath();
        try {
            if (savedStatesFile.exists()) {
                String savedStates = Reader.read(pathOfSavedStates);
                String[] rows = savedStates.split("\\n");
                for (String row : rows) {
                    String[] split = row.split(",");
                    Log.e("Here1", split[0] + ", " + split[1] + ", " + split[2] + ", " + enemyIDString);
                    if (split[0].equals(enemyIDString)) {
                        Log.e("Here2", split[0] + ", " + split[1] + ", " + split[2]);
                        saveStateForEnemyModel.addSavedState(split);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("Start")) {
            connectionFacade.unregisterReceiver(this);
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("PLAYER_COLOR", "white");
            intent.putExtra("ENEMY_ID", String.valueOf(evt.getOldValue()));
            Bundle bundle = new Bundle();
            bundle.putSerializable("SAVED_STATE", (int[][]) evt.getNewValue());
            intent.putExtras(bundle);
            intent.putExtra("OWN_ID", ownID);
            startActivity(intent);
        }
    }
}
