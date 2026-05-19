package com.example.kitchenbrain.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.kitchenbrain.Notification;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.repository.FollowRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Premium Notification Adapter with Material 3 styling
 * Updated to use correct Firestore pathing: notifications/{userId}/user_notifications
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private static final String TAG = "NotificationAdapter";
    
    private final Context context;
    private final List<Notification> notifications;
    private final OnNotificationClickListener clickListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notifications, 
                               OnNotificationClickListener clickListener) {
        this.context = context;
        this.notifications = notifications;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView avatarImage;
        private final TextView titleText;
        private final TextView timestampText;
        private final LinearLayout layoutActions;
        private final MaterialButton followBackButton;
        private final MaterialButton rejectButton;
        private final ImageView unreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.notificationCard);
            avatarImage = itemView.findViewById(R.id.imageAvatar);
            titleText = itemView.findViewById(R.id.textTitle);
            timestampText = itemView.findViewById(R.id.textTimestamp);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            followBackButton = itemView.findViewById(R.id.buttonFollowBack);
            rejectButton = itemView.findViewById(R.id.buttonReject);
            unreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }

        public void bind(Notification notification) {
            String type = notification.getType();
            boolean isRead = notification.isRead();
            
            String username = notification.getSenderUsername();
            if (username == null || username.isEmpty()) {
                username = "Someone";
            }
            
            if ("follow".equals(type)) {
                titleText.setText(username + " followed you");
                layoutActions.setVisibility(View.VISIBLE);
            } else if ("mutual_follow".equals(type)) {
                titleText.setText("🎉 " + username + " followed you back!");
                layoutActions.setVisibility(View.GONE);
            } else {
                titleText.setText(notification.getMessage());
                layoutActions.setVisibility(View.GONE);
            }

            timestampText.setText(formatTimestamp(notification.getCreatedAt()));
            loadAvatar(notification);

            // Action Buttons
            followBackButton.setOnClickListener(v -> followBack(notification));
            rejectButton.setOnClickListener(v -> rejectNotification(notification));

            unreadDot.setVisibility(isRead ? View.GONE : View.VISIBLE);
            
            // Mark as read when bound
            if (!isRead) {
                markAsRead(notification);
            }
        }

        private void followBack(Notification notification) {
            String currentUserId = FirebaseAuth.getInstance().getUid();
            if (currentUserId == null) return;

            FollowRepository followRepository = new FollowRepository();
            followRepository.followUser(currentUserId, notification.getSenderId(), (success, error) -> {
                if (success) {
                    Toast.makeText(context, "Following back " + notification.getSenderUsername(), Toast.LENGTH_SHORT).show();
                    deleteNotification(notification);
                } else {
                    Toast.makeText(context, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void rejectNotification(Notification notification) {
            deleteNotification(notification);
            Toast.makeText(context, "Notification removed", Toast.LENGTH_SHORT).show();
        }

        private void deleteNotification(Notification notification) {
            String currentUserId = FirebaseAuth.getInstance().getUid();
            if (currentUserId == null) return;

            FirebaseFirestore.getInstance().collection("notifications")
                    .document(currentUserId)
                    .collection("user_notifications")
                    .document(notification.getId())
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete notification", e));
        }

        private void markAsRead(Notification notification) {
            String currentUserId = FirebaseAuth.getInstance().getUid();
            if (currentUserId == null) return;

            FirebaseFirestore.getInstance().collection("notifications")
                    .document(currentUserId)
                    .collection("user_notifications")
                    .document(notification.getId())
                    .update("isRead", true);
        }

        private void loadAvatar(Notification notification) {
            String avatarUrl = notification.getSenderAvatarUrl();
            Glide.with(context)
                .load(avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : R.drawable.ic_user)
                .placeholder(R.drawable.ic_user)
                .circleCrop()
                .into(avatarImage);
        }

        private String formatTimestamp(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            if (diff < TimeUnit.MINUTES.toMillis(1)) return "Just now";
            if (diff < TimeUnit.HOURS.toMillis(1)) return TimeUnit.MILLISECONDS.toMinutes(diff) + "m ago";
            if (diff < TimeUnit.DAYS.toMillis(1)) return TimeUnit.MILLISECONDS.toHours(diff) + "h ago";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
