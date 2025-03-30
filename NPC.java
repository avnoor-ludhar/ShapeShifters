package ShapeShifters;

import java.util.List;
import java.util.Random;
import org.jogamp.java3d.*;
import org.jogamp.java3d.loaders.Scene;
import org.jogamp.java3d.loaders.objectfile.ObjectFile;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.vecmath.*;

// Class that represents a non-player character
public class NPC {
    // Model constants
    private static final double MODEL_SCALE = 0.05;
    private static final String MODEL_PATH = "src/ShapeShifters/assets/ghost.obj";
    private static final double CHARACTER_HALF = 0.035; // Match this with GhostModel or adjust as needed
    
    // Transform hierarchy
    private TransformGroup positionTG; // Root TG - handles position only
    private TransformGroup rotationTG; // Child TG - handles rotation only
    
    private Vector3d position;
    private Vector3d direction;
    double step;

    private GhostModel userGhost;

    public interface CollisionChecker {
        boolean collides(double x, double z);
    }

    // Initializes the NPC’s position, direction, model, and rotation
    public NPC(Vector3d pos, Vector3d dir, double step, Appearance unusedAppearance) {
        this.position = new Vector3d(pos);
        this.direction = new Vector3d(dir);
        
        // Normalize the direction if it's diagonal
        if (dir.x != 0 && dir.z != 0) {
            double length = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            this.direction.x = dir.x / length;
            this.direction.z = dir.z / length;
        }
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
        
        // Print debug info at creation
        System.out.println("NPC created with direction: " + direction);
    }

    // Loads the 3D ghost model and applies green appearance
    // Adds LOD support or fallback box
    private void loadGhostModel() {
        try {
            // Create ObjectFile loader
            ObjectFile loader = new ObjectFile(ObjectFile.RESIZE);
            loader.setFlags(ObjectFile.RESIZE | ObjectFile.TRIANGULATE | ObjectFile.STRIPIFY);

            // Load the model
            Scene modelScene = loader.load(MODEL_PATH);
            BranchGroup modelBG = modelScene.getSceneGroup();
            modelBG.setCapability(BranchGroup.ALLOW_PICKABLE_READ);
            
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
            
            // This ensures the model is facing the correct direction initially (down)
            Transform3D modelOrientation = new Transform3D();
            TransformGroup orientationTG = new TransformGroup(modelOrientation);
            orientationTG.addChild(modelBG);

            // Scale the model
            Transform3D modelScale = new Transform3D();
            modelScale.setScale(MODEL_SCALE);
            TransformGroup modelScaleTG = new TransformGroup(modelScale);
            modelScaleTG.addChild(orientationTG);

            // Create simplified models for LOD that match the ghost dimensions
            Node mediumDetailNode = createSimplifiedGhost(greenAppearance, 0.8);
            Node lowDetailNode = createSimplifiedGhost(greenAppearance, 0.6);
            
            // Set appropriate LOD distances
                // Not too close, not too far
            double[] distances = {1.2, 1.5, 2.0};
            
            // Create LOD node with all detail levels
            BranchGroup lodBG = LODHelper.createLOD(modelScaleTG, mediumDetailNode, lowDetailNode, distances);
            
            // Add the LOD setup to the rotation group
            rotationTG.addChild(lodBG);
            
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

    // Creates a simplified green box for medium/low LOD rendering
    private Node createSimplifiedGhost(Appearance appearance, double scaleFactor) {
        // Create a box similar to the ghost size
        // Use a smaller box - CHARACTER_HALF is 0.03, GhostModel uses 0.02
        // This will make the box similar in size to the ghost model
        float boxSize = 0.025f;
        
        TransformGroup tg = new TransformGroup();
        
        // Create a box with the ghost's green color
        Box box = new Box(boxSize, boxSize, boxSize, Box.GENERATE_NORMALS, appearance);
        tg.addChild(box);
        
        // Apply the scale factor
        Transform3D transform = new Transform3D();
        transform.setScale(scaleFactor);
        TransformGroup scaleTG = new TransformGroup(transform);
        scaleTG.addChild(tg);
        
        return scaleTG;
    }

    // Recursively applies the green appearance to all parts of the model
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

    // Updates the NPCs rotation based on direction vector
    public void updateRotation() {
        // Create a fresh transform for rotation
        Transform3D rotTransform = new Transform3D();
        
        // Calculate rotation angle based on direction vector
        double angle = 0.0;
        
        // Calculate angle from direction vector
        // This matches the GhostModel's rotation logic
        if (Math.abs(direction.x) > 0 && Math.abs(direction.z) > 0) {
            // Diagonal movement
            if (direction.x < 0 && direction.z > 0) {
                // Down left
                angle = -Math.PI/4;
            }
            else if (direction.x < 0 && direction.z < 0) {
                // Up left
                angle = -3*Math.PI/4;
            }
            else if (direction.x > 0 && direction.z > 0) {
                // Down right
                angle = Math.PI/4;
            }
            else {
                // Up right
                angle = 3*Math.PI/4;
            }
        }
        else if (Math.abs(direction.x) > Math.abs(direction.z)) {
            // Moving primarily along X axis
            if (direction.x > 0) {
                // Right
                angle = Math.PI/2;
            }
            else {
                // Left
                angle = -Math.PI/2;
            }
        }
        else {
            // Moving primarily along Z axis
            if (direction.z < 0) {
                // Up
                angle = Math.PI;
            }
            else {
                // Down (default orientation)
                angle = 0.0;
            }
        }
        
        // Apply rotation around Y axis
        rotTransform.rotY(angle);
        
        // Apply the transform to the rotation group
        rotationTG.setTransform(rotTransform);
    }

    // Gets the direction of the NPC
    public Vector3d getDirection() {
        return new Vector3d(direction);
    }

    // Gets the position of the NPC
    public Vector3d getPosition() {
        return new Vector3d(position);
    }

    // How the NPC bounces off surfaces
    public void bounce() {
        // Reverse the direction vector
        direction.scale(-1);
        // Push back further than one step to prevent sticking
        position.x += direction.x * step * 2;
        position.z += direction.z * step * 2;
        updateRotation();
        Transform3D posTransform = new Transform3D();
        posTransform.setTranslation(position);
        positionTG.setTransform(posTransform);
    }

    // Sets position of NPC
    public void setPosition(Vector3d newPos) {
        this.position = new Vector3d(newPos);
    }

    // TG
    public TransformGroup getTransformGroup() {
        return positionTG;
    }

    // Gets the middle of the NPC model
    public static double getCharacterHalf() {
        return CHARACTER_HALF;
    }

    // Sets the direction of the NPC
    public void setDirection(Vector3d newDir) {
        this.direction.set(newDir);
        this.direction.normalize();
        updateRotation();
    }

    public double getStep() {
        return this.step;
    }

    // Updates NPC position and handles collisions with maze and player
    public void update(CollisionChecker checker, GhostModel userGhost) {
        // Calculate new position based on current direction
        double newX = position.x + direction.x * step;
        double newZ = position.z + direction.z * step;

        boolean directionChanged = false;
        
        if (checker.collides(newX, newZ) || CollisionDetector.collidesWithUser(newX, newZ, NPC.getCharacterHalf(), userGhost)) {
            // Store old direction for comparison
            Vector3d oldDirection = new Vector3d(direction);
            
            // Try changing direction up to 4 times
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
            
            if (!directionChanged) {
                // No valid move found
                // Dont change position
                return;
            }
        }

        // Update position
        position.x = newX;
        position.z = newZ;

        // Update position transform
        Transform3D posTransform = new Transform3D();
        posTransform.setTranslation(position);
        positionTG.setTransform(posTransform);

        if (directionChanged) {
            // Force rotation update when direction changes
            updateRotation();
        }
        
        // Update LOD position for LOD behavior
        updateLODPositions();
    }

    // Updates the LOD system with current NPC position
    private void updateLODPositions() {
        // Search for DistanceLOD behaviors in the scene graph
        if (rotationTG != null) {
            updateLODPositionsInGroup(rotationTG);
        }
    }

    private void updateLODPositionsInGroup(Node node) {
        if (node instanceof DistanceLOD) {
            // Update the position of the LOD behavior
            LODHelper.updateLODPosition((DistanceLOD) node, position);
        }
        else if (node instanceof Group) {
            Group group = (Group) node;
            // Search all children
            for (int i = 0; i < group.numChildren(); i++) {
                updateLODPositionsInGroup(group.getChild(i));
            }
        }
    }

    // Helper method to randomize direction when collision occurs
        // Supports 8 directions
    private Vector3d randomizeDirection() {
        Random rand = new Random();
        int choice = rand.nextInt(8);
        Vector3d newDir;
        
        switch (choice) {
            case 0: newDir = new Vector3d(1, 0, 0);
                break;
            case 1: newDir = new Vector3d(-1, 0, 0);
                break;
            case 2: newDir = new Vector3d(0, 0, 1);
                break;
            case 3: newDir = new Vector3d(0, 0, -1);
                break;
            case 4: newDir = new Vector3d(1, 0, 1);
                break;
            case 5: newDir = new Vector3d(-1, 0, 1);
                break;
            case 6: newDir = new Vector3d(1, 0, -1);
                break;
            default: newDir = new Vector3d(-1, 0, -1);
                break;
        }
        
        // Normalize diagonal directions
        if (newDir.x != 0 && newDir.z != 0) {
            double length = Math.sqrt(newDir.x * newDir.x + newDir.z * newDir.z);
            newDir.x = newDir.x / length;
            newDir.z = newDir.z / length;
        }
        
        return newDir;
    }

    // Creating an NPC at a random position
    public static NPC generateRandomNPC(List<Vector3d> validPositions, Appearance appearance, double step) {
        if (validPositions.isEmpty()) {
            throw new IllegalArgumentException("No valid positions available");
        }

        int index = (int) (Math.random() * validPositions.size());
        Vector3d pos = validPositions.remove(index);

        Random rand = new Random();
        int choice = rand.nextInt(8);
        Vector3d dir;
        
        switch (choice) {
            case 0: dir = new Vector3d(1, 0, 0);
                break;
            case 1: dir = new Vector3d(-1, 0, 0);
                break;
            case 2: dir = new Vector3d(0, 0, 1);
                break;
            case 3: dir = new Vector3d(0, 0, -1);
                break;
            case 4: dir = new Vector3d(1, 0, 1);
                break;
            case 5: dir = new Vector3d(-1, 0, 1);
                break;
            case 6: dir = new Vector3d(1, 0, -1);
                break;
            default: dir = new Vector3d(-1, 0, -1);
                break;
        }
        
        // Normalize diagonal directions
        if (dir.x != 0 && dir.z != 0) {
            double length = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            dir.x = dir.x / length;
            dir.z = dir.z / length;
        }
        
        return new NPC(pos, dir, step, appearance);
    }


    // Updates direction of NPC model after bounce
    public void updateDirection(Vector3d newDirection) {
        // Normalize the direction
        if (newDirection.x != 0 && newDirection.z != 0) {
            double length = Math.sqrt(newDirection.x * newDirection.x + newDirection.z * newDirection.z);
            newDirection.x = newDirection.x / length;
            newDirection.z = newDirection.z / length;
        }
        
        // Update direction
        this.direction = newDirection;
        
        // Update rotation based on new direction
        updateRotation();
    }
}