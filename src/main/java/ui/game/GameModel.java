package ui.game;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
public class GameModel extends ViewModel {
    private MutableLiveData<int[][]> uiState = new MutableLiveData<>();

    LiveData<int[][]> getUiState() {
        return uiState;
    }

    void setUiState(MutableLiveData<int[][]> state) {
        uiState = state;
    }

}
