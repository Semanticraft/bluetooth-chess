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
import java.io.IOException;

import connectionengine.ConnectionFacade;
import endstates.EndState;
import gamelogic.FENGenerator;
import ui.game.GameActivity;
import ui.main.MainActivity;

/**
 * This Activity class is used to present the message about the end state of the game and gives the
 * user options for a rematch or navigation back to the main activity.
 */
public class EndActivity extends AppCompatActivity implements PropertyChangeListener {

    private ConnectionFacade connectionFacade;
    private String playerColor;
    private long ownID;
    private String enemyID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);
        TextView endMessage = findViewById(R.id.txt_end_message_end);
        Intent intent = getIntent();
        int typeOfEnd = intent.getIntExtra("TYPE_OF_END", -1);
        playerColor = intent.getStringExtra("PLAYER_COLOR");
        ownID = intent.getLongExtra("OWN_ID", 0);
        enemyID = intent.getStringExtra("ENEMY_ID");
        endMessage.setText(6 > typeOfEnd && typeOfEnd > 0 ?
                EndState.fromStateCode(typeOfEnd).getDescription() : "Draw by Agreement");
        Button rematch = findViewById(R.id.btn_rematch_end);
        Button navToMain = findViewById(R.id.btn_nav_back_to_main_end);
        rematch.setOnClickListener(this::onRematchButtonClick);
        navToMain.setOnClickListener(this::onNavBackToMainButtonClick);
        connectionFacade = ConnectionFacade.getInstance(intent.getLongExtra("OWN_ID", 0), this);
    }

    private void onRematchButtonClick(View view) {
        try {
            synchronized (connectionFacade.getLock()) {
                connectionFacade.connectTo(Long.parseLong(enemyID.replaceAll("\\D", "")));
                connectionFacade.getLock().wait();
            }
            connectionFacade.serializeStartPDU(FENGenerator.initGame(playerColor.equals("white") ? "black" : "white"));
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("PLAYER_COLOR", playerColor);
            intent.putExtra("ENEMY_ID", enemyID);
            intent.putExtra("OWN_ID", ownID);
            startActivity(intent);
        } catch (IOException ignored) {} catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finish();
    }

    private void onNavBackToMainButtonClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("Start")) {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("PLAYER_COLOR", playerColor);
            intent.putExtra("ENEMY_ID", enemyID);
            intent.putExtra("OWN_ID", ownID);
            startActivity(intent);
            finish();
        }
    }
}
