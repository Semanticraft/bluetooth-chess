package ui.savestateforenemy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.bluetoothchess.R;

public class SaveStateForEnemyActivity extends AppCompatActivity {
    private SaveStateForEnemyModel saveStateForEnemyModel;
    private SaveStateForEnemyAdapter saveStateForEnemyAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_state_for_enemy);
        TextView enemyID = findViewById(R.id.txt_id_ssfe);
        Intent intent = getIntent();
        enemyID.setText(intent.getStringExtra("ENEMY_ID"));

        saveStateForEnemyModel = new ViewModelProvider(this).get(SaveStateForEnemyModel.class);
        saveStateForEnemyModel.getUiState().observe(this, saveStateForEnemyAdapter::updateData);
    }
}
