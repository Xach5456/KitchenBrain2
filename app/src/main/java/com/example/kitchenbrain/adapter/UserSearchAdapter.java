package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.example.kitchenbrain.repository.FollowRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Premium UserSearchAdapter 2026.
 * Полностью сохраняет функционал подписок и сообщений, обновляя только визуальную часть.
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private final Context context;
    private List<User> allUsers = new ArrayList<>();
    private final FollowRepository followRepository;
    private final OnUserInteractionListener listener;
    private final String currentUserId;
    private int lastPosition = -1;

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
        this.differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return Objects.equals(oldItem.getUserId(), newItem.getUserId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return Objects.equals(oldItem.getUsername(), newItem.getUsername()) &&
                   oldItem.getRecipesCount() == newItem.getRecipesCount() &&
                   Objects.equals(oldItem.getAvatarUrl(), newItem.getAvatarUrl());
        }
    };

    public void cleanup() {
        differ.submitList(null);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(differ.getCurrentList().get(position));
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            animation.setDuration(400);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void setUsers(List<User> newUsers) {
        this.allUsers = newUsers != null ? new ArrayList<>(newUsers) : new ArrayList<>();
        differ.submitList(allUsers);
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final de.hdodenhof.circleimageview.CircleImageView imageProfile;
        private final TextView textUsername, textEmail, textRecipeCount;
        private final com.google.android.material.button.MaterialButton buttonFollow, buttonUnfollow;

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
            textEmail.setText(user.getEmail() != null ? maskEmail(user.getEmail()) : "");
            textEmail.setVisibility(user.getEmail() != null ? View.VISIBLE : View.GONE);
            
            textRecipeCount.setText(context.getString(R.string.recipes_count_format, user.getRecipesCount()));

            Glide.with(context)
                .load(user.getAvatarUrl())
                .transition(DrawableTransitionOptions.withCrossFade())
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
                if (namePart.length() > 2) {
                    return namePart.substring(0, 2) + "***" + email.substring(atIndex);
                }
            }
            return email;
        }

        private void updateFollowButton(User user) {
            String userId = user.getUserId();
            buttonUnfollow.setVisibility(View.GONE);
            
            if (userId == null) {
                setButtonState("Follow", true, v -> followUser(user));
                return;
            }
            
            FollowGraphRepository graph = FollowGraphRepository.getInstance();
            boolean isFollowing = graph.isFollowing(userId);
            boolean isFollower = graph.isFollower(userId);
            
            if (isFollowing && isFollower) {
                setButtonState("Message", true, v -> {
                    if (listener != null) listener.onMessageClick(user);
                });
                buttonUnfollow.setVisibility(View.VISIBLE);
                buttonUnfollow.setOnClickListener(v -> unfollowUser(user));
            } else if (isFollowing) {
                setButtonState("Following", false, v -> unfollowUser(user));
            } else if (isFollower) {
                setButtonState("Follow Back", true, v -> followUser(user));
            } else {
                setButtonState("Follow", true, v -> followUser(user));
            }
        }
        
        private void setButtonState(String text, boolean isPrimary, View.OnClickListener clickListener) {
            buttonFollow.setText(text);
            if (isPrimary) {
                buttonFollow.setBackgroundColor(ContextCompat.getColor(context, R.color.md_primary_light));
                buttonFollow.setTextColor(Color.WHITE);
                buttonFollow.setStrokeWidth(0);
            } else {
                buttonFollow.setBackgroundColor(ContextCompat.getColor(context, R.color.md_surface_container_high_light));
                buttonFollow.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                buttonFollow.setStrokeWidth(0);
            }
            buttonFollow.setOnClickListener(clickListener);
        }

        private void followUser(User user) {
            if (currentUserId == null) return;
            followRepository.followUser(currentUserId, user.getUserId(), (success, error) -> {
                if (success) {
                    FollowGraphRepository.getInstance().refresh();
                    if (listener != null) listener.onFollowToggle(user, true);
                    notifyItemChanged(getBindingAdapterPosition());
                }
            });
        }

        private void unfollowUser(User user) {
            if (currentUserId == null) return;
            followRepository.unfollowUser(currentUserId, user.getUserId(), (success, error) -> {
                if (success) {
                    FollowGraphRepository.getInstance().refresh();
                    if (listener != null) listener.onFollowToggle(user, false);
                    notifyItemChanged(getBindingAdapterPosition());
                }
            });
        }
    }
}
