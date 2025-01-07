package ui.main;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.bluetoothchess.R;

import java.io.File;
import java.io.IOException;

import persistence.Reader;
import persistence.Writer;
import ui.connectionacceptance.ConnectionAcceptanceActivity;
import ui.connectionmenu.ConnectionActivity;
import ui.savestatehistory.SaveStateHistoryActivity;

public class MainActivity extends AppCompatActivity {
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
        }
        ActivityCompat.requestPermissions(this, permissions, 1);
        findViewById(R.id.btn_search_connection_nav_m).setOnClickListener(this::onButtonConnectionNavClick);
        findViewById(R.id.btn_accept_connection_nav_m).setOnClickListener(this::onButtonConnectionAcceptanceNavClick);
        findViewById(R.id.btn_save_state_history_nav_m).setOnClickListener(this::onButtonSaveStateHistoryNavClick);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 1) {
            Toast.makeText(this, "Bitte akzeptiere, um die App zu verwenden.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    public void onButtonConnectionNavClick(View view) {
        Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
        intent.putExtra("OWN_ID", ownID);
        startActivity(intent);
    }

    public void onButtonConnectionAcceptanceNavClick(View view) {
        Intent intent = new Intent(MainActivity.this, ConnectionAcceptanceActivity.class);
        intent.putExtra("OWN_ID", ownID);
        startActivity(intent);
    }

    public void onButtonSaveStateHistoryNavClick(View view) {
        Intent intent = new Intent(MainActivity.this, SaveStateHistoryActivity.class);
        intent.putExtra("OWN_ID", ownID);
        startActivity(intent);
    }
}
