package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.FeedItem;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ OPTIMIZED FeedPostAdapter
 * Uses AsyncListDiffer to prevent ANRs and RecyclerView inconsistency crashes.
 * AsyncListDiffer handles background diffing and ensures atomic updates to the UI.
 */
public class FeedPostAdapter extends RecyclerView.Adapter<FeedPostAdapter.FeedViewHolder> {

    private static final String TAG = "FeedPostAdapter";
    
    private final Context context;
    private OnFeedInteractionListener listener;
    
    private final AsyncListDiffer<FeedItem> differ;

    public interface OnFeedInteractionListener {
        void onLikeClick(FeedItem item, int position);
        void onCommentClick(FeedItem item, int position);
        void onSendClick(FeedItem item, int position);
        void onSaveClick(FeedItem item, int position);
        void onShareClick(FeedItem item, int position);
        void onArticleClick(FeedItem item, int position);
    }

    public FeedPostAdapter(Context context) {
        this.context = context;
        this.differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<FeedItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<FeedItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull FeedItem oldItem, @NonNull FeedItem newItem) {
            return oldItem.getStableId().equals(newItem.getStableId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FeedItem oldItem, @NonNull FeedItem newItem) {
            // FeedItem implements equals() to check social states (liked/saved)
            return oldItem.equals(newItem);
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull FeedItem oldItem, @NonNull FeedItem newItem) {
            // If items are same but content changed, use payload to avoid full re-bind
            return "action_buttons";
        }
    };

    public void setOnFeedInteractionListener(OnFeedInteractionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed_post, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        holder.bind(differ.getCurrentList().get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position, @NonNull List<Object> payloads) {
        holder.bind(differ.getCurrentList().get(position), payloads);
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    /**
     * ✅ THREAD-SAFE: Uses AsyncListDiffer to handle background diffing and multiple updates.
     */
    public void setFeedItems(List<FeedItem> newFeedItems) {
        differ.submitList(newFeedItems);
    }

    /**
     * Update a single item by submitting a new list to the differ.
     */
    public void updateItem(FeedItem updatedItem) {
        List<FeedItem> newList = new ArrayList<>(differ.getCurrentList());
        for (int i = 0; i < newList.size(); i++) {
            if (newList.get(i).getStableId().equals(updatedItem.getStableId())) {
                newList.set(i, updatedItem);
                differ.submitList(newList);
                break;
            }
        }
    }

    public void clear() {
        differ.submitList(null);
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageAvatar, imagePost;
        private final TextView textUsername, textCaptionUsername, textCaption, textLikesCount, textTimestamp;
        private final ImageButton buttonLike, buttonComment, buttonSend, buttonShare, buttonSave;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            imagePost = itemView.findViewById(R.id.imagePost);
            textUsername = itemView.findViewById(R.id.textUsername);
            textCaptionUsername = itemView.findViewById(R.id.textCaptionUsername);
            textCaption = itemView.findViewById(R.id.textCaption);
            textLikesCount = itemView.findViewById(R.id.textLikesCount);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            buttonLike = itemView.findViewById(R.id.buttonLike);
            buttonComment = itemView.findViewById(R.id.buttonComment);
            buttonSend = itemView.findViewById(R.id.buttonSend);
            buttonShare = itemView.findViewById(R.id.buttonShare);
            buttonSave = itemView.findViewById(R.id.buttonSave);
            
            setupClickListeners();
        }

        private void setupClickListeners() {
            View.OnClickListener clickListener = v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || listener == null) return;
                
                // Always get the current item from the differ's list
                FeedItem item = differ.getCurrentList().get(pos);
                
                if (v == buttonLike) listener.onLikeClick(item, pos);
                else if (v == buttonComment) listener.onCommentClick(item, pos);
                else if (v == buttonSend) listener.onSendClick(item, pos);
                else if (v == buttonShare) listener.onShareClick(item, pos);
                else if (v == buttonSave) listener.onSaveClick(item, pos);
                else if (v == itemView) listener.onArticleClick(item, pos);
            };

            buttonLike.setOnClickListener(clickListener);
            buttonComment.setOnClickListener(clickListener);
            buttonSend.setOnClickListener(clickListener);
            buttonShare.setOnClickListener(clickListener);
            buttonSave.setOnClickListener(clickListener);
            itemView.setOnClickListener(clickListener);
        }

        public void bind(FeedItem item, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) {
                updateSocialStates(item);
                return;
            }
            bind(item);
        }

        public void bind(FeedItem item) {
            textUsername.setText(item.getUsername());
            textCaptionUsername.setText(item.getUsername());
            textCaption.setText(item.getCaption());
            textLikesCount.setText(context.getString(R.string.recipe_likes_count, item.getDisplayLikesCount()));
            
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(imagePost);

            updateSocialStates(item);
        }

        private void updateSocialStates(FeedItem item) {
            buttonLike.setImageResource(item.isLikedByMe() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            buttonLike.setColorFilter(context.getResources().getColor(item.isLikedByMe() ? R.color.error_red : R.color.text_primary));
            buttonSave.setImageResource(item.isSavedByMe() ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            textLikesCount.setText(item.getFormattedLikes() + " likes");
        }
    }
}
