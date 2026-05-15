package com.example.kitchenbrain;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import androidx.core.util.Pair;

import com.example.kitchenbrain.model.EngagementState;
import com.example.kitchenbrain.model.FeedItem;
import com.example.kitchenbrain.repository.SpoonacularRepository;
import com.example.kitchenbrain.repository.NewsRepository;
import com.example.kitchenbrain.api.spoonacular.SpoonacularRecipe;
import com.example.kitchenbrain.models.Article;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeFeedViewModel - Unified Feed (Recipes + Food News)
 */
public class HomeFeedViewModel extends AndroidViewModel {

    private static final String TAG = "HomeFeedViewModel";

    private final SpoonacularRepository repository;
    private final NewsRepository newsRepository;
    
    private final MutableLiveData<EngagementState> engagementState = new MutableLiveData<>(new EngagementState());
    private final MutableLiveData<FeedState> feedState = new MutableLiveData<>(FeedState.LOADING);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Recipe>> rawRecipes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isDataFromCache = new MutableLiveData<>(false);
    private final List<Recipe> cachedRecipes = new ArrayList<>();

    private final LiveData<List<FeedItem>> feedItems;
    private final LiveData<HomeUiState> singleUiState;

    public enum FeedState {
        LOADING, SUCCESS, EMPTY, ERROR
    }

    public HomeFeedViewModel(@NonNull Application application) {
        super(application);
        this.repository = new SpoonacularRepository(application);
        this.newsRepository = new NewsRepository(application);
        
        feedItems = Transformations.map(combineRecipesAndState(), this::projectFeedItems);
        singleUiState = createSingleUiState();
    }

    private LiveData<HomeUiState> createSingleUiState() {
        return new MediatorLiveData<HomeUiState>() {{
            setValue(HomeUiState.loading());
            
            final List<FeedItem>[] latestData = new List[]{null};
            final Boolean[] isLoading = new Boolean[]{true};
            
            addSource(feedItems, data -> {
                latestData[0] = data;
                updateState(this, latestData[0], isLoading[0]);
            });
            
            addSource(feedState, state -> {
                isLoading[0] = (state == FeedState.LOADING);
                updateState(this, latestData[0], isLoading[0]);
            });

            addSource(isDataFromCache, fromCache -> {
                if (feedState.getValue() == FeedState.SUCCESS && latestData[0] != null) {
                    setValue(HomeUiState.success(latestData[0], fromCache != null && fromCache));
                }
            });
        }};
    }

    private void updateState(MediatorLiveData<HomeUiState> mediator, List<FeedItem> data, boolean loading) {
        if (loading) {
            mediator.setValue(HomeUiState.loading());
        } else if (feedState.getValue() == FeedState.ERROR) {
            mediator.setValue(HomeUiState.error(errorMessage.getValue()));
        } else if (data != null && !data.isEmpty()) {
            mediator.setValue(HomeUiState.success(data, isDataFromCache.getValue() != null && isDataFromCache.getValue()));
        } else {
            mediator.setValue(HomeUiState.empty());
        }
    }

    private LiveData<Pair<List<Recipe>, EngagementState>> combineRecipesAndState() {
        return new MediatorLiveData<Pair<List<Recipe>, EngagementState>>() {{
            setValue(new Pair<>(new ArrayList<>(), new EngagementState()));
            addSource(rawRecipes, recipes -> {
                EngagementState state = getValue() != null ? getValue().second : new EngagementState();
                setValue(new Pair<>(recipes, state));
            });
            addSource(engagementState, state -> {
                List<Recipe> recipes = getValue() != null ? getValue().first : new ArrayList<>();
                setValue(new Pair<>(recipes, state));
            });
        }};
    }

    private List<FeedItem> projectFeedItems(Pair<List<Recipe>, EngagementState> combined) {
        List<Recipe> recipes = combined.first;
        EngagementState state = combined.second;
        List<FeedItem> items = new ArrayList<>(recipes.size());
        for (Recipe recipe : recipes) {
            items.add(new FeedItem(recipe, state.isLiked(recipe.getId()), state.isSaved(recipe.getId())));
        }
        return items;
    }

    public LiveData<HomeUiState> getSingleUiState() { return singleUiState; }

    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void refreshFeed() {
        feedState.setValue(FeedState.LOADING);
        
        repository.getRandomRecipes(10, new SpoonacularRepository.SpoonacularCallback<List<SpoonacularRecipe>>() {
            @Override
            public void onSuccess(List<SpoonacularRecipe> result, boolean isFromCache) {
                List<Recipe> mappedRecipes = new ArrayList<>();
                for (SpoonacularRecipe s : result) mappedRecipes.add(mapSpoonacularToRecipe(s));
                fetchNewsAndMerge(mappedRecipes, isFromCache);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Spoonacular quota reached, showing News only");
                fetchNewsAndMerge(new ArrayList<>(), true);
            }
        });
    }

    private void fetchNewsAndMerge(List<Recipe> recipes, boolean isCache) {
        newsRepository.getNews(1, false, new NewsRepository.NewsCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                Log.d(TAG, "✅ Merging " + articles.size() + " news articles into feed");
                for (Article a : articles) {
                    recipes.add(mapArticleToRecipe(a));
                }
                onRecipesFetched(recipes, isCache);
            }

            @Override
            public void onLoading() {}
            @Override
            public void onEmpty() { onRecipesFetched(recipes, isCache); }

            @Override
            public void onError(String error, List<Article> fallbackData) {
                if (recipes.isEmpty()) {
                    onRecipesError("Failed to load feed: " + error);
                } else {
                    onRecipesFetched(recipes, isCache);
                }
            }
        });
    }

    private Recipe mapSpoonacularToRecipe(SpoonacularRecipe s) {
        Recipe r = new Recipe();
        r.setId(String.valueOf(s.id));
        r.setName(s.title);
        r.setImageUrl(s.image);
        r.setCookingTime(s.readyInMinutes);
        r.setDescription(s.summary);
        r.setLikesCount(s.aggregateLikes);
        r.setServings(s.servings);
        r.setCookingInstructions(s.instructions);
        r.setVideoUrl(s.sourceUrl);
        return r;
    }

    private Recipe mapArticleToRecipe(Article a) {
        Recipe r = new Recipe();
        // 🔥 FIX: Generate Firestore-safe ID (Avoids path // crash)
        String rawId = a.getStableId();
        String safeId = (rawId != null && rawId.contains("://")) 
            ? "news_" + Math.abs(rawId.hashCode()) 
            : rawId;
        
        r.setId(safeId);
        r.setName(a.getTitle());
        r.setDescription(a.getDescription());
        r.setImageUrl(a.getUrlToImage());
        r.setUsername(a.getSource() != null ? a.getSource().getName() : "Culinary News");
        r.setVideoUrl(a.getUrl());
        r.setDifficulty("Article");
        return r;
    }

    public void loadFeed() {
        if (!cachedRecipes.isEmpty()) {
            onRecipesFetched(new ArrayList<>(cachedRecipes), false);
            return;
        }
        refreshFeed();
    }

    public void toggleLike(String recipeId) {
        EngagementState currentState = engagementState.getValue();
        if (currentState == null) currentState = new EngagementState();
        EngagementState newState = currentState.toggleLike(recipeId);
        engagementState.setValue(newState);
    }

    public void toggleSave(String recipeId) {
        EngagementState currentState = engagementState.getValue();
        if (currentState == null) currentState = new EngagementState();
        EngagementState newState = currentState.toggleSave(recipeId);
        engagementState.setValue(newState);
    }

    void onRecipesFetched(List<Recipe> recipes, boolean fromCache) {
        isDataFromCache.setValue(fromCache);
        cachedRecipes.clear();
        cachedRecipes.addAll(recipes);
        rawRecipes.setValue(recipes);
        feedState.setValue(recipes.isEmpty() ? FeedState.EMPTY : FeedState.SUCCESS);
    }
    
    void onRecipesError(String errorMsg) {
        this.errorMessage.setValue(errorMsg);
        feedState.setValue(FeedState.ERROR);
    }
}
