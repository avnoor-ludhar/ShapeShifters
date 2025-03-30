package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;
import java.util.Random;

// generates and animates a field of static and shooting stars
public class ShootingStars {

    // constants
    private static final int STAR_COUNT = 15000;
    private static final int SHOOTING_STAR_COUNT = 150;
    private static final float STAR_FIELD_RADIUS = 10.0f;

    // instance variables
    private TransformGroup starSystemTG;
    private PointArray shootingStarPoints;
    private Shape3D shootingStarShape;
    private Random random = new Random();

    // sets up the star system
    public ShootingStars() {
        Transform3D starSystemTransform = new Transform3D();
        starSystemTG = new TransformGroup(starSystemTransform);
        starSystemTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        createStaticStars();
        createShootingStars();
        startShootingStarAnimation();
    }

    // creates the static star field
    private void createStaticStars() {
        PointArray starPoints = new PointArray(STAR_COUNT,
                GeometryArray.COORDINATES | GeometryArray.COLOR_3); // allocate geometry with position and color

        for (int i = 0; i < STAR_COUNT; i++) {
            double theta = 2.0 * Math.PI * random.nextDouble(); // random azimuthal angle
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0); // random polar angle using inverse CDF

            // convert spherical coordinates to cartesian
            float x = (float)(STAR_FIELD_RADIUS * Math.sin(phi) * Math.cos(theta));
            float y = (float)(STAR_FIELD_RADIUS * Math.sin(phi) * Math.sin(theta));
            float z = (float)(STAR_FIELD_RADIUS * Math.cos(phi));

            starPoints.setCoordinate(i, new Point3f(x, y, z)); // set star pos

            float brightness = 0.5f + random.nextFloat() * 0.5f; // brightness between 0.5 and 1.0
            starPoints.setColor(i, new Color3f(brightness, brightness, brightness)); // grayscale brightness
        }

        Appearance starAppearance = new Appearance();
        PointAttributes starPointAttributes = new PointAttributes();
        starPointAttributes.setPointSize(2.0f); // small dot size for stars
        starAppearance.setPointAttributes(starPointAttributes);

        Shape3D starShape = new Shape3D(starPoints, starAppearance); // combine geometry and appearance
        starSystemTG.addChild(starShape); // add stars to the scene graph
    }


    // creates shooting stars with capability for animation
    private void createShootingStars() {
        shootingStarPoints = new PointArray(SHOOTING_STAR_COUNT,
                GeometryArray.COORDINATES | GeometryArray.COLOR_3); // allocate geometry for shooting stars

        shootingStarPoints.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE); // allow dynamic position updates
        shootingStarPoints.setCapability(GeometryArray.ALLOW_COLOR_WRITE); // allow color changes

        for (int i = 0; i < SHOOTING_STAR_COUNT; i++) {
            initializeShootingStar(i); // set initial position and color
        }

        Appearance shootingStarAppearance = new Appearance();
        PointAttributes shootingStarPointAttributes = new PointAttributes();
        shootingStarPointAttributes.setPointSize(5.0f); // larger size to distinguish from static stars
        shootingStarAppearance.setPointAttributes(shootingStarPointAttributes);

        shootingStarShape = new Shape3D(shootingStarPoints, shootingStarAppearance); // combine geometry and appearance
        shootingStarShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE); // allow runtime geometry updates

        starSystemTG.addChild(shootingStarShape); // add shooting stars to the scene
    }


    // initializes shooting star position and color
    private void initializeShootingStar(int index) {
        double angle = random.nextDouble() * 2.0 * Math.PI;
        float x = STAR_FIELD_RADIUS * 0.9f;
        float y = (float)(STAR_FIELD_RADIUS * 0.7f * Math.sin(angle));
        float z = (float)(STAR_FIELD_RADIUS * 0.7f * Math.cos(angle));
        shootingStarPoints.setCoordinate(index, new Point3f(x, y, z));
        shootingStarPoints.setColor(index, new Color3f(1.0f, 0.9f, 0.5f));
    }

    // animates shooting stars by updating positions in a loop
    private void startShootingStarAnimation() {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // delay before animation starts
                while (true) { // continuous animation loop
                    for (int i = 0; i < SHOOTING_STAR_COUNT; i++) {
                        Point3f pos = new Point3f();
                        shootingStarPoints.getCoordinate(i, pos); // get current position

                        pos.x -= 0.15f; // move left
                        pos.y -= 0.05f; // move downward

                        if (pos.x < -STAR_FIELD_RADIUS || Math.abs(pos.y) > STAR_FIELD_RADIUS) {
                            initializeShootingStar(i); // reinitialize position and color
                        } else {
                            shootingStarPoints.setCoordinate(i, pos); // update position
                        }
                    }
                    Thread.sleep(16);
                }
            } catch (InterruptedException e) {
                e.printStackTrace(); // log interruption errors
            }
        }).start(); // launch animation on a separate thread
    }


    // returns the root transform group
    public TransformGroup getStarSystemTG() {
        return starSystemTG;
    }
}
