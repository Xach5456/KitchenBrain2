package com.example.kitchenbrain.util;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.Map;

public class MessageTranslator {

    private static final String TAG = "MessageTranslator";
    private final Map<String, String> translationCache = new HashMap<>();

    public LiveData<TranslationResult> translate(String text, String targetLanguage) {
        MutableLiveData<TranslationResult> result = new MutableLiveData<>();

        if (text == null || text.trim().isEmpty()) {
            result.setValue(new TranslationResult("", false));
            return result;
        }

        String cacheKey = text + "_" + targetLanguage;
        if (translationCache.containsKey(cacheKey)) {
            TranslationResult cached = new TranslationResult(translationCache.get(cacheKey), true);
            result.setValue(cached);
            return result;
        }

        performTranslation(text, targetLanguage, translatedText -> {
            translationCache.put(cacheKey, translatedText);
            result.setValue(new TranslationResult(translatedText, false));
        });

        return result;
    }

    private void performTranslation(String text, String targetLanguage, OnTranslationCallback callback) {
        new Thread(() -> {
            try {
                // REMOVED: Thread.sleep(100) - unnecessary delay
                // Translation is instant for simulated translation
                
                String translated = simulateTranslation(text, targetLanguage);
                
                if (callback != null) {
                    callback.onTranslated(translated);
                }
            } catch (Exception e) {
                Log.e(TAG, "Translation error", e);
                if (callback != null) {
                    callback.onTranslated(text);
                }
            }
        }).start();
    }

    private String simulateTranslation(String text, String targetLanguage) {
        switch (targetLanguage) {
            case "es":
                return "[ES] " + text;
            case "fr":
                return "[FR] " + text;
            case "de":
                return "[DE] " + text;
            case "it":
                return "[IT] " + text;
            case "pt":
                return "[PT] " + text;
            case "ru":
                return "[RU] " + text;
            case "ja":
                return "[JA] " + text;
            case "zh":
                return "[ZH] " + text;
            default:
                return text;
        }
    }

    public void clearCache() {
        translationCache.clear();
    }

    public interface OnTranslationCallback {
        void onTranslated(String translatedText);
    }

    public static class TranslationResult {
        public final String translatedText;
        public final boolean isCached;

        public TranslationResult(String translatedText, boolean isCached) {
            this.translatedText = translatedText;
            this.isCached = isCached;
        }
    }
}
