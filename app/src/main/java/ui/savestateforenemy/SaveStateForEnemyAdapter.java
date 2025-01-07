package ui.savestateforenemy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothchess.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import connectionengine.ConnectionFacade;
import gamelogic.FENGenerator;
import util.Util;

public class SaveStateForEnemyAdapter extends RecyclerView.Adapter<SaveStateForEnemyAdapter.SaveStateForEnemyViewHolder> {
    private List<String[]> savedStates = new ArrayList<>();
    private final Context context;
    private final ConnectionFacade connectionFacade;

    public SaveStateForEnemyAdapter(Context context, ConnectionFacade connectionFacade) {
        this.context = context;
        this.connectionFacade = connectionFacade;
    }

    void updateData(List<String[]> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(savedStates, newData));
        savedStates.clear();
        savedStates.addAll(newData);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return ConnectionAcceptanceViewHolder
     */
    @NonNull
    @Override
    public SaveStateForEnemyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_save_state_for_enemy_row, parent, false);
        return new SaveStateForEnemyViewHolder(view);
    }

    /**
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull SaveStateForEnemyViewHolder holder, int position) {
        String[] savedState = savedStates.get(position);
        holder.timestampTextView.setText(savedState[1]);
        holder.requestButton.setOnClickListener(v -> {
            synchronized (connectionFacade.getLock()) {
                connectionFacade.connectTo(Long.parseLong(savedState[0].replaceAll("\\D", "")));
                try {
                    connectionFacade.getLock().wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                connectionFacade.serializeStartPDU(Util.reversePlayer(FENGenerator.getState(savedState[2])));
            } catch (IOException e) {
                Toast.makeText(context, "Connection unsuccessful!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * @return item count of Recycler View items
     */
    @Override
    public int getItemCount() {
        return savedStates.size();
    }

    public static class SaveStateForEnemyViewHolder extends RecyclerView.ViewHolder {
        private TextView timestampTextView;
        private Button requestButton;

        public SaveStateForEnemyViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.txt_timestamp_ssfe);
            requestButton = itemView.findViewById(R.id.btn_request_ssfe);
        }
    }

    private static class MyDiffCallback extends DiffUtil.Callback {
        private final List<String[]> oldList;
        private final List<String[]> newList;

        public MyDiffCallback(List<String[]> oldList, List<String[]> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Arrays.equals(oldList.get(oldItemPosition), newList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return Arrays.equals(oldList.get(oldItemPosition), newList.get(newItemPosition));
        }
    }
}
