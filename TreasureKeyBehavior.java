package ShapeShifters;
// First, create a behavior class similar to FanKeyBehavior

import org.jogamp.java3d.*;
import java.awt.event.KeyEvent;
import java.awt.AWTEvent;
import java.util.Iterator;

import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Point3d;

public class TreasureKeyBehavior extends Behavior {
    private WakeupOnAWTEvent wakeupEvent;
    private TransformGroup treasureGroup;
    private boolean treasureIsCoin = true;
    private static final double TREASURE_INTERACT_DISTANCE = 0.15;
    private Vector3d redBoxPos, blueBoxPos;
    private int playerId;

    public TreasureKeyBehavior(TransformGroup treasureGroup, Vector3d redBoxPos, Vector3d blueBoxPos, int playerId) {
        this.treasureGroup = treasureGroup;
        this.redBoxPos = redBoxPos;
        this.blueBoxPos = blueBoxPos;
        this.playerId = playerId;
    }

    public void updateRedPosition(Vector3d newPos) {
        synchronized(redBoxPos) {
            redBoxPos.set(newPos);
        }
    }

    public void updateBluePosition(Vector3d newPos) {
        synchronized(blueBoxPos) {
            blueBoxPos.set(newPos);
        }
    }

    @Override
    public void initialize() {
        wakeupEvent = new WakeupOnAWTEvent(KeyEvent.KEY_PRESSED);
        wakeupOn(wakeupEvent);
    }


    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public void processStimulus(Iterator<WakeupCriterion> criteria) {
        while (criteria.hasNext()) {
            WakeupCriterion c = criteria.next();
            if (c instanceof WakeupOnAWTEvent) {
                AWTEvent[] events = ((WakeupOnAWTEvent) c).getAWTEvent();
                for (AWTEvent e : events) {
                    if (e instanceof KeyEvent) {
                        KeyEvent ke = (KeyEvent) e;
                        if (ke.getID() == KeyEvent.KEY_PRESSED) {
                            int code = ke.getKeyCode();
                            if (code == KeyEvent.VK_E) {
                                // Check if player is close enough to interact with treasure
                                if (treasureGroup != null) {
                                    // Get treasure position (translation component only)
                                    Transform3D treasureTransform = new Transform3D();
                                    treasureGroup.getTransform(treasureTransform);
                                    Vector3d treasurePos = new Vector3d();
                                    treasureTransform.get(treasurePos);

                                    // Get player position safely
                                    Vector3d playerPos = new Vector3d();
                                    synchronized(playerId == 1 ? redBoxPos : blueBoxPos) {
                                        playerPos.set(playerId == 1 ? redBoxPos : blueBoxPos);
                                    }

                                    // Calculate distance
                                    Vector3d diff = new Vector3d();
                                    diff.sub(treasurePos, playerPos);

                                    // Check distance and state
                                    if (diff.length() < TREASURE_INTERACT_DISTANCE && treasureIsCoin) {
                                        morphTreasureToStar();
                                        treasureIsCoin = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        wakeupOn(wakeupEvent);
    }

    private void morphTreasureToStar() {
        if (treasureGroup == null) return;
        treasureGroup.removeAllChildren();

        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Appearance treasureAppearance = new Appearance();
        treasureAppearance.setMaterial(new Material(
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));

        Shape3D starShape = createStarShape(0.025f, 0.005f, treasureAppearance);
        starShape.setPickable(true);
        rotationTG.addChild(starShape);

        Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE,
                0, 0, 4000, 0, 0, 0, 0, 0);
        Transform3D yAxis = new Transform3D();
        RotationInterpolator rotator = new RotationInterpolator(
                rotationAlpha,
                rotationTG,
                yAxis,
                0.0f, (float) (Math.PI * 2.0f));
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0, 0, 0), 100.0));
        rotationTG.addChild(rotator);

        treasureGroup.addChild(rotationTG);
        treasureGroup.setUserData("treasure");
    }

    private Shape3D createStarShape(float radius, float height, Appearance appearance) {
        int numPoints = 5;
        int numVertices = numPoints * 2;
        Point3f[] frontVertices = new Point3f[numVertices + 1];
        frontVertices[0] = new Point3f(0.0f, 0.0f, height / 2);
        float innerFactor = 0.5f;
        for (int i = 0; i < numVertices; i++) {
            double angle = Math.PI / 2 + i * Math.PI / numPoints;
            float r = (i % 2 == 0) ? radius : radius * innerFactor;
            float x = (float)(r * Math.cos(angle));
            float y = (float)(r * Math.sin(angle));
            frontVertices[i + 1] = new Point3f(x, y, height / 2);
        }
        int[] stripCounts = { numVertices + 1 };
        TriangleFanArray frontFace = new TriangleFanArray(numVertices + 1,
                GeometryArray.COORDINATES, stripCounts);
        frontFace.setCoordinates(0, frontVertices);

        return new Shape3D(frontFace, appearance);
    }
}
