package ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetoothchess.R;

import java.io.File;
import java.io.IOException;

import connectionengine.ConnectionFacade;
import persistence.Reader;
import persistence.Writer;
import ui.connectionacceptance.ConnectionAcceptanceActivity;
import ui.connectionmenu.ConnectionActivity;
import ui.savestatehistory.SaveStateHistoryActivity;

public class MainActivity extends AppCompatActivity {
    private String ownID;
    private ConnectionFacade connectionFacade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        ownID = intent.getStringExtra("OWN_ID");
        if (ownID == null) {
            try {
                File ownIDFile = new File(getFilesDir(), "own_id.txt");
                String pathOfOwnID = ownIDFile.getAbsolutePath();
                if (ownIDFile.exists()) {
                    ownID = Reader.read(pathOfOwnID);
                    if (ownID.isEmpty()) {
                        Writer.writeID(pathOfOwnID);
                        ownID = Reader.read(pathOfOwnID);
                    }
                } else {
                    ownIDFile.createNewFile();
                    Writer.writeID(pathOfOwnID);
                    ownID = Reader.read(pathOfOwnID);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            connectionFacade = new ConnectionFacade(Long.parseLong(ownID.replaceAll("\\D", "")), this);
        }

        findViewById(R.id.btn_search_connection_nav_m).setOnClickListener(this::onButtonConnectionNavClick);
        findViewById(R.id.btn_accept_connection_nav_m).setOnClickListener(this::onButtonConnectionAcceptanceNavClick);
        findViewById(R.id.btn_save_state_history_nav_m).setOnClickListener(this::onButtonSaveStateHistoryNavClick);
    }

    public void onButtonConnectionNavClick(View view) {
        Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
        intent.putExtra("OWN_ID", ownID);
        intent.putExtra("CONNECTION_FACADE", connectionFacade);
        startActivity(intent);
    }

    public void onButtonConnectionAcceptanceNavClick(View view) {
        Intent intent = new Intent(MainActivity.this, ConnectionAcceptanceActivity.class);
        intent.putExtra("OWN_ID", ownID);
        intent.putExtra("CONNECTION_FACADE", connectionFacade);
        startActivity(intent);
    }

    public void onButtonSaveStateHistoryNavClick(View view) {
        Intent intent = new Intent(MainActivity.this, SaveStateHistoryActivity.class);
        intent.putExtra("OWN_ID", ownID);
        startActivity(intent);
    }
}
