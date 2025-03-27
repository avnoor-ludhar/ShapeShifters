package ShapeShifters;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Random;
import org.jogamp.java3d.*;
import org.jogamp.java3d.loaders.Scene;
import org.jogamp.java3d.loaders.objectfile.ObjectFile;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.vecmath.*;

public class NPC {
    // Model constants
    private static final double MODEL_SCALE = 0.05;
    private static final String MODEL_PATH = "src/ShapeShifters/assets/ghost.obj";
    private static final double CHARACTER_HALF = 0.03; // Match this with GhostModel or adjust as needed
    
    // Transform hierarchy
    private TransformGroup positionTG; // Root TG - handles position only
    private TransformGroup rotationTG; // Child TG - handles rotation only
    
    private Vector3d position;
    private Vector3d direction;
    private double step;

    /**
     * Interface for collision detection.
     */
    public interface CollisionChecker {
        boolean collides(double x, double z);
    }

    public NPC(Vector3d pos, Vector3d dir, double step, Appearance unusedAppearance) {
        this.position = new Vector3d(pos);
        this.direction = new Vector3d(dir);
        this.step = step;

        // Create position TransformGroup (root)
        Transform3D posTransform = new Transform3D();
        posTransform.setTranslation(position);
        positionTG = new TransformGroup(posTransform);
        positionTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        // Create rotation TransformGroup (child)
        rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        positionTG.addChild(rotationTG);
        
        // Load ghost model
        loadGhostModel();
        
        // Apply initial rotation based on direction
        updateRotation();
    }
    
    /**
     * Load the ghost model and apply green appearance
     */
    private void loadGhostModel() {
        try {
            // Create ObjectFile loader
            ObjectFile loader = new ObjectFile(ObjectFile.RESIZE);
            loader.setFlags(ObjectFile.RESIZE | ObjectFile.TRIANGULATE | ObjectFile.STRIPIFY);

            // Load the model
            Scene modelScene = loader.load(MODEL_PATH);
            BranchGroup modelBG = modelScene.getSceneGroup();
            
            // Create green appearance
            Appearance greenAppearance = new Appearance();
            Color3f greenColor = new Color3f(0.0f, 1.0f, 0.0f); // Green for NPCs
            Material material = new Material(
                greenColor,                     // Ambient color
                new Color3f(0.1f, 0.1f, 0.1f),  // Emissive color
                greenColor,                     // Diffuse color
                new Color3f(1.0f, 1.0f, 1.0f),  // Specular color
                64.0f                           // Shininess
            );
            greenAppearance.setMaterial(material);
            
            // Apply appearance to all shapes in the model
            applyAppearanceToModel(modelBG, greenAppearance);

            // Scale the model
            Transform3D modelScale = new Transform3D();
            modelScale.setScale(MODEL_SCALE);
            TransformGroup modelScaleTG = new TransformGroup(modelScale);
            modelScaleTG.addChild(modelBG);

            // Add the scaled model to the rotation group
            rotationTG.addChild(modelScaleTG);
            
        } catch (Exception e) {
            System.err.println("Error loading ghost model: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to a box if model loading fails
            Appearance greenAppearance = new Appearance();
            greenAppearance.setMaterial(new Material(
                    new Color3f(0.0f, 1.0f, 0.0f),
                    new Color3f(0.0f, 0.0f, 0.0f),
                    new Color3f(0.0f, 1.0f, 0.0f),
                    new Color3f(1.0f, 1.0f, 1.0f),
                    64.0f));
            Box npcBox = new Box(0.03f, 0.03f, 0.03f, Box.GENERATE_NORMALS, greenAppearance);
            rotationTG.addChild(npcBox);
        }
    }
    
    /**
     * Apply appearance to all shapes in the model
     */
    private void applyAppearanceToModel(Node node, Appearance appearance) {
        if (node instanceof Shape3D) {
            Shape3D shape = (Shape3D) node;
            shape.setAppearance(appearance);
        } else if (node instanceof Group) {
            Group group = (Group) node;
            for (int i = 0; i < group.numChildren(); i++) {
                applyAppearanceToModel(group.getChild(i), appearance);
            }
        }
    }
    
    /**
     * Update rotation based on current direction
     */
    private void updateRotation() {
        Transform3D rotTransform = new Transform3D();
        
        // Determine rotation based on primary movement direction
        if (Math.abs(direction.x) > Math.abs(direction.z)) {
            // Moving primarily along X axis
            if (direction.x > 0) {
                // Right
                rotTransform.rotY(Math.PI/2);
            } else {
                // Left
                rotTransform.rotY(-Math.PI/2);
            }
        } else {
            // Moving primarily along Z axis
            if (direction.z < 0) {
                // Up
                rotTransform.rotY(Math.PI);
            } else {
                // Down (default orientation)
                // No rotation needed
            }
        }
        
        // Apply rotation - this only affects the rotation transform group
        rotationTG.setTransform(rotTransform);
    }

    public Vector3d getDirection() {
        return new Vector3d(direction);
    }

    public Vector3d getPosition() {
        return new Vector3d(position);
    }

    public TransformGroup getTransformGroup() {
        return positionTG; // Return the root transform group
    }
    
    public static double getCharacterHalf() {
        return CHARACTER_HALF;
    }

    /**
     * Update the NPC's position. Change direction if a collision occurs.
     */
    public void update(CollisionChecker checker) {
        // Calculate new position based on current direction
        double newX = position.x + direction.x * step;
        double newZ = position.z + direction.z * step;

        if (checker.collides(newX, newZ)) {
            // Try changing direction up to 4 times
            Vector3d oldDirection = new Vector3d(direction);
            boolean directionChanged = false;
            
            for (int i = 0; i < 4; i++) {
                direction = randomizeDirection();
                
                // Skip if we randomly chose the same direction
                if (oldDirection.equals(direction)) {
                    continue;
                }
                
                newX = position.x + direction.x * step;
                newZ = position.z + direction.z * step;
                
                if (!checker.collides(newX, newZ)) {
                    directionChanged = true;
                    break;
                }
            }
            
            if (directionChanged) {
                // Direction changed, update rotation
                updateRotation();
            } else {
                // No valid move found, don't change position
                return;
            }
        }

        // Update position
        position.x = newX;
        position.z = newZ;

        // Update position transform - this doesn't affect rotation
        Transform3D posTransform = new Transform3D();
        posTransform.setTranslation(position);
        positionTG.setTransform(posTransform);
    }

    // Helper method to randomize direction when collision occurs
    private Vector3d randomizeDirection() {
        Random rand = new Random();
        int choice = rand.nextInt(4);
        switch (choice) {
            case 0: return new Vector3d(1, 0, 0);  // Right
            case 1: return new Vector3d(-1, 0, 0); // Left
            case 2: return new Vector3d(0, 0, 1);  // Down
            default: return new Vector3d(0, 0, -1); // Up
        }
    }

    /**
     * Creates an NPC at a random valid position with a random cardinal direction.
     */
    public static NPC generateRandomNPC(List<Vector3d> validPositions, Appearance appearance, double step) {
        if (validPositions.isEmpty()) {
            throw new IllegalArgumentException("No valid positions available");
        }

        int index = (int) (Math.random() * validPositions.size());
        Vector3d pos = validPositions.remove(index);

        Random rand = new Random();
        int choice = rand.nextInt(4);
        Vector3d dir;
        switch (choice) {
            case 0:
                dir = new Vector3d(1, 0, 0);  // Right
                break;
            case 1:
                dir = new Vector3d(-1, 0, 0); // Left
                break;
            case 2:
                dir = new Vector3d(0, 0, 1);  // Down
                break;
            default:
                dir = new Vector3d(0, 0, -1); // Up
                break;
        }
        return new NPC(pos, dir, step, appearance);
    }
}
