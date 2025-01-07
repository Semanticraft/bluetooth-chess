package ui.savestatehistory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothchess.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import persistence.Deleter;

public class SaveStateHistoryAdapter extends RecyclerView.Adapter<ui.savestatehistory.SaveStateHistoryAdapter.SaveStateHistoryViewHolder> {

    private List<SaveState> saveStates = new ArrayList<>();
    private Context context;

    public SaveStateHistoryAdapter(Context context) {
        this.context = context;
    }

    void updateData(List<SaveState> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(saveStates, newData));
        saveStates.clear();
        saveStates.addAll(newData);
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
    public SaveStateHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_save_state_history_row, parent, false);
        return new SaveStateHistoryViewHolder(view);
    }

    /**
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull SaveStateHistoryViewHolder holder, int position) {
        SaveState saveState = saveStates.get(position);
        holder.IDTextView.setText(String.valueOf(saveState.getId()));
        holder.timestampTextView.setText(saveState.getTimestamp());
        holder.deleteButton.setOnClickListener(v -> {
            try {
                Deleter.delete((new File(context.getFilesDir(), "saved_states.txt")).getAbsolutePath(), saveState.getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            saveStates.remove(position);
            notifyItemRemoved(position);
        });
    }

    /**
     * @return item count of Recycler View items
     */
    @Override
    public int getItemCount() {
        return saveStates.size();
    }

    public static class SaveStateHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView IDTextView;
        private TextView timestampTextView;
        private Button deleteButton;

        public SaveStateHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            IDTextView = itemView.findViewById(R.id.txt_id_ssh);
            timestampTextView = itemView.findViewById(R.id.txt_timestamp_ssh);
            deleteButton = itemView.findViewById(R.id.btn_delete_ssh);
        }
    }

    private static class MyDiffCallback extends DiffUtil.Callback {
        private final List<SaveState> oldList;
        private final List<SaveState> newList;

        public MyDiffCallback(List<SaveState> oldList, List<SaveState> newList) {
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
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId()
                    && Objects.equals(oldList.get(oldItemPosition).getTimestamp(), newList.get(newItemPosition).getTimestamp());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
