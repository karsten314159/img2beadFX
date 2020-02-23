package img2bead;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;

import com.sun.javafx.collections.*;

import img2bead.NamedColor.*;
import javafx.application.*;
import javafx.beans.value.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.*;

public class App extends Application {
    final int WIN_HEIGHT = 600;
    final int SCROLL_WIDTH = 24;
    final int MENU_WIDTH = 180;

    static String[] cmdLineArgs;

    List<NamedColor> colors = Collections.emptyList();
    String inputName = "";
    String csvName = "";
    ChangeListener<Object> listen;
    double windowWidth = 800;

    BufferedImage input, output;

    HBox root;
    VBox menu;
    Pane img;
    ScrollBar scrollR, scrollG, scrollB;
    Label scrollVal;
    TextField fileInput, csvInput;
    VBox checks;
    ScrollPane scrollPane;
    Button colorResetBtn;
    CheckBox autoSave, includeTransp, availOnly;

    ComboBox<SumMethod> sumMethod;
    ComboBox<DistanceMethod> distanceMethod;

    @Override
    public void start(Stage primaryStage) {
        try {
            String[] split = Program.extractParams(cmdLineArgs);
            if (split != null) {
                inputName = split[0];
                csvName = split[1];
                colors = Program.readColors(csvName);
                loadImage();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        createRootLayout();


        createInputFields();

        listen = createListener();

        scrollVal = new Label();
        menu.getChildren().add(scrollVal);
        scrollR = createColorShifter();
        scrollG = createColorShifter();
        scrollB = createColorShifter();
        colorResetBtn = new Button("reset color shift");
        colorResetBtn.pressedProperty().addListener((a, b, c) -> {
            scrollR.setValue(0);
            scrollG.setValue(0);
            scrollB.setValue(0);
        });
        menu.getChildren().add(colorResetBtn);

        addLabel("distance options:");
        sumMethod = new ComboBox<SumMethod>(new ObservableListWrapper<SumMethod>(
                Arrays.asList(SumMethod.values())));
        sumMethod.setValue(SumMethod.unweighted);
        sumMethod.setOnAction(ev -> redrawImage());
        menu.getChildren().add(sumMethod);

        distanceMethod = new ComboBox<DistanceMethod>(
                new ObservableListWrapper<DistanceMethod>(
                        Arrays.asList(DistanceMethod.values())));
        distanceMethod.setValue(DistanceMethod.quadratic);
        distanceMethod.setOnAction(ev -> redrawImage());
        menu.getChildren().add(distanceMethod);

        createColorSelection();
        redrawImage();

        root.setPrefWidth(800);
        root.setPrefHeight(600);

        primaryStage.setTitle("img2bead");
        primaryStage.setScene(new Scene(root));
        primaryStage.widthProperty().addListener((a, b, c) -> {
            windowWidth = primaryStage.getWidth();
            resizeImage();
        });
        primaryStage.show();
    }

    private void redrawImage() {
        listen.changed(null, null, null);
    }

    private void loadImage() {
        try {
            if (!inputName.isEmpty()) {
                input = ImageIO.read(new File(inputName));
                int w = input.getWidth();
                int h = input.getHeight();
                output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            }
        } catch (Exception e) {
            Program.handleError("reading <" + inputName + ">", e);
        }
    }

    void createInputFields() {
        fileInput = new TextField(inputName);
        fileInput.setOnMouseClicked(x -> {
            if (x.getClickCount() == 2) {
                FileChooser fileChooser = new FileChooser();
                File showOpenDialog = fileChooser.showOpenDialog(null);
                if (showOpenDialog != null) {
                    inputName = showOpenDialog.getAbsolutePath();
                    fileInput.setText(inputName);
                    loadImage();
                    redrawImage();
                }
            }
        });
        addLabel("image path:");
        menu.getChildren().add(fileInput);
        Button fileButton = new Button("load image");
        menu.getChildren().add(fileButton);
        fileButton.pressedProperty().addListener((a, b, newVal) -> {
            if (newVal) {
                inputName = fileInput.getText();
                loadImage();
                redrawImage();
            }
        });
        csvInput = new TextField(csvName);
        csvInput.setOnMouseClicked(x -> {
            if (x.getClickCount() == 2) {
                FileChooser fileChooser = new FileChooser();
                File showOpenDialog = fileChooser.showOpenDialog(null);
                if (showOpenDialog != null) {
                    csvName = showOpenDialog.getAbsolutePath();
                    csvInput.setText(csvName);
                    reloadCsv();
                }
            }
        });
        addLabel("csv path:");
        menu.getChildren().add(csvInput);
        Button csvButton = new Button("load CSV");
        menu.getChildren().add(csvButton);
        csvButton.pressedProperty().addListener((a, b, newVal) -> {
            if (newVal) {
                csvName = csvInput.getText();
                reloadCsv();
            }
        });

        addLabel("options:");
        Button saveButton = new Button("save converted image");
        menu.getChildren().add(saveButton);
        saveButton.pressedProperty().addListener((a, b, newVal) -> {
            saveImage();
        });

        autoSave = new CheckBox("auto save (disk intensive)");
        menu.getChildren().add(autoSave);

    }

    private void reloadCsv() {
        colors = Program.readColors(csvName);
        fillColorSelection();
        redrawImage();
    }

    private void saveImage() {
        if (output != null) {
            File outputFile = new File(inputName + ".bead.png");
            try {
                ImageIO.write(output, "PNG", outputFile);
            } catch (Exception e) {
                Program.handleError("writing <" + outputFile + ">", e);
            }
        }
    }

    ChangeListener<Object> createListener() {
        return (a, b, c) -> {
            int shiftValR = (int) scrollR.getValue();
            int shiftValG = (int) scrollG.getValue();
            int shiftValB = (int) scrollB.getValue();
            scrollVal.setText(
                    "shifted RGB: " + shiftValR + " " + shiftValG + " " + shiftValB);
            try {
                ConversionConfig config = new ConversionConfig(colors,
                        distanceMethod.getValue(), sumMethod.getValue());
                Program.convertFile(config, input, output, shiftValR, shiftValG,
                        shiftValB);
                File outputFile = new File(inputName + ".bead.png");

                ImageIO.write(output, "PNG", outputFile);
                Image image = new Image(new FileInputStream(outputFile), windowWidth,
                        WIN_HEIGHT, true, false);

                if (autoSave.isSelected()) {
                    saveImage();
                }

                BackgroundImage backgroundImage = new BackgroundImage(image,
                        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(100, 100, true, true, true, true));

                img.setBackground(new Background(backgroundImage));
            } catch (Exception e) {
                Program.handleError("writing image", e);
            }
        };
    }

    void createRootLayout() {
        root = new HBox();

        menu = new VBox();
        menu.setPrefWidth(MENU_WIDTH);

        scrollPane = new ScrollPane(menu);
        scrollPane.setPrefWidth(MENU_WIDTH + SCROLL_WIDTH);
        root.getChildren().add(scrollPane);

        img = new Pane();
        resizeImage();
        root.getChildren().add(img);
    }

    void resizeImage() {
        img.setPrefHeight(WIN_HEIGHT);
        img.setPrefWidth(windowWidth - scrollPane.getPrefWidth());
    }

    ScrollBar createColorShifter() {
        ScrollBar scroll = new ScrollBar();

        scroll.setMin(-255);
        scroll.setValue(0);
        scroll.setMax(255);
        scroll.valueProperty().addListener(listen);
        menu.getChildren().add(scroll);
        return scroll;
    }

    void createColorSelection() {
        addLabel("colors:");
        checks = new VBox();
        availOnly = new CheckBox("show available only");
        availOnly.setSelected(true);
        availOnly.setOnAction(x -> {
            fillColorSelection();
            redrawImage();
        });
        includeTransp = new CheckBox("show transp");
        includeTransp.setSelected(true);
        includeTransp.setOnAction(x -> {
            fillColorSelection();
            redrawImage();
        });
        menu.getChildren().add(checks);
        fillColorSelection();
    }

    void addLabel(String text) {
        Label e = new Label(text);
        e.setPadding(new Insets(4, 0, 0, 0));
        menu.getChildren().add(e);
    }

    void fillColorSelection() {
        checks.getChildren().clear();
        checks.getChildren().add(availOnly);
        checks.getChildren().add(includeTransp);
        for (NamedColor color : colors) {
            boolean skipTransp = color.kind != ColorKind.normal
                    && !includeTransp.isSelected();
            boolean skipNotAvail = availOnly.isSelected() && color.count == 0;
            if (skipTransp || skipNotAvail) {
                color.active = false;
            } else {
                color.active = true;
                CheckBox check = new CheckBox();
                check.setSelected(true);
                check.selectedProperty().addListener((a, b, c) -> {
                    color.active = !color.active;
                    listen.changed(a, b, c);
                });
                String format = String.format("%s (%s, %sx)", color.name, color.code,
                        color.count);
                check.setText(format);

                Color rgb = Color.rgb(color.r, color.g, color.b);
                Paint gradient = new LinearGradient(0.0, 0.5, 0.5, 0.5, true,
                        CycleMethod.NO_CYCLE, new Stop(0.33, rgb),
                        new Stop(1, Color.TRANSPARENT));
                BackgroundFill bgFill = new BackgroundFill(gradient, CornerRadii.EMPTY,
                        Insets.EMPTY);
                check.setBackground(new Background(bgFill));
                checks.getChildren().add(check);
            }
        }
    }

    static void main(String[] args) {
        cmdLineArgs = args;
        launch();
    }
}
