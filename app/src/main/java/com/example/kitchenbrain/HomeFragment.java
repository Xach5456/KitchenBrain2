package com.example.kitchenbrain;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.kitchenbrain.adapter.FeedPostAdapter;
import com.example.kitchenbrain.model.FeedItem;
import com.example.kitchenbrain.social.LikeManager;
import com.example.kitchenbrain.social.CommentsManager;
import com.example.kitchenbrain.ui.CommentsBottomSheet;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Home Fragment - Instagram-style Food Feed (Powered by Spoonacular Recipes)
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private RecyclerView recyclerViewFeed;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private LinearLayout layoutEmpty;

    private HomeFeedViewModel viewModel;
    private FeedPostAdapter feedAdapter;
    
    // Instagram-style social features
    private LikeManager likeManager;
    private CommentsManager commentsManager;
    private FirebaseAuth auth;
    private String currentUserId;
    
    // Auto-refresh
    private Handler autoRefreshHandler;
    private static final int AUTO_REFRESH_INTERVAL = 60000; // 1 minute
    
    // 🔥 CRITICAL: Debounce protection for like clicks
    private boolean isLiking = false;
    
    // 🔴 CRITICAL: API error handling
    private boolean isApiError = false;
    private int lastErrorCode = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "🚀 HomeFragment - Instagram-style Edition");

        initSocialFeatures();
        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupSwipeRefresh();
        setupAutoRefresh();
        observeViewModel();
        
        // Initial load
        viewModel.loadFeed();
    }

    private void initSocialFeatures() {
        Log.d(TAG, "🔥 Initializing Instagram-style social features");
        
        // Firebase Auth
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "✅ User authenticated: " + currentUserId);
        } else {
            Log.w(TAG, "⚠️ User not authenticated");
            currentUserId = "anonymous";
        }
        
        // Social managers
        likeManager = new LikeManager();
        commentsManager = new CommentsManager();
        
        // Auto-refresh handler
        autoRefreshHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "✅ Social features initialized");
    }

    private void initViews(View view) {
        recyclerViewFeed = view.findViewById(R.id.recyclerViewFeed);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutError = view.findViewById(R.id.layoutError);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeFeedViewModel.class);
    }

    private void setupRecyclerView() {
        feedAdapter = new FeedPostAdapter(getContext());
        feedAdapter.setOnFeedInteractionListener(new FeedPostAdapter.OnFeedInteractionListener() {
            @Override
            public void onLikeClick(FeedItem item, int position) {
                handleLikeClick(item, position);
            }

            @Override
            public void onCommentClick(FeedItem item, int position) {
                handleCommentClick(item, position);
            }

            @Override
            public void onSendClick(FeedItem item, int position) {
                // 🔥 FIXED: Now opens dedicated ShareNewsFragment for mutual followers
                handleSendToChat(item);
            }

            @Override
            public void onSaveClick(FeedItem item, int position) {
                viewModel.toggleSave(item.getStableId());
                feedAdapter.updateItem(item);
            }

            @Override
            public void onShareClick(FeedItem item, int position) {
                shareRecipe(item.getRecipe());
            }

            @Override
            public void onArticleClick(FeedItem item, int position) {
                openRecipeDetail(item.getRecipe());
            }
        });
        
        recyclerViewFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewFeed.setAdapter(feedAdapter);
        recyclerViewFeed.setHasFixedSize(true);
    }

    /**
     * 🔥 Opens the dedicated ShareNewsFragment to pick a mutual follower to send the news to.
     */
    private void handleSendToChat(FeedItem item) {
        Log.d(TAG, "📤 Opening ShareNewsFragment for: " + item.getRecipe().getName());
        
        // Create the dedicated fragment for sharing with mutual followers
        ShareNewsFragment shareFragment = ShareNewsFragment.newInstance(
            item.getRecipe().getName(), 
            item.getRecipe().getVideoUrl()
        );
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToFragment(shareFragment, true);
        }
    }

    private void setupSwipeRefresh() {
        Log.d(TAG, "🔄 Setting up SwipeRefreshLayout");
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "🔄 Swipe refresh triggered");
            viewModel.refreshFeed();
        });
        
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
        
        Log.d(TAG, "✅ SwipeRefreshLayout setup complete");
    }
    
    private void setupAutoRefresh() {
        Log.d(TAG, "⏰ Setting up auto-refresh every " + AUTO_REFRESH_INTERVAL + "ms");
        
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // 🔴 CRITICAL: Stop auto-refresh on API error
                if (isApiError && lastErrorCode == 402) {
                    Log.d(TAG, "🚫 Auto-refresh stopped due to API 402 error");
                    return;
                }
                
                if (isAdded() && !isDetached()) {
                    Log.d(TAG, "⏰ Auto-refresh triggered");
                    viewModel.refreshFeed();
                    autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
                }
            }
        };
        
        // Start auto-refresh after initial delay
        autoRefreshHandler.postDelayed(refreshRunnable, AUTO_REFRESH_INTERVAL);
        
        Log.d(TAG, "✅ Auto-refresh setup complete");
    }

    private void observeViewModel() {
        viewModel.getSingleUiState().observe(getViewLifecycleOwner(), this::render);
            
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "🔍 [ERROR_LOG] " + error);
            }
        });
    }
    
    private void render(HomeUiState state) {
        Log.d(TAG, "🎯 [RENDER] State: " + state.getStatus());
            
        switch (state.getStatus()) {
            case LOADING:
                showLoading();
                break;
                    
            case SUCCESS:
                if (state.getData() != null) {
                    feedAdapter.setFeedItems(state.getData());
                }
                showContent();
                
                // 🔥 Notify user if showing cached data due to API limits
                if (state.isFromCache() && getContext() != null) {
                    Toast.makeText(getContext(), "Daily API quota reached. Showing cached recipes.", Toast.LENGTH_LONG).show();
                }

                // 🔴 CRITICAL: Reset API error state on success
                if (isApiError) {
                    isApiError = false;
                    lastErrorCode = 0;
                    Log.d(TAG, "✅ API error state reset");
                }
                break;
                    
            case EMPTY:
                showEmpty();
                break;
                    
            case ERROR:
                // 🔴 CRITICAL: Check for 402 error
                String errorMessage = state.getErrorMessage() != null ? state.getErrorMessage() : "Unknown Error";
                if (errorMessage.contains("402")) {
                    isApiError = true;
                    lastErrorCode = 402;
                    Log.d(TAG, "🚫 API 402 error detected, stopping auto-refresh");
                }
                showError(errorMessage);
                break;
        }
    }

    private void showLoading() {
        if (layoutLoading != null) layoutLoading.setVisibility(View.VISIBLE);
        if (layoutError != null) layoutError.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (recyclerViewFeed != null) recyclerViewFeed.setVisibility(View.GONE);
    }

    private void showContent() {
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (recyclerViewFeed != null) recyclerViewFeed.setVisibility(View.VISIBLE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private void showEmpty() {
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
        if (recyclerViewFeed != null) recyclerViewFeed.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.VISIBLE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (recyclerViewFeed != null) recyclerViewFeed.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        if (message != null && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareRecipe(com.example.kitchenbrain.Recipe recipe) {
        if (recipe != null) {
            String shareText = "Check out this recipe: " + recipe.getName() + "\n" + recipe.getVideoUrl();
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share via"));
        }
    }

    private void openRecipeDetail(com.example.kitchenbrain.Recipe recipe) {
        if (recipe != null) {
            RecipeDetailFragment fragment = RecipeDetailFragment.newInstance(recipe);
            getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        }
    }

    private void handleLikeClick(FeedItem item, int position) {
        Log.d(TAG, "❤️ Like clicked: position=" + position);
        
        // 🔥 CRITICAL: Debounce protection
        if (isLiking) {
            Log.d(TAG, "⏳ Like already in progress, ignoring click");
            return;
        }
        
        String postId = generatePostId(item);
        isLiking = true;
        
        likeManager.toggleLike(postId, currentUserId, new LikeManager.LikeCallback() {
            @Override
            public void onSuccess(LikeManager.LikeResult result) {
                Log.d(TAG, "✅ Like toggle success: " + result.toString());
                // Update UI through ViewModel
                viewModel.toggleLike(item.getStableId());
                isLiking = false; // Reset debounce
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Like toggle failed: " + error);
                isLiking = false; // Reset debounce on error
                Toast.makeText(getContext(), "Like failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCommentClick(FeedItem item, int position) {
        Log.d(TAG, "💬 Comment clicked: position=" + position);
        
        String postId = generatePostId(item);
        String postTitle = item.getRecipe() != null ? item.getRecipe().getName() : "Recipe";
        
        // Open Instagram-style comments bottom sheet
        CommentsBottomSheet commentsSheet = CommentsBottomSheet.newInstance(postId, postTitle);
        commentsSheet.show(getChildFragmentManager(), "CommentsBottomSheet");
    }

    private String generatePostId(FeedItem item) {
        if (item != null && item.getRecipe() != null) {
            return "recipe_" + item.getRecipe().getId();
        }
        return "unknown_post";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (feedAdapter != null) feedAdapter.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop auto-refresh
        if (autoRefreshHandler != null) {
            autoRefreshHandler.removeCallbacksAndMessages(null);
        }
        
        viewModel = null;
        likeManager = null;
        commentsManager = null;
        
        Log.d(TAG, "🏳️ HomeFragment destroyed - social features cleaned up");
    }
}
