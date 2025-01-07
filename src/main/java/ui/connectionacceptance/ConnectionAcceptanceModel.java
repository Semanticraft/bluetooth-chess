package ui.connectionacceptance;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;


public class ConnectionAcceptanceModel extends ViewModel {
    private List<ConnectionRequest> connectionRequests = new ArrayList<>();
    private final MutableLiveData<List<ConnectionRequest>> uiState = new MutableLiveData<>();

    LiveData<List<ConnectionRequest>> getUiState() {
        return uiState;
    }

    void addConnectionRequest(ConnectionRequest connectionRequest) {
        connectionRequests.add(connectionRequest);
        uiState.postValue(connectionRequests);
    }
}
