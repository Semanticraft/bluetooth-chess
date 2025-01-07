package ui.game;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.bluetoothchess.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

import connectionengine.ConnectionFacade;
import endstates.EndState;
import gamelogic.FENGenerator;
import gamelogic.GameRules;
import persistence.Writer;
import records.MoveResult;
import ui.end.EndActivity;
import ui.main.MainActivity;
import util.Util;

import android.view.DragEvent;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends AppCompatActivity implements PropertyChangeListener {

    private GameModel gameModel;
    private String playerColor;
    private MutableLiveData<int[][]> currentState;
    private GridLayout chessboardGrid;
    private ConnectionFacade connectionFacade;
    private long ownID;
    private String enemyID;
    private boolean isReceiverRegistered = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        TextView enemyIDView = findViewById(R.id.txt_id_game);

        Intent intent = getIntent();
        playerColor = intent.getStringExtra("PLAYER_COLOR");
        ownID = intent.getLongExtra("OWN_ID", 0);
        connectionFacade = ConnectionFacade.getInstance(ownID, this);
        enemyID = intent.getStringExtra("ENEMY_ID");

        enemyIDView.setText(enemyID);

        Bundle bundle = intent.getExtras();
        int[][] savedState = (int[][]) bundle.getSerializable("SAVED_STATE");

        currentState = new MutableLiveData<>(new int[8][8]);

        chessboardGrid = findViewById(R.id.chessboardGrid);
        int gridSize = 8;
        int cellSize = 125; // size in pixels for each cell

        chessboardGrid.setRowCount(gridSize);
        chessboardGrid.setColumnCount(gridSize);

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                ImageView cell = new ImageView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                cell.setLayoutParams(params);

                if ((row + col) % 2 == 0) {
                    cell.setBackgroundColor(Color.argb(255, 184, 139, 74));
                } else {
                    cell.setBackgroundColor(Color.argb(255, 227, 193, 111));
                }

                chessboardGrid.addView(cell);
                setDragAndDropListeners(cell, row, col);
            }
        }
        updateData(savedState != null ? savedState : FENGenerator.initGame(playerColor));

        gameModel = new ViewModelProvider(this).get(GameModel.class);
        gameModel.getUiState().observe(this, this::updateData);

        findViewById(R.id.btn_disconnect_game).setOnClickListener(v -> {
            connectionFacade.stopEngine(this);
            connectionFacade.unregisterReceiver(this);
            File savedStatesFile = new File(getFilesDir(), "saved_states.txt");
            String pathOfSavedStates = savedStatesFile.getAbsolutePath();
            try {
                //Writer.saveGame(pathOfSavedStates, FENGenerator.getFEN(gameModel.getUiState().getValue()), ownID);
                Writer.saveGame(pathOfSavedStates, FENGenerator.getFEN(currentState.getValue()), Long.parseLong(enemyID.replaceAll("\\D", "")));
            } catch (IOException e) {Toast.makeText(this, "Couldn't save game!", Toast.LENGTH_SHORT).show();}
            Intent mainNavIntent = new Intent(this, MainActivity.class);
            startActivity(mainNavIntent);
        });
        findViewById(R.id.btn_accept_draw_game).setOnClickListener(v -> {
            connectionFacade.unregisterReceiver(this);
            Intent endNavIntent = new Intent(this, EndActivity.class);
            endNavIntent.putExtra("TYPE_OF_END", 6);
            endNavIntent.putExtra("OWN_ID", ownID);
            endNavIntent.putExtra("ENEMY_ID", enemyID);
            endNavIntent.putExtra("PLAYER_COLOR", playerColor);
            startActivity(endNavIntent);
            finish();
        });
        findViewById(R.id.btn_accept_draw_game).setEnabled(false);
        findViewById(R.id.btn_request_draw_game).setOnClickListener(v -> {
            try {
                synchronized (connectionFacade.getLock()) {
                    connectionFacade.connectTo(Long.parseLong(enemyID.replaceAll("\\D", "")));
                    connectionFacade.getLock().wait();
                }
                connectionFacade.serializeDrawPDU(false);
            } catch (IOException ignored) {} catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @SuppressLint("ClickableViewAccessibility")
    private void setDragAndDropListeners(ImageView cell, int row, int col) {
        cell.setOnClickListener(v -> {});

        cell.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Start drag operation with piece info as clip data
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(null, shadowBuilder, new int[]{row, col}, 0);
                return true;
            }
            return false;
        });

        cell.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true; // Accept the drag
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.7f); // Highlight target cell
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setAlpha(1.0f); // Remove highlight
                    return true;
                case DragEvent.ACTION_DROP:
                    v.setAlpha(1.0f); // Reset highlight
                    int[] startPosition = (int[]) event.getLocalState();
                    int startRow = startPosition[0];
                    int startCol = startPosition[1];
                    // Calculate the end position
                    int index = chessboardGrid.indexOfChild(v);
                    int gridSize = 8;
                    int endRow = index / gridSize;
                    int endCol = index % gridSize;
                    int[][] newState = new int[currentState.getValue().length][];
                    for (int i = 0; i < currentState.getValue().length; i++) {
                        newState[i] = Arrays.copyOf(currentState.getValue()[i], currentState.getValue()[i].length);
                    }

                    newState[endRow][endCol] = newState[startRow][startCol];
                    newState[startRow][startCol] = 0;
                    GameRules gameRules = new GameRules(playerColor);
                    MoveResult moveResult = gameRules.evaluate(7 - startRow, startCol, 7 - endRow, endCol, currentState.getValue(), newState);
                    if (moveResult.isLegal()) {
                        //gameModel.setUiState(new MutableLiveData<>(newState));
                        updateData(newState);
                        try {
                            synchronized (connectionFacade.getLock()) {
                                connectionFacade.connectTo(Long.parseLong(enemyID.replaceAll("\\D", "")));
                                connectionFacade.getLock().wait();
                            }
                            connectionFacade.serializeMovePDU(Util.reversePlayer(newState), moveResult.endState());
                        } catch (IOException e) {
                            Toast.makeText(this, "Connection Lost", Toast.LENGTH_SHORT).show();
                            connectionFacade.unregisterReceiver(this);
                            isReceiverRegistered = false;
                            connectionFacade.stopEngine(this);
                            File savedStatesFile = new File(getFilesDir(), "saved_states.txt");
                            String pathOfSavedStates = savedStatesFile.getAbsolutePath();
                            try {
                                Writer.saveGame(pathOfSavedStates, FENGenerator.getFEN(gameModel.getUiState().getValue()), ownID);
                            } catch (IOException e2) {Toast.makeText(this, "Couldn't save game!", Toast.LENGTH_SHORT).show();}
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (moveResult.endState() != EndState.NO_END.getStateCode()) {
                            if (isReceiverRegistered) {
                                connectionFacade.unregisterReceiver(this);
                                Intent intent = new Intent(this, EndActivity.class);
                                intent.putExtra("TYPE_OF_END", moveResult.endState());
                                intent.putExtra("OWN_ID", ownID);
                                intent.putExtra("ENEMY_ID", enemyID);
                                intent.putExtra("PLAYER_COLOR", playerColor);
                                startActivity(intent);
                                finish();
                            }
                        }
                        setDragAndDropEnabled(false);
                    } else {
                        Toast.makeText(this, "Invalid move!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    /**
     * Enable or disable drag-and-drop listeners for the grid.
     *
     * @param enable Pass true to enable drag-and-drop, false to disable.
     */
    private void setDragAndDropEnabled(boolean enable) {
        int childCount = chessboardGrid.getChildCount();

        for (int i = 0; i < childCount; i++) {
            ImageView cell = (ImageView) chessboardGrid.getChildAt(i);

            if (enable) {
                int row = i / chessboardGrid.getColumnCount();
                int col = i % chessboardGrid.getColumnCount();
                setDragAndDropListeners(cell, row, col);
            } else {
                cell.setOnDragListener(null);
            }
        }
    }


    /**
     * Updates the GridLayout to display the new board state based on integer values.
     * @param newState   The new board state as a 2D int array.
     */
    private void updateData(int[][] newState) {
        int gridSize = 8;
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (Objects.requireNonNull(currentState.getValue())[row][col] != newState[row][col]) {
                    currentState.getValue()[row][col] = newState[row][col];
                    int cellIndex = row * gridSize + col;
                    ImageView cell = (ImageView) chessboardGrid.getChildAt(cellIndex);

                    if (newState[row][col] != 0) {
                        int newImageResource = getImageResourceForPiece(newState[row][col]);
                        cell.setImageResource(newImageResource);
                    } else {
                        cell.setImageResource(0);
                    }

                    GridLayout.LayoutParams layoutParams = (GridLayout.LayoutParams) cell.getLayoutParams();
                    layoutParams.setGravity(Gravity.CENTER);
                    cell.setLayoutParams(layoutParams);
                    cell.requestLayout();
                    cell.invalidate();
                }
            }
        }
    }

    /**
     * Maps an integer piece to a drawable resource ID.
     *
     * @param piece The integer representing the piece.
     * @return The drawable resource ID for the corresponding piece.
     */
    private int getImageResourceForPiece(int piece) {
        char FENCharacter = FENGenerator.getFENCharacter(piece);
        return switch (FENCharacter) {
            case 'P' -> R.drawable.white_pawn;
            case 'p' -> R.drawable.black_pawn;
            case 'N' -> R.drawable.white_knight;
            case 'n' -> R.drawable.black_knight;
            case 'B' -> R.drawable.white_bishop;
            case 'b' -> R.drawable.black_bishop;
            case 'R' -> R.drawable.white_rook;
            case 'r' -> R.drawable.black_rook;
            case 'Q' -> R.drawable.white_queen;
            case 'q' -> R.drawable.black_queen;
            case 'K' -> R.drawable.white_king;
            case 'k' -> R.drawable.black_king;
            default -> 0;
        };
    }


    /**
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("Move")) {
            if ((int) evt.getOldValue() == EndState.NO_END.getStateCode()) {
                setDragAndDropEnabled(true);
                //gameModel.setUiState(new MutableLiveData<>((int[][]) evt.getNewValue()));

                runOnUiThread(() -> updateData((int[][]) evt.getNewValue()));
            } else {
                connectionFacade.unregisterReceiver(this);
                Intent intent = new Intent(this, EndActivity.class);
                intent.putExtra("TYPE_OF_END", (int) evt.getOldValue());
                intent.putExtra("OWN_ID", ownID);
                intent.putExtra("ENEMY_ID", enemyID);
                intent.putExtra("PLAYER_COLOR", playerColor);
                startActivity(intent);
                finish();
            }
        }
        if (evt.getPropertyName().equals("Draw")) {
            if ((boolean) evt.getNewValue()) {
                connectionFacade.unregisterReceiver(this);
                Intent intent = new Intent(this, EndActivity.class);
                intent.putExtra("TYPE_OF_END", 6);
                intent.putExtra("OWN_ID", ownID);
                intent.putExtra("ENEMY_ID", enemyID);
                intent.putExtra("PLAYER_COLOR", playerColor);
                startActivity(intent);
                finish();
            } else {
                runOnUiThread(() -> findViewById(R.id.btn_accept_draw_game).setEnabled(true));
            }
        }
        if (evt.getPropertyName().equals("Connection")) {
            if ((int) evt.getNewValue() == BluetoothAdapter.STATE_DISCONNECTED) {
                Toast.makeText(this, "Connection Lost", Toast.LENGTH_SHORT).show();
                connectionFacade.unregisterReceiver(this);
                connectionFacade.stopEngine(this);
                File savedStatesFile = new File(getFilesDir(), "saved_states.txt");
                String pathOfSavedStates = savedStatesFile.getAbsolutePath();
                try {
                    Writer.saveGame(pathOfSavedStates, FENGenerator.getFEN(currentState.getValue()), Long.parseLong(enemyID.replaceAll("\\D", "")));
                    //Writer.saveGame(pathOfSavedStates, FENGenerator.getFEN(gameModel.getUiState().getValue()), ownID);
                } catch (IOException e) {Toast.makeText(this, "Couldn't save game!", Toast.LENGTH_SHORT).show();}
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
