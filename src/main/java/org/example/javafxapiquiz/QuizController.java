package org.example.javafxapiquiz;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebView;

public class QuizController {

    @FXML
    private WebView webView;
    @FXML
    private Button submitButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label resultsLabel;

    private GeminiGrader grader;

    @FXML
    public void initialize() {
        grader = new GeminiGrader();
        String url = getClass().getResource("/quiz.html").toExternalForm();
        webView.getEngine().load(url);
    }

    @FXML
    private void handleSubmitButtonAction() {
        setUiForGrading(true);
        resultsLabel.setText("Grading in progress... Contacting Gemini API...");

        String script = """
            var answers = {};
            var inputs = document.querySelectorAll('#quizForm input[type="radio"]:checked');
            inputs.forEach(function(input) {
                answers[input.name] = input.value;
            });
            JSON.stringify(answers);
            """;

        webView.getEngine().executeScript(script, userAnswersJson -> {
            if (userAnswersJson == null || userAnswersJson.toString().equals("{}")) {
                updateResults("Please answer at least one question before submitting.");
                setUiForGrading(false);
                return;
            }

            grader.gradeQuiz(userAnswersJson.toString()).whenComplete((grade, throwable) -> {
                Platform.runLater(() -> {
                    if (throwable != null) {
                        updateResults("An error occurred: " + throwable.getMessage());
                    } else {
                        updateResults(grade);
                    }
                    setUiForGrading(false);
                });
            });
        });
    }

    private void setUiForGrading(boolean isGrading) {
        submitButton.setDisable(isGrading);
        progressIndicator.setVisible(isGrading);
    }

    private void updateResults(String text) {
        resultsLabel.setText(text);
    }
}