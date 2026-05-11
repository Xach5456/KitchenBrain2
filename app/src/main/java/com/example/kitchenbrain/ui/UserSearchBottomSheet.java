package com.example.kitchenbrain.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kitchenbrain.MainActivity;
import com.example.kitchenbrain.OtherUserProfileFragment;
import com.example.kitchenbrain.R;
import com.example.kitchenbrain.adapter.UserSearchAdapter;
import com.example.kitchenbrain.viewmodel.SearchViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

/**
 * Полноэкранный Fragment для поиска пользователей (Instagram-style).
 * Использует ту же SearchViewModel (Activity Scope) для доступа к результатам поиска.
 */
public class UserSearchBottomSheet extends Fragment {

    private SearchViewModel viewModel;
    private UserSearchAdapter adapter;
    private TextInputEditText editTextSearch;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Используем requireActivity() для доступа к той же ViewModel, что и в SearchFragment
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);

        initViews(view);
        setupAdapter();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        editTextSearch = view.findViewById(R.id.editTextUserSearch);
        recyclerView = view.findViewById(R.id.recyclerViewUserResults);
        ImageButton buttonBack = view.findViewById(R.id.buttonBack);
        
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    private void setupAdapter() {
        adapter = new UserSearchAdapter(getContext(), new UserSearchAdapter.OnUserInteractionListener() {
            @Override
            public void onUserClick(com.example.kitchenbrain.User user) {
                // При клике открываем профиль пользователя
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToFragment(
                            OtherUserProfileFragment.newInstance(user.getUserId()), true);
                }
            }

            @Override
            public void onFollowToggle(com.example.kitchenbrain.User user, boolean isFollowing) {
                // Логика подписки уже в адаптере (через FollowRepository)
                // Можно добавить аналитику или логирование здесь
                Log.d("UserSearchBottomSheet", "User " + user.getUsername() + " follow state changed to: " + isFollowing);
            }

            @Override
            public void onMessageClick(com.example.kitchenbrain.User user) {
                // Открываем чат через MainActivity
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openChat(user.getUserId(), user.getUsername());
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                viewModel.searchUsers(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), items -> {
            // Преобразуем SearchItem обратно в User для текущего адаптера
            java.util.List<com.example.kitchenbrain.User> users = new ArrayList<>();
            for (com.example.kitchenbrain.model.SearchItem item : items) {
                if (item.getType() == com.example.kitchenbrain.model.SearchItem.Type.USER) {
                    users.add(item.getUser());
                }
            }
            Log.d("UserSearchBottomSheet", "📊 [OBSERVE] Received " + users.size() + " users from ViewModel");
            adapter.setUsers(users);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.cleanup();
        }
    }
}
