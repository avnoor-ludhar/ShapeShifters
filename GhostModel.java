package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.java3d.loaders.Scene;
import org.jogamp.java3d.loaders.objectfile.ObjectFile;
import org.jogamp.vecmath.*;

/**
 * Represents a ghost character model in the 3D scene.
 * Handles loading, scaling, coloring, and manipulating the 3D model.
 */
public class GhostModel {
    // Model constants
    private static final double MODEL_SCALE = 0.05;
//    private static final String MODEL_PATH = "src/ShapeShifters/assets/ghost.obj";
    private static final String MODEL_PATH = "src/ShapeShifters/assets/ghost.obj";

    // Character collision constants
    private static final double CHARACTER_HALF = 0.02;
    
    // Model information
    private TransformGroup modelRootTG;
    private Vector3d position;
    private Color3f modelColor;
    private boolean isRedPlayer;
    
    /**
     * Creates a new ghost model.
     * 
     * @param isRedPlayer Whether this is the red player (player 1)
     * @param initialPosition Initial position for the model
     */
    public GhostModel(boolean isRedPlayer, Vector3d initialPosition) {
        this.isRedPlayer = isRedPlayer;
        this.position = new Vector3d(initialPosition);
        
        // Set color based on player
        this.modelColor = isRedPlayer ? 
                new Color3f(1.0f, 0.2f, 0.2f) :  // Red for player 1
                new Color3f(0.2f, 0.2f, 1.0f);   // Blue for player 2
                
        // Create the transform group
        Transform3D initialTransform = new Transform3D();
        initialTransform.setTranslation(position);
        modelRootTG = new TransformGroup(initialTransform);
        modelRootTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        // Load the model
        loadModel();
    }
    
    /**
     * Loads the 3D model and applies color and scaling.
     */
    private void loadModel() {
        try {
            // Create the ObjectFile loader
            ObjectFile loader = new ObjectFile(ObjectFile.RESIZE);
            loader.setFlags(ObjectFile.RESIZE | ObjectFile.TRIANGULATE | ObjectFile.STRIPIFY);

            // Load the OBJ file
            Scene modelScene = loader.load(MODEL_PATH);
            BranchGroup modelBG = modelScene.getSceneGroup();
            
            // Create appearance with the selected color
            Appearance coloredAppearance = new Appearance();
            Material material = new Material(
                modelColor,                      // Ambient color
                new Color3f(0.1f, 0.1f, 0.1f),   // Emissive color
                modelColor,                      // Diffuse color
                new Color3f(1.0f, 1.0f, 1.0f),   // Specular color
                64.0f                            // Shininess
            );
            coloredAppearance.setMaterial(material);
            
            // Apply the appearance to all shapes in the model
            applyAppearanceToModel(modelBG, coloredAppearance);

            // Scale the model
            Transform3D modelScale = new Transform3D();
            modelScale.setScale(MODEL_SCALE);
            TransformGroup modelScaleTG = new TransformGroup(modelScale);
            modelScaleTG.addChild(modelBG);

            // Add the model (scaled) to the parent TransformGroup
            modelRootTG.addChild(modelScaleTG);

        } catch (Exception e) {
            System.err.println("Error loading model (" + MODEL_PATH + "): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void applyAppearanceToModel(Node node, Appearance appearance) {
        if (node instanceof Shape3D) {
            Shape3D shape = (Shape3D) node;
            shape.setAppearance(appearance);
        } else if (node instanceof Group) {
            Group group = (Group) node;
            // Recursively process all children
            for (int i = 0; i < group.numChildren(); i++) {
                applyAppearanceToModel(group.getChild(i), appearance);
            }
        }
    }
    
    public void updatePosition(double newX, double newZ) {
        position.x = newX;
        position.z = newZ;
        
        Transform3D newTransform = new Transform3D();
        newTransform.setTranslation(position);
        modelRootTG.setTransform(newTransform);
    }
    
    public TransformGroup getTransformGroup() {
        return modelRootTG;
    }
    
    public Vector3d getPosition() {
        return new Vector3d(position);
    }
    
    public static double getCharacterHalf() {
        return CHARACTER_HALF;
    }

    public boolean isRedPlayer() {
        return isRedPlayer;
    }
}