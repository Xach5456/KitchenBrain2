package com.example.kitchenbrain.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.kitchenbrain.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartReplyGenerator {

    private static final String[] POSITIVE_PATTERNS = {
        "great", "awesome", "excellent", "good", "perfect", "wonderful",
        "amazing", "fantastic", "love", "happy", "glad", "thanks", "thank you"
    };

    private static final String[] NEGATIVE_PATTERNS = {
        "bad", "terrible", "awful", "hate", "sad", "angry", "upset",
        "disappointed", "wrong", "error", "problem", "issue"
    };

    private static final String[] QUESTION_PATTERNS = {
        "what", "when", "where", "why", "how", "who", "which",
        "can you", "could you", "would you", "do you", "are you", "is it"
    };

    private static final String[][] GENERIC_RESPONSES = {
        {"That's interesting!", "Tell me more", "I see", "Really?", "Wow!"},
        {"I agree", "Absolutely", "Definitely", "For sure", "Exactly"},
        {"Okay", "Sure", "No problem", "Got it", "Understood"}
    };

    public LiveData<List<String>> generateSuggestions(String receivedMessage) {
        MutableLiveData<List<String>> suggestions = new MutableLiveData<>();
        List<String> result = new ArrayList<>();

        if (receivedMessage == null || receivedMessage.trim().isEmpty()) {
            suggestions.setValue(result);
            return suggestions;
        }

        String lowerCaseMessage = receivedMessage.toLowerCase();

        for (String pattern : POSITIVE_PATTERNS) {
            if (lowerCaseMessage.contains(pattern)) {
                result.add("😊 That's great!");
                result.add("I'm happy to hear that!");
                result.add("Awesome!");
                break;
            }
        }

        for (String pattern : NEGATIVE_PATTERNS) {
            if (lowerCaseMessage.contains(pattern)) {
                result.add("😔 I'm sorry to hear that");
                result.add("Is there anything I can do?");
                result.add("Hope things get better");
                break;
            }
        }

        for (String pattern : QUESTION_PATTERNS) {
            if (lowerCaseMessage.startsWith(pattern)) {
                result.add("Good question!");
                result.add("Let me think about that");
                result.add("I'm not sure, but...");
                break;
            }
        }

        if (result.isEmpty()) {
            for (int i = 0; i < Math.min(3, GENERIC_RESPONSES.length); i++) {
                int index = (int) (Math.random() * GENERIC_RESPONSES[i].length);
                result.add(GENERIC_RESPONSES[i][index]);
            }
        }

        suggestions.setValue(result);
        return suggestions;
    }

    public boolean containsEmoji(String text) {
        if (text == null) return false;
        Pattern emojiPattern = Pattern.compile(
            "[\uD83C-\uDBFF]" +
            "[\uDC00-\uDFFF]" +
            "(?:[\u200D\uFE0F]|\\uFE0F|\\u20E3)?" +
            "(?:[\uD83C-\uDBFF]" +
            "[\uDC00-\uDFFF])?"
        );
        Matcher matcher = emojiPattern.matcher(text);
        return matcher.find();
    }
}
