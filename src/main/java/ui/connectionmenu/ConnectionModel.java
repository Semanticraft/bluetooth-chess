package ui.connectionmenu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ConnectionModel extends ViewModel {
    private List<Long> IDs = new ArrayList<>();
    private final MutableLiveData<List<Long>> uiState = new MutableLiveData<>();

    LiveData<List<Long>> getUiState() {
        return uiState;
    }

    void addID(long ID) {
        IDs.add(ID);
        uiState.postValue(IDs);
    }
}
