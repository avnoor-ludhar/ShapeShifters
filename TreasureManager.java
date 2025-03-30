package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.geometry.Cylinder;
import org.jogamp.java3d.utils.geometry.Primitive;
import org.jogamp.vecmath.*;

// Class that creates and manages a spinning treasure object in the scene
public class TreasureManager {
    // The treasure branch and transform group
    private BranchGroup treasureBranchGroup;
    private TransformGroup treasureGroup;
    private Appearance treasureAppearance;

    // Initializes the treasure at the given (x, y, z) position
    public TreasureManager(double x, double y, double z) {
        // Create treasure appearance (logic unchanged)
        treasureAppearance = new Appearance();
        treasureAppearance.setMaterial(new Material(
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(1.0f, 0.84f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
        // Create the treasure node
        createTreasure(x, y, z);
    }

    // Builds the treasure object, adds spin animation, and places it in the scene
    private void createTreasure(double x, double y, double z) {
        Cylinder treasureDisk = new Cylinder(0.025f, 0.005f,
                Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS,
                treasureAppearance);
        treasureDisk.setCapability(Shape3D.ALLOW_PICKABLE_READ);
        treasureDisk.setCapability(Shape3D.ALLOW_PICKABLE_WRITE);
        treasureDisk.setPickable(true);
        treasureDisk.setUserData("treasure");

        // Create transformation hierarchy for the treasure
        Transform3D coinRotation = new Transform3D();
        coinRotation.rotZ(Math.PI / 2);
        TransformGroup coinOrientationTG = new TransformGroup(coinRotation);
        coinOrientationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        coinOrientationTG.addChild(treasureDisk);
        coinOrientationTG.setUserData("treasure");
        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotationTG.addChild(coinOrientationTG);
        Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 4000, 0, 0, 0, 0, 0);
        RotationInterpolator rotator = new RotationInterpolator(
                rotationAlpha, rotationTG, new Transform3D(), 0.0f, (float) (Math.PI * 2));
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        rotationTG.addChild(rotator);
        Transform3D position = new Transform3D();
        position.setTranslation(new Vector3d(x, y, z));
        TransformGroup positionedTG = new TransformGroup(position);
        positionedTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        positionedTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        positionedTG.addChild(rotationTG);
        positionedTG.setUserData("treasure");

        // Store the treasure transform group and branch group
        treasureGroup = positionedTG;
        BranchGroup treasureBG = new BranchGroup();
        treasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        treasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        treasureBG.setCapability(BranchGroup.ALLOW_DETACH);
        treasureBG.addChild(positionedTG);
        treasureBranchGroup = treasureBG;
    }

    // Getters for the treasure nodes
    public BranchGroup getTreasureBranchGroup() {
        return treasureBranchGroup;
    }

    public TransformGroup getTreasureGroup() {
        return treasureGroup;
    }
}