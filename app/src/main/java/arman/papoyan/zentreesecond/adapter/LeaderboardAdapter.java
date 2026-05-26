package arman.papoyan.zentreesecond.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import arman.papoyan.zentreesecond.R;
import arman.papoyan.zentreesecond.models.LeaderboardUser;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardUser> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(String userId, String userName, String userEmail);
    }

    public LeaderboardAdapter(List<LeaderboardUser> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void setUsers(List<LeaderboardUser> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardUser user = users.get(position);

        if (user.getRank() == 1) {
            holder.textViewRank.setText("🥇");
            holder.textViewRank.setTextSize(24);
        } else if (user.getRank() == 2) {
            holder.textViewRank.setText("🥈");
            holder.textViewRank.setTextSize(24);
        } else if (user.getRank() == 3) {
            holder.textViewRank.setText("🥉");
            holder.textViewRank.setTextSize(24);
        } else {
            holder.textViewRank.setText(String.valueOf(user.getRank()));
            holder.textViewRank.setTextSize(16);
        }

        holder.textViewName.setText(user.getName());

        long hours = user.getTotalHours();
        holder.textViewHours.setText(String.format(Locale.getDefault(), "%d ч", hours));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user.getUserId(), user.getName(), user.getEmail());
            }
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRank;
        ImageView imageViewAvatar;
        TextView textViewName;
        TextView textViewHours;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRank = itemView.findViewById(R.id.text_view_rank);
            imageViewAvatar = itemView.findViewById(R.id.image_view_avatar);
            textViewName = itemView.findViewById(R.id.text_view_name);
            textViewHours = itemView.findViewById(R.id.text_view_hours);
        }
    }
}