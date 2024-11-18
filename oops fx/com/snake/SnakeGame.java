package com.snake;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ParallelTransition;
import javafx.scene.text.Font;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGame extends Application {
    // Constants
    private static final int WIDTH = 20;
    private static final int HEIGHT = 15;
    private static final int TILE_SIZE = 30;
    private static final long GAME_SPEED = 150_000_000; // Lower = faster

    // Game state
    private List<Rectangle> snake;
    private ImageView food;
    private int direction; // 0=up, 1=right, 2=down, 3=left
    private boolean growing;
    private int score;
    private boolean gameOver;
    private AnimationTimer gameLoop;

    // UI elements
    private Label scoreLabel;
    private BorderPane root;
    private Pane gameArea;

    private Font customFont;

    private VBox gameOverScreen;

    // Add these new fields
    private MediaPlayer menuMusic;
    private MediaPlayer gameMusic;
    private MediaPlayer gameOverMusic;
    private MediaPlayer eatSound;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        String css = getClass().getResource("style.css").toExternalForm(); // Load the CSS file
        root.getStylesheets().add(css); // Apply the CSS to the root pane
        setupStartScreen(primaryStage);
    }

    private void setupStartScreen(Stage primaryStage) {
        loadCustomFont();
        loadSounds();
        
        // Stop other music and play menu music
        if (gameMusic != null) gameMusic.stop();
        if (gameOverMusic != null) gameOverMusic.stop();
        if (menuMusic != null) menuMusic.play();
        
        VBox startScreen = new VBox(20);
        startScreen.setAlignment(Pos.CENTER);
        startScreen.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE + 50);
        
        // Add background image to start screen
        try {
            String backgroundUrl = SnakeGame.class.getResource("background.jpg").toExternalForm();
            startScreen.setStyle(
                "-fx-background-image: url('" + backgroundUrl + "');" +
                "-fx-background-size: cover;" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center center;" +
                "-fx-min-height: " + (HEIGHT * TILE_SIZE + 50) + "px;" +
                "-fx-min-width: " + (WIDTH * TILE_SIZE) + "px;"
            );
        } catch (Exception e) {
            System.err.println("Error loading background image: " + e.getMessage());
            e.printStackTrace();
            startScreen.setStyle("-fx-background-color: black;"); // Fallback
        }

        Label titleLabel = new Label("SNAKE GAME");
        titleLabel.setFont(customFont);
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setEffect(new DropShadow(10, Color.BLACK));
        titleLabel.setStyle("-fx-font-size: 48px;");

        Label instructionLabel = new Label("Press 'Start' to play");
        instructionLabel.setFont(Font.font(customFont.getFamily(), 24));
        instructionLabel.setTextFill(Color.WHITE);
        instructionLabel.setEffect(new DropShadow(5, Color.BLACK));
        
        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-family: 'CCOverbyteOff Regular'; -fx-font-size: 18px; -fx-min-width: 150px; -fx-min-height: 40px;");
        startButton.getStyleClass().add("game-button");

        // Set initial positions (off-screen at the top)
        titleLabel.setTranslateY(-200);
        instructionLabel.setTranslateY(-200);
        startButton.setTranslateY(-200);

        // Set initial opacity to 0
        titleLabel.setOpacity(0);
        instructionLabel.setOpacity(0);
        startButton.setOpacity(0);

        // Create animations for title
        FadeTransition titleFade = new FadeTransition(Duration.seconds(1), titleLabel);
        titleFade.setFromValue(0);
        titleFade.setToValue(1);

        TranslateTransition titleSlide = new TranslateTransition(Duration.seconds(1), titleLabel);
        titleSlide.setFromY(-200);
        titleSlide.setToY(0);

        // Create animations for instruction label
        FadeTransition instructionFade = new FadeTransition(Duration.seconds(1), instructionLabel);
        instructionFade.setFromValue(0);
        instructionFade.setToValue(1);
        
        TranslateTransition instructionSlide = new TranslateTransition(Duration.seconds(1), instructionLabel);
        instructionSlide.setFromY(-200);
        instructionSlide.setToY(0);

        // Create animations for button
        FadeTransition buttonFade = new FadeTransition(Duration.seconds(1), startButton);
        buttonFade.setFromValue(0);
        buttonFade.setToValue(1);
        
        TranslateTransition buttonSlide = new TranslateTransition(Duration.seconds(1), startButton);
        buttonSlide.setFromY(-200);
        buttonSlide.setToY(0);

        // Combine animations
        ParallelTransition titleAnimation = new ParallelTransition(titleFade, titleSlide);
        ParallelTransition instructionAnimation = new ParallelTransition(instructionFade, instructionSlide);
        ParallelTransition buttonAnimation = new ParallelTransition(buttonFade, buttonSlide);

        // Play animations in sequence with small delays
        SequentialTransition sequence = new SequentialTransition(
            new javafx.animation.PauseTransition(Duration.seconds(0.2)), // Initial pause
            titleAnimation,
            new javafx.animation.PauseTransition(Duration.seconds(0.2)), // Pause between animations
            instructionAnimation,
            new javafx.animation.PauseTransition(Duration.seconds(0.2)), // Pause between animations
            buttonAnimation
        );

        startScreen.getChildren().addAll(titleLabel, instructionLabel, startButton);
        
        Scene startScene = new Scene(startScreen, WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE + 50);
        startScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        
        primaryStage.setScene(startScene);
        primaryStage.setResizable(false);
        primaryStage.show();
        sequence.play(); // Play the animation sequence after showing the stage

        startButton.setOnAction(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), startScreen);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                initializeGame();
                setupGameUI(primaryStage);
                startGameLoop();
            });
            fadeOut.play();
        });
    }

    private void initializeGame() {
        // Stop menu music and play game music
        if (menuMusic != null) menuMusic.stop();
        if (gameOverMusic != null) gameOverMusic.stop();
        if (gameMusic != null) gameMusic.play();
        
        snake = new ArrayList<>();
        direction = 1; // Start moving right
        growing = false;
        score = 0;
        gameOver = false;
        gameArea = new Pane();
        gameArea.setPrefSize(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE);
        
        // Add background image to game area
        try {
            String backgroundUrl = SnakeGame.class.getResource("green-grass-mat-background.jpg").toExternalForm();
            gameArea.setStyle(
                "-fx-background-image: url('" + backgroundUrl + "');" +
                "-fx-background-size: cover;" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center center;"
            );
        } catch (Exception e) {
            System.err.println("Error loading game background image: " + e.getMessage());
            e.printStackTrace();
            gameArea.setStyle("-fx-background-color: black;"); // Fallback to black if image fails to load
        }
        
        // Initialize food as ImageView instead of Rectangle
        try {
            Image appleImage = new Image(getClass().getResourceAsStream("/com/snake/apple.png"));
            food = new ImageView(appleImage);
            food.setFitWidth(TILE_SIZE);
            food.setFitHeight(TILE_SIZE);
        } catch (Exception e) {
            System.err.println("Error loading apple image: " + e.getMessage());
            // Fallback to red rectangle if image fails to load
            Rectangle fallbackFood = new Rectangle(TILE_SIZE, TILE_SIZE);
            fallbackFood.setFill(Color.RED);
            food = new ImageView();
        }
    }

    private void setupGameUI(Stage primaryStage) {
        // Create UI elements
        root = new BorderPane();
        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-font-family: 'CCOverbyteOff Regular'; -fx-font-size: 20px; -fx-padding: 10;");

        // Setup game area
        root.setTop(scoreLabel);
        root.setCenter(gameArea);

        // Load CSS file
        String css = getClass().getResource("style.css").toExternalForm(); // Load the CSS file
        root.getStylesheets().add(css); // Apply the CSS to the root pane

        // Initialize snake
        Rectangle head = new Rectangle(TILE_SIZE, TILE_SIZE);
        head.setFill(Color.GREEN);
        head.setTranslateX(WIDTH / 2 * TILE_SIZE);
        head.setTranslateY(HEIGHT / 2 * TILE_SIZE);
        snake.add(head);
        gameArea.getChildren().add(head);

        // Initialize food
        gameArea.getChildren().add(food);
        spawnFood();

        // Create scene
        Scene scene = new Scene(root, WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE + 50);
        setupKeyHandling(scene);

        // Configure stage
        primaryStage.setTitle("Snake Game");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void setupKeyHandling(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (gameOver) return;
            switch (event.getCode()) {
                case UP:    if (direction != 2) direction = 0; break;
                case RIGHT: if (direction != 3) direction = 1; break;
                case DOWN:  if (direction != 0) direction = 2; break;
                case LEFT:  if (direction != 1) direction = 3; break;
            }
        });
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= GAME_SPEED) {
                    updateGame();
                    lastUpdate = now;
                }
            }
        };
        gameLoop.start();
    }

    private void updateGame() {
        if (gameOver) return;

        // Store current head position
        double oldX = snake.get(0).getTranslateX();
        double oldY = snake.get(0).getTranslateY();

        // Move head
        switch (direction) {
            case 0: snake.get(0).setTranslateY(oldY - TILE_SIZE); break; // Up
            case 1: snake.get(0).setTranslateX(oldX + TILE_SIZE); break; // Right
            case 2: snake.get(0).setTranslateY(oldY + TILE_SIZE); break; // Down
            case 3: snake.get(0).setTranslateX(oldX - TILE_SIZE); break; // Left
        }

        // Check collisions
        if (checkCollision()) {
            gameOver = true;
            showGameOverScreen();
            return;
        }

        // Check food collision
        if (snake.get(0).getTranslateX() == food.getTranslateX() &&
            snake.get(0).getTranslateY() == food.getTranslateY()) {
            growing = true;
            score += 10;
            scoreLabel.setText("Score: " + score);
            spawnFood();
            
            // Play eat sound effect
            if (eatSound != null) {
                eatSound.stop();
                eatSound.play();
            }
        }

        // Move body
        for (int i = snake.size() - 1; i > 0; i--) {
            snake.get(i).setTranslateX(snake.get(i - 1).getTranslateX());
            snake.get(i).setTranslateY(snake.get(i - 1).getTranslateY());
        }

        // Grow snake
        if (growing) {
            Rectangle newSegment = new Rectangle(TILE_SIZE, TILE_SIZE);
            newSegment.setFill(Color.PURPLE);
            newSegment.setTranslateX(oldX);
            newSegment.setTranslateY(oldY);
            snake.add(newSegment);
            gameArea.getChildren().add(newSegment);
            growing = false;
        }
    }

    private boolean checkCollision() {
        double headX = snake.get(0).getTranslateX() / TILE_SIZE;
        double headY = snake.get(0).getTranslateY() / TILE_SIZE;

        // Wall collision
        if (headX < 0 || headX >= WIDTH || headY < 0 || headY >= HEIGHT) {
            return true;
        }

        // Self collision
        for (int i = 1; i < snake.size(); i++) {
            if (snake.get(0).getTranslateX() == snake.get(i).getTranslateX() &&
                snake.get(0).getTranslateY() == snake.get(i).getTranslateY()) {
                return true;
            }
        }
        return false;
    }

    private void spawnFood() {
        Random random = new Random();
        int x, y;
        do {
            x = random.nextInt(WIDTH);
            y = random.nextInt(HEIGHT);
        } while (isSnakePosition(x, y));

        food.setTranslateX(x * TILE_SIZE);
        food.setTranslateY(y * TILE_SIZE);
    }

    private boolean isSnakePosition(int x, int y) {
        for (Rectangle segment : snake) {
            if (segment.getTranslateX() / TILE_SIZE == x &&
                segment.getTranslateY() / TILE_SIZE == y) {
                return true;
            }
        }
        return false;
    }

    private void showGameOverScreen() {
        // Stop game music and play game over music
        if (gameMusic != null) gameMusic.stop();
        if (gameOverMusic != null) gameOverMusic.play();
        
        gameOverScreen = new VBox(20); // Increased spacing between elements
        gameOverScreen.setAlignment(Pos.CENTER);

        // Set the Doom wallpaper background using absolute path for testing
        try {
            String backgroundUrl = getClass().getResource("/com/snake/doom__wallpaper_hd___cutout__by_novaclip43_dcm5zhf.png").toExternalForm();
            gameOverScreen.setStyle(
                "-fx-background-image: url('" + backgroundUrl + "');" +
                "-fx-background-size: cover;" +
                "-fx-background-repeat: no-repeat;" +
                "-fx-background-position: center center;" +
                "-fx-min-height: " + (HEIGHT * TILE_SIZE + 50) + "px;" +
                "-fx-min-width: " + (WIDTH * TILE_SIZE) + "px;"
            );
            System.out.println("Background URL: " + backgroundUrl); // Debug print
        } catch (Exception e) {
            System.err.println("Error loading game over background image: " + e.getMessage());
            e.printStackTrace();
            gameOverScreen.setStyle("-fx-background-color: black;"); // Fallback to black if image fails to load
        }

        // Game Over Label
        Label gameOverLabel = new Label("GAME OVER");
        gameOverLabel.setFont(customFont);
        gameOverLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #ff0000;");
        gameOverLabel.setEffect(new DropShadow(10, Color.BLACK));

        // Score Label
        Label finalScoreLabel = new Label("Final Score: " + score);
        finalScoreLabel.setFont(customFont);
        finalScoreLabel.setStyle("-fx-text-fill: white;");
        finalScoreLabel.setEffect(new DropShadow(5, Color.BLACK));

        // Buttons VBox
        VBox buttonBox = new VBox(15); // Increased spacing between buttons
        buttonBox.setAlignment(Pos.CENTER);

        // Play Again Button
        Button playAgainButton = new Button("Play Again");
        playAgainButton.setFont(customFont);
        playAgainButton.getStyleClass().add("game-button");
        playAgainButton.setMinWidth(200);
        playAgainButton.setMinHeight(50);
        playAgainButton.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white;");
        
        // Add hover animation
        playAgainButton.setOnMouseEntered(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), playAgainButton);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.play();
        });
        
        playAgainButton.setOnMouseExited(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), playAgainButton);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.play();
        });

        // Main Menu Button
        Button mainMenuButton = new Button("Main Menu");
        mainMenuButton.setFont(customFont);
        mainMenuButton.getStyleClass().add("game-button");
        mainMenuButton.setMinWidth(200);
        mainMenuButton.setMinHeight(50);
        mainMenuButton.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white;");
        
        // Add hover animation
        mainMenuButton.setOnMouseEntered(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), mainMenuButton);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.play();
        });
        
        mainMenuButton.setOnMouseExited(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), mainMenuButton);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.play();
        });

        // Add click effect
        playAgainButton.setOnMousePressed(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), playAgainButton);
            scaleTransition.setToX(0.9);
            scaleTransition.setToY(0.9);
            scaleTransition.play();
        });

        playAgainButton.setOnMouseReleased(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), playAgainButton);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.play();
        });

        mainMenuButton.setOnMousePressed(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), mainMenuButton);
            scaleTransition.setToX(0.9);
            scaleTransition.setToY(0.9);
            scaleTransition.play();
        });

        mainMenuButton.setOnMouseReleased(e -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), mainMenuButton);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.play();
        });

        // Set button actions
        playAgainButton.setOnAction(e -> restartGame());
        mainMenuButton.setOnAction(e -> {
            Stage primaryStage = (Stage) gameOverScreen.getScene().getWindow();
            setupStartScreen(primaryStage);
        });

        // Add buttons to button box
        buttonBox.getChildren().addAll(playAgainButton, mainMenuButton);

        // Add all elements to game over screen
        gameOverScreen.getChildren().addAll(gameOverLabel, finalScoreLabel, buttonBox);
        root.setCenter(gameOverScreen);

        // Create fade animations
        FadeTransition fadeGameOver = new FadeTransition(Duration.seconds(1), gameOverLabel);
        fadeGameOver.setFromValue(0);
        fadeGameOver.setToValue(1);

        FadeTransition fadeScore = new FadeTransition(Duration.seconds(1), finalScoreLabel);
        fadeScore.setFromValue(0);
        fadeScore.setToValue(1);

        FadeTransition fadeButtons = new FadeTransition(Duration.seconds(1), buttonBox);
        fadeButtons.setFromValue(0);
        fadeButtons.setToValue(1);

        // Create slide animations
        TranslateTransition slideGameOver = new TranslateTransition(Duration.seconds(1), gameOverLabel);
        slideGameOver.setFromY(-50);
        slideGameOver.setToY(0);

        TranslateTransition slideScore = new TranslateTransition(Duration.seconds(1), finalScoreLabel);
        slideScore.setFromY(-50);
        slideScore.setToY(0);

        TranslateTransition slideButtons = new TranslateTransition(Duration.seconds(1), buttonBox);
        slideButtons.setFromY(-50);
        slideButtons.setToY(0);

        // Create parallel transitions for each element
        ParallelTransition gameOverAnim = new ParallelTransition(fadeGameOver, slideGameOver);
        ParallelTransition scoreAnim = new ParallelTransition(fadeScore, slideScore);
        ParallelTransition buttonsAnim = new ParallelTransition(fadeButtons, slideButtons);

        // Play animations in sequence
        SequentialTransition sequence = new SequentialTransition(
            new javafx.animation.PauseTransition(Duration.seconds(0.5)), // Initial delay
            gameOverAnim,
            new javafx.animation.PauseTransition(Duration.seconds(0.3)), // Delay between animations
            scoreAnim,
            new javafx.animation.PauseTransition(Duration.seconds(0.3)), // Delay between animations
            buttonsAnim
        );

        sequence.play();
    }

    private void restartGame() {
        // Stop game over music and play game music
        if (gameOverMusic != null) gameOverMusic.stop();
        if (gameMusic != null) gameMusic.play();
        
        // Stop the existing game loop if it's running
        if (gameLoop != null) {
            gameLoop.stop();
        }

        snake.clear();
        gameArea.getChildren().clear();
        
        initializeGame();
        gameArea = new Pane();
        root.setCenter(gameArea);
        
        // Reinitialize snake
        Rectangle head = new Rectangle(TILE_SIZE, TILE_SIZE);
        head.setFill(Color.PURPLE);
        head.setTranslateX(WIDTH / 2 * TILE_SIZE);
        head.setTranslateY(HEIGHT / 2 * TILE_SIZE);
        snake.add(head);
        gameArea.getChildren().add(head);
        
        // Reinitialize food
        try {
            Image appleImage = new Image(getClass().getResourceAsStream("/com/snake/apple.png"));
            food = new ImageView(appleImage);
            food.setFitWidth(TILE_SIZE);
            food.setFitHeight(TILE_SIZE);
        } catch (Exception e) {
            System.err.println("Error loading apple image: " + e.getMessage());
            // Fallback to red rectangle if image fails to load
            Rectangle fallbackFood = new Rectangle(TILE_SIZE, TILE_SIZE);
            fallbackFood.setFill(Color.RED);
            food = new ImageView();
        }
        gameArea.getChildren().add(food);
        spawnFood();
        
        scoreLabel.setText("Score: 0");
        startGameLoop(); // Start the game loop again
    }

    private void loadCustomFont() {
        try {
            customFont = Font.loadFont(getClass().getResourceAsStream("/com/snake/ccoverbyteoff-regular.ttf"), 40);
            if (customFont != null) {
                System.out.println("Font loaded successfully: " + customFont.getFamily());
            } else {
                System.err.println("Failed to load font");
            }
        } catch (Exception e) {
            System.err.println("Error loading font: " + e.getMessage());
            e.printStackTrace();
        }

        // Add this to your loadCustomFont method
        System.out.println("Available fonts:");
        Font.getFamilies().forEach(System.out::println);
    }

    private void loadSounds() {
        try {
            // Load menu music with explicit error handling for each file
            String menuMusicPath = getClass().getResource("/com/snake/menu-music.mp3").toExternalForm();
            System.out.println("Loading menu music from: " + menuMusicPath); // Debug print
            Media menuMusicFile = new Media(menuMusicPath);
            menuMusic = new MediaPlayer(menuMusicFile);
            menuMusic.setCycleCount(MediaPlayer.INDEFINITE);
            menuMusic.setVolume(0.5);
            
            // Load game music
            String gameMusicPath = getClass().getResource("/com/snake/game-music.mp3").toExternalForm();
            System.out.println("Loading game music from: " + gameMusicPath); // Debug print
            Media gameMusicFile = new Media(gameMusicPath);
            gameMusic = new MediaPlayer(gameMusicFile);
            gameMusic.setCycleCount(MediaPlayer.INDEFINITE);
            gameMusic.setVolume(0.3);

            // Load game over music
            String gameOverMusicPath = getClass().getResource("/com/snake/game-over-music.mp3").toExternalForm();
            System.out.println("Loading game over music from: " + gameOverMusicPath); // Debug print
            Media gameOverMusicFile = new Media(gameOverMusicPath);
            gameOverMusic = new MediaPlayer(gameOverMusicFile);
            gameOverMusic.setVolume(0.5);

            // Load eat sound effect
            String eatSoundPath = getClass().getResource("/com/snake/eat-sound.mp3").toExternalForm();
            System.out.println("Loading eat sound from: " + eatSoundPath); // Debug print
            Media eatSoundFile = new Media(eatSoundPath);
            eatSound = new MediaPlayer(eatSoundFile);
            eatSound.setVolume(0.4);
            
        } catch (Exception e) {
            System.err.println("Error loading sound files: " + e.getMessage());
            e.printStackTrace();
            // Initialize players as null so the game can still run without sound
            menuMusic = null;
            gameMusic = null;
            gameOverMusic = null;
            eatSound = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
