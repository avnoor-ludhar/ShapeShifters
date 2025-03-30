package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.java3d.loaders.Scene;
import org.jogamp.java3d.loaders.objectfile.ObjectFile;
import org.jogamp.vecmath.*;

// ghost model for 3d scnee
public class GhostModel {

    // constants
    private static final double MODEL_SCALE = 0.05;
    private static final String MODEL_PATH = "src/ShapeShifters/assets/ghost.obj";
    private static final double CHARACTER_HALF = 0.025;

    // direction values for movements
    public static final int DIRECTION_DOWN = 0;
    public static final int DIRECTION_LEFT = 1;
    public static final int DIRECTION_UP = 2;
    public static final int DIRECTION_RIGHT = 3;
    public static final int DIRECTION_UPRIGHT = 4;
    public static final int DIRECTION_DOWNRIGHT = 5;
    public static final int DIRECTION_DOWNLEFT = 6;
    public static final int DIRECTION_UPLEFT = 7;

    // Instance variables
    private TransformGroup modelRootTG;
    private TransformGroup rotationTG;
    private Vector3d position;
    private Color3f modelColor;
    private boolean isRedPlayer;
    public boolean isTransformed = false;
    private int currentDirection = DIRECTION_DOWN;
    public double step = .01;

    // set up model
    public GhostModel(boolean isRedPlayer, Vector3d initialPosition) {
        this.isRedPlayer = isRedPlayer;
        this.position = new Vector3d(initialPosition);

        // Set model color based on player
        this.modelColor = isRedPlayer ?
                new Color3f(1.0f, 0.2f, 0.2f) :
                new Color3f(0.2f, 0.2f, 1.0f);

        // Set up transform groups
        Transform3D initialTransform = new Transform3D();
        initialTransform.setTranslation(position);
        modelRootTG = new TransformGroup(initialTransform);
        modelRootTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        modelRootTG.addChild(rotationTG);

        // Load model
        loadModel();

        updateRotation(DIRECTION_DOWN);
    }

    // update rotation based on movement direction
    public void updateRotation(int direction) {
        currentDirection = direction;
        Transform3D rotationTransform = new Transform3D();

        switch (direction) {
            case DIRECTION_LEFT:
                rotationTransform.rotY(-Math.PI / 2);
                break;
            case DIRECTION_UP:
                rotationTransform.rotY(Math.PI);
                break;
            case DIRECTION_RIGHT:
                rotationTransform.rotY(Math.PI / 2);
                break;
            case DIRECTION_DOWNLEFT:
                rotationTransform.rotY(-Math.PI / 4);
                break;
            case DIRECTION_UPLEFT:
                rotationTransform.rotY(-3 * Math.PI / 4);
                break;
            case DIRECTION_UPRIGHT:
                rotationTransform.rotY(3 * Math.PI / 4);
                break;
            case DIRECTION_DOWNRIGHT:
                rotationTransform.rotY(Math.PI / 4);
                break;
            case DIRECTION_DOWN:
            default:
                break; // Default, no rotation
        }

        rotationTG.setTransform(rotationTransform);
    }

    // load ghost model
    private void loadModel() {
        try {
            ObjectFile loader = new ObjectFile(ObjectFile.RESIZE);
            loader.setFlags(ObjectFile.RESIZE | ObjectFile.TRIANGULATE | ObjectFile.STRIPIFY);
            Scene modelScene = loader.load(MODEL_PATH);
            BranchGroup modelBG = modelScene.getSceneGroup();

            // apply appearance
            Appearance coloredAppearance = new Appearance();
            Material material = new Material(
                    modelColor,
                    new Color3f(0.1f, 0.1f, 0.1f),
                    modelColor,
                    new Color3f(1.0f, 1.0f, 1.0f),
                    64.0f
            );
            coloredAppearance.setMaterial(material);
            applyAppearanceToModel(modelBG, coloredAppearance);

            // scale and attach model to scene
            Transform3D modelScale = new Transform3D();
            modelScale.setScale(MODEL_SCALE);
            TransformGroup modelScaleTG = new TransformGroup(modelScale);
            modelScaleTG.addChild(modelBG);

            rotationTG.addChild(modelScaleTG);

        } catch (Exception e) {
            System.err.println("Error loading model (" + MODEL_PATH + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

     // Recursively applies appearance to all shapes in the model
    private void applyAppearanceToModel(Node node, Appearance appearance) {
        if (node instanceof Shape3D) {
            Shape3D shape = (Shape3D) node;
            shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
            shape.setAppearance(appearance);
        } else if (node instanceof Group) {
            Group group = (Group) node;
            for (int i = 0; i < group.numChildren(); i++) {
                applyAppearanceToModel(group.getChild(i), appearance);
            }
        }
    }

    // Updates the ghosts position in the scene
    public void updatePosition(double newX, double newZ) {
        position.x = newX;
        position.z = newZ;

        Transform3D newTransform = new Transform3D();
        newTransform.setTranslation(position);
        modelRootTG.setTransform(newTransform);
    }

    // Updates both pos and direction
    public void updatePositionAndRotation(double newX, double newZ, int direction) {
        position.x = newX;
        position.z = newZ;

        Transform3D newTransform = new Transform3D();
        newTransform.setTranslation(position);
        modelRootTG.setTransform(newTransform);

        updateRotation(direction);
    }

     // Returns the root TransformGroup of the ghost model
    public TransformGroup getTransformGroup() {
        return modelRootTG;
    }


     // Returns the current position of the model
    public Vector3d getPosition() {
        return new Vector3d(position);
    }

    // Returns character bounding box radius
    public static double getCharacterHalf() {
        return CHARACTER_HALF;
    }

     // Returns whether the model is the red player
    public boolean isRedPlayer() {
        return isRedPlayer;
    }

     // Returns the current direction the model is facing
    public int getCurrentDirection() {
        return currentDirection;
    }
}
