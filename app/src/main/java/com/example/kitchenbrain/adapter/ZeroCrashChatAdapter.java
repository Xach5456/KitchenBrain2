package com.example.kitchenbrain.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.model.ChatMessage;
import com.example.kitchenbrain.model.DeliveryState;
import com.example.kitchenbrain.state.ChatStateManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 🔥 ZERO-CRASH CHAT ADAPTER - Voice Support Removed
 */
public class ZeroCrashChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> 
    implements ChatStateManager.StateListener {
    
    private static final String TAG = "ZeroCrashAdapter";
    private static final int TYPE_MY_TEXT = 1;
    private static final int TYPE_OTHER_TEXT = 2;
    private static final int TYPE_MY_IMAGE = 5;
    private static final int TYPE_OTHER_IMAGE = 6;
    
    private final List<ChatMessage> currentList = new ArrayList<>();
    private final Object renderLock = new Object();
    
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;
    private OnMessageRetryClickListener retryClickListener;
    
    private final List<OnMessagesChangeListener> stateListeners = new CopyOnWriteArrayList<>();
    private int lastAnimatedPosition = -1;

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
            if ("image".equals(type)) return TYPE_MY_IMAGE;
            return TYPE_MY_TEXT;
        } else {
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
            case TYPE_MY_IMAGE:
                return new MyImageViewHolder(inflater.inflate(R.layout.item_image_my, parent, false));
            case TYPE_OTHER_IMAGE:
                return new OtherImageViewHolder(inflater.inflate(R.layout.item_image_other, parent, false));
            case TYPE_MY_TEXT:
                return new MyMessageViewHolder(inflater.inflate(R.layout.item_message_my, parent, false));
            case TYPE_OTHER_TEXT:
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
        Animation fade = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_in);
        view.startAnimation(fade);
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
            if (m != null) {
                statusText.setText(formatStatus(m));
                return;
            }
            statusText.setText("");
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
            if (m != null) {
                statusText.setText(formatStatus(m));
                return;
            }
            statusText.setText("");
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

    private static void applyMargins(View view, boolean isFirst) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.topMargin = (int) ((isFirst ? 12 : 2) * view.getResources().getDisplayMetrics().density);
        view.setLayoutParams(params);
    }

    private static String formatStatus(ChatMessage message) {
        if (message.getStatus() == ChatMessage.MessageStatus.SENDING) return "...";
        if (message.getStatus() == ChatMessage.MessageStatus.FAILED) return "!";
        return message.isRead() || message.getDeliveryState() == DeliveryState.DELIVERED
                ? "\u2713\u2713"
                : "\u2713";
    }

    private static String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        return new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(timestamp.toDate());
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
