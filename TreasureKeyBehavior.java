package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;

import java.awt.event.KeyEvent;
import java.awt.AWTEvent;
import java.io.PrintWriter;
import java.util.Iterator;

public class TreasureKeyBehavior extends Behavior {

    // constants
    private static final double TREASURE_INTERACT_DISTANCE = 0.15;

    // instance variables
    private WakeupOnAWTEvent wakeupEvent;
    private BranchGroup treasureBranchGroup;
    private TransformGroup treasureGroup;
    private boolean treasureIsCoin = true;
    private Vector3d redBoxPos, blueBoxPos;
    private int playerId;
    private BranchGroup rootBG;
    private Morph morph;
    private Alpha morphAlpha;
    private PrintWriter out;

    // constructor
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

    // update red player position
    public void updateRedPosition(Vector3d newPos) {
        synchronized(redBoxPos) {
            redBoxPos.set(newPos);
        }
    }

    // update blue player position
    public void updateBluePosition(Vector3d newPos) {
        synchronized(blueBoxPos) {
            blueBoxPos.set(newPos);
        }
    }

    // register key event
    @Override
    public void initialize() {
        wakeupEvent = new WakeupOnAWTEvent(KeyEvent.KEY_PRESSED);
        wakeupOn(wakeupEvent);
    }

    // handle key press event
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

    // check player proximity and trigger morph
    private void checkTreasureInteraction() {
        if (playerId != 2) return;

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
                out.println("TREASURE_ACTIVATE");
                out.println("GAME_END Blue");
            }
        }
    }

    // animate morphing treasure from coin to star
    public void startMorphAnimation() {
        Transform3D currentPosition = new Transform3D(); // store current position
        treasureGroup.getTransform(currentPosition);
        treasureBranchGroup.detach(); // remove current treasure from scene

        GeometryArray[] geometries = new GeometryArray[2]; // morph targets
        geometries[0] = createCoinGeometry(); // initial shape
        geometries[1] = createStarGeometry(); // target shape
        if (geometries[0].getVertexCount() != geometries[1].getVertexCount()) return; // must match

        Appearance treasureAppearance = new Appearance(); // set up appearance
        Material goldMaterial = new Material( // gold material
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f);
        goldMaterial.setLightingEnable(true);
        treasureAppearance.setMaterial(goldMaterial);

        PolygonAttributes polyAttrib = new PolygonAttributes(); // fill mode
        polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
        treasureAppearance.setPolygonAttributes(polyAttrib);

        TransparencyAttributes transparency = new TransparencyAttributes(); // no transparency
        transparency.setTransparencyMode(TransparencyAttributes.NONE);
        treasureAppearance.setTransparencyAttributes(transparency);

        morph = new Morph(geometries, treasureAppearance); // morph object
        morph.setCapability(Morph.ALLOW_WEIGHTS_WRITE); // allow weight changes
        morph.setWeights(new double[]{1.0, 0.0}); // start fully coin

        TransformGroup rotationTG = new TransformGroup(); // rotation wrapper
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotationTG.addChild(morph);

        morphAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 2000, 0, 0, 0, 0, 0); // morph timer
        Behavior morphBehavior = new Behavior() {
            private WakeupOnElapsedFrames wakeupFrame = new WakeupOnElapsedFrames(0);
            public void initialize() { wakeupOn(wakeupFrame); }
            public void processStimulus(Iterator<WakeupCriterion> criteria) {
                if (morphAlpha != null && morph != null) {
                    float alphaValue = morphAlpha.value(); // morph weight
                    morph.setWeights(new double[]{1.0 - alphaValue, alphaValue});
                }
                wakeupOn(wakeupFrame);
            }
        };
        morphBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        rotationTG.addChild(morphBehavior); // add morph updater

        Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 4000, 0, 0, 0, 0, 0); // rotation timer
        RotationInterpolator rotator = new RotationInterpolator(
                rotationAlpha, rotationTG, new Transform3D(), 0.0f, (float)(Math.PI * 2.0f)); // spin full circle
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        rotationTG.addChild(rotator); // add rotator

        TransformGroup newTreasureTG = new TransformGroup(currentPosition); // new transform group
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        newTreasureTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        newTreasureTG.addChild(rotationTG); // add morph and rotation

        BranchGroup newTreasureBG = new BranchGroup(); // scene branch
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        newTreasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        newTreasureBG.setCapability(BranchGroup.ALLOW_DETACH);
        newTreasureBG.addChild(newTreasureTG);

        if (newTreasureBG.getParent() != null) {
            ((Group)newTreasureBG.getParent()).removeChild(newTreasureBG); // remove if already added
        }

        this.treasureBranchGroup = newTreasureBG; // update reference
        this.treasureGroup = newTreasureTG;
        rootBG.addChild(newTreasureBG); // add new treasure to scene
    }



    // create coin geometry
    private GeometryArray createCoinGeometry() {
        int points = 5;
        int perimeterPoints = points * 2 + 1; // number of outer edge points
        float radius = 0.025f;
        float height = 0.005f;

        Point3f[] vertices = new Point3f[24];
        Vector3f[] normals = new Vector3f[24];

        vertices[0] = new Point3f(0, 0, height/2); // top center point
        normals[0] = new Vector3f(0, 0, 1); // top normal

        for(int i=0; i<perimeterPoints; i++) {
            float angle = (float)(2*Math.PI*i/(perimeterPoints-1)); // angle step
            vertices[1+i] = new Point3f( // top perimeter
                    radius*(float)Math.cos(angle),
                    radius*(float)Math.sin(angle),
                    height/2
            );
            normals[1+i] = new Vector3f(0, 0, 1); // top normals
        }

        vertices[12] = new Point3f(0, 0, -height/2); // bottom center
        normals[12] = new Vector3f(0, 0, -1); // bottom normal

        for(int i=0; i<perimeterPoints; i++) {
            float angle = (float)(2*Math.PI*i/(perimeterPoints-1)); // same angle step
            vertices[13+i] = new Point3f( // bottom perimeter
                    radius*(float)Math.cos(angle),
                    radius*(float)Math.sin(angle),
                    -height/2
            );
            normals[13+i] = new Vector3f(0, 0, -1); // bottom normals
        }

        int[] indices = new int[(perimeterPoints-1)*3*2 + perimeterPoints*6]; // total triangles
        int idx = 0;

        for(int i=1; i<perimeterPoints; i++) { // top fan
            indices[idx++] = 0;
            indices[idx++] = i;
            indices[idx++] = i+1;
        }
        indices[idx++] = 0; indices[idx++] = perimeterPoints; indices[idx++] = 1; // close top fan

        for(int i=13; i<13+perimeterPoints-1; i++) { // bottom fan
            indices[idx++] = 12;
            indices[idx++] = i+1;
            indices[idx++] = i;
        }
        indices[idx++] = 12; indices[idx++] = 13; indices[idx++] = 23; // close bottom fan

        for(int i=1; i<=perimeterPoints; i++) { // side strip
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

    // create star geometry
    private GeometryArray createStarGeometry() {
        int points = 5;
        int perimeterPoints = points * 2 + 1; // must match coin vertex count
        float outerRadius = 0.025f;
        float innerRadius = outerRadius * 0.5f;
        float height = 0.005f;

        Point3f[] vertices = new Point3f[24];
        Vector3f[] normals = new Vector3f[24];

        vertices[0] = new Point3f(0, 0, height/2);
        normals[0] = new Vector3f(0, 0, 1);

        for(int i=0; i<perimeterPoints; i++) {
            double angle = Math.PI/2 + i*Math.PI/points; // angle step
            float r = (i%2 == 0) ? outerRadius : innerRadius; // alternate radius
            vertices[1+i] = new Point3f( // top perimeter
                    (float)(r*Math.cos(angle)),
                    (float)(r*Math.sin(angle)),
                    height/2
            );
            normals[1+i] = new Vector3f(0, 0, 1); // top normals
        }

        vertices[12] = new Point3f(0, 0, -height/2); // bottom center vertex
        normals[12] = new Vector3f(0, 0, -1); // bottom normal

        for(int i=0; i<perimeterPoints; i++) {
            double angle = Math.PI/2 + i*Math.PI/points; // same angle
            float r = (i%2 == 0) ? outerRadius : innerRadius; // alternate radius
            vertices[13+i] = new Point3f( // bottom perimeter
                    (float)(r*Math.cos(angle)),
                    (float)(r*Math.sin(angle)),
                    -height/2
            );
            normals[13+i] = new Vector3f(0, 0, -1); // bottom normals
        }

        int[] indices = new int[(perimeterPoints-1)*3*2 + perimeterPoints*6]; // triangle indices
        int idx = 0;

        for(int i=1; i<perimeterPoints; i++) { // top fan
            indices[idx++] = 0;
            indices[idx++] = i;
            indices[idx++] = i+1;
        }
        indices[idx++] = 0; indices[idx++] = perimeterPoints; indices[idx++] = 1; // close top

        for(int i=13; i<13+perimeterPoints-1; i++) { // bottom fan
            indices[idx++] = 12;
            indices[idx++] = i+1;
            indices[idx++] = i;
        }
        indices[idx++] = 12; indices[idx++] = 13; indices[idx++] = 23; // close bottom

        for(int i=1; i<=perimeterPoints; i++) { // side strip
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
        geom.setCoordinates(0, vertices); // set vertices
        geom.setNormals(0, normals); // set normals
        geom.setCoordinateIndices(0, indices); // set triangle indices
        geom.setNormalIndices(0, indices); // same indices for normals

        return geom;
    }
}