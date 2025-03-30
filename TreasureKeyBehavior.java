package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;

import java.awt.event.KeyEvent;
import java.awt.AWTEvent;
import java.io.PrintWriter;
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
    private PrintWriter out;

    public TreasureKeyBehavior(TreasureManager tm,
                               Vector3d redBoxPos,
                               Vector3d blueBoxPos,
                               int playerId,
                               BranchGroup rootBG,
                               PrintWriter out) {
        this.treasureBranchGroup = tm.getTreasureBranchGroup();
        this.treasureGroup = tm.getTreasureGroup();
        this.redBoxPos = redBoxPos;
        this.blueBoxPos = blueBoxPos;
        this.playerId = playerId;
        this.rootBG = rootBG;
        this.out = out;
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
        // Only allow blue player (playerId == 2) to activate the treasure.
        if (playerId != 2) {
            return;
        }

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

            if (diff.length() < TREASURE_INTERACT_DISTANCE && !BasicScene.getGameEnded()) {
                startMorphAnimation();
                treasureIsCoin = false;
                // Send game end message for treasure win.
                out.println("GAME_END Blue");
            }
        }
    }

    public void startMorphAnimation() {
        // 1. Get current position and detach old treasure
        Transform3D currentPosition = new Transform3D();
        treasureGroup.getTransform(currentPosition);
        treasureBranchGroup.detach();  // Detach from scene

        // 2. Create morph target geometries, appearance, and morph object as before
        GeometryArray[] geometries = new GeometryArray[2];
        geometries[0] = createCoinGeometry();
        geometries[1] = createStarGeometry();
        if (geometries[0].getVertexCount() != geometries[1].getVertexCount()) {
            System.err.println("Error: Morph targets have different vertex counts!");
            return;
        }
        Appearance treasureAppearance = new Appearance();
        Material goldMaterial = new Material(
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f);
        goldMaterial.setLightingEnable(true);
        treasureAppearance.setMaterial(goldMaterial);
        PolygonAttributes polyAttrib = new PolygonAttributes();
        polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
        treasureAppearance.setPolygonAttributes(polyAttrib);
        TransparencyAttributes transparency = new TransparencyAttributes();
        transparency.setTransparencyMode(TransparencyAttributes.NONE);
        treasureAppearance.setTransparencyAttributes(transparency);

        morph = new Morph(geometries, treasureAppearance);
        morph.setCapability(Morph.ALLOW_WEIGHTS_WRITE);
        double[] weights = new double[]{1.0, 0.0};
        morph.setWeights(weights);

        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotationTG.addChild(morph);
        morphAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 2000, 0, 0, 0, 0, 0);
        Behavior morphBehavior = new Behavior() {
            private WakeupOnElapsedFrames wakeupFrame = new WakeupOnElapsedFrames(0);
            public void initialize() { wakeupOn(wakeupFrame); }
            public void processStimulus(Iterator<WakeupCriterion> criteria) {
                if (morphAlpha != null && morph != null) {
                    float alphaValue = morphAlpha.value();
                    double[] newWeights = new double[]{1.0 - alphaValue, alphaValue};
                    morph.setWeights(newWeights);
                }
                wakeupOn(wakeupFrame);
            }
        };
        morphBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        rotationTG.addChild(morphBehavior);

        Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 4000, 0, 0, 0, 0, 0);
        RotationInterpolator rotator = new RotationInterpolator(
                rotationAlpha, rotationTG, new Transform3D(), 0.0f, (float)(Math.PI * 2.0f));
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        rotationTG.addChild(rotator);

        TransformGroup newTreasureTG = new TransformGroup(currentPosition);
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        newTreasureTG.addChild(rotationTG);

        BranchGroup newTreasureBG = new BranchGroup();
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        newTreasureBG.setCapability(BranchGroup.ALLOW_DETACH);
        newTreasureBG.addChild(newTreasureTG);

        // Ensure the new treasure branch isn't already attached
        if(newTreasureBG.getParent() != null) {
            ((Group)newTreasureBG.getParent()).removeChild(newTreasureBG);
        }

        this.treasureBranchGroup = newTreasureBG;
        this.treasureGroup = newTreasureTG;
        rootBG.addChild(newTreasureBG);
    }


    // Updated Geometry Creation Methods
    private GeometryArray createCoinGeometry() {
        int points = 5;
        int perimeterPoints = points * 2 + 1; // 11 points for star/coin matching
        float radius = 0.025f;
        float height = 0.005f;

        // Total vertices:
        // 1 (top center) + 11 (top perimeter) +
        // 1 (bottom center) + 11 (bottom perimeter) = 24
        Point3f[] vertices = new Point3f[24];
        Vector3f[] normals = new Vector3f[24];

        // Top center (index 0)
        vertices[0] = new Point3f(0, 0, height/2);
        normals[0] = new Vector3f(0, 0, 1);

        // Top perimeter (indices 1-11)
        for(int i=0; i<perimeterPoints; i++) {
            float angle = (float)(2*Math.PI*i/(perimeterPoints-1));
            vertices[1+i] = new Point3f(
                    radius*(float)Math.cos(angle),
                    radius*(float)Math.sin(angle),
                    height/2
            );
            normals[1+i] = new Vector3f(0, 0, 1);
        }

        // Bottom center (index 12)
        vertices[12] = new Point3f(0, 0, -height/2);
        normals[12] = new Vector3f(0, 0, -1);

        // Bottom perimeter (indices 13-23)
        for(int i=0; i<perimeterPoints; i++) {
            float angle = (float)(2*Math.PI*i/(perimeterPoints-1));
            vertices[13+i] = new Point3f(
                    radius*(float)Math.cos(angle),
                    radius*(float)Math.sin(angle),
                    -height/2
            );
            normals[13+i] = new Vector3f(0, 0, -1);
        }

        // Create triangles for:
        // - Top face (fan)
        // - Bottom face (fan)
        // - Sides (strips)
        int[] indices = new int[(perimeterPoints-1)*3*2 + perimeterPoints*6];
        int idx = 0;

        // Top face (triangle fan)
        for(int i=1; i<perimeterPoints; i++) {
            indices[idx++] = 0;
            indices[idx++] = i;
            indices[idx++] = i+1;
        }
        indices[idx++] = 0;
        indices[idx++] = perimeterPoints;
        indices[idx++] = 1;

        // Bottom face (triangle fan)
        for(int i=13; i<13+perimeterPoints-1; i++) {
            indices[idx++] = 12;
            indices[idx++] = i+1;
            indices[idx++] = i;
        }
        indices[idx++] = 12;
        indices[idx++] = 13;
        indices[idx++] = 23;

        // Sides (triangle strips)
        for(int i=1; i<=perimeterPoints; i++) {
            int next = (i%perimeterPoints)+1;
            indices[idx++] = i;
            indices[idx++] = i+12;
            indices[idx++] = next;
            indices[idx++] = next+12;
        }

        IndexedTriangleArray geom = new IndexedTriangleArray(
                vertices.length,
                GeometryArray.COORDINATES | GeometryArray.NORMALS,
                indices.length
        );
        geom.setCoordinates(0, vertices);
        geom.setNormals(0, normals);
        geom.setCoordinateIndices(0, indices);
        geom.setNormalIndices(0, indices);

        return geom;
    }

    private GeometryArray createStarGeometry() {
        int points = 5;
        int perimeterPoints = points * 2 + 1; // Must match coin's 11 points
        float outerRadius = 0.025f;
        float innerRadius = outerRadius * 0.5f;
        float height = 0.005f;

        Point3f[] vertices = new Point3f[24];
        Vector3f[] normals = new Vector3f[24];

        // Top center (index 0)
        vertices[0] = new Point3f(0, 0, height/2);
        normals[0] = new Vector3f(0, 0, 1);

        // Top perimeter (indices 1-11)
        for(int i=0; i<perimeterPoints; i++) {
            double angle = Math.PI/2 + i*Math.PI/points;
            float r = (i%2 == 0) ? outerRadius : innerRadius;
            vertices[1+i] = new Point3f(
                    (float)(r*Math.cos(angle)),
                    (float)(r*Math.sin(angle)),
                    height/2
            );
            normals[1+i] = new Vector3f(0, 0, 1);
        }

        // Bottom center (index 12)
        vertices[12] = new Point3f(0, 0, -height/2);
        normals[12] = new Vector3f(0, 0, -1);

        // Bottom perimeter (indices 13-23)
        for(int i=0; i<perimeterPoints; i++) {
            double angle = Math.PI/2 + i*Math.PI/points;
            float r = (i%2 == 0) ? outerRadius : innerRadius;
            vertices[13+i] = new Point3f(
                    (float)(r*Math.cos(angle)),
                    (float)(r*Math.sin(angle)),
                    -height/2
            );
            normals[13+i] = new Vector3f(0, 0, -1);
        }

        // Use the EXACT SAME index structure as coin
        int[] indices = new int[(perimeterPoints-1)*3*2 + perimeterPoints*6];
        int idx = 0;

        // Top face
        for(int i=1; i<perimeterPoints; i++) {
            indices[idx++] = 0;
            indices[idx++] = i;
            indices[idx++] = i+1;
        }
        indices[idx++] = 0;
        indices[idx++] = perimeterPoints;
        indices[idx++] = 1;

        // Bottom face
        for(int i=13; i<13+perimeterPoints-1; i++) {
            indices[idx++] = 12;
            indices[idx++] = i+1;
            indices[idx++] = i;
        }
        indices[idx++] = 12;
        indices[idx++] = 13;
        indices[idx++] = 23;

        // Sides
        for(int i=1; i<=perimeterPoints; i++) {
            int next = (i%perimeterPoints)+1;
            indices[idx++] = i;
            indices[idx++] = i+12;
            indices[idx++] = next;
            indices[idx++] = next+12;
        }

        IndexedTriangleArray geom = new IndexedTriangleArray(
                vertices.length,
                GeometryArray.COORDINATES | GeometryArray.NORMALS,
                indices.length
        );
        geom.setCoordinates(0, vertices);
        geom.setNormals(0, normals);
        geom.setCoordinateIndices(0, indices);
        geom.setNormalIndices(0, indices);

        return geom;
    }
}