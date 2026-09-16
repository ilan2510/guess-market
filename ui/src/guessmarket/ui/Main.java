package guessmarket.ui;

import guessmarket.engine.EventInfo;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.GuessMarketEngineImpl;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Main extends Application
{

    private static final int SIMULATED_LOAD_DELAY_MS = 1500;
    private static final Duration ANIMATION_DURATION = Duration.seconds(0.5);

    private static final String DARK_THEME_CSS =
            ".root { -fx-background-color: #2b2b2b; } "
            + ".button { -fx-background-color: #444444; -fx-text-fill: #ffffff; } "
            + ".label { -fx-font-size: 13px; -fx-font-family: 'Consolas'; } "
            + ".check-box { -fx-text-fill: #e0e0e0; } "
            + ".choice-box { -fx-background-color: #444444; -fx-mark-color: #e0e0e0; } "
            + ".choice-box .label { -fx-text-fill: #e0e0e0; }";

    private static final String OCEAN_THEME_CSS =
            ".root { -fx-background-color: #d6eaf8; } "
            + ".button { -fx-background-color: #2980b9; -fx-text-fill: #ffffff; } "
            + ".label { -fx-font-size: 13px; -fx-font-family: 'Georgia'; -fx-text-fill: #154360; } "
            + ".check-box { -fx-text-fill: #154360; } "
            + ".choice-box, .choice-box .label { -fx-text-fill: #154360; }";

    private final GuessMarketEngine engine = new GuessMarketEngineImpl();

    private Label filePathLabel;
    private Button loadButton;
    private ProgressBar loadProgressBar;
    private CheckBox animationsCheckBox;
    private ChoiceBox<String> themeChoiceBox;
    private TabPane tabPane;
    private EventsTab eventsTab;
    private UsersTab usersTab;
    private Stage primaryStage;
    private Scene scene;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage)
    {
        this.primaryStage = stage;

        eventsTab = new EventsTab(engine, this);
        usersTab = new UsersTab(engine, this);

        loadButton = new Button("Load File");
        loadButton.setOnAction(event -> handleLoadFile());

        filePathLabel = new Label("No file loaded yet.");

        loadProgressBar = new ProgressBar(0);
        loadProgressBar.setVisible(false);

        animationsCheckBox = new CheckBox("Enable animations");
        animationsCheckBox.setSelected(true);

        themeChoiceBox = new ChoiceBox<>();
        themeChoiceBox.getItems().addAll("Default", "Dark", "Ocean");
        themeChoiceBox.setValue("Default");
        themeChoiceBox.setOnAction(event -> applyTheme(themeChoiceBox.getValue()));

        HBox topBar = new HBox(10, loadButton, filePathLabel, loadProgressBar, animationsCheckBox,
                new Label("Theme:"), themeChoiceBox);
        topBar.setPadding(new Insets(10));

        Tab eventsTabControl = new Tab("Events", eventsTab.getContent());
        eventsTabControl.setClosable(false);
        Tab usersTabControl = new Tab("Users", usersTab.getContent());
        usersTabControl.setClosable(false);

        tabPane = new TabPane(eventsTabControl, usersTabControl);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);

        scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
        stage.setTitle("Guess Market");
        stage.show();
    }

    private void handleLoadFile()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a Guess Market events file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file == null)
        {
            return;
        }

        String path = file.getAbsolutePath();

        Task<List<EventInfo>> loadTask = new Task<List<EventInfo>>()
        {
            @Override
            protected List<EventInfo> call() throws Exception
            {
                Thread.sleep(SIMULATED_LOAD_DELAY_MS);
                return engine.loadFile(path);
            }
        };

        loadTask.setOnRunning(event ->
        {
            loadButton.setDisable(true);
            loadProgressBar.setVisible(true);
            loadProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        });

        loadTask.setOnSucceeded(event ->
        {
            loadButton.setDisable(false);
            loadProgressBar.setVisible(false);
            filePathLabel.setText(path);
            refreshAll();
            fadeIn(tabPane);
        });

        loadTask.setOnFailed(event ->
        {
            loadButton.setDisable(false);
            loadProgressBar.setVisible(false);
            Throwable error = loadTask.getException();
            showError("Could not load file", error.getMessage());
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    void refreshAll()
    {
        eventsTab.refresh();
        usersTab.refresh();
    }

    boolean isAnimationsEnabled()
    {
        return animationsCheckBox.isSelected();
    }

    void fadeIn(Node node)
    {
        if (!isAnimationsEnabled())
        {
            return;
        }
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void applyTheme(String themeName)
    {
        if (themeName.equals("Default"))
        {
            scene.getStylesheets().clear();
            return;
        }

        String css;
        if (themeName.equals("Dark"))
        {
            css = DARK_THEME_CSS;
        }
        else
        {
            css = OCEAN_THEME_CSS;
        }

        try
        {
            File cssFile = File.createTempFile("guessmarket-theme", ".css");
            cssFile.deleteOnExit();
            FileWriter writer = new FileWriter(cssFile);
            writer.write(css);
            writer.close();
            scene.getStylesheets().setAll(cssFile.toURI().toString());
        }
        catch (IOException e)
        {
            showError("Could not apply theme", e.getMessage());
        }
    }

    static void preventFullDeselection(ToggleGroup group)
    {
        group.selectedToggleProperty().addListener((observable, oldToggle, newToggle) ->
        {
            if (newToggle == null && oldToggle != null)
            {
                oldToggle.setSelected(true);
            }
        });
    }

    static String formatStat(Double value)
    {
        if (value == null)
        {
            return "-";
        }
        return String.format("%.2f", value);
    }

    void showError(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    void showInfo(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
