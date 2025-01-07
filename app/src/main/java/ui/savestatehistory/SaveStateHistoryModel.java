package ui.savestatehistory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class SaveStateHistoryModel extends ViewModel {
    private List<SaveState> savedStates = new ArrayList<>();
    private final MutableLiveData<List<SaveState>> uiState = new MutableLiveData<>();

    LiveData<List<SaveState>> getUiState() {
        return uiState;
    }

    List<SaveState> getSavedStates() {
        return savedStates;
    }

    void addSavedState(SaveState saveState) {
        savedStates.add(saveState);
        uiState.setValue(savedStates);
    }
}
