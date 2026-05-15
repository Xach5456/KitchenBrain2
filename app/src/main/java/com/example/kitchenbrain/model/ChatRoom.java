package com.example.kitchenbrain.model;

import com.example.kitchenbrain.provider.ChatIdProvider;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ UNIVERSAL CHAT ROOM MODEL
 * Supports multiple legacy field names and structures for maximum compatibility.
 * Updated to handle mixed types (Long/Timestamp) for time fields.
 */
@IgnoreExtraProperties
public class ChatRoom {
    private String roomId;
    private String user1Id;
    private String user2Id;
    private String userId1;
    private String userId2;
    private List<String> participants;
    private List<String> participantIds;
    private String lastMessageText;
    private String lastMessage;
    private long lastMessageAt;
    private Object lastMessageTime; // Changed to Object to handle both Long and Timestamp
    private String lastMessageSenderId;
    private String lastMessageSender;
    private int unreadCount;
    private boolean isActive;

    public ChatRoom() {
        this.participants = new ArrayList<>();
        this.participantIds = new ArrayList<>();
        this.isActive = true;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getUser1Id() { return user1Id != null ? user1Id : userId1; }
    public void setUser1Id(String user1Id) { this.user1Id = user1Id; }

    public String getUser2Id() { return user2Id != null ? user2Id : userId2; }
    public void setUser2Id(String user2Id) { this.user2Id = user2Id; }

    public String getUserId1() { return userId1; }
    public void setUserId1(String userId1) { this.userId1 = userId1; }

    public String getUserId2() { return userId2; }
    public void setUserId2(String userId2) { this.userId2 = userId2; }

    public List<String> getParticipants() { 
        if (participants != null && !participants.isEmpty()) return participants;
        return participantIds;
    }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }

    public String getLastMessageText() { 
        if (lastMessageText != null && !lastMessageText.isEmpty()) return lastMessageText;
        if (lastMessage != null && !lastMessage.isEmpty()) return lastMessage;
        return "Start a conversation!";
    }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageAt() { 
        if (lastMessageAt != 0) return lastMessageAt;
        
        if (lastMessageTime instanceof Timestamp) {
            return ((Timestamp) lastMessageTime).toDate().getTime();
        } else if (lastMessageTime instanceof Long) {
            return (Long) lastMessageTime;
        } else if (lastMessageTime instanceof Double) {
            return ((Double) lastMessageTime).longValue();
        }
        
        return 0;
    }
    public void setLastMessageAt(long lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Object getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Object lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getLastMessageSenderId() { 
        if (lastMessageSenderId != null && !lastMessageSenderId.isEmpty()) return lastMessageSenderId;
        return lastMessageSender;
    }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public String getLastMessageSender() { return lastMessageSender; }
    public void setLastMessageSender(String lastMessageSender) { this.lastMessageSender = lastMessageSender; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public String getOtherUserId(String currentUserId) {
        if (currentUserId == null) return getUser1Id() != null ? getUser1Id() : getUser2Id();
        
        String u1 = getUser1Id();
        String u2 = getUser2Id();
        
        if (currentUserId.equals(u1)) return u2;
        if (currentUserId.equals(u2)) return u1;
        
        List<String> p = getParticipants();
        if (p != null) {
            for (String id : p) {
                if (id != null && !id.equals(currentUserId)) return id;
            }
        }

        // Final fallback: Extract from RoomId if it follows user1_user2 pattern
        if (roomId != null && ChatIdProvider.isValidChatId(roomId)) {
            String[] ids = ChatIdProvider.extractUserIds(roomId);
            if (ids != null) {
                if (currentUserId.equals(ids[0])) return ids[1];
                if (currentUserId.equals(ids[1])) return ids[0];
            }
        }

        return null;
    }
}
