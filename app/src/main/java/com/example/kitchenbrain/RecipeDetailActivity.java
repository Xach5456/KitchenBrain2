package com.example.kitchenbrain;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

/**
 * Host for {@link RecipeDetailFragment}. Keeps navigation simple: lists still launch an Activity,
 * while the actual UI lives in a fragment (View Binding, easier testing, matches Navigation Component style).
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE = "extra_recipe";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start);
        setContentView(R.layout.activity_recipe_detail);

        Recipe recipe = readRecipeFromIntent();
        if (recipe == null) {
            Toast.makeText(this, R.string.recipe_detail_missing, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setReorderingAllowed(true);
            ft.setCustomAnimations(
                    R.anim.fragment_fade_in,
                    R.anim.fragment_fade_out,
                    R.anim.fragment_fade_in,
                    R.anim.fragment_fade_out
            );
            ft.replace(R.id.recipeDetailHost, RecipeDetailFragment.newInstance(recipe));
            ft.commit();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overrideExitTransition();
    }

    private void overrideExitTransition() {
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_to_end);
    }

    @Nullable
    private Recipe readRecipeFromIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getIntent().getParcelableExtra(EXTRA_RECIPE, Recipe.class);
        }
        return getIntent().getParcelableExtra(EXTRA_RECIPE);
    }
}
