package ui.savestatehistory;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.bluetoothchess.R;

import java.io.File;
import java.io.IOException;

import persistence.Reader;

/**
 * This Activity class is used to display and potentially delete saved game states.
 */
public class SaveStateHistoryActivity extends AppCompatActivity {

    private final SaveStateHistoryAdapter saveStateHistoryAdapter = new SaveStateHistoryAdapter(this);
    private SaveStateHistoryModel saveStateHistoryModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_state_history);
        try {
            File savedStatesFile = new File(getFilesDir(), "saved_states.txt");
            String pathOfOwnID = savedStatesFile.getAbsolutePath();
            String[] savedStatesAsStrings;
            if (savedStatesFile.exists()) {
                savedStatesAsStrings = Reader.read(pathOfOwnID).split("\n");
                for (String savedState : savedStatesAsStrings) {
                    if (!savedState.isEmpty()) {
                        long ID = Long.parseLong(savedState.split(",")[0]);
                        String timestamp = savedState.split(",")[1];
                        saveStateHistoryModel.addSavedState(new SaveState(ID, timestamp));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        saveStateHistoryModel = new ViewModelProvider(this).get(SaveStateHistoryModel.class);
        saveStateHistoryModel.getUiState().observe(this, saveStateHistoryAdapter::updateData);
        findViewById(R.id.btn_back_ssh).setOnClickListener(v -> finish());
    }

}
