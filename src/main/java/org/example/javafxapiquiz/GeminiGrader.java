package org.example.javafxapiquiz;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiGrader {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final GenerativeModelFutures model;

    public GeminiGrader() {
        String apiKey = loadApiKey();
        if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            System.err.println("API Key not found or not replaced in config.properties.");
            throw new IllegalStateException("API Key is missing. Please check your config.properties file.");
        }

        GenerativeModel generativeModel = new GenerativeModel("gemini-1.5-flash", apiKey);
        this.model = GenerativeModelFutures.from(generativeModel);
    }

    private String loadApiKey() {
        try (InputStream input = GeminiGrader.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                return null;
            }
            prop.load(input);
            return prop.getProperty("api.key");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static final String CORRECT_ANSWERS = """
        1. b (final)
        2. b (4 bytes)
        3. c (friendly is default access, not a keyword)
        4. c (Object)
        5. b (List)
        6. c (Belongs to the class, not an instance)
        7. a (try-catch block)
        8. b (An immutable class)
        9. d (java command)
        10. b (The main() method)
        """;

    public CompletableFuture<String> gradeQuiz(String userAnswersJson) {
        CompletableFuture<String> futureResult = new CompletableFuture<>();
        Content prompt = Content.newBuilder()
                .addText(buildPrompt(userAnswersJson))
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);

        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                futureResult.complete(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                futureResult.complete("Error: Could not get grade from AI. " + t.getMessage());
            }
        }, executor);

        return futureResult;
    }

    private String buildPrompt(String userAnswersJson) {
        return "You are an automated quiz grader for a Java quiz. " +
                "Your response MUST be formatted in simple text or markdown, suitable for displaying in a basic text area. " +
                "Do NOT use HTML in your response.\n\n" +
                "Here are the correct answers for reference:\n" +
                "--- CORRECT ANSWERS ---\n" + CORRECT_ANSWERS + "\n\n" +
                "Here are the user's submitted answers in JSON format:\n" +
                "--- USER'S ANSWERS ---\n" + userAnswersJson + "\n\n" +
                "--- YOUR TASK ---\n" +
                "1. Go through each question one by one.\n" +
                "2. State if the user's answer was 'Correct' or 'Incorrect'.\n" +
                "3. If incorrect, briefly state the correct answer.\n" +
                "4. At the very end, provide a total score in the format 'Total Score: X/10'.\n" +
                "5. Finally, add a single, short, encouraging feedback sentence for the user.";
    }
}