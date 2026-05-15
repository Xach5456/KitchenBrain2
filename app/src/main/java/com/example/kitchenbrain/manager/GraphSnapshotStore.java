package com.example.kitchenbrain.manager;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.kitchenbrain.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ✅ UI LAYER - Derived Snapshot Store
 * 
 * This class DERIVES UI state from SocialGraphState (domain layer).
 * It does NOT manage domain logic - only transforms it for UI rendering.
 * 
 * Instagram Pattern:
 * Domain State (SocialGraphState) → Derive → UI Snapshot (this class)
 * 
 * Responsibilities:
 * - Convert IDs to User objects
 * - Build renderable lists
 * - Provide LiveData for UI
 * - Cache user objects
 */
public class GraphSnapshotStore {
    private static final String TAG = "GraphSnapshotStore";
    private static GraphSnapshotStore instance;
    
    // ✅ Reference to domain state (source of truth)
    private final SocialGraphState domainState = SocialGraphState.getInstance();
    
    // ✅ User cache - maps ID → User object
    private final Map<String, User> userCache = new HashMap<>();
    
    // ✅ IMMUTABLE SNAPSHOT - Only modified through update()
    private GraphSnapshot currentSnapshot;
    
    // ✅ SINGLE LiveData source for all UI
    private final MutableLiveData<GraphSnapshot> snapshotLiveData = new MutableLiveData<>();
    
    private GraphSnapshotStore() {
        currentSnapshot = new GraphSnapshot.Builder().build();
        snapshotLiveData.postValue(currentSnapshot);
    }
    
    public static GraphSnapshotStore getInstance() {
        if (instance == null) {
            synchronized (GraphSnapshotStore.class) {
                if (instance == null) {
                    instance = new GraphSnapshotStore();
                }
            }
        }
        return instance;
    }
    
    /**
     * ✅ EXPLICIT REBUILD - Called by repository after domain mutation
     * 
     * This is the SINGLE trigger point for UI updates.
     * Flow: Repository → mutate domain → call this → UI updates
     */
    public void rebuildFromDomain() {
        Log.d(TAG, "🔄 [SNAPSHOT] Rebuilding from domain state...");
        
        // Get domain state
        Set<String> followingIds = domainState.getFollowingSet();
        Set<String> followerIds = domainState.getFollowersSet();
        
        // Convert IDs to Users (from cache)
        List<User> followingUsers = new ArrayList<>();
        for (String id : followingIds) {
            User user = userCache.get(id);
            if (user != null) {
                followingUsers.add(user);
            }
        }
        
        List<User> followerUsers = new ArrayList<>();
        for (String id : followerIds) {
            User user = userCache.get(id);
            if (user != null) {
                followerUsers.add(user);
            }
        }
        
        // Build new snapshot
        GraphSnapshot newSnapshot = new GraphSnapshot.Builder()
            .setFollowingIds(followingIds)
            .setFollowerIds(followerIds)
            .setFollowingUsers(followingUsers)
            .setFollowerUsers(followerUsers)
            .computeMutual()
            .build();
        
        currentSnapshot = newSnapshot;
        snapshotLiveData.postValue(newSnapshot);
        
        Log.d(TAG, "✅ [SNAPSHOT] Rebuilt - Following: " + followingUsers.size() + 
              ", Followers: " + followerUsers.size());
    }
    
    /**
     * ✅ PRELOAD USERS - Fill cache for better UI
     */
    public void preloadUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        List<String> missingIds = new ArrayList<>();
        for (String id : userIds) {
            if (!userCache.containsKey(id)) {
                missingIds.add(id);
            }
        }
        
        if (missingIds.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "📡 [SNAPSHOT] Preloading " + missingIds.size() + " users into cache");
        
        // Fetch from Firestore and update cache
        // ... (Firestore batch fetch logic)
        // After fetch completes:
        // userCache.put(userId, user);
        // rebuildSnapshot();
    }
    
    /**
     * ✅ Observe snapshot changes (UI layer uses this)
     */
    public LiveData<GraphSnapshot> observeSnapshot() {
        return snapshotLiveData;
    }
    
    /**
     * ✅ Get current snapshot (for non-reactive access)
     */
    public GraphSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }
    
    /**
     * ✅ Merge with server state (reconciliation)
     * 
     * Called by Firestore listener to sync domain with server
     * Updates domain state, then rebuilds snapshot
     */
    public void reconcileWithServer(Set<String> serverFollowing, Set<String> serverFollowers) {
        Log.d(TAG, "🔄 [SNAPSHOT] Reconciling with server...");
        
        // Update domain state with server data
        domainState.reconcileWithServer(serverFollowing, serverFollowers);
        
        // Rebuild snapshot from updated domain
        rebuildFromDomain();
    }
    
    /**
     * ✅ Functional interface for mutations
     */
    @FunctionalInterface
    public interface SnapshotMutator {
        void apply(GraphSnapshot.Builder builder);
    }
    
    /**
     * ✅ IMMUTABLE SNAPSHOT
     * 
     * Represents complete state at a point in time
     * Never mutated - always rebuilt
     */
    public static class GraphSnapshot {
        private final Set<String> followingIds;
        private final Set<String> followerIds;
        private final List<User> followingUsers;
        private final List<User> followerUsers;
        private final List<User> mutualUsers;
        
        private GraphSnapshot(Builder builder) {
            this.followingIds = new HashSet<>(builder.followingIds);
            this.followerIds = new HashSet<>(builder.followerIds);
            this.followingUsers = new ArrayList<>(builder.followingUsers);
            this.followerUsers = new ArrayList<>(builder.followerUsers);
            this.mutualUsers = new ArrayList<>(builder.mutualUsers);
        }
        
        public Set<String> getFollowingIds() { return followingIds; }
        public Set<String> getFollowerIds() { return followerIds; }
        public List<User> getFollowingUsers() { return followingUsers; }
        public List<User> getFollowerUsers() { return followerUsers; }
        public List<User> getMutualUsers() { return mutualUsers; }
        
        public Builder toBuilder() {
            return new Builder(this);
        }
        
        /**
         * Builder for creating/modifying snapshots
         */
        public static class Builder {
            private final Set<String> followingIds = new HashSet<>();
            private final Set<String> followerIds = new HashSet<>();
            private final List<User> followingUsers = new ArrayList<>();
            private final List<User> followerUsers = new ArrayList<>();
            private final List<User> mutualUsers = new ArrayList<>();
            
            public Builder() {}
            
            public Builder(GraphSnapshot snapshot) {
                this.followingIds.addAll(snapshot.followingIds);
                this.followerIds.addAll(snapshot.followerIds);
                this.followingUsers.addAll(snapshot.followingUsers);
                this.followerUsers.addAll(snapshot.followerUsers);
                this.mutualUsers.addAll(snapshot.mutualUsers);
            }
            
            // ✅ ID manipulation
            public Builder addFollowing(String userId) {
                followingIds.add(userId);
                return this;
            }
            
            public Builder removeFollowing(String userId) {
                followingIds.remove(userId);
                return this;
            }
            
            public Builder addFollower(String userId) {
                followerIds.add(userId);
                return this;
            }
            
            public Builder removeFollower(String userId) {
                followerIds.remove(userId);
                return this;
            }
            
            public Builder setFollowingIds(Set<String> ids) {
                followingIds.clear();
                followingIds.addAll(ids);
                return this;
            }
            
            public Builder setFollowerIds(Set<String> ids) {
                followerIds.clear();
                followerIds.addAll(ids);
                return this;
            }
            
            // ✅ User list manipulation
            public Builder setFollowingUsers(List<User> users) {
                followingUsers.clear();
                followingUsers.addAll(users);
                return this;
            }
            
            public Builder setFollowerUsers(List<User> users) {
                followerUsers.clear();
                followerUsers.addAll(users);
                return this;
            }
            
            // ✅ Compute mutual from IDs
            public Builder computeMutual() {
                mutualUsers.clear();
                for (User user : followingUsers) {
                    if (followerIds.contains(user.getUserId())) {
                        mutualUsers.add(user);
                    }
                }
                return this;
            }
            
            public GraphSnapshot build() {
                return new GraphSnapshot(this);
            }
            
            public Set<String> getFollowingIds() { return followingIds; }
            public Set<String> getFollowerIds() { return followerIds; }
        }
    }
}
