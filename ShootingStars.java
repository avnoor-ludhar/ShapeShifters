// ShootingStars.java
package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;
import java.util.Random;

public class ShootingStars {
    private static final int STAR_COUNT = 15000;
    private static final int SHOOTING_STAR_COUNT = 150;
    private static final float STAR_FIELD_RADIUS = 10.0f;

    private TransformGroup starSystemTG;
    private PointArray shootingStarPoints;
    private Shape3D shootingStarShape;
    private Random random = new Random();

    public ShootingStars() {
        // Create a new TransformGroup for the star system.
        Transform3D starSystemTransform = new Transform3D();
        starSystemTG = new TransformGroup(starSystemTransform);
        starSystemTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        createStaticStars();
        createShootingStars();
        startShootingStarAnimation();
    }

    private void createStaticStars() {
        PointArray starPoints = new PointArray(STAR_COUNT,
                GeometryArray.COORDINATES | GeometryArray.COLOR_3);
        for (int i = 0; i < STAR_COUNT; i++) {
            double theta = 2.0 * Math.PI * random.nextDouble();
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
            float x = (float)(STAR_FIELD_RADIUS * Math.sin(phi) * Math.cos(theta));
            float y = (float)(STAR_FIELD_RADIUS * Math.sin(phi) * Math.sin(theta));
            float z = (float)(STAR_FIELD_RADIUS * Math.cos(phi));
            starPoints.setCoordinate(i, new Point3f(x, y, z));
            float brightness = 0.5f + random.nextFloat() * 0.5f;
            starPoints.setColor(i, new Color3f(brightness, brightness, brightness));
        }
        Appearance starAppearance = new Appearance();
        PointAttributes starPointAttributes = new PointAttributes();
        starPointAttributes.setPointSize(2.0f);
        starAppearance.setPointAttributes(starPointAttributes);
        Shape3D starShape = new Shape3D(starPoints, starAppearance);
        starSystemTG.addChild(starShape);
    }

    private void createShootingStars() {
        shootingStarPoints = new PointArray(SHOOTING_STAR_COUNT,
                GeometryArray.COORDINATES | GeometryArray.COLOR_3);
        shootingStarPoints.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        shootingStarPoints.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
        for (int i = 0; i < SHOOTING_STAR_COUNT; i++) {
            initializeShootingStar(i);
        }
        Appearance shootingStarAppearance = new Appearance();
        PointAttributes shootingStarPointAttributes = new PointAttributes();
        shootingStarPointAttributes.setPointSize(5.0f);
        shootingStarAppearance.setPointAttributes(shootingStarPointAttributes);
        shootingStarShape = new Shape3D(shootingStarPoints, shootingStarAppearance);
        shootingStarShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        starSystemTG.addChild(shootingStarShape);
    }

    private void initializeShootingStar(int index) {
        double angle = random.nextDouble() * 2.0 * Math.PI;
        float x = STAR_FIELD_RADIUS * 0.9f;
        float y = (float)(STAR_FIELD_RADIUS * 0.7f * Math.sin(angle));
        float z = (float)(STAR_FIELD_RADIUS * 0.7f * Math.cos(angle));
        shootingStarPoints.setCoordinate(index, new Point3f(x, y, z));
        shootingStarPoints.setColor(index, new Color3f(1.0f, 0.9f, 0.5f));
    }

    private void startShootingStarAnimation() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                while (true) {
                    for (int i = 0; i < SHOOTING_STAR_COUNT; i++) {
                        Point3f pos = new Point3f();
                        shootingStarPoints.getCoordinate(i, pos);
                        pos.x -= 0.15f;
                        pos.y -= 0.05f;
                        if (pos.x < -STAR_FIELD_RADIUS || Math.abs(pos.y) > STAR_FIELD_RADIUS) {
                            initializeShootingStar(i);
                        } else {
                            shootingStarPoints.setCoordinate(i, pos);
                        }
                    }
                    Thread.sleep(16);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public TransformGroup getStarSystemTG() {
        return starSystemTG;
    }
}
