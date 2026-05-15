package com.example.kitchenbrain.manager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.kitchenbrain.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ✅ HIGHLY OPTIMIZED Follow Graph Repository
 * 
 * Clean architecture with proper debouncing, thread safety, and zero main thread heavy work.
 * Manages the social graph (following/followers) and user profile cache.
 */
public class FollowGraphRepository {
    private static final String TAG = "FollowGraphRepository";
    private static final int BATCH_SIZE = 25;
    private static final long DEBOUNCE_DELAY_MS = 300; 

    private final FirebaseFirestore db;
    private final Map<String, User> userCache = new ConcurrentHashMap<>();
    private final Set<String> followingSet = ConcurrentHashMap.newKeySet();
    private final Set<String> followersSet = ConcurrentHashMap.newKeySet();

    private final MutableLiveData<List<User>> followingLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<User>> followersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<User>> mutualLiveData = new MutableLiveData<>(new ArrayList<>());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isRebuildScheduled = new AtomicBoolean(false);

    private ListenerRegistration followingListener;
    private ListenerRegistration followersListener;
    private String currentInitializedUserId;
    private static FollowGraphRepository instance;

    public static FollowGraphRepository getInstance() {
        if (instance == null) {
            synchronized (FollowGraphRepository.class) {
                if (instance == null) {
                    instance = new FollowGraphRepository();
                }
            }
        }
        return instance;
    }

    private FollowGraphRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * 🚀 INITIALIZE graph listeners for a specific user
     */
    public void initialize(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ [GRAPH] INIT FAILED: userId is NULL or EMPTY");
            return;
        }

        if (userId.equals(currentInitializedUserId)) {
            Log.d(TAG, "ℹ️ [GRAPH] Already initialized for user: " + userId);
            return;
        }

        cleanup();
        this.currentInitializedUserId = userId;
        Log.d(TAG, "🚀 [GRAPH] Initializing listeners for: " + userId);

        // Listen for Following
        followingListener = db.collection("following").document(userId).collection("userFollowing")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Following listener error", e);
                        return;
                    }
                    if (snap != null) {
                        Log.d(TAG, "📥 [FOLLOWING] Updated IDs: " + snap.size());
                        followingSet.clear();
                        for (QueryDocumentSnapshot doc : snap) {
                            followingSet.add(doc.getId());
                        }
                        scheduleRebuild();
                    }
                });

        // Listen for Followers
        followersListener = db.collection("followers").document(userId).collection("userFollowers")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Followers listener error", e);
                        return;
                    }
                    if (snap != null) {
                        Log.d(TAG, "📥 [FOLLOWERS] Updated IDs: " + snap.size());
                        followersSet.clear();
                        for (QueryDocumentSnapshot doc : snap) {
                            followersSet.add(doc.getId());
                        }
                        scheduleRebuild();
                    }
                });
    }

    public void refresh() {
        scheduleRebuild();
    }

    public void updateFollowGraph(String userId, boolean isFollowing) {
        if (isFollowing) {
            followingSet.add(userId);
        } else {
            followingSet.remove(userId);
        }
        scheduleRebuild();
    }

    private void scheduleRebuild() {
        if (isRebuildScheduled.compareAndSet(false, true)) {
            mainHandler.postDelayed(() -> {
                isRebuildScheduled.set(false);
                executor.execute(this::rebuildSnapshot);
            }, DEBOUNCE_DELAY_MS);
        }
    }

    private void rebuildSnapshot() {
        try {
            Log.d(TAG, "🔄 [GRAPH] Rebuilding snapshots...");
            
            Set<String> allRequiredIds = new HashSet<>(followingSet);
            allRequiredIds.addAll(followersSet);

            List<String> missingIds = new ArrayList<>();
            for (String id : allRequiredIds) {
                if (!userCache.containsKey(id)) {
                    missingIds.add(id);
                }
            }

            if (!missingIds.isEmpty()) {
                Log.d(TAG, "📦 [GRAPH] Fetching " + missingIds.size() + " missing user profiles");
                fetchUsersParallel(missingIds);
            }

            final List<User> followingUsers = mapIdsToUsers(followingSet);
            final List<User> followersUsers = mapIdsToUsers(followersSet);
            
            Set<String> mutualIds = new HashSet<>(followingSet);
            mutualIds.retainAll(followersSet);
            final List<User> mutualUsers = mapIdsToUsers(mutualIds);

            mainHandler.post(() -> {
                followingLiveData.setValue(followingUsers);
                followersLiveData.setValue(followersUsers);
                mutualLiveData.setValue(mutualUsers);
                Log.d(TAG, "✅ [GRAPH] Snapshot updated: Following=" + followingUsers.size() + 
                      ", Followers=" + followersUsers.size() + ", Mutual=" + mutualUsers.size());
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ [GRAPH] Rebuild failed", e);
        }
    }

    private void fetchUsersParallel(List<String> ids) {
        // Fetch in batches of 10 to be safe with whereIn or just parallel gets
        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, ids.size());
            List<String> batchIds = ids.subList(i, end);
            
            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (String id : batchIds) {
                tasks.add(db.collection("users").document(id).get());
            }
            
            try {
                Tasks.await(Tasks.whenAllComplete(tasks), 10, TimeUnit.SECONDS);
                for (Task<DocumentSnapshot> task : tasks) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setId(doc.getId());
                                userCache.put(doc.getId(), user);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in batch fetch", e);
            }
        }
    }

    private List<User> mapIdsToUsers(Collection<String> ids) {
        List<User> users = new ArrayList<>();
        for (String id : ids) {
            User user = userCache.get(id);
            if (user != null) {
                users.add(user);
            } else {
                User placeholder = new User();
                placeholder.setId(id);
                placeholder.setUserId(id);
                placeholder.setUsername("User " + (id.length() > 4 ? id.substring(0, 4) : id));
                users.add(placeholder);
            }
        }
        return users;
    }

    public void cleanup() {
        if (followingListener != null) followingListener.remove();
        if (followersListener != null) followersListener.remove();
        followingSet.clear();
        followersSet.clear();
        userCache.clear();
        currentInitializedUserId = null;
        isRebuildScheduled.set(false);
        
        followingLiveData.postValue(new ArrayList<>());
        followersLiveData.postValue(new ArrayList<>());
        mutualLiveData.postValue(new ArrayList<>());
    }

    public boolean isFollowing(String userId) {
        return userId != null && followingSet.contains(userId);
    }

    public boolean isFollower(String userId) {
        return userId != null && followersSet.contains(userId);
    }

    public boolean isMutual(String userId) {
        return isFollowing(userId) && isFollower(userId);
    }

    public List<User> getFollowingUsers() { return followingLiveData.getValue() != null ? followingLiveData.getValue() : new ArrayList<>(); }
    public List<User> getFollowersUsers() { return followersLiveData.getValue() != null ? followersLiveData.getValue() : new ArrayList<>(); }
    public List<User> getMutualUsers() { return mutualLiveData.getValue() != null ? mutualLiveData.getValue() : new ArrayList<>(); }

    public LiveData<List<User>> getFollowingLiveData() { return followingLiveData; }
    public LiveData<List<User>> getFollowersLiveData() { return followersLiveData; }
    public LiveData<List<User>> getMutualLiveData() { return mutualLiveData; }
}
