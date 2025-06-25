package com.example.translator.controller;

import com.example.translator.service.TranslationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TranslatorController {

    @FXML private TextArea sourceText;
    @FXML private TextArea translatedText;
    @FXML private ComboBox<String> sourceLanguage;
    @FXML private ComboBox<String> targetLanguage;
    @FXML private Button translateButton;
    @FXML private Button swapButton;
    @FXML private Button clearButton;
    @FXML private Button copyButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;

    @Autowired
    private TranslationService translationService;

    private final Map<String, String> languageCodes = new HashMap<>();

    @FXML
    public void initialize() {
        setupLanguages();
        setupUI();
        setupEventHandlers();
    }

    private void setupLanguages() {
        // Language mapping
        languageCodes.put("English", "en");
        languageCodes.put("Spanish", "es");
        languageCodes.put("French", "fr");
        languageCodes.put("German", "de");
        languageCodes.put("Chinese", "zh");
        languageCodes.put("Japanese", "ja");
        languageCodes.put("Russian", "ru");
        languageCodes.put("Arabic", "ar");
        languageCodes.put("Hindi", "hi");
        languageCodes.put("Swahili", "sw");
        languageCodes.put("Italian", "it");
        languageCodes.put("Portuguese", "pt");
        languageCodes.put("Korean", "ko");
        languageCodes.put("Dutch", "nl");
        languageCodes.put("Turkish", "tr");

        // Populate combo boxes
        sourceLanguage.getItems().addAll(languageCodes.keySet());
        targetLanguage.getItems().addAll(languageCodes.keySet());

        // Set defaults
        sourceLanguage.setValue("English");
        targetLanguage.setValue("Spanish");
    }

    private void setupUI() {
        // Set placeholders
        sourceText.setPromptText("Enter text to translate...");
        translatedText.setPromptText("Translation will appear here...");
        translatedText.setEditable(false);

        // Initial states
        if (loadingIndicator != null) loadingIndicator.setVisible(false);
        if (copyButton != null) copyButton.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Ready to translate");

        // Style text areas for better UX
        sourceText.setWrapText(true);
        translatedText.setWrapText(true);
    }

    private void setupEventHandlers() {
        // Main translate button
        translateButton.setOnAction(event -> translateText());

        // Swap button (if exists)
        if (swapButton != null) {
            swapButton.setOnAction(event -> swapLanguages());
        }

        // Clear button (if exists)
        if (clearButton != null) {
            clearButton.setOnAction(event -> clearFields());
        }

        // Copy button (if exists)
        if (copyButton != null) {
            copyButton.setOnAction(event -> copyToClipboard());
        }

        // Enable/disable copy button based on translated text
        if (copyButton != null) {
            translatedText.textProperty().addListener((obs, oldText, newText) -> {
                copyButton.setDisable(newText == null || newText.trim().isEmpty());
            });
        }

        // Auto-translate on language change
        sourceLanguage.setOnAction(event -> {
            if (!sourceText.getText().trim().isEmpty()) {
                translateText();
            }
        });

        targetLanguage.setOnAction(event -> {
            if (!sourceText.getText().trim().isEmpty()) {
                translateText();
            }
        });
    }

    private void translateText() {
        String text = sourceText.getText().trim();
        String sourceLang = sourceLanguage.getValue();
        String targetLang = targetLanguage.getValue();

        // Basic validation
        if (text.isEmpty()) {
            updateStatus("Please enter text to translate", true);
            return;
        }

        if (sourceLang == null || targetLang == null) {
            updateStatus("Please select both languages", true);
            return;
        }

        if (sourceLang.equals(targetLang)) {
            updateStatus("Source and target languages must be different", true);
            return;
        }

        // Show loading state
        setLoadingState(true);
        updateStatus("Translating...", false);

        // Get language codes
        String sourceCode = languageCodes.get(sourceLang);
        String targetCode = languageCodes.get(targetLang);

        // Call translation service
        translationService.translateAsync(text, sourceCode, targetCode)
            .whenComplete((result, throwable) -> {
                Platform.runLater(() -> {
                    setLoadingState(false);
                    
                    if (throwable != null) {
                        handleError(throwable);
                    } else if (result != null) {
                        handleSuccess(result);
                    } else {
                        handleError(new RuntimeException("Translation returned null"));
                    }
                });
            });
    }

    private void handleSuccess(TranslationService.TranslationResult result) {
        translatedText.setText(result.getTranslatedText());
        
        String statusMsg = result.isRealTranslation() 
            ? "Translation completed via " + result.getSource()
            : "Mock translation (APIs unavailable) - " + result.getSource();
            
        updateStatus(statusMsg, !result.isRealTranslation());
    }

    private void handleError(Throwable error) {
        translatedText.setText("Translation failed. Please try again.");
        updateStatus("Error: " + error.getMessage(), true);
        System.err.println("Translation error: " + error.getMessage());
    }

    private void swapLanguages() {
        if (swapButton == null) return;
        
        String tempLang = sourceLanguage.getValue();
        String tempText = sourceText.getText();

        sourceLanguage.setValue(targetLanguage.getValue());
        targetLanguage.setValue(tempLang);

        sourceText.setText(translatedText.getText());
        translatedText.setText(tempText);

        updateStatus("Languages swapped", false);
    }

    private void clearFields() {
        if (clearButton == null) return;
        
        sourceText.clear();
        translatedText.clear();
        sourceLanguage.setValue("English");
        targetLanguage.setValue("Spanish");
        updateStatus("Fields cleared", false);
    }

    private void copyToClipboard() {
        if (copyButton == null) return;
        
        String textToCopy = translatedText.getText();
        if (textToCopy != null && !textToCopy.trim().isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(textToCopy);
            Clipboard.getSystemClipboard().setContent(content);
            updateStatus("Copied to clipboard", false);
        }
    }

    private void setLoadingState(boolean loading) {
        translateButton.setDisable(loading);
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(loading);
        }
        
        translateButton.setText(loading ? "Translating..." : "Translate");
        
        if (swapButton != null) swapButton.setDisable(loading);
        sourceLanguage.setDisable(loading);
        targetLanguage.setDisable(loading);
    }

    private void updateStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(isError ? 
                "-fx-text-fill: red;" : 
                "-fx-text-fill: green;");
        }
    }

    // Simple version without TranslationResult - if you prefer this approach
    public void handleSuccessSimple(String result) {
        translatedText.setText(result);
        updateStatus("Translation completed", false);
    }
}