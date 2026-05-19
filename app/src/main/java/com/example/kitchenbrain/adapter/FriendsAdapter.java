package com.example.kitchenbrain.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.manager.FollowGraphRepository;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    
    public interface OnFriendActionListener {
        void onUserClick(User user); // Added for profile navigation
        void onMessageClick(User user);
        void onFollowClick(User user);
        void onUnfollowClick(User user);
    }

    private List<User> userList;
    private String currentUserId;
    private OnFriendActionListener listener;
    private int currentTabType;

    public FriendsAdapter(List<User> userList, String currentUserId, OnFriendActionListener listener) {
        this.userList = userList;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.currentTabType = 1; // Following default
        setHasStableIds(true);
    }

    public void setTabType(int tabType) {
        this.currentTabType = tabType;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, currentTabType);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    @Override
    public long getItemId(int position) {
        User user = userList.get(position);
        return user.getUserId() != null ? user.getUserId().hashCode() : position;
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageAvatar;
        private TextView textUsername;
        private TextView textFullName;
        private Button buttonAction;
        private Button buttonUnfollowFriend;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            textUsername = itemView.findViewById(R.id.textUsername);
            textFullName = itemView.findViewById(R.id.textFullName);
            buttonAction = itemView.findViewById(R.id.buttonAction);
            buttonUnfollowFriend = itemView.findViewById(R.id.buttonUnfollowFriend);
        }

        void bind(User user, int tabType) {
            textUsername.setText(user.getUsername() != null ? user.getUsername() : "Unknown");
            textFullName.setVisibility(View.GONE);

            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(imageAvatar);
            } else {
                imageAvatar.setImageResource(R.drawable.ic_user);
            }
            
            // Click to open profile
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(user);
            });

            if (currentUserId != null && currentUserId.equals(user.getUserId())) {
                buttonAction.setText("You");
                buttonAction.setEnabled(false);
                buttonUnfollowFriend.setVisibility(View.GONE);
                return;
            }

            buttonUnfollowFriend.setVisibility(View.GONE);

            String followState = getFollowState(user.getUserId());
            boolean isMutual = "FRIEND".equals(followState);

            if (isMutual) {
                buttonAction.setText("Message");
                buttonAction.setOnClickListener(v -> {
                    if (listener != null) listener.onMessageClick(user);
                });
                buttonUnfollowFriend.setVisibility(View.VISIBLE);
                buttonUnfollowFriend.setOnClickListener(v -> {
                    if (listener != null) listener.onUnfollowClick(user);
                });
            } else {
                if ("FOLLOWING".equals(followState)) {
                    buttonAction.setText("Unfollow");
                    buttonAction.setOnClickListener(v -> {
                        if (listener != null) listener.onUnfollowClick(user);
                    });
                } else if ("FOLLOW_BACK".equals(followState)) {
                    buttonAction.setText("Follow Back");
                    buttonAction.setOnClickListener(v -> {
                        if (listener != null) listener.onFollowClick(user);
                    });
                } else {
                    buttonAction.setText("Follow");
                    buttonAction.setOnClickListener(v -> {
                        if (listener != null) listener.onFollowClick(user);
                    });
                }
            }
        }

        private String getFollowState(String userId) {
            if (userId == null) return "FOLLOW";
            FollowGraphRepository graph = FollowGraphRepository.getInstance();
            if (graph == null) return "FOLLOW";
            boolean isFollowing = graph.isFollowing(userId);
            boolean isFollower = graph.isFollower(userId);
            if (isFollowing && isFollower) return "FRIEND";
            if (isFollower) return "FOLLOW_BACK";
            if (isFollowing) return "FOLLOWING";
            return "FOLLOW";
        }
    }
}
