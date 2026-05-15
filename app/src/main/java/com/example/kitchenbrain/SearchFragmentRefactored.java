package com.example.kitchenbrain;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.adapter.UnifiedSearchAdapter;
import com.example.kitchenbrain.manager.FollowGraphRepository;
import com.example.kitchenbrain.repository.FollowRepository;
import com.example.kitchenbrain.model.Recipe;
import com.example.kitchenbrain.model.SearchItem;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.SearchMode;
import com.example.kitchenbrain.viewmodel.SearchViewModel;
import com.example.kitchenbrain.R;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🔥 INSTAGRAM-LEVEL SEARCH FRAGMENT - Clean Architecture
 * Single RecyclerView + Unified Adapter + Single Data Stream
 */
public class SearchFragmentRefactored extends Fragment {

    private static final String TAG = "SearchFragment";
    private static final long SEARCH_DEBOUNCE_DELAY_MS = 300;

    // 🔥 VIEWMODEL & REPOSITORY
    private SearchViewModel viewModel;
    private FollowRepository followRepository;

    // 🔥 UI COMPONENTS
    private RecyclerView recyclerView;
    private UnifiedSearchAdapter searchAdapter;
    private TextView textResultCount;
    private TextInputEditText editTextSearch;
    private ChipGroup chipGroupSearchType;

    // 🔥 THREADING
    private ExecutorService backgroundExecutor;
    private Handler searchDebounceHandler;
    private Runnable searchRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🔥 INIT INFRASTRUCTURE
        backgroundExecutor = Executors.newSingleThreadExecutor();
        searchDebounceHandler = new Handler(Looper.getMainLooper());
        followRepository = new FollowRepository();
        
        Log.d(TAG, "🔥 Instagram Search Fragment created");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_unified, container, false);
        
        viewModel = new ViewModelProvider(requireActivity(), 
                new SavedStateViewModelFactory(requireActivity().getApplication(), requireActivity()))
                .get(SearchViewModel.class);
        
        initViews(view);
        setupListeners();
        observeViewModel();
        
        // 🔥 LOAD INITIAL DATA
        if (!viewModel.isDataLoaded()) {
            loadInitialData();
        }
        
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewSearch);
        textResultCount = view.findViewById(R.id.textResultCount);
        editTextSearch = view.findViewById(R.id.editTextSearch);
        chipGroupSearchType = view.findViewById(R.id.chipGroupSearchType);
        
        // 🔥 SETUP UNIFIED ADAPTER WITH FOLLOW LISTENER
        searchAdapter = new UnifiedSearchAdapter(this::onSearchItemClick);
        searchAdapter.setOnUserFollowClickListener(this::handleFollowAction);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(searchAdapter);
        
        chipGroupSearchType.setOnCheckedChangeListener((group, checkedId) -> {
            SearchMode mode = (checkedId == R.id.chipUsers) ? SearchMode.USERS : SearchMode.RECIPES;
            viewModel.setSearchMode(mode);
            
            String currentQuery = editTextSearch.getText().toString().trim();
            performSearch(currentQuery, mode);
        });
        
        // 🔥 DEFAULT TO USERS MODE as requested
        viewModel.setSearchMode(SearchMode.USERS);
        chipGroupSearchType.check(R.id.chipUsers);
        
        Log.d(TAG, "🔥 Views initialized");
    }

    private void setupListeners() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (searchRunnable != null) searchDebounceHandler.removeCallbacks(searchRunnable);
                
                searchRunnable = () -> {
                    SearchMode currentMode = viewModel.getSearchMode().getValue();
                    if (currentMode != null) performSearch(query, currentMode);
                };
                searchDebounceHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSearchMode().observe(getViewLifecycleOwner(), this::updateSearchModeUI);
        
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            searchAdapter.submitList(new ArrayList<>(results));
            updateResultCountUI(results.size());
        });

        // 🔥 OBSERVE GLOBAL FOLLOW STATE (Updates buttons in real-time)
        FollowGraphRepository.getInstance().getFollowingLiveData().observe(getViewLifecycleOwner(), following -> {
            List<SearchItem> currentResults = viewModel.getSearchResults().getValue();
            if (currentResults != null) {
                searchAdapter.submitList(new ArrayList<>(currentResults));
            }
        });
    }

    private void performSearch(String query, SearchMode mode) {
        if (mode == SearchMode.USERS) {
            viewModel.searchUsers(query);
        } else {
            // Recipe search logic (existing)
            searchRecipesLocally(query);
        }
    }

    private void handleFollowAction(User user, boolean isCurrentlyFollowing) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null || user.getId() == null) return;

        if (isCurrentlyFollowing) {
            followRepository.unfollowUser(currentUid, user.getId(), (success, error) -> {
                if (!success) Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            });
        } else {
            followRepository.followUser(currentUid, user.getId(), (success, error) -> {
                if (!success) Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void searchRecipesLocally(String query) {
        backgroundExecutor.execute(() -> {
            List<Recipe> all = viewModel.getAllRecipes().getValue();
            List<SearchItem> filtered = new ArrayList<>();
            if (all != null) {
                String q = query.toLowerCase();
                for (Recipe r : all) {
                    if (q.isEmpty() || (r.getTitle() != null && r.getTitle().toLowerCase().contains(q))) {
                        filtered.add(new SearchItem(SearchItem.Type.RECIPE, r, r.getId()));
                    }
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> viewModel.setSearchResults(filtered));
        });
    }

    private void updateSearchModeUI(SearchMode mode) {
        chipGroupSearchType.check(mode == SearchMode.USERS ? R.id.chipUsers : R.id.chipRecipes);
        editTextSearch.setHint(mode == SearchMode.USERS ? "Search users by nickname..." : "Search recipes...");
    }

    private void updateResultCountUI(int count) {
        SearchMode mode = viewModel.getSearchMode().getValue();
        String label = (mode == SearchMode.USERS) ? " users found" : " recipes found";
        textResultCount.setText(count + label);
    }

    private void onSearchItemClick(SearchItem item) {
        if (item.getType() == SearchItem.Type.USER) {
            String userId = item.getUser().getId();
            if (userId != null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(OtherUserProfileFragment.newInstance(userId), true);
            }
        }
    }

    private void loadInitialData() {
        // ViewModel already handles recipe loading logic in the project architecture
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) searchDebounceHandler.removeCallbacks(searchRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (backgroundExecutor != null) backgroundExecutor.shutdown();
    }
}
