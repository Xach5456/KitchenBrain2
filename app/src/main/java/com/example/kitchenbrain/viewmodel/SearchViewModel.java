package com.example.kitchenbrain.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.example.kitchenbrain.model.SearchItem;
import com.example.kitchenbrain.model.Recipe;
import com.example.kitchenbrain.User;
import com.example.kitchenbrain.SearchMode;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SearchViewModel с использованием SavedStateHandle для персистентности состояния.
 * Реализован "Instagram-style" поиск пользователей с локальным кэшированием для поддержки "contains" и мгновенного отклика.
 */
public class SearchViewModel extends ViewModel {

    private static final String KEY_INGREDIENTS = "selected_ingredients_key";
    private static final String KEY_ALL_RECIPES = "all_recipes_key";
    private static final String KEY_SEARCH_MODE = "search_mode_key";

    private final SavedStateHandle savedStateHandle;

    private final MutableLiveData<List<Recipe>> allRecipesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Recipe>> filteredRecipesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<SearchItem>> searchResultsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<SearchMode> searchModeLiveData = new MutableLiveData<>(SearchMode.RECIPES);

    private boolean isDataLoaded = false;
    private ListenerRegistration recipeListener;
    
    // Кэш пользователей для мгновенного поиска "contains"
    private List<User> allUsersCache = null;
    
    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DEBOUNCE_DELAY_MS = 300;

    public SearchViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        
        if (!this.savedStateHandle.contains(KEY_INGREDIENTS)) {
            this.savedStateHandle.set(KEY_INGREDIENTS, new ArrayList<String>());
        }
        
        restoreFromSavedState();
    }

    private void restoreFromSavedState() {
        List<Recipe> savedAllRecipes = savedStateHandle.get(KEY_ALL_RECIPES);
        if (savedAllRecipes != null) {
            allRecipesLiveData.setValue(savedAllRecipes);
            isDataLoaded = true;
        }

        SearchMode savedMode = savedStateHandle.get(KEY_SEARCH_MODE);
        if (savedMode != null) {
            searchModeLiveData.setValue(savedMode);
        }
    }

    // --- Логика Рецептов с синхронизацией в реальном времени ---

    public void startRecipeListener() {
        if (recipeListener != null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("SearchVM", "🔄 Starting real-time recipe listener");
        recipeListener = db.collection("recipes").limit(100).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("SearchVM", "❌ Recipe listen failed.", error);
                return;
            }

            if (value != null) {
                List<Recipe> recipes = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    try {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setId(doc.getId());
                            recipes.add(r);
                        }
                    } catch (Exception e) {
                        Log.e("SearchVM", "Error deserializing recipe: " + doc.getId(), e);
                    }
                }
                Log.d("SearchVM", "✅ Recipes updated: " + recipes.size());
                setAllRecipes(recipes);
            }
        });
    }

    // --- Логика Ингредиентов ---

    public LiveData<List<String>> getSelectedIngredients() {
        return savedStateHandle.getLiveData(KEY_INGREDIENTS);
    }

    public void addIngredient(String ingredientName) {
        List<String> current = savedStateHandle.get(KEY_INGREDIENTS);
        if (current == null) current = new ArrayList<>();
        
        if (!current.contains(ingredientName)) {
            List<String> updated = new ArrayList<>(current);
            updated.add(ingredientName);
            savedStateHandle.set(KEY_INGREDIENTS, updated);
            Log.d("SearchVM", "Added ingredient: " + ingredientName);
        }
    }

    public void removeIngredient(String ingredientName) {
        List<String> current = savedStateHandle.get(KEY_INGREDIENTS);
        if (current != null && current.contains(ingredientName)) {
            List<String> updated = new ArrayList<>(current);
            updated.remove(ingredientName);
            savedStateHandle.set(KEY_INGREDIENTS, updated);
            Log.d("SearchVM", "Removed ingredient: " + ingredientName);
        }
    }

    public void clearIngredients() {
        savedStateHandle.set(KEY_INGREDIENTS, new ArrayList<String>());
    }

    // --- Логика Рецептов ---

    public void setAllRecipes(List<Recipe> recipes) {
        allRecipesLiveData.setValue(recipes);
        savedStateHandle.set(KEY_ALL_RECIPES, new ArrayList<>(recipes));
        isDataLoaded = true;
    }

    public LiveData<List<Recipe>> getAllRecipes() {
        return allRecipesLiveData;
    }

    public void setFilteredRecipes(List<Recipe> recipes) {
        filteredRecipesLiveData.setValue(recipes);
    }

    public LiveData<List<Recipe>> getFilteredRecipes() {
        return filteredRecipesLiveData;
    }

    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    // --- Поиск Пользователей (Instagram-style с кэшированием) ---

    public void searchUsers(String query) {
        if (searchRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchRunnable);
        }

        if (query == null || query.trim().isEmpty()) {
            setSearchResults(new ArrayList<>());
            return;
        }

        final String cleanQuery = query.toLowerCase().trim();

        // Debounce 300ms
        searchRunnable = () -> {
            if (allUsersCache == null) {
                // Если кэш пуст, загружаем всех пользователей один раз
                fetchAndFilterUsers(cleanQuery);
            } else {
                // Если кэш уже есть, фильтруем локально (zero latency)
                performLocalFilter(cleanQuery);
            }
        };
        searchDebounceHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
    }

    private void fetchAndFilterUsers(String query) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d("SearchVM", "🔥 [USER SEARCH] Warming cache from Firestore...");

        db.collection("users")
                .limit(1000) // Загружаем достаточное кол-во для качественного поиска
                .get()
                .addOnSuccessListener(snapshot -> {
                    allUsersCache = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            if (user.getUserId() == null) user.setUserId(doc.getId());
                            allUsersCache.add(user);
                        }
                    }
                    Log.d("SearchVM", "✅ [USER SEARCH] Cache warmed with " + allUsersCache.size() + " users");
                    performLocalFilter(query);
                })
                .addOnFailureListener(e -> {
                    Log.e("SearchVM", "❌ [USER SEARCH] Failed to fetch users", e);
                    setSearchResults(new ArrayList<>());
                });
    }

    private void performLocalFilter(String query) {
        if (allUsersCache == null) return;

        List<SearchItem> results = new ArrayList<>();
        for (User user : allUsersCache) {
            String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
            
            // "Contains" логика (как в Instagram/Telegram)
            if (username.contains(query)) {
                results.add(new SearchItem(SearchItem.Type.USER, user, user.getUserId()));
            }
        }

        // Сортировка по релевантности: сначала те, кто НАЧИНАЕТСЯ на запрос
        Collections.sort(results, (a, b) -> {
            String nameA = a.getUser().getUsername().toLowerCase();
            String nameB = b.getUser().getUsername().toLowerCase();
            
            boolean startsA = nameA.startsWith(query);
            boolean startsB = nameB.startsWith(query);
            
            if (startsA && !startsB) return -1;
            if (!startsA && startsB) return 1;
            
            // Если оба начинаются (или оба только содержат), сортируем по длине (короткие выше)
            if (nameA.length() != nameB.length()) {
                return Integer.compare(nameA.length(), nameB.length());
            }
            
            return nameA.compareTo(nameB);
        });

        Log.d("SearchVM", "🔍 [USER SEARCH] Found " + results.size() + " local matches for: '" + query + "'");
        setSearchResults(results);
    }

    public LiveData<List<SearchItem>> getSearchResults() {
        return searchResultsLiveData;
    }

    public void setSearchResults(List<SearchItem> items) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            searchResultsLiveData.setValue(items);
        } else {
            searchResultsLiveData.postValue(items);
        }
    }

    public LiveData<SearchMode> getSearchMode() {
        return searchModeLiveData;
    }

    public void setSearchMode(SearchMode mode) {
        searchModeLiveData.setValue(mode);
        savedStateHandle.set(KEY_SEARCH_MODE, mode);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (searchRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchRunnable);
        }
        searchDebounceHandler.removeCallbacksAndMessages(null);
        if (recipeListener != null) {
            recipeListener.remove();
        }
    }
}
