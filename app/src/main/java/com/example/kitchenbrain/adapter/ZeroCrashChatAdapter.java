package com.example.kitchenbrain.adapter;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.model.DeliveryState;
import com.example.kitchenbrain.state.ChatStateManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 🔥 ZERO-CRASH CHAT ADAPTER - Media Support Added
 */
public class ZeroCrashChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> 
    implements ChatStateManager.StateListener {
    
    private static final String TAG = "ZeroCrashAdapter";
    private static final int TYPE_MY_TEXT = 1;
    private static final int TYPE_OTHER_TEXT = 2;
    private static final int TYPE_MY_VOICE = 3;
    private static final int TYPE_OTHER_VOICE = 4;
    private static final int TYPE_MY_IMAGE = 5;
    private static final int TYPE_OTHER_IMAGE = 6;
    
    private final List<ChatMessage> currentList = new ArrayList<>();
    private final Object renderLock = new Object();
    
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;
    private OnMessageRetryClickListener retryClickListener;
    
    private final List<OnMessagesChangeListener> stateListeners = new CopyOnWriteArrayList<>();
    private int lastAnimatedPosition = -1;

    // Audio Playback Management
    private MediaPlayer mediaPlayer;
    private String currentlyPlayingUrl = null;
    private VoiceViewHolder currentlyPlayingHolder = null;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, int position);
    }
    
    public interface OnMessageRetryClickListener {
        void onRetryMessage(ChatMessage message);
    }
    
    public interface OnMessagesChangeListener {
        void onMessagesChanged(int count);
    }
    
    public ZeroCrashChatAdapter(String currentUserId, 
                               OnMessageLongClickListener longClickListener,
                               OnMessageRetryClickListener retryClickListener) {
        this.currentUserId = currentUserId;
        this.longClickListener = longClickListener;
        this.retryClickListener = retryClickListener;
    }
    
    public void addOnMessagesChangeListener(OnMessagesChangeListener listener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }
    
    @Override
    public void onStateChanged(List<ChatMessage> messages) {
        renderMessages(messages);
    }
    
    public void renderMessages(List<ChatMessage> newMessages) {
        synchronized (renderLock) {
            List<ChatMessage> oldList = new ArrayList<>(currentList);
            List<ChatMessage> sortedNewList = sortMessages(new ArrayList<>(newMessages));
            
            ChatDiffCallback diffCallback = new ChatDiffCallback(oldList, sortedNewList);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
            
            currentList.clear();
            currentList.addAll(sortedNewList);
            diffResult.dispatchUpdatesTo(this);
            
            int count = currentList.size();
            for (OnMessagesChangeListener listener : stateListeners) {
                listener.onMessagesChanged(count);
            }
        }
    }
    
    private List<ChatMessage> sortMessages(List<ChatMessage> messages) {
        Collections.sort(messages, (a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return Long.compare(a.getTimestamp().getSeconds(), b.getTimestamp().getSeconds());
        });
        return messages;
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = currentList.get(position);
        boolean isMine = message.getSenderId().equals(currentUserId);
        String type = message.getMessageType();
        
        if (isMine) {
            if ("voice".equals(type)) return TYPE_MY_VOICE;
            if ("image".equals(type)) return TYPE_MY_IMAGE;
            return TYPE_MY_TEXT;
        } else {
            if ("voice".equals(type)) return TYPE_OTHER_VOICE;
            if ("image".equals(type)) return TYPE_OTHER_IMAGE;
            return TYPE_OTHER_TEXT;
        }
    }

    public ChatMessage getMessage(int position) {
        if (position >= 0 && position < currentList.size()) {
            return currentList.get(position);
        }
        return null;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_MY_VOICE:
                return new MyVoiceViewHolder(inflater.inflate(R.layout.item_voice_my, parent, false));
            case TYPE_OTHER_VOICE:
                return new OtherVoiceViewHolder(inflater.inflate(R.layout.item_voice_other, parent, false));
            case TYPE_MY_IMAGE:
                return new MyImageViewHolder(inflater.inflate(R.layout.item_image_my, parent, false));
            case TYPE_OTHER_IMAGE:
                return new OtherImageViewHolder(inflater.inflate(R.layout.item_image_other, parent, false));
            case TYPE_MY_TEXT:
                return new MyMessageViewHolder(inflater.inflate(R.layout.item_message_my, parent, false));
            default:
                return new OtherMessageViewHolder(inflater.inflate(R.layout.item_message_other, parent, false));
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = currentList.get(position);
        boolean isFirstInGroup = true;
        if (position > 0) {
            ChatMessage prevMessage = currentList.get(position - 1);
            if (prevMessage.getSenderId().equals(message.getSenderId())) isFirstInGroup = false;
        }
        
        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message, isFirstInGroup);
        } else if (holder instanceof OtherMessageViewHolder) {
            ((OtherMessageViewHolder) holder).bind(message, isFirstInGroup, isFirstInGroup);
        } else if (holder instanceof VoiceViewHolder) {
            ((VoiceViewHolder) holder).bind(message, isFirstInGroup, holder instanceof OtherVoiceViewHolder);
        } else if (holder instanceof MyImageViewHolder) {
            ((MyImageViewHolder) holder).bind(message, isFirstInGroup);
        } else if (holder instanceof OtherImageViewHolder) {
            ((OtherImageViewHolder) holder).bind(message, isFirstInGroup);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onMessageLongClick(message, holder.getAdapterPosition());
            return true;
        });

        if (position > lastAnimatedPosition) {
            startAppearanceAnimation(holder.itemView);
            lastAnimatedPosition = position;
        }
    }

    private void startAppearanceAnimation(View view) {
        AnimationSet set = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(200);
        AlphaAnimation fade = new AlphaAnimation(0.0f, 1.0f);
        fade.setDuration(200);
        set.addAnimation(scale);
        set.addAnimation(fade);
        view.startAnimation(set);
    }
    
    @Override
    public int getItemCount() {
        return currentList.size();
    }

    private static class MyMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText, timestampText, statusText, textEdited;
        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            statusText = itemView.findViewById(R.id.statusText);
            textEdited = itemView.findViewById(R.id.textEdited);
        }
        public void bind(ChatMessage message, boolean isFirst) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            timestampText.setVisibility(View.VISIBLE);
            updateStatus(message);
            applyMargins(itemView, isFirst);
            if (textEdited != null) {
                textEdited.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
            }
        }
        private void updateStatus(ChatMessage m) {
            if (m.getStatus() == ChatMessage.MessageStatus.SENDING) statusText.setText("⏳");
            else if (m.getStatus() == ChatMessage.MessageStatus.FAILED) statusText.setText("❗");
            else statusText.setText(m.isRead() ? "✔✔" : m.getDeliveryState() == DeliveryState.DELIVERED ? "✔✔" : "✔");
        }
    }

    private static class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText, timestampText, senderNameText, textEdited;
        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            senderNameText = itemView.findViewById(R.id.senderNameText);
            textEdited = itemView.findViewById(R.id.textEdited);
        }
        public void bind(ChatMessage message, boolean showName, boolean isFirst) {
            messageText.setText(message.getText());
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            timestampText.setVisibility(View.VISIBLE);
            senderNameText.setVisibility(showName ? View.VISIBLE : View.GONE);
            senderNameText.setText(message.getSenderName());
            applyMargins(itemView, isFirst);
            if (textEdited != null) {
                textEdited.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);
            }
        }
    }

    private static class MyImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImage;
        private TextView timestampText, statusText;
        public MyImageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImage = itemView.findViewById(R.id.messageImage);
            timestampText = itemView.findViewById(R.id.timestampText);
            statusText = itemView.findViewById(R.id.statusText);
        }
        public void bind(ChatMessage message, boolean isFirst) {
            Glide.with(itemView.getContext()).load(message.getMediaUrl()).into(messageImage);
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            updateStatus(message);
            applyMargins(itemView, isFirst);
        }
        private void updateStatus(ChatMessage m) {
            if (m.getStatus() == ChatMessage.MessageStatus.SENDING) statusText.setText("⏳");
            else if (m.getStatus() == ChatMessage.MessageStatus.FAILED) statusText.setText("❗");
            else statusText.setText(m.isRead() ? "✔✔" : m.getDeliveryState() == DeliveryState.DELIVERED ? "✔✔" : "✔");
        }
    }

    private static class OtherImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView messageImage;
        private TextView timestampText, senderNameText;
        public OtherImageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageImage = itemView.findViewById(R.id.messageImage);
            timestampText = itemView.findViewById(R.id.timestampText);
            senderNameText = itemView.findViewById(R.id.senderNameText);
        }
        public void bind(ChatMessage message, boolean isFirst) {
            Glide.with(itemView.getContext()).load(message.getMediaUrl()).into(messageImage);
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            senderNameText.setVisibility(isFirst ? View.VISIBLE : View.GONE);
            senderNameText.setText(message.getSenderName());
            applyMargins(itemView, isFirst);
        }
    }

    private abstract class VoiceViewHolder extends RecyclerView.ViewHolder {
        protected ImageButton playPauseButton;
        protected ProgressBar progressBar;
        protected TextView textDuration, timestampText;
        public VoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            playPauseButton = itemView.findViewById(R.id.buttonPlayPause);
            progressBar = itemView.findViewById(R.id.voiceProgress);
            textDuration = itemView.findViewById(R.id.textDuration);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
        public void bind(ChatMessage message, boolean isFirst, boolean showName) {
            textDuration.setText(formatDuration(message.getDuration()));
            timestampText.setText(formatTimestamp(message.getTimestamp()));
            timestampText.setVisibility(View.VISIBLE);
            applyMargins(itemView, isFirst);
            
            boolean isPlaying = message.getMediaUrl() != null && message.getMediaUrl().equals(currentlyPlayingUrl);
            playPauseButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            
            playPauseButton.setOnClickListener(v -> handleVoicePlay(message, this));
        }
        public void updateProgress(int current, int total) {
            progressBar.setProgress((int) ((float) current / total * 100));
            textDuration.setText(formatDuration(current / 1000));
        }
        public void resetUI(long duration) {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow);
            progressBar.setProgress(0);
            textDuration.setText(formatDuration(duration));
        }
    }

    private class MyVoiceViewHolder extends VoiceViewHolder {
        private TextView statusText;
        public MyVoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            statusText = itemView.findViewById(R.id.statusText);
        }
        @Override
        public void bind(ChatMessage message, boolean isFirst, boolean showName) {
            super.bind(message, isFirst, showName);
            if (message.getStatus() == ChatMessage.MessageStatus.SENDING) statusText.setText("⏳");
            else statusText.setText(message.isRead() ? "✔✔" : "✔");
        }
    }

    private class OtherVoiceViewHolder extends VoiceViewHolder {
        private TextView senderNameText;
        public OtherVoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameText = itemView.findViewById(R.id.senderNameText);
        }
        @Override
        public void bind(ChatMessage message, boolean isFirst, boolean showName) {
            super.bind(message, isFirst, showName);
            senderNameText.setVisibility(showName ? View.VISIBLE : View.GONE);
            senderNameText.setText(message.getSenderName());
        }
    }

    private void handleVoicePlay(ChatMessage message, VoiceViewHolder holder) {
        String url = message.getMediaUrl();
        if (url == null) return;

        if (url.equals(currentlyPlayingUrl)) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                holder.playPauseButton.setImageResource(R.drawable.ic_play_arrow);
                stopProgressUpdates();
            } else if (mediaPlayer != null) {
                mediaPlayer.start();
                holder.playPauseButton.setImageResource(R.drawable.ic_pause);
                startProgressUpdates();
            }
        } else {
            stopVoice();
            currentlyPlayingUrl = url;
            currentlyPlayingHolder = holder;
            startVoice(url, message.getDuration());
        }
    }

    private void startVoice(String url, long duration) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                if (currentlyPlayingHolder != null) {
                    currentlyPlayingHolder.playPauseButton.setImageResource(R.drawable.ic_pause);
                    startProgressUpdates();
                }
            });
            mediaPlayer.setOnCompletionListener(mp -> stopVoice());
        } catch (IOException e) {
            Log.e(TAG, "Error playing voice", e);
            stopVoice();
        }
    }

    private void stopVoice() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopProgressUpdates();
        if (currentlyPlayingHolder != null) {
            currentlyPlayingHolder.resetUI(0); 
            currentlyPlayingHolder = null;
        }
        currentlyPlayingUrl = null;
    }

    private void startProgressUpdates() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && currentlyPlayingHolder != null) {
                    currentlyPlayingHolder.updateProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                    progressHandler.postDelayed(this, 100);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
    }

    private static void applyMargins(View view, boolean isFirst) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.topMargin = (int) ((isFirst ? 12 : 2) * view.getResources().getDisplayMetrics().density);
        view.setLayoutParams(params);
    }

    private static String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        return new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(timestamp.toDate());
    }

    private static String formatDuration(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static class ChatDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList, newList;
        public ChatDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) { this.oldList = oldList; this.newList = newList; }
        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }
        @Override public boolean areItemsTheSame(int o, int n) { return oldList.get(o).getMessageId().equals(newList.get(n).getMessageId()); }
        @Override public boolean areContentsTheSame(int o, int n) {
            ChatMessage oldM = oldList.get(o), newM = newList.get(n);
            return oldM.getText().equals(newM.getText()) && 
                   oldM.getStatus() == newM.getStatus() && 
                   oldM.isRead() == newM.isRead() &&
                   oldM.isEdited() == newM.isEdited() &&
                   (oldM.getMediaUrl() == null ? newM.getMediaUrl() == null : oldM.getMediaUrl().equals(newM.getMediaUrl()));
        }
    }
}
