package com.steviecodesit.ourhomedev.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() throws IOException {
        String firebaseCredentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH");

        if (firebaseCredentialsPath == null) {
            throw new IOException("FIREBASE_CREDENTIALS_PATH environment variable is not set");
        }

        FileInputStream serviceAccount =
                new FileInputStream(firebaseCredentialsPath);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
    }
}
