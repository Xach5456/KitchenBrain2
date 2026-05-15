package com.example.kitchenbrain.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.R;
import com.example.kitchenbrain.User;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ShareUserAdapter extends RecyclerView.Adapter<ShareUserAdapter.ViewHolder> {

    private List<User> users;
    private final OnUserShareClickListener listener;

    public interface OnUserShareClickListener {
        void onShareClick(User user);
    }

    public ShareUserAdapter(List<User> users, OnUserShareClickListener listener) {
        this.users = users != null ? users : new ArrayList<>();
        this.listener = listener;
    }

    public void setUsers(List<User> newUsers) {
        this.users = newUsers != null ? newUsers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.textUserName.setText(user.getUsername() != null ? user.getUsername() : "User");
        
        holder.buttonSend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageUserAvatar;
        TextView textUserName;
        MaterialButton buttonSend;

        ViewHolder(View itemView) {
            super(itemView);
            imageUserAvatar = itemView.findViewById(R.id.imageUserAvatar);
            textUserName = itemView.findViewById(R.id.textUserName);
            buttonSend = itemView.findViewById(R.id.buttonSendToUser);
        }
    }
}
