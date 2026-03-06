package com.example.kitchenbrain;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class IngredientsFragment extends Fragment {

    private LinearLayout mainIngredientsContainer, vegetableCategoryCheckBoxContainer, fruitCategoryCheckBoxContainer, dairyCategoryCheckBoxContainer;
    private TextView mainCategoryTitle, vegetableCategoryTitle, fruitCategoryTitle, dairyCategoryTitle;
    private Button confirmIngredientsButton;
    private ArrayList<String> selectedIngredientsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingredients, container, false);

        mainCategoryTitle = view.findViewById(R.id.mainCategoryTitle);
        mainIngredientsContainer = view.findViewById(R.id.mainIngredientsContainer);

        vegetableCategoryTitle = view.findViewById(R.id.vegetableCategoryTitle);
        vegetableCategoryCheckBoxContainer = view.findViewById(R.id.vegetableCategoryCheckBoxContainer);

        fruitCategoryTitle = view.findViewById(R.id.fruitCategoryTitle);
        fruitCategoryCheckBoxContainer = view.findViewById(R.id.fruitCategoryCheckBoxContainer);

        dairyCategoryTitle = view.findViewById(R.id.dairyCategoryTitle);  // Новый TextView для молочных продуктов
        dairyCategoryCheckBoxContainer = view.findViewById(R.id.dairyCategoryCheckBoxContainer);  // Новый контейнер для чекбоксов молочных продуктов

        confirmIngredientsButton = view.findViewById(R.id.confirmIngredientsButton);

        restoreCheckedIngredients(); // Восстанавливаем состояние чекбоксов

        confirmIngredientsButton.setOnClickListener(v -> {
            selectedIngredientsList.clear();
            addSelectedIngredientFromContainer(mainIngredientsContainer);
            addSelectedIngredientFromContainer(vegetableCategoryCheckBoxContainer);
            addSelectedIngredientFromContainer(fruitCategoryCheckBoxContainer);
            addSelectedIngredientFromContainer(dairyCategoryCheckBoxContainer);  // Обработка молочных продуктов

            saveSelectedIngredients();
            requireActivity().getSupportFragmentManager().popBackStack(); // Возвращаемся на предыдущий фрагмент
        });

        mainCategoryTitle.setOnClickListener(v -> toggleVisibility(mainIngredientsContainer));
        vegetableCategoryTitle.setOnClickListener(v -> toggleVisibility(vegetableCategoryCheckBoxContainer));
        fruitCategoryTitle.setOnClickListener(v -> toggleVisibility(fruitCategoryCheckBoxContainer));
        dairyCategoryTitle.setOnClickListener(v -> toggleVisibility(dairyCategoryCheckBoxContainer));  // Обработчик для молочных продуктов

        return view;
    }

    private void addSelectedIngredientFromContainer(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View childView = container.getChildAt(i);
            if (childView instanceof CheckBox && ((CheckBox) childView).isChecked()) {
                selectedIngredientsList.add(((CheckBox) childView).getText().toString());
            }
        }
    }

    private void saveSelectedIngredients() {
        SharedPreferences preferences = requireActivity().getSharedPreferences("SelectedIngredients", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("ingredients", new HashSet<>(selectedIngredientsList));
        editor.apply();
    }

    private void restoreCheckedIngredients() {
        SharedPreferences preferences = requireActivity().getSharedPreferences("SelectedIngredients", Context.MODE_PRIVATE);
        Set<String> selectedIngredientsSet = preferences.getStringSet("ingredients", new HashSet<>());

        restoreCheckedState(mainIngredientsContainer, selectedIngredientsSet);
        restoreCheckedState(vegetableCategoryCheckBoxContainer, selectedIngredientsSet);
        restoreCheckedState(fruitCategoryCheckBoxContainer, selectedIngredientsSet);
        restoreCheckedState(dairyCategoryCheckBoxContainer, selectedIngredientsSet);  // Восстановление для молочных продуктов
    }

    private void restoreCheckedState(LinearLayout container, Set<String> selectedIngredientsSet) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View childView = container.getChildAt(i);
            if (childView instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) childView;
                if (selectedIngredientsSet.contains(checkBox.getText().toString())) {
                    checkBox.setChecked(true);
                }
            }
        }
    }

    private void toggleVisibility(final View view) {
        final int initialHeight = view.getMeasuredHeight();
        if (view.getVisibility() == View.GONE) {
            view.setVisibility(View.VISIBLE);
            view.getLayoutParams().height = 1;
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            final int targetHeight = view.getMeasuredHeight();

            Animation expandAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    view.getLayoutParams().height = interpolatedTime == 1
                            ? targetHeight
                            : (int) (initialHeight + (targetHeight - initialHeight) * interpolatedTime);
                    view.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };

            expandAnimation.setDuration(300);
            view.startAnimation(expandAnimation);
        } else {
            final int collapseHeight = view.getMeasuredHeight();

            Animation collapseAnimation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    if (interpolatedTime == 1) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.getLayoutParams().height = (int) (collapseHeight - collapseHeight * interpolatedTime);
                        view.requestLayout();
                    }
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };

            collapseAnimation.setDuration(300);
            view.startAnimation(collapseAnimation);
        }
    }
}
