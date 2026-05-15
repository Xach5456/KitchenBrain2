package com.example.kitchenbrain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * ViewModel for HomeFragment
 * Clean architecture - handles UI state and data operations
 */
public class HomeViewModel extends ViewModel {
    
    private final HomeRepository repository;
    
    // UI State
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<List<Recipe>> recipes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public enum UiState {
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }
    
    public HomeViewModel() {
        this.repository = new HomeRepository();
    }
    
    // LiveData observers
    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<List<Recipe>> getRecipes() { return recipes; }
    public LiveData<Boolean> isRefreshing() { return isRefreshing; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    
    /**
     * Load recipes from Firestore
     */
    public void loadRecipes() {
        uiState.setValue(UiState.LOADING);
        
        repository.getRecipes(50, new HomeRepository.RecipeLoadCallback() {
            @Override
            public void onSuccess(List<Recipe> loadedRecipes) {
                if (loadedRecipes.isEmpty()) {
                    uiState.setValue(UiState.EMPTY);
                } else {
                    recipes.setValue(loadedRecipes);
                    uiState.setValue(UiState.SUCCESS);
                }
                isRefreshing.setValue(false);
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
                uiState.setValue(UiState.ERROR);
                isRefreshing.setValue(false);
            }
        });
    }
    
    /**
     * Refresh recipes (pull-to-refresh)
     */
    public void refreshRecipes() {
        isRefreshing.setValue(true);
        loadRecipes();
    }
    
    /**
     * Like/Unlike a recipe
     */
    public void toggleLike(String recipeId, String userId, boolean isCurrentlyLiked) {
        repository.likeRecipe(recipeId, userId, !isCurrentlyLiked, new HomeRepository.ResultCallback() {
            @Override
            public void onSuccess(Object result) {
                // Update local UI state immediately for smooth UX
                // The real-time listener will update the count automatically
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }
    
    /**
     * Save/Unsave a recipe
     */
    public void toggleSave(String recipeId, String userId, boolean isCurrentlySaved) {
        repository.saveRecipe(recipeId, userId, !isCurrentlySaved, new HomeRepository.ResultCallback() {
            @Override
            public void onSuccess(Object result) {
                // Success - UI will update via callback
            }
            
            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }
    
    /**
     * Check like status for a recipe
     */
    public void checkLikeStatus(String recipeId, String userId, LikeStatusCallback callback) {
        repository.isRecipeLiked(recipeId, userId, new HomeRepository.LikedCallback() {
            @Override
            public void onLiked(boolean isLiked) {
                callback.onStatus(isLiked);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Check save status for a recipe
     */
    public void checkSaveStatus(String recipeId, String userId, SaveStatusCallback callback) {
        repository.isRecipeSaved(recipeId, userId, new HomeRepository.SavedCallback() {
            @Override
            public void onSaved(boolean isSaved) {
                callback.onStatus(isSaved);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    // Callbacks for UI
    public interface LikeStatusCallback {
        void onStatus(boolean isLiked);
        void onError(String error);
    }
    
    public interface SaveStatusCallback {
        void onStatus(boolean isSaved);
        void onError(String error);
    }
}
