package ShapeShifters;

import org.jdesktop.j3d.examples.morphing.MorphingBehavior;
import org.jogamp.java3d.*;
        import org.jogamp.vecmath.*;

        import java.awt.event.KeyEvent;
import java.awt.AWTEvent;
import java.util.Iterator;

public class TreasureKeyBehavior extends Behavior {
    private WakeupOnAWTEvent wakeupEvent;
    private BranchGroup treasureBranchGroup;
    private TransformGroup treasureGroup;
    private boolean treasureIsCoin = true;
    private static final double TREASURE_INTERACT_DISTANCE = 0.15;
    private Vector3d redBoxPos, blueBoxPos;
    private int playerId;
    private BranchGroup rootBG;
    private Morph morph;
    private Alpha morphAlpha;

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

    @Override
    public void processStimulus(Iterator<WakeupCriterion> criteria) {
        while (criteria.hasNext()) {
            WakeupCriterion c = criteria.next();
            if (c instanceof WakeupOnAWTEvent) {
                AWTEvent[] events = ((WakeupOnAWTEvent) c).getAWTEvent();
                for (AWTEvent e : events) {
                    if (e instanceof KeyEvent) {
                        KeyEvent ke = (KeyEvent) e;
                        if (ke.getID() == KeyEvent.KEY_PRESSED && ke.getKeyCode() == KeyEvent.VK_E) {
                            checkTreasureInteraction();
                        }
                    }
                }
            }
        }
        wakeupOn(wakeupEvent);
    }

    private void checkTreasureInteraction() {
        if (treasureGroup != null && treasureIsCoin) {
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

            if (diff.length() < TREASURE_INTERACT_DISTANCE) {
                startMorphAnimation();
                treasureIsCoin = false;
            }
        }
    }

    private void startMorphAnimation() {
        // 1. Get current position and remove old treasure
        Transform3D currentPosition = new Transform3D();
        treasureGroup.getTransform(currentPosition);
        treasureBranchGroup.detach();

        // 2. Create morph target geometries
        GeometryArray[] geometries = new GeometryArray[2];
        geometries[0] = createCoinGeometry();  // Starting shape (coin)
        geometries[1] = createStarGeometry();  // Target shape (star)

        // 3. Create morph appearance
        Appearance treasureAppearance = new Appearance();
        treasureAppearance.setMaterial(new Material(
                new Color3f(1.0f, 0.84f, 0.0f),  // Gold color
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));

        // 4. Create morph object
        morph = new Morph(geometries, treasureAppearance);
        morph.setCapability(Morph.ALLOW_WEIGHTS_WRITE);

        // 5. Create morph animation controller
        morphAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE,
                0, 0, 2000, 0, 0, 0, 0, 0);  // 2 second morph

        MorphingBehavior morphBehavior = new MorphingBehavior(morphAlpha, morph);
        morphBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));

        // 6. Create rotation animation
        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotationTG.addChild(morph);

        Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE,
                0, 0, 4000, 0, 0, 0, 0, 0);
        RotationInterpolator rotator = new RotationInterpolator(
                rotationAlpha, rotationTG, new Transform3D(),
                0.0f, (float) (Math.PI * 2.0f));
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        rotationTG.addChild(rotator);

        // 7. Create new treasure group
        TransformGroup newTreasureTG = new TransformGroup(currentPosition);
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        newTreasureTG.addChild(rotationTG);

        BranchGroup newTreasureBG = new BranchGroup();
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        newTreasureBG.setCapability(BranchGroup.ALLOW_DETACH);
        newTreasureBG.addChild(newTreasureTG);

        // 8. Update references and add to scene
        this.treasureBranchGroup = newTreasureBG;
        this.treasureGroup = newTreasureTG;
        rootBG.addChild(newTreasureBG);
    }

    private GeometryArray createCoinGeometry() {
        int segments = 32;  // Number of segments for smooth circle
        int vertexCount = (segments + 1) * 2;
        int[] stripCounts = {vertexCount};

        Point3f[] vertices = new Point3f[vertexCount];
        Vector3f[] normals = new Vector3f[vertexCount];

        float radius = 0.025f;
        float height = 0.005f;

        // Create vertices for top and bottom of coin
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            float x = radius * (float) Math.cos(angle);
            float y = radius * (float) Math.sin(angle);

            // Top vertex
            vertices[i*2] = new Point3f(x, y, height/2);
            normals[i*2] = new Vector3f(0, 0, 1);

            // Bottom vertex
            vertices[i*2+1] = new Point3f(x, y, -height/2);
            normals[i*2+1] = new Vector3f(0, 0, -1);
        }

        TriangleStripArray geometry = new TriangleStripArray(
                vertexCount,
                GeometryArray.COORDINATES | GeometryArray.NORMALS,
                stripCounts
        );

        geometry.setCoordinates(0, vertices);
        geometry.setNormals(0, normals);

        return geometry;
    }

    private GeometryArray createStarGeometry() {
        int points = 5;  // 5-pointed star
        int vertexCount = (points * 2 + 1) * 2;
        int[] stripCounts = {vertexCount};

        Point3f[] vertices = new Point3f[vertexCount];
        Vector3f[] normals = new Vector3f[vertexCount];

        float outerRadius = 0.025f;
        float innerRadius = outerRadius * 0.5f;
        float height = 0.005f;

        // Create vertices for top and bottom of star
        for (int i = 0; i <= points * 2; i++) {
            double angle = Math.PI / 2 + i * Math.PI / points;
            float r = (i % 2 == 0) ? outerRadius : innerRadius;
            float x = (float)(r * Math.cos(angle));
            float y = (float)(r * Math.sin(angle));

            // Top vertex
            vertices[i*2] = new Point3f(x, y, height/2);
            normals[i*2] = new Vector3f(0, 0, 1);

            // Bottom vertex
            vertices[i*2+1] = new Point3f(x, y, -height/2);
            normals[i*2+1] = new Vector3f(0, 0, -1);
        }

        TriangleStripArray geometry = new TriangleStripArray(
                vertexCount,
                GeometryArray.COORDINATES | GeometryArray.NORMALS,
                stripCounts
        );

        geometry.setCoordinates(0, vertices);
        geometry.setNormals(0, normals);

        return geometry;
    }
}