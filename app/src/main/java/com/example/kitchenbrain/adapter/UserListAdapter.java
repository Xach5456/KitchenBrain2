package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.util.Log;
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
import com.example.kitchenbrain.model.ListItem;
import com.example.kitchenbrain.repository.FollowRepository;

import java.util.ArrayList;
import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    public enum DisplayMode {
        FOLLOWERS,    // Show "Follow back" button
        FOLLOWING,    // Show "Unfollow" button
        MUTUAL        // Show "Message" button
    }
    
    private static final int TYPE_USER = 1;
    private static final int TYPE_LOADING = 2;
    
    private List<ListItem> itemList;
    private String currentUserId;
    private FollowRepository followRepository;
    private OnUserActionListener listener;
    private DisplayMode displayMode;

    public interface OnUserActionListener {
        void onMessageClick(User user);
        void onFollowClick(User user);
        void onUnfollowClick(User user);
    }

    public UserListAdapter(List<User> userList, String currentUserId, DisplayMode displayMode, OnUserActionListener listener) {
        this.currentUserId = currentUserId;
        this.displayMode = displayMode;
        this.listener = listener;
        this.followRepository = new FollowRepository();
        
        // Convert User list to ListItem
        updateList(userList);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == TYPE_LOADING) {
            // Loading placeholder layout
            View view = inflater.inflate(R.layout.item_user_follow, parent, false);
            return new LoadingViewHolder(view);
        } else {
            // Real user layout
            View view = inflater.inflate(R.layout.item_user_follow, parent, false);
            return new UserViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = itemList.get(position);
        
        if (item.getType() == ListItem.ItemType.LOADING) {
            ((LoadingViewHolder) holder).bind((ListItem.LoadingItem) item);
        } else {
            ((UserViewHolder) holder).bind((ListItem.UserItem) item);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ListItem item = itemList.get(position);
        return item.getType() == ListItem.ItemType.USER ? TYPE_USER : TYPE_LOADING;
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    /**
     * ✅ LOADING VIEW HOLDER - For placeholder items
     */
    class LoadingViewHolder extends RecyclerView.ViewHolder {
        private TextView textUsername;
        private TextView textFullName;
        private ImageView imageAvatar;
        private Button buttonAction;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            textFullName = itemView.findViewById(R.id.textFullName);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            buttonAction = itemView.findViewById(R.id.buttonAction);
        }

        public void bind(ListItem.LoadingItem item) {
            // Show skeleton loading state
            textUsername.setText("Loading...");
            textFullName.setVisibility(View.GONE);
            imageAvatar.setImageResource(R.drawable.ic_user);
            buttonAction.setText("Loading...");
            buttonAction.setEnabled(false);
            
            // Visual indicator - faded
            itemView.setAlpha(0.5f);
        }
    }

    /**
     * ✅ USER VIEW HOLDER - For real users
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageAvatar;
        private TextView textUsername;
        private TextView textFullName;
        private Button buttonAction;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            textUsername = itemView.findViewById(R.id.textUsername);
            textFullName = itemView.findViewById(R.id.textFullName);
            buttonAction = itemView.findViewById(R.id.buttonAction);
        }

        public void bind(ListItem.UserItem item) {
            User user = item.getUser();
            Context context = itemView.getContext();
            
            // Normal user display
            itemView.setAlpha(1.0f);
            textUsername.setText(user.getUsername() != null ? user.getUsername() : "Unknown");
            
            // User model doesn't have fullName, hide it
            textFullName.setVisibility(View.GONE);
            
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(context)
                        .load(user.getAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(imageAvatar);
            } else {
                imageAvatar.setImageResource(R.drawable.ic_user);
            }

            if (user.getUserId().equals(currentUserId)) {
                buttonAction.setText("You");
                buttonAction.setEnabled(false);
                return;
            }

            // ✅ DYNAMIC STATE CHECK - Don't rely solely on DisplayMode
            // Check actual graph state to prevent button mismatches
            String userId = user.getUserId();
            String followState = getFollowState(userId);
            
            Log.d("UserListAdapter", "🔍 [BIND] User: " + user.getUsername() + 
                  ", DisplayMode: " + displayMode + 
                  ", Actual State: " + followState);
            
            updateButtonBasedOnState(user, followState);
        }
        
        /**
         * ✅ GET ACTUAL FOLLOW STATE from graph
         */
        private String getFollowState(String userId) {
            FollowGraphRepository graph = FollowGraphRepository.getInstance();
            boolean isFollowing = graph.isFollowing(userId);
            boolean isFollower = graph.isFollower(userId);
            
            Log.d("UserListAdapter", "🔍 [FOLLOW_STATE] userId: " + userId.substring(0, Math.min(8, userId.length())) + 
                  " | isFollowing: " + isFollowing + 
                  " | isFollower: " + isFollower);
            
            if (isFollowing && isFollower) return "FRIEND";
            if (isFollower) return "FOLLOW_BACK";
            if (isFollowing) return "FOLLOWING";
            return "FOLLOW";
        }
        
        /**
         * ✅ UPDATE BUTTON based on actual state
         */
        private void updateButtonBasedOnState(User user, String state) {
            String userIdShort = user.getUserId() != null ? user.getUserId().substring(0, Math.min(8, user.getUserId().length())) : "null";
            
            Log.d("UserListAdapter", "🎨 [BUTTON UI] User: " + user.getUsername() + 
                  " | State: " + state + " | userId: " + userIdShort);
            
            switch (state) {
                case "FRIEND":
                    buttonAction.setText("Message");
                    buttonAction.setEnabled(true);
                    buttonAction.setOnClickListener(v -> {
                        Log.d("UserListAdapter", "💬 [CLICK] Message | User: " + user.getUsername() + " | userId: " + userIdShort);
                        if (listener != null) listener.onMessageClick(user);
                    });
                    break;
                    
                case "FOLLOW_BACK":
                    buttonAction.setText("Follow Back");
                    buttonAction.setEnabled(true);
                    buttonAction.setOnClickListener(v -> {
                        Log.d("UserListAdapter", "👥 [CLICK] Follow Back | User: " + user.getUsername() + " | userId: " + userIdShort + " → will become FRIEND");
                        if (listener != null) listener.onFollowClick(user);
                    });
                    break;
                    
                case "FOLLOWING":
                    buttonAction.setText("Unfollow");
                    buttonAction.setEnabled(true);
                    buttonAction.setOnClickListener(v -> {
                        Log.d("UserListAdapter", "👥 [CLICK] Unfollow | User: " + user.getUsername() + " | userId: " + userIdShort + " → will become FOLLOW_BACK/FOLLOW");
                        if (listener != null) listener.onUnfollowClick(user);
                    });
                    break;
                    
                case "FOLLOW":
                default:
                    buttonAction.setText("Follow");
                    buttonAction.setEnabled(true);
                    buttonAction.setOnClickListener(v -> {
                        Log.d("UserListAdapter", "👥 [CLICK] Follow | User: " + user.getUsername() + " | userId: " + userIdShort + " → will become FOLLOWING");
                        if (listener != null) listener.onFollowClick(user);
                    });
                    break;
            }
        }
    }

    /**
     * ✅ UPDATE LIST - Converts User objects to ListItem types
     */
    public void updateList(List<User> newUserList) {
        if (newUserList == null) {
            this.itemList = new ArrayList<>();
        } else {
            this.itemList = new ArrayList<>();
            for (User user : newUserList) {
                if (user != null && user.getUserId() != null) {
                    // Real user
                    this.itemList.add(new ListItem.UserItem(user));
                }
            }
        }
        notifyDataSetChanged();
    }
}
