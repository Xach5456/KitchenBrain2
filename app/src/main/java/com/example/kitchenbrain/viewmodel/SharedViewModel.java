package com.example.kitchenbrain.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.kitchenbrain.Friend;
import com.example.kitchenbrain.FriendManager;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared ViewModel for managing friend state across fragments
 * Use activity-scoped ViewModel to share data between SearchFragment and NewFriendsFragment
 */
public class SharedViewModel extends AndroidViewModel {
    
    private static final String TAG = "SharedViewModel";
    
    // Friend data
    private final MutableLiveData<List<Friend>> friendsList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Follow status tracking
    private final MutableLiveData<User> latestFollowedUser = new MutableLiveData<>();
    
    // Event for opening chat after mutual follow (using LiveData for Java compatibility)
    private final MutableLiveData<String> openChatUserId = new MutableLiveData<>();
    
    // Firebase & Manager
    private final FirebaseFirestore db;
    private final String currentUserId;
    private FriendManager friendManager;
    
    public SharedViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
            : null;
        
        if (currentUserId != null) {
            friendManager = new FriendManager(db, currentUserId);
        }
    }
    
    /**
     * Get observable friends list
     */
    public LiveData<List<Friend>> getFriendsList() {
        return friendsList;
    }
    
    /**
     * Get loading state
     */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    
    /**
     * Get error messages
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Get latest followed user (for triggering UI updates)
     */
    public LiveData<User> getLatestFollowedUser() {
        return latestFollowedUser;
    }
    
    /**
     * Get LiveData for opening chat after mutual follow
     */
    public LiveData<String> getOpenChatUserId() {
        return openChatUserId;
    }
    
    /**
     * Emit event to open chat with specific user
     */
    public void emitOpenChatEvent(String otherUserId) {
        openChatUserId.setValue(otherUserId);
    }
    
    /**
     * Load friends from FollowGraphRepository (CORRECT source)
     */
    public void loadFriends() {
        isLoading.setValue(true);
        
        // ✅ FIXED: Use FollowGraphRepository instead of FriendManager
        // This reads from following/followers collections (correct architecture)
        FollowGraphRepository followGraph = FollowGraphRepository.getInstance();
        List<User> mutualUsers = followGraph.getMutualUsers();
        
        Log.d(TAG, "Loaded " + mutualUsers.size() + " mutual friends from graph");
        
        // Convert User list to Friend list for backward compatibility
        // TODO: Migrate to use User objects directly
        List<Friend> friendsList = new ArrayList<>();
        for (User user : mutualUsers) {
            // Create Friend object from User (temporary adapter)
            Friend friend = new Friend(
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                user.getUserId(),
                user.getUsername(),
                user.getAvatarUrl()
            );
            friend.setOnline(user.getOnline());
            friendsList.add(friend);
        }
        
        isLoading.setValue(false);
        this.friendsList.setValue(friendsList);
    }
    
    /**
     * Refresh friends list (call after follow/unfollow actions)
     */
    public void refreshFriends() {
        loadFriends();
    }
    
    /**
     * Notify that a follow action occurred
     * This triggers other fragments to refresh their UI
     */
    public void notifyFollowAction(User user) {
        latestFollowedUser.setValue(user);
        
        // Auto-refresh friends list after a short delay
        // to allow Firestore replication
        new android.os.Handler(android.os.Looper.getMainLooper())
            .postDelayed(this::refreshFriends, 500);
    }
    
    /**
     * Clear the latest followed user event
     * Call this after consuming the event to prevent re-triggering
     */
    public void clearFollowEvent() {
        latestFollowedUser.setValue(null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (friendManager != null) {
            friendManager.cleanup();
        }
    }
}
