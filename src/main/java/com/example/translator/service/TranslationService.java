package com.example.translator.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TranslationService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int TIMEOUT_SECONDS = 10;
    private static final List<String> FALLBACK_APIS = Arrays.asList(
        "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s",
        "https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s"
    );

    /**
     * Translates text asynchronously using multiple translation APIs
     */
    public CompletableFuture<TranslationResult> translateAsync(String text, String sourceCode, String targetCode) {
        return CompletableFuture.supplyAsync(() -> {
            // Try primary API first
            try {
                String result = translateWithMyMemory(text, sourceCode, targetCode);
                if (result != null && !result.trim().isEmpty()) {
                    return new TranslationResult(result, "MyMemory API", true);
                }
            } catch (Exception e) {
                System.err.println("MyMemory API failed: " + e.getMessage());
            }

            // Fallback to Google Translate (unofficial)
            try {
                String result = translateWithGoogleTranslate(text, sourceCode, targetCode);
                if (result != null && !result.trim().isEmpty()) {
                    return new TranslationResult(result, "Google Translate", true);
                }
            } catch (Exception e) {
                System.err.println("Google Translate failed: " + e.getMessage());
            }

            // If all APIs fail, return mock translation
            return new TranslationResult(
                generateIntelligentMockTranslation(text, sourceCode, targetCode),
                "Mock Translation",
                false
            );
        }).orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
         .exceptionally(throwable -> {
             System.err.println("Translation timeout or error: " + throwable.getMessage());
             return new TranslationResult(
                 generateIntelligentMockTranslation(text, sourceCode, targetCode),
                 "Mock Translation (Timeout)",
                 false
             );
         });
    }

    private String translateWithMyMemory(String text, String sourceCode, String targetCode) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String urlString = String.format(
            "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s",
            encodedText, sourceCode, targetCode
        );

        String response = makeHttpRequest(urlString);
        JsonNode jsonNode = objectMapper.readTree(response);
        
        JsonNode translatedText = jsonNode.path("responseData").path("translatedText");
        if (!translatedText.isMissingNode()) {
            String translation = translatedText.asText();
            // Check for quality indicators
            JsonNode match = jsonNode.path("responseData").path("match");
            double confidence = match.asDouble(0.0);
            
            if (confidence > 0.3 || !translation.toLowerCase().contains("translated by")) {
                return translation;
            }
        }
        
        return null;
    }

    private String translateWithGoogleTranslate(String text, String sourceCode, String targetCode) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String urlString = String.format(
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s",
            sourceCode, targetCode, encodedText
        );

        String response = makeHttpRequest(urlString);
        // Parse Google Translate's response format
        if (response.startsWith("[[")) {
            int firstQuoteIndex = response.indexOf("\"", 2);
            int secondQuoteIndex = response.indexOf("\"", firstQuoteIndex + 1);
            if (firstQuoteIndex > 0 && secondQuoteIndex > firstQuoteIndex) {
                return response.substring(firstQuoteIndex + 1, secondQuoteIndex);
            }
        }
        
        return null;
    }

    private String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP response code: " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String generateIntelligentMockTranslation(String text, String sourceCode, String targetCode) {
        // Enhanced mock translation with language-specific characteristics
        String mockTranslation = text;
        
        // Apply language-specific transformations
        switch (targetCode) {
            case "es": // Spanish
                mockTranslation = applySpanishPatterns(text);
                break;
            case "fr": // French
                mockTranslation = applyFrenchPatterns(text);
                break;
            case "de": // German
                mockTranslation = applyGermanPatterns(text);
                break;
            case "zh": // Chinese
                mockTranslation = "您好！这是一个模拟翻译：" + text + " (模拟)";
                break;
            case "ja": // Japanese
                mockTranslation = "こんにちは！これは模擬翻訳です：" + text + " (シミュレーション)";
                break;
            case "ru": // Russian
                mockTranslation = "Привет! Это симулированный перевод: " + text + " (симуляция)";
                break;
            case "ar": // Arabic
                mockTranslation = "مرحبا! هذه ترجمة محاكاة: " + text + " (محاكاة)";
                break;
            case "hi": // Hindi
                mockTranslation = "नमस्ते! यह एक अनुकरणीय अनुवाद है: " + text + " (अनुकरण)";
                break;
            case "sw": // Swahili
                mockTranslation = "Hujambo! Hii ni tafsiri ya kuigiza: " + text + " (mfano)";
                break;
            default:
                mockTranslation = String.format("[Mock: %s → %s] %s", sourceCode, targetCode, text);
        }
        
        return mockTranslation;
    }

    private String applySpanishPatterns(String text) {
        return "¡Hola! Esta es una traducción simulada al español: " + 
               text.replace("hello", "hola")
                   .replace("world", "mundo")
                   .replace("good", "bueno")
                   .replace("thank you", "gracias") + 
               " (traducción simulada)";
    }

    private String applyFrenchPatterns(String text) {
        return "Bonjour! Ceci est une traduction simulée en français: " + 
               text.replace("hello", "bonjour")
                   .replace("world", "monde")
                   .replace("good", "bon")
                   .replace("thank you", "merci") + 
               " (traduction simulée)";
    }

    private String applyGermanPatterns(String text) {
        return "Hallo! Dies ist eine simulierte deutsche Übersetzung: " + 
               text.replace("hello", "hallo")
                   .replace("world", "Welt")
                   .replace("good", "gut")
                   .replace("thank you", "danke") + 
               " (simulierte Übersetzung)";
    }

    /**
     * Validates if the translation looks legitimate
     */
    public boolean isValidTranslation(String original, String translated, String targetLanguage) {
        if (translated == null || translated.trim().isEmpty()) {
            return false;
        }
        
        // Check if it's just a copy of original
        if (original.equals(translated)) {
            return false;
        }
        
        // Check for common error patterns
        String lowerTranslated = translated.toLowerCase();
        if (lowerTranslated.contains("error") || 
            lowerTranslated.contains("failed") ||
            lowerTranslated.contains("quota exceeded") ||
            lowerTranslated.contains("service unavailable")) {
            return false;
        }
        
        return true;
    }

    /**
     * Result class for translation operations
     */
    public static class TranslationResult {
        private final String translatedText;
        private final String source;
        private final boolean isRealTranslation;
        private final long timestamp;

        public TranslationResult(String translatedText, String source, boolean isRealTranslation) {
            this.translatedText = translatedText;
            this.source = source;
            this.isRealTranslation = isRealTranslation;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getTranslatedText() { return translatedText; }
        public String getSource() { return source; }
        public boolean isRealTranslation() { return isRealTranslation; }
        public long getTimestamp() { return timestamp; }
    }
}