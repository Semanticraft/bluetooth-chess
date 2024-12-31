package ui.game;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.bluetoothchess.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;

import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

import connectionengine.ConnectionFacade;
import endstates.EndState;
import gamelogic.FENGenerator;
import gamelogic.GameRules;
import persistence.Reader;
import records.MoveResult;

import android.view.DragEvent;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends AppCompatActivity implements PropertyChangeListener {

    private GameModel gameModel;
    private String playerColor;
    private MutableLiveData<int[][]> currentState;
    private GridLayout chessboardGrid;
    private ConnectionFacade connectionFacade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        TextView enemyID = findViewById(R.id.txt_id_game);

        Intent intent = new Intent();
        playerColor = intent.getStringExtra("PLAYER_COLOR");
        connectionFacade = (ConnectionFacade) intent.getSerializableExtra("CONNECTION_FACADE");
        enemyID.setText(intent.getStringExtra("ENEMY_ID"));
        currentState = new MutableLiveData<>(FENGenerator.initGame(playerColor));

        connectionFacade.addPropertyChangeListener(this);

        chessboardGrid = findViewById(R.id.chessboardGrid);
        int gridSize = 8;
        int cellSize = 100; // size in pixels for each cell

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

                // Set alternating background colors
                if ((row + col) % 2 == 0) {
                    cell.setBackgroundColor(Color.WHITE);
                } else {
                    cell.setBackgroundColor(Color.BLACK);
                }

                // Add the cell to the GridLayout
                chessboardGrid.addView(cell);
                setDragAndDropListeners(cell, row, col);
            }
        }

        updateData(currentState.getValue());

        gameModel = new ViewModelProvider(this).get(GameModel.class);
        gameModel.getUiState().observe(this, this::updateData);

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

                    int[][] newState = currentState.getValue();
                    newState[endRow][endCol] = newState[startRow][startCol];
                    newState[startRow][startCol] = 0;
                    // Attempt to move piece
                    GameRules gameRules = new GameRules(playerColor);
                    MoveResult moveResult = gameRules.evaluate(startRow, startCol, endRow, endCol, currentState.getValue(), newState);
                    if (moveResult.isLegal()) {
                        setDragAndDropEnabled(false);
                        gameModel.setUiState(new MutableLiveData<>(newState));
                        try {
                            connectionFacade.serializeMovePDU(newState, moveResult.endState());
                        } catch (IOException e) {
                            // dunno
                        }
                        if (moveResult.endState() == EndState.WON.getStateCode()) {
                            // move to end screen
                        }
                        if (moveResult.endState() == EndState.DRAW_BY_DEAD_POSITION.getStateCode()) {
                            // move to end screen
                        }
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
        int gridSize = 8; // Assuming square grid
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                // Check if the state has changed
                if (Objects.requireNonNull(currentState.getValue())[row][col] != newState[row][col]) {
                    // Calculate the index of the child in GridLayout
                    int cellIndex = row * gridSize + col;

                    // Get the ImageView for the current cell
                    ImageView cell = (ImageView) chessboardGrid.getChildAt(cellIndex);

                    if (newState[row][col] != 0) {
                        // Set the new image resource based on the new state
                        int newImageResource = getImageResourceForPiece(newState[row][col]);
                        cell.setImageResource(newImageResource);
                    } else {
                        cell.setImageResource(0);
                    }
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
                gameModel.setUiState(new MutableLiveData<>((int[][]) evt.getNewValue()));
            }
        }
        if (evt.getPropertyName().equals("Draw")) {
            // button soll available werden oder bei Akzeptanz soll neues intent ausgelöst werden
        }
        if (evt.getPropertyName().equals("Connection")) {
            // falls Abbruch -> Hauptmenü Nav
        }
    }
}
