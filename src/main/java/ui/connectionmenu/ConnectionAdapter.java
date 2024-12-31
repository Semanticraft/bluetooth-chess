package ui.connectionmenu;

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

public class ConnectionAdapter extends RecyclerView.Adapter<ConnectionAdapter.ConnectionViewHolder> {
    List<Long> IDs = new ArrayList<>();

    public void updateData(List<Long> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ConnectionAdapter.MyDiffCallback(IDs, newData));
        IDs.clear();
        IDs.addAll(newData);
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
    public ConnectionAdapter.ConnectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_connection_acceptance_row, parent, false);
        return new ConnectionAdapter.ConnectionViewHolder(view);
    }

    /**
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ConnectionAdapter.ConnectionViewHolder holder, int position) {
        long ID = IDs.get(position);
        holder.IDTextView.setText(String.valueOf(ID));
        holder.startButton.setOnClickListener(v -> {
            // TODO: impl
        });
        holder.loadButton.setOnClickListener(v -> {
            // TODO: impl
        });
    }

    /**
     * @return item count of Recycler View items
     */
    @Override
    public int getItemCount() {
        return IDs.size();
    }

    public static class ConnectionViewHolder extends RecyclerView.ViewHolder {
        private TextView IDTextView;
        private Button loadButton;
        private Button startButton;

        public ConnectionViewHolder(@NonNull View itemView) {
            super(itemView);
            IDTextView = itemView.findViewById(R.id.txt_id_c);
            loadButton = itemView.findViewById(R.id.btn_load_c);
            startButton = itemView.findViewById(R.id.btn_start_c);
        }
    }

    private static class MyDiffCallback extends DiffUtil.Callback {
        private final List<Long> oldList;
        private final List<Long> newList;

        public MyDiffCallback(List<Long> oldList, List<Long> newList) {
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
            return oldList.get(oldItemPosition).longValue() == newList.get(newItemPosition).longValue();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
