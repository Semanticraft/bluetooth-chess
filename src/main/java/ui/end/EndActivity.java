package ui.end;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetoothchess.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This Activity class is used to present the message about the end state of the game and gives the
 * user options for a rematch or navigation back to the main activity.
 */
public class EndActivity extends AppCompatActivity implements PropertyChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);
        TextView endMessage = findViewById(R.id.txt_end_message_end);
        Intent intent = getIntent();
        endMessage.setText(intent.getStringExtra("END_MESSAGE"));
        Button rematch = findViewById(R.id.btn_rematch_end);
        Button navToMain = findViewById(R.id.btn_nav_back_to_main_end);
        rematch.setOnClickListener(this::onRematchButtonClick);
        navToMain.setOnClickListener(this::onNavBackToMainButtonClick);
    }

    private void onRematchButtonClick(View view) {
        // TODO: implement
    }

    private void onNavBackToMainButtonClick(View view) {
        // TODO: implement
    }

    /**
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("Start")) {
            // TODO: implement
        }
    }
}
