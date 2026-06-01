package elevatorsim.tools;

import elevatorsim.ElevatorSimulationApp;
import elevatorsim.ui.SimulationController;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WalkthroughRecorder extends Application {
    private static final int OVERVIEW_HOLD_FRAMES = 10;
    private static final int SAME_DIRECTION_FRAMES = 28;
    private static final int OPPOSITE_DIRECTION_FRAMES = 32;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ElevatorSimulationApp.class.getResource("/elevatorsim/ui/main-view.fxml"));
        Parent root = loader.load();
        SimulationController controller = loader.getController();

        Scene scene = new Scene(root, 1180, 760);
        stage.setTitle("Elevator Simulation Asset Capture");
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            try {
                scheduleAssetCapture(root, controller);
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.exit();
            }
        });
    }

    private void scheduleAssetCapture(Parent root, SimulationController controller) throws IOException {
        Path mediaDirectory = Path.of("docs", "media").toAbsolutePath().normalize();
        Files.createDirectories(mediaDirectory);
        cleanGeneratedFrames(mediaDirectory);

        List<CaptureStep> steps = new ArrayList<>();
        steps.add(new CaptureStep(controller::prepareOverviewForCapture,
                mediaDirectory.resolve("screenshot-overview.png")));
        steps.add(new CaptureStep(controller::prepareSameDirectionForCapture,
                mediaDirectory.resolve("screenshot-same-direction.png")));
        steps.add(new CaptureStep(controller::prepareOppositeDirectionForCapture,
                mediaDirectory.resolve("screenshot-opposite-direction.png")));

        int frameNumber = 1;
        steps.add(new CaptureStep(controller::prepareOverviewForCapture, framePath(mediaDirectory, frameNumber++)));
        for (int i = 1; i < OVERVIEW_HOLD_FRAMES; i++) {
            steps.add(new CaptureStep(() -> { }, framePath(mediaDirectory, frameNumber++)));
        }

        steps.add(new CaptureStep(() -> controller.prepareWalkthroughScenarioForCapture(true),
                framePath(mediaDirectory, frameNumber++)));
        for (int i = 1; i < SAME_DIRECTION_FRAMES; i++) {
            steps.add(new CaptureStep(() -> controller.advanceForCapture(1), framePath(mediaDirectory, frameNumber++)));
        }

        steps.add(new CaptureStep(() -> controller.prepareWalkthroughScenarioForCapture(false),
                framePath(mediaDirectory, frameNumber++)));
        for (int i = 1; i < OPPOSITE_DIRECTION_FRAMES; i++) {
            steps.add(new CaptureStep(() -> controller.advanceForCapture(1), framePath(mediaDirectory, frameNumber++)));
        }

        runCaptureStep(root, steps, 0, mediaDirectory);
    }

    private void runCaptureStep(Parent root, List<CaptureStep> steps, int index, Path mediaDirectory) {
        if (index >= steps.size()) {
            System.out.println("Captured screenshots and walkthrough frames in " + mediaDirectory);
            Platform.exit();
            return;
        }

        CaptureStep step = steps.get(index);
        step.action().run();
        PauseTransition pause = new PauseTransition(Duration.millis(70));
        pause.setOnFinished(event -> {
            try {
                capture(root, step.outputPath());
                runCaptureStep(root, steps, index + 1, mediaDirectory);
            } catch (IOException ex) {
                ex.printStackTrace();
                Platform.exit();
            }
        });
        pause.play();
    }

    private void cleanGeneratedFrames(Path mediaDirectory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mediaDirectory, "frame-*.png")) {
            for (Path frame : stream) {
                Files.deleteIfExists(frame);
            }
        }
    }

    private Path framePath(Path mediaDirectory, int frameNumber) {
        return mediaDirectory.resolve(String.format("frame-%03d.png", frameNumber));
    }

    private void capture(Parent root, Path outputPath) throws IOException {
        root.applyCss();
        root.layout();
        WritableImage image = root.snapshot(new SnapshotParameters(), null);
        ImageIO.write(toBufferedImage(image), "png", outputPath.toFile());
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }
        return bufferedImage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record CaptureStep(Runnable action, Path outputPath) {
    }
}
