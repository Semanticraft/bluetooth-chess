package ui.savestateforenemy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothchess.R;

import java.util.ArrayList;
import java.util.List;

public class SaveStateForEnemyAdapter extends RecyclerView.Adapter<SaveStateForEnemyAdapter.SaveStateForEnemyViewHolder> {
    private List<String> timestamps = new ArrayList<>();

    void updateData(List<String> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(timestamps, newData));
        timestamps.clear();
        timestamps.addAll(newData);
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
                .inflate(R.layout.activity_connection_acceptance_row, parent, false);
        return new SaveStateForEnemyViewHolder(view);
    }

    /**
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull SaveStateForEnemyViewHolder holder, int position) {
        String timestamp = timestamps.get(position);
        holder.timestampTextView.setText(timestamp);
    }

    /**
     * @return item count of Recycler View items
     */
    @Override
    public int getItemCount() {
        return timestamps.size();
    }

    public static class SaveStateForEnemyViewHolder extends RecyclerView.ViewHolder {
        private TextView timestampTextView;
        private Button requestButton;

        public SaveStateForEnemyViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.txt_timestamp_ssfe);
            requestButton = itemView.findViewById(R.id.btn_request_ssfe);

            requestButton.setOnClickListener(this::onRequestButtonClick);
        }

        private void onRequestButtonClick(View view) {
            // TODO: implement
        }
    }

    private static class MyDiffCallback extends DiffUtil.Callback {
        private final List<String> oldList;
        private final List<String> newList;

        public MyDiffCallback(List<String> oldList, List<String> newList) {
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
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
