package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Instagram-style stories showing followed users
 * Supports "Create Story" button as first item
 */
public class StoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CREATE_STORY = 0;
    private static final int TYPE_STORY = 1;

    private Context context;
    private List<User> followedUsers;
    private OnStoryClickListener listener;
    private String currentUserId; // For "Your Story" button

    public interface OnStoryClickListener {
        void onStoryClick(User user);
        void onCreateStoryClick();
    }

    public StoryAdapter(Context context, String currentUserId, OnStoryClickListener listener) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.followedUsers = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        // First item is always "Create Story" button
        return position == 0 ? TYPE_CREATE_STORY : TYPE_STORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CREATE_STORY) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_create_story, parent, false);
            return new CreateStoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
            return new StoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CreateStoryViewHolder) {
            ((CreateStoryViewHolder) holder).bind();
        } else if (holder instanceof StoryViewHolder && position > 0) {
            User user = followedUsers.get(position - 1); // Adjust for "Create Story" at position 0
            ((StoryViewHolder) holder).bind(user);
        }
    }

    @Override
    public int getItemCount() {
        // +1 for "Create Story" button
        return followedUsers.size() + 1;
    }

    public void setUsers(List<User> users) {
        this.followedUsers = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for "Create Story" button
     */
    class CreateStoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageCreateStoryRing;
        private ImageView imageCreateStoryAvatar;
        private ImageView imageCreateStoryPlus;
        private TextView textCreateStoryLabel;

        public CreateStoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCreateStoryRing = itemView.findViewById(R.id.imageCreateStoryRing);
            imageCreateStoryAvatar = itemView.findViewById(R.id.imageCreateStoryAvatar);
            imageCreateStoryPlus = itemView.findViewById(R.id.imageCreateStoryPlus);
            textCreateStoryLabel = itemView.findViewById(R.id.textCreateStoryLabel);
        }

        public void bind() {
            // Click listener for creating story
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCreateStoryClick();
                }
            });
        }
    }

    class StoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageStoryRing;
        private ImageView imageStoryAvatar;
        private TextView textStoryUsername;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageStoryRing = itemView.findViewById(R.id.imageStoryRing);
            imageStoryAvatar = itemView.findViewById(R.id.imageStoryAvatar);
            textStoryUsername = itemView.findViewById(R.id.textStoryUsername);
        }

        public void bind(User user) {
            // Display username
            textStoryUsername.setText(user.getUsername() != null ? user.getUsername() : "User");

            // Load avatar with Glide (circular)
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(context)
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(imageStoryAvatar);
            } else {
                imageStoryAvatar.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Click listener - open user profile or view story
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(user);
                }
            });
        }
    }
}
