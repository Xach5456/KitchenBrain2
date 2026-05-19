package com.example.kitchenbrain;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
 * 🔥 UPDATED: Only updates on manual interaction (Click or Swipe).
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private RecyclerView recyclerViewFeed;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private LinearLayout layoutEmpty;
    private ImageButton btnRefreshFeed;

    private HomeFeedViewModel viewModel;
    private FeedPostAdapter feedAdapter;
    
    // Instagram-style social features
    private LikeManager likeManager;
    private CommentsManager commentsManager;
    private FirebaseAuth auth;
    private String currentUserId;
    
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
        Log.d(TAG, "🚀 HomeFragment - Manual Refresh Mode");

        initSocialFeatures();
        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupSwipeRefresh();
        setupRefreshButton();
        observeViewModel();
        
        // Initial load
        viewModel.loadFeed();
    }

    private void initSocialFeatures() {
        // Firebase Auth
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            currentUserId = "anonymous";
        }
        
        // Social managers
        likeManager = new LikeManager();
        commentsManager = new CommentsManager();
    }

    private void initViews(View view) {
        recyclerViewFeed = view.findViewById(R.id.recyclerViewFeed);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutError = view.findViewById(R.id.layoutError);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        btnRefreshFeed = view.findViewById(R.id.btnRefreshFeed);
        
        view.findViewById(R.id.buttonRetry).setOnClickListener(v -> viewModel.refreshFeed());
    }

    private void setupRefreshButton() {
        if (btnRefreshFeed != null) {
            btnRefreshFeed.setOnClickListener(v -> {
                Log.d(TAG, "🔄 Refresh button clicked");
                viewModel.refreshFeed();
            });
        }
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

    private void handleSendToChat(FeedItem item) {
        ShareNewsFragment shareFragment = ShareNewsFragment.newInstance(
            item.getRecipe().getName(), 
            item.getRecipe().getVideoUrl()
        );
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToFragment(shareFragment, true);
        }
    }

    private void setupSwipeRefresh() {
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
        switch (state.getStatus()) {
            case LOADING:
                showLoading();
                break;
                    
            case SUCCESS:
                if (state.getData() != null) {
                    feedAdapter.setFeedItems(state.getData());
                }
                showContent();
                
                if (state.isFromCache() && getContext() != null) {
                    Toast.makeText(getContext(), "Daily API quota reached. Showing cached recipes.", Toast.LENGTH_LONG).show();
                }

                if (isApiError) {
                    isApiError = false;
                    lastErrorCode = 0;
                }
                break;
                    
            case EMPTY:
                showEmpty();
                break;
                    
            case ERROR:
                String errorMessage = state.getErrorMessage() != null ? state.getErrorMessage() : "Unknown Error";
                if (errorMessage.contains("402")) {
                    isApiError = true;
                    lastErrorCode = 402;
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
        if (isLiking) return;
        
        String postId = generatePostId(item);
        isLiking = true;
        
        likeManager.toggleLike(postId, currentUserId, new LikeManager.LikeCallback() {
            @Override
            public void onSuccess(LikeManager.LikeResult result) {
                viewModel.toggleLike(item.getStableId());
                isLiking = false;
            }
            
            @Override
            public void onError(String error) {
                isLiking = false;
                Toast.makeText(getContext(), "Like failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCommentClick(FeedItem item, int position) {
        String postId = generatePostId(item);
        String postTitle = item.getRecipe() != null ? item.getRecipe().getName() : "Recipe";
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
}
