package com.example.translator;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TranslatorApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}