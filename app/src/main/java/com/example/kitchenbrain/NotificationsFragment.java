package com.example.kitchenbrain;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.adapter.NotificationAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Premium Notifications Fragment - Material 3 Expressive Design
 * Displays follow notifications with mutual follow detection and chat access
 */
public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    // UI Components
    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewNotifications;
    private LinearLayout layoutEmpty;
    private ProgressBar progressLoading;
    private FloatingActionButton fabRefresh;

    // Data
    private NotificationAdapter notificationAdapter;
    private List<Notification> notifications;

    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;
    private com.google.firebase.firestore.ListenerRegistration notificationListener;
    
    // Threading
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);
    private volatile boolean isDestroyed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "🎬 onViewCreated called - NotificationsFragment is now visible");

        initViews(view);
        setupToolbar();
        setupRecyclerView();
        setupRefresh();
        loadNotifications();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressLoading = view.findViewById(R.id.progressLoading);
        fabRefresh = view.findViewById(R.id.fabRefresh);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            // Use FragmentManager to navigate back properly
            // This ensures we pop the fragment back stack instead of exiting the Activity
            if (getActivity() != null) {
                v.post(() -> {
                    try {
                        if (getActivity() != null && isAdded()) {
                            int backStackCount = getParentFragmentManager().getBackStackEntryCount();
                            Log.d(TAG, "🔙 Back button clicked - back stack entries: " + backStackCount);
                            
                            // Check if there are fragments in the back stack to pop
                            if (backStackCount > 0) {
                                // Pop the back stack to return to previous fragment
                                getParentFragmentManager().popBackStack();
                                Log.d(TAG, "✅ Popped back stack - returning to previous fragment");
                                Log.d(TAG, "📊 Remaining back stack entries: " + getParentFragmentManager().getBackStackEntryCount());
                            } else {
                                // No back stack - use Activity's back navigation
                                // This will go to the previous screen or exit app
                                Log.d(TAG, "⚠️ No back stack entries - using Activity back navigation");
                                getActivity().getOnBackPressedDispatcher().onBackPressed();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during back navigation", e);
                        // Fallback: use Activity back navigation
                        if (getActivity() != null && !getActivity().isFinishing()) {
                            getActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
            }
        });
    }

    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        // Pass null click listener - notifications are read-only, no click actions
        notificationAdapter = new NotificationAdapter(getContext(), notifications, null);
        
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewNotifications.setAdapter(notificationAdapter);
        recyclerViewNotifications.setHasFixedSize(true);
    }

    private void setupRefresh() {
        fabRefresh.setOnClickListener(v -> {
            Log.d(TAG, "Refreshing notifications...");
            loadNotifications();
        });
    }

    private void loadNotifications() {
        progressLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        recyclerViewNotifications.setVisibility(View.GONE);

        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null 
            ? auth.getCurrentUser().getUid() 
            : null;

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            progressLoading.setVisibility(View.GONE);
            return;
        }

        db = FirebaseFirestore.getInstance();

        // Remove existing listener if any to prevent duplicates
        if (notificationListener != null) {
            notificationListener.remove();
        }

        // ✅ IMMEDIATE FIX: Use fallback query (works WITHOUT creating an index)
        // Notifications will work immediately, just without server-side sorting
        Log.d(TAG, "Loading notifications (no index required)");
        loadNotificationsFallback();
    }
    
    /**
     * Fallback query - works WITHOUT composite index
     * Sorts client-side instead of server-side
     * Perfect for immediate use while index creation is optional
     */
    private void loadNotificationsFallback() {
        if (isDestroyed || currentUserId == null) return;
        
        // Remove previous listener
        if (notificationListener != null) {
            notificationListener.remove();
        }
        
        Log.d(TAG, "Using fallback query (client-side sorting)");
        
        // Simple query WITHOUT orderBy - NO index required!
        notificationListener = db.collection("notifications")
            .whereEqualTo("receiverId", currentUserId)
            .limit(50)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Notification query failed", error);
                    if (!isDestroyed && getContext() != null) {
                        Toast.makeText(getContext(), 
                            "Unable to load notifications. Please try again later.",
                            Toast.LENGTH_LONG).show();
                    }
                    progressLoading.setVisibility(View.GONE);
                    showEmptyState();
                    return;
                }
                
                if (snapshot == null || snapshot.isEmpty()) {
                    showEmptyState();
                    return;
                }
                
                List<Notification> newNotifications = new ArrayList<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Notification notification = doc.toObject(Notification.class);
                    if (notification != null) {
                        notification.setId(doc.getId());
                        newNotifications.add(notification);
                    }
                }
                
                // Sort client-side by createdAt (instead of server-side)
                newNotifications.sort((n1, n2) -> Long.compare(n2.getCreatedAt(), n1.getCreatedAt()));
                
                updateNotifications(newNotifications);
            });
    }

    private void updateNotifications(List<Notification> newNotifications) {
        if (isDestroyed) {
            Log.w(TAG, "Fragment destroyed, skipping notification update");
            return;
        }
        
        // Move DiffUtil calculation to background thread to prevent ANR
        backgroundExecutor.execute(() -> {
            try {
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NotificationDiffCallback(notifications, newNotifications));
                
                mainHandler.post(() -> {
                    if (isDestroyed) {
                        Log.w(TAG, "Fragment destroyed during diff dispatch, skipping UI update");
                        return;
                    }
                    
                    notifications.clear();
                    notifications.addAll(newNotifications);
                    diffResult.dispatchUpdatesTo(notificationAdapter);
                    
                    progressLoading.setVisibility(View.GONE);
                    
                    if (notifications.isEmpty()) {
                        showEmptyState();
                    } else {
                        recyclerViewNotifications.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error calculating diff", e);
                mainHandler.post(() -> {
                    if (!isDestroyed) {
                        progressLoading.setVisibility(View.GONE);
                        showEmptyState();
                    }
                });
            }
        });
    }

    private void showEmptyState() {
        progressLoading.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    /**
     * DiffUtil callback for efficient RecyclerView updates
     */
    private static class NotificationDiffCallback extends DiffUtil.Callback {
        private final List<Notification> oldList;
        private final List<Notification> newList;

        public NotificationDiffCallback(List<Notification> oldList, List<Notification> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Notification old = oldList.get(oldItemPosition);
            Notification newer = newList.get(newItemPosition);
            return old.getMessage().equals(newer.getMessage()) &&
                   old.isRead() == newer.isRead();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        Log.d(TAG, "🧹 onDestroyView() called - cleaning up all listeners");
        
        // Set destroyed flag to prevent callbacks after this point
        isDestroyed = true;
        
        // Remove ALL Firebase listeners immediately to prevent memory leaks and query interference
        removeAllFirebaseListeners();
        
        // Clear RecyclerView to release ViewHolders and prevent memory leaks
        if (recyclerViewNotifications != null) {
            recyclerViewNotifications.setAdapter(null);
            recyclerViewNotifications.removeAllViews();
        }
        
        // Clear adapter references
        if (notificationAdapter != null) {
            notificationAdapter = null;
        }
        
        Log.d(TAG, "✅ onDestroyView() cleanup complete");
        
        // Clear data lists
        if (notifications != null) {
            notifications.clear();
            notifications = null;
        }
        
        // Shutdown executor gracefully with timeout
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            Log.d(TAG, "Background executor shutdown");
        }
        
        // Cancel pending main thread callbacks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        Log.d(TAG, "NotificationsFragment destroyed - all resources cleaned up");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        
        Log.d(TAG, "⚠️ onStop() called - ensuring listeners are removed");
        
        // Extra safety: remove listeners again if still active
        // This catches cases where onDestroyView() was skipped or interrupted
        if (notificationListener != null) {
            Log.w(TAG, "⚠️ Listener still active in onStop(), removing now");
            notificationListener.remove();
            notificationListener = null;
        }
    }
    
    /**
     * Remove all Firebase listeners to prevent memory leaks and query interference
     * Called from onDestroyView() and as safety cleanup in onStop()
     */
    private void removeAllFirebaseListeners() {
        Log.d(TAG, "🗑️ Removing all Firebase listeners");
        
        if (notificationListener != null) {
            try {
                notificationListener.remove();
                Log.d(TAG, "✅ Notification listener removed successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error removing notification listener", e);
            }
            notificationListener = null;
        }
        
        // If you have other listeners (user search, etc.), remove them too
        // if (userSearchListener != null) {
        //     userSearchListener.remove();
        //     userSearchListener = null;
        // }
        
        Log.d(TAG, "✅ All listeners removed");
    }
}
