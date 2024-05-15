// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.gui.imageinfo;

import java.io.Serial;

import org.openstreetmap.josm.plugins.streetside.cubemap.CameraTransformer;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapBox;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Create a panel for viewing cube mapped 360 iamges
 */
public class ThreeSixtyDegreeViewerPanel extends JFXPanel {

    @Serial
    private static final long serialVersionUID = -7032369684012156320L;
    private static final CameraTransformer cameraTransform = new CameraTransformer();
    private static final double CAMERA_DISTANCE = 5000;
    private static Scene cubemapScene;
    private static Scene defaultScene;
    private static Scene loadingScene;
    private static Group root;
    private static Group subGroup;
    private static CubemapBox cubemapBox;
    private static PerspectiveCamera camera;
    private static double mousePosX;
    private static double mousePosY;
    private static double mouseOldX;
    private static double mouseOldY;

    /**
     * Create the default scene
     * @return The default scene (pretty much to tell the user that nothing is selected)
     */
    private static Scene createDefaultScene() {

        final var textArea = new TextArea();
        textArea.setText("No Streetside image selected.");

        final var vbox = new VBox(textArea);

        initializeStatic();

        cubemapScene = new Scene(new Group(root), 1024, 668, true, SceneAntialiasing.BALANCED);
        cubemapScene.setFill(Color.TRANSPARENT);
        cubemapScene.setCamera(camera);

        cubemapScene.setOnKeyPressed(ThreeSixtyDegreeViewerPanel::keyPressed);
        cubemapScene.setOnMousePressed(ThreeSixtyDegreeViewerPanel::mouseClicked);
        cubemapScene.setOnMouseDragged(ThreeSixtyDegreeViewerPanel::mouseDragged);

        root.getChildren().addAll(cubemapBox, subGroup);
        root.setAutoSizeChildren(true);

        subGroup.setAutoSizeChildren(true);

        // prevent content from disappearing after resizing
        Platform.setImplicitExit(false);

        defaultScene = new Scene(vbox, 200, 100);
        return defaultScene;
    }

    private static void keyPressed(KeyEvent event) {
        var change = 10.0;
        if (event.isShiftDown()) {
            change = 50.0;
        }
        final var keycode = event.getCode();

        if (keycode == KeyCode.W) {
            camera.setTranslateZ(camera.getTranslateZ() + change);
        }
        if (keycode == KeyCode.S) {
            camera.setTranslateZ(camera.getTranslateZ() - change);
        }

        if (keycode == KeyCode.A) {
            camera.setTranslateX(camera.getTranslateX() - change);
        }
        if (keycode == KeyCode.D) {
            camera.setTranslateX(camera.getTranslateX() + change);
        }
    }

    private static void mouseClicked(MouseEvent me) {
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseOldX = me.getSceneX();
        mouseOldY = me.getSceneY();
    }

    private static void mouseDragged(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        final double mouseDeltaX = mousePosX - mouseOldX;
        final double mouseDeltaY = mousePosY - mouseOldY;

        var modifier = 0.375;
        final var modifierFactor = 0.1;

        if (me.isControlDown()) {
            modifier = 0.1;
        }
        if (me.isShiftDown()) {
            modifier = 50.0;
        }
        if (me.isSecondaryButtonDown()) { // JOSM viewer uses right-click for moving.
            cameraTransform.setRy(
                    ((cameraTransform.ry.getAngle() - mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360
                            - 180); // +
            cameraTransform.setRx(
                    ((cameraTransform.rx.getAngle() + mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360
                            - 180); // -
        } else if (me.isPrimaryButtonDown()) {
            final double z = camera.getTranslateZ();
            final double newZ = z + mouseDeltaX * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.setTx(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.setTy(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }
    }

    private static void createLoadingScene() {
        final var label = new Label(" Loading...");
        label.setFont(Font.font(null, FontWeight.BOLD, 14));
        final var vbox = new VBox(label);
        loadingScene = new Scene(vbox, 200, 100);
    }

    private static void initializeStatic() {
        root = new Group();

        camera = new PerspectiveCamera(true);
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().addAll(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(1000000.0);
        camera.setFieldOfView(42);
        camera.setTranslateZ(-CAMERA_DISTANCE);
        final var light = new PointLight(Color.WHITE);

        cameraTransform.getChildren().add(light);
        light.setTranslateX(camera.getTranslateX());
        light.setTranslateY(camera.getTranslateY());
        light.setTranslateZ(camera.getTranslateZ());

        root.getChildren().add(cameraTransform);

        cubemapBox = new CubemapBox(null, null, null, null, null, null, 100_000d, camera);

        subGroup = new Group();
        subGroup.getChildren().add(cameraTransform);
    }

    void initialize() {
        initializeStatic();
        createLoadingScene();
        Platform.runLater(() -> setScene(createDefaultScene()));
    }

    public CubemapBox getCubemapBox() {
        if (cubemapBox == null) {
            // shouldn't happen
            initialize();
        }
        return cubemapBox;
    }

    public Scene getDefaultScene() {
        return defaultScene;
    }

    public Scene getCubemapScene() {
        return cubemapScene;
    }

    public Scene getLoadingScene() {
        return loadingScene;
    }
}
