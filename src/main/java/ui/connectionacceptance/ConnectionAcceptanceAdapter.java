package ui.connectionacceptance;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;

import com.example.bluetoothchess.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import connectionengine.ConnectionFacade;
import ui.game.GameActivity;

public class ConnectionAcceptanceAdapter extends RecyclerView.Adapter<ConnectionAcceptanceAdapter.ConnectionAcceptanceViewHolder> {
    private List<ConnectionRequest> connectionRequests = new ArrayList<>();
    private final Context context;
    private final ConnectionFacade connectionFacade;

    public ConnectionAcceptanceAdapter(Context context, ConnectionFacade connectionFacade) {
        this.context = context;
        this.connectionFacade = connectionFacade;
    }

    void updateData(List<ConnectionRequest> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(connectionRequests, newData));
        connectionRequests.clear();
        connectionRequests.addAll(newData);
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
    public ConnectionAcceptanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_connection_acceptance_row, parent, false);
        return new ConnectionAcceptanceViewHolder(view);
    }

    /**
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ConnectionAcceptanceViewHolder holder, int position) {
        ConnectionRequest connectionRequest = connectionRequests.get(position);
        holder.IDTextView.setText(String.valueOf(connectionRequest.getEnemyID()));
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a", Locale.GERMANY);
        String formattedDate = sdf.format(date);
        holder.timestampTextView.setText(formattedDate);
        holder.acceptButton.setOnClickListener(v -> {
            connectionFacade.connectTo(connectionRequest.getEnemyID());
            try {
                connectionFacade.serializeStartPDU(connectionRequest.getState());
            } catch (IOException e) {
                Toast.makeText(context, "Connection unsuccessful!", Toast.LENGTH_SHORT).show();
                notifyItemRemoved(position);
            }
            Intent intent = new Intent(context, GameActivity.class);
            intent.putExtra("PLAYER_COLOR", "black");
            intent.putExtra("ENEMY_ID", connectionRequest.getEnemyID());
            intent.putExtra("CONNECTION_FACADE", connectionFacade);
            intent.putExtra("CURRENT_DEVICE", connectionFacade.getCurrentDevice());
            context.startActivity(intent);
        });
    }

    /**
     * @return item count of Recycler View items
     */
    @Override
    public int getItemCount() {
        return connectionRequests.size();
    }

    public static class ConnectionAcceptanceViewHolder extends RecyclerView.ViewHolder {
        private TextView IDTextView;
        private TextView timestampTextView;
        private Button acceptButton;

        public ConnectionAcceptanceViewHolder(@NonNull View itemView) {
            super(itemView);
            IDTextView = itemView.findViewById(R.id.txt_id_ca);
            timestampTextView = itemView.findViewById(R.id.txt_timestamp_ca);
            acceptButton = itemView.findViewById(R.id.btn_accept_ca);}
    }

    private static class MyDiffCallback extends DiffUtil.Callback {
        private final List<ConnectionRequest> oldList;
        private final List<ConnectionRequest> newList;

        public MyDiffCallback(List<ConnectionRequest> oldList, List<ConnectionRequest> newList) {
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
            return oldList.get(oldItemPosition).getEnemyID() == newList.get(newItemPosition).getEnemyID()
                    && oldList.get(oldItemPosition).getState() == newList.get(newItemPosition).getState() ;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
