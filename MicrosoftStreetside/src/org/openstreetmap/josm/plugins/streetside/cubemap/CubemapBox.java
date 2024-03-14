// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import org.openstreetmap.josm.plugins.streetside.utils.GraphicsUtils;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

/**
 * A box for showing the cubemap images
 * @author renerr18
 */
public class CubemapBox extends Group {

    private final Affine affine = new Affine();
    private final ImageView front = new ImageView();
    private final ImageView right = new ImageView();
    private final ImageView back = new ImageView();
    private final ImageView left = new ImageView();
    private final ImageView up = new ImageView();
    private final ImageView down = new ImageView();
    private final ImageView[] views = new ImageView[] { front, right, back, left, up, down };
    private final Image frontImg;
    private final Image rightImg;
    private final Image backImg;
    private final Image leftImg;
    private final Image upImg;
    private final Image downImg;
    private final PerspectiveCamera camera;

    /**
     * Create a new CubemapBox
     * @param frontImg The front image
     * @param rightImg The right image
     * @param backImg The back image
     * @param leftImg The left image
     * @param upImg The up image
     * @param downImg The down image
     * @param size The size of each cube side
     * @param camera The camera to use
     */
    public CubemapBox(Image frontImg, Image rightImg, Image backImg, Image leftImg, Image upImg, Image downImg,
            double size, PerspectiveCamera camera) {
        super();

        this.front.setId(CubemapUtils.CubemapFaces.FRONT.getValue());
        this.right.setId(CubemapUtils.CubemapFaces.RIGHT.getValue());
        this.back.setId(CubemapUtils.CubemapFaces.BACK.getValue());
        this.left.setId(CubemapUtils.CubemapFaces.LEFT.getValue());
        this.up.setId(CubemapUtils.CubemapFaces.UP.getValue());
        this.down.setId(CubemapUtils.CubemapFaces.DOWN.getValue());

        this.frontImg = frontImg;
        this.rightImg = rightImg;
        this.backImg = backImg;
        this.leftImg = leftImg;
        this.upImg = upImg;
        this.downImg = downImg;
        this.size.set(size);
        this.camera = camera;

        loadImageViews();

        getTransforms().add(affine);

        getChildren().addAll(views);

        startTimer();
    }

    /**
     * Load the image views
     */
    public void loadImageViews() {

        for (ImageView iv : views) {
            iv.setSmooth(true);
            iv.setPreserveRatio(true);
        }

        validateImageType();
    }

    private void layoutViews() {

        for (ImageView v : views) {
            v.setFitWidth(getSize());
            v.setFitHeight(getSize());
        }

        back.setTranslateX(-0.5 * getSize());
        back.setTranslateY(-0.5 * getSize());
        back.setTranslateZ(-0.5 * getSize());

        front.setTranslateX(-0.5 * getSize());
        front.setTranslateY(-0.5 * getSize());
        front.setTranslateZ(0.5 * getSize());
        front.setRotationAxis(Rotate.Z_AXIS);
        front.setRotate(-180);
        front.getTransforms().add(new Rotate(180, front.getFitHeight() / 2, 0, 0, Rotate.X_AXIS));
        front.setTranslateY(front.getTranslateY() - getSize());

        up.setTranslateX(-0.5 * getSize());
        up.setTranslateY(-1 * getSize());
        up.setRotationAxis(Rotate.X_AXIS);
        up.setRotate(-90);

        down.setTranslateX(-0.5 * getSize());
        down.setTranslateY(0);
        down.setRotationAxis(Rotate.X_AXIS);
        down.setRotate(90);

        left.setTranslateX(-1 * getSize());
        left.setTranslateY(-0.5 * getSize());
        left.setRotationAxis(Rotate.Y_AXIS);
        left.setRotate(90);

        right.setTranslateX(0);
        right.setTranslateY(-0.5 * getSize());
        right.setRotationAxis(Rotate.Y_AXIS);
        right.setRotate(-90);
    }

    /**
     * for single image creates viewports and sets all views(image) to singleImg for multiple... sets images per view.
     */
    private void validateImageType() {
        setMultipleImages();
    }

    private void setMultipleImages() {
        GraphicsUtils.PlatformHelper.run(() -> {
            layoutViews();
            front.setImage(frontImg);
            right.setImage(rightImg);
            back.setImage(backImg);
            left.setImage(leftImg);
            up.setImage(upImg);
            down.setImage(downImg);

        });
    }

    /**
     * Start the UI timer for updates
     */
    public void startTimer() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Transform ct = (camera != null) ? camera.getLocalToSceneTransform() : null;
                if (ct != null) {
                    affine.setTx(ct.getTx());
                    affine.setTy(ct.getTy());
                    affine.setTz(ct.getTz());
                }
            }
        };
        timer.start();
    }

    public final double getSize() {
        return size.get();
    }

    private final DoubleProperty size = new SimpleDoubleProperty();

    public ImageView[] getViews() {
        return views;
    }
}
