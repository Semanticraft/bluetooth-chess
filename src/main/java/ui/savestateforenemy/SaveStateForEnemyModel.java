package ui.savestateforenemy;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class SaveStateForEnemyModel extends ViewModel {
    private List<String> savedStates = new ArrayList<>();
    private final MutableLiveData<List<String>> uiState = new MutableLiveData<>();

    LiveData<List<String>> getUiState() {
        return uiState;
    }

    List<String> getSavedStates() {
        return savedStates;
    }

    void addSavedState(String saveState) {
        savedStates.add(saveState);
        uiState.setValue(savedStates);
    }
}
