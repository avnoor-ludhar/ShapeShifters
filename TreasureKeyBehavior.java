package ShapeShifters;

import org.jogamp.java3d.*;
import java.awt.event.KeyEvent;
import java.awt.AWTEvent;
import java.util.Iterator;
import org.jogamp.vecmath.*;

public class TreasureKeyBehavior extends Behavior {
    private WakeupOnAWTEvent wakeupEvent;
    private BranchGroup treasureBranchGroup;
    private TransformGroup treasureGroup;
    private boolean treasureIsCoin = true;
    private static final double TREASURE_INTERACT_DISTANCE = 0.15;
    private Vector3d redBoxPos, blueBoxPos;
    private int playerId;
    private BranchGroup rootBG; // Reference to scene's root BranchGroup

    public TreasureKeyBehavior(BranchGroup treasureBranchGroup,
                               TransformGroup treasureGroup,
                               Vector3d redBoxPos,
                               Vector3d blueBoxPos,
                               int playerId,
                               BranchGroup rootBG) {
        this.treasureBranchGroup = treasureBranchGroup;
        this.treasureGroup = treasureGroup;
        this.redBoxPos = redBoxPos;
        this.blueBoxPos = blueBoxPos;
        this.playerId = playerId;
        this.rootBG = rootBG;
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
                                if (treasureGroup != null) {
                                    Transform3D treasureTransform = new Transform3D();
                                    treasureGroup.getTransform(treasureTransform);
                                    Vector3d treasurePos = new Vector3d();
                                    treasureTransform.get(treasurePos);

                                    Vector3d playerPos = new Vector3d();
                                    synchronized(playerId == 1 ? redBoxPos : blueBoxPos) {
                                        playerPos.set(playerId == 1 ? redBoxPos : blueBoxPos);
                                    }

                                    Vector3d diff = new Vector3d();
                                    diff.sub(treasurePos, playerPos);

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
        if (treasureBranchGroup == null || treasureGroup == null || rootBG == null) {
            return;
        }

        // 1. Get current position
        Transform3D currentPosition = new Transform3D();
        treasureGroup.getTransform(currentPosition);

        // 2. Detach the old treasure from scene graph
        treasureBranchGroup.detach();

        // 3. Create new star content
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

        // 4. Create new TransformGroup with same position
        TransformGroup newTreasureTG = new TransformGroup(currentPosition);
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        newTreasureTG.addChild(rotationTG);
        newTreasureTG.setUserData("treasure");

        // 5. Create new BranchGroup and add to scene
        BranchGroup newTreasureBG = new BranchGroup();
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        newTreasureBG.setCapability(BranchGroup.ALLOW_DETACH);
        newTreasureBG.addChild(newTreasureTG);

        // 6. Update references
        this.treasureBranchGroup = newTreasureBG;
        this.treasureGroup = newTreasureTG;

        // 7. Add the new treasure to the scene
        rootBG.addChild(newTreasureBG);
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
