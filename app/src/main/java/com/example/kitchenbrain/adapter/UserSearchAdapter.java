package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.example.kitchenbrain.repository.FollowRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ STABLE UserSearchAdapter
 * Uses AsyncListDiffer to prevent "Inconsistency detected" crashes during rapid filtering.
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private static final String TAG = "UserSearchAdapter";
    
    private final Context context;
    private List<User> allUsers = new ArrayList<>();
    private final FollowRepository followRepository;
    private final OnUserInteractionListener listener;
    private final String currentUserId;
    
    private final int colorPrimaryBlue;
    private final int colorDarkGray;
    private final int colorWhite;
    private final int colorSecondary;

    private final AsyncListDiffer<User> differ;

    public interface OnUserInteractionListener {
        void onUserClick(User user);
        void onFollowToggle(User user, boolean isFollowing);
        void onMessageClick(User user);
    }

    public UserSearchAdapter(Context context, OnUserInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
            : null;
        this.followRepository = new FollowRepository();
        
        this.colorPrimaryBlue = ContextCompat.getColor(context, R.color.primary_blue);
        this.colorDarkGray = ContextCompat.getColor(context, android.R.color.darker_gray);
        this.colorWhite = ContextCompat.getColor(context, android.R.color.white);
        this.colorSecondary = ContextCompat.getColor(context, R.color.md_secondary_light);

        this.differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    }

    /**
     * Cleanup resources. In the AsyncListDiffer version, we just clear the list.
     */
    public void cleanup() {
        differ.submitList(null);
    }

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getUserId() != null && oldItem.getUserId().equals(newItem.getUserId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getUsername().equals(newItem.getUsername()) &&
                   oldItem.getRecipesCount() == newItem.getRecipesCount() &&
                   (oldItem.getAvatarUrl() == null ? newItem.getAvatarUrl() == null : oldItem.getAvatarUrl().equals(newItem.getAvatarUrl()));
        }
    };

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(differ.getCurrentList().get(position));
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void setUsers(List<User> newUsers) {
        this.allUsers = newUsers != null ? new ArrayList<>(newUsers) : new ArrayList<>();
        differ.submitList(allUsers);
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            differ.submitList(new ArrayList<>(allUsers));
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<User> filtered = new ArrayList<>();
        for (User user : allUsers) {
            if ((user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery)) ||
                (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery))) {
                filtered.add(user);
            }
        }
        differ.submitList(filtered);
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final de.hdodenhof.circleimageview.CircleImageView imageProfile;
        private final TextView textUsername;
        private final TextView textEmail;
        private final TextView textRecipeCount;
        private final com.google.android.material.button.MaterialButton buttonFollow;
        private final com.google.android.material.button.MaterialButton buttonUnfollow;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            textUsername = itemView.findViewById(R.id.textUsername);
            textEmail = itemView.findViewById(R.id.textEmail);
            textRecipeCount = itemView.findViewById(R.id.textRecipeCount);
            buttonFollow = itemView.findViewById(R.id.buttonFollow);
            buttonUnfollow = itemView.findViewById(R.id.buttonUnfollow);
        }

        public void bind(User user) {
            textUsername.setText(user.getUsername());
            if (user.getEmail() != null) {
                textEmail.setText(maskEmail(user.getEmail()));
                textEmail.setVisibility(View.VISIBLE);
            } else {
                textEmail.setVisibility(View.GONE);
            }

            textRecipeCount.setText(context.getString(R.string.recipes_count_format, user.getRecipesCount()));

            Glide.with(context)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(imageProfile);

            updateFollowButton(user);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(user);
            });
        }

        private String maskEmail(String email) {
            if (email != null && email.contains("@")) {
                int atIndex = email.indexOf("@");
                String namePart = email.substring(0, atIndex);
                String domainPart = email.substring(atIndex);
                if (namePart.length() > 2) {
                    return namePart.substring(0, 2) + "***" + domainPart;
                }
            }
            return email;
        }

        private void updateFollowButton(User user) {
            String userId = user.getUserId();
            buttonUnfollow.setVisibility(View.GONE); // Default hidden
            
            if (userId == null) {
                setButtonState("Follow", colorPrimaryBlue, v -> followUser(user));
                return;
            }
            
            FollowGraphRepository graph = FollowGraphRepository.getInstance();
            boolean isFollowing = graph.isFollowing(userId);
            boolean isFollower = graph.isFollower(userId);
            
            if (isFollowing && isFollower) {
                setButtonState("Message", colorPrimaryBlue, v -> {
                    if (listener != null) listener.onMessageClick(user);
                });
                buttonUnfollow.setVisibility(View.VISIBLE);
                buttonUnfollow.setOnClickListener(v -> unfollowUser(user));
            } else if (isFollowing) {
                setButtonState("Following", colorDarkGray, v -> unfollowUser(user));
            } else if (isFollower) {
                setButtonState("Follow Back", colorPrimaryBlue, v -> followUser(user));
            } else {
                setButtonState("Follow", colorPrimaryBlue, v -> followUser(user));
            }
        }
        
        private void setButtonState(String text, int bgColor, View.OnClickListener clickListener) {
            buttonFollow.setText(text);
            buttonFollow.setBackgroundColor(bgColor);
            buttonFollow.setTextColor(colorWhite);
            buttonFollow.setOnClickListener(clickListener);
        }

        private void followUser(User user) {
            if (currentUserId == null) {
                Toast.makeText(context, "Please log in to follow users", Toast.LENGTH_SHORT).show();
                return;
            }

            if (followRepository != null) {
                int position = getBindingAdapterPosition();
                followRepository.followUser(currentUserId, user.getUserId(), (success, error) -> {
                    if (success) {
                        FollowGraphRepository.getInstance().refresh();
                        if (listener != null) listener.onFollowToggle(user, true);
                        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position);
                    } else {
                        Toast.makeText(context, "Failed to follow: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private void unfollowUser(User user) {
            if (currentUserId == null) return;

            if (followRepository != null) {
                int position = getBindingAdapterPosition();
                followRepository.unfollowUser(currentUserId, user.getUserId(), (success, error) -> {
                    if (success) {
                        FollowGraphRepository.getInstance().refresh();
                        if (listener != null) listener.onFollowToggle(user, false);
                        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position);
                    } else {
                        Toast.makeText(context, "Failed to unfollow: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
