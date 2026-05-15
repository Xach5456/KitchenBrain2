package com.example.kitchenbrain.model;

import com.google.firebase.Timestamp;
import com.example.kitchenbrain.model.DeliveryState;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ChatMessage {
    public enum MessageStatus {
        SENDING,
        SENT,
        FAILED
    }

    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private Timestamp timestamp;
    private MessageStatus status;
    private boolean edited;
    private boolean deleted;
    private boolean isRead;
    private long readAt;
    private DeliveryState deliveryState;
    private String messageType;
    private String mediaUrl;
    private String thumbnailUrl;
    private long duration;
    private boolean isPinned;
    private String editedText;
    private Timestamp editedAt;
    private String senderName;
    private String messageStatus; // 🔥 FIX: Add missing field for Firestore
    private java.util.Map<String, String> reactions;
    
    // 🔥 FIX Firestore warnings
    private boolean editedComplex;
    private int reactionCount;

    public ChatMessage() {
        this.status = MessageStatus.SENDING;
        this.edited = false;
        this.deleted = false;
        this.messageType = "text";
        this.isPinned = false;
        this.reactions = new java.util.HashMap<>();
    }

    public ChatMessage(String messageId, String senderId, String receiverId, String text, 
                       Timestamp timestamp, MessageStatus status) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.status = status;
        this.edited = false;
        this.deleted = false;
        this.isRead = false;  // Default to unread
        this.readAt = 0;
        this.deliveryState = DeliveryState.SENT;  // Default to sent
        this.messageType = "text";
        this.isPinned = false;
        this.reactions = new java.util.HashMap<>();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public String getEditedText() { return editedText; }
    public void setEditedText(String editedText) { this.editedText = editedText; }

    public Timestamp getEditedAt() { return editedAt; }
    public void setEditedAt(Timestamp editedAt) { this.editedAt = editedAt; }

    public java.util.Map<String, String> getReactions() { return reactions; }
    public void setReactions(java.util.Map<String, String> reactions) { this.reactions = reactions; }

    // 🔥 FIX: Added field and proper getter/setter for Firestore
    public boolean isEditedComplex() {
        return editedComplex;
    }
    public void setEditedComplex(boolean editedComplex) {
        this.editedComplex = editedComplex;
    }

    // 🔥 FIX: Added field and proper getter/setter for Firestore
    public int getReactionCount() {
        return reactionCount;
    }
    public void setReactionCount(int reactionCount) {
        this.reactionCount = reactionCount;
    }

    public boolean hasUserReacted(String userId) {
        return reactions != null && reactions.containsKey(userId);
    }

    // 🔥 SENDER NAME GETTERS/SETTERS
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    // 🔥 MESSAGE STATUS GETTERS/SETTERS (Firestore compatibility)
    public String getMessageStatus() { return messageStatus; }
    public void setMessageStatus(String messageStatus) { this.messageStatus = messageStatus; }

    // 🔥 READ RECEIPT GETTERS/SETTERS
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public long getReadAt() { return readAt; }
    public void setReadAt(long readAt) { this.readAt = readAt; }
    
    // 🔥 DELIVERY STATE GETTERS/SETTERS
    public DeliveryState getDeliveryState() { return deliveryState; }
    public void setDeliveryState(DeliveryState deliveryState) { this.deliveryState = deliveryState; }
}
