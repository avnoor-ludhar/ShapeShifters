package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;

/**
 * A utility class for implementing Level of Detail (LOD) in the game.
 * Makes it easy to create different detail levels for any 3D object.
 */
public class LODHelper {
    
    /**
     * Creates a Level of Detail (LOD) node for any model.
     * 
     * @param highDetailNode The high detail representation (used when close)
     * @param mediumDetailNode The medium detail representation (used at medium distance)
     * @param lowDetailNode The low detail representation (used when far away)
     * @param distances Array of distances at which to switch levels [close, medium, far]
     * @return A BranchGroup containing the LOD setup
     */
    public static BranchGroup createLOD(Node highDetailNode, Node mediumDetailNode, 
                                       Node lowDetailNode, double[] distancesDouble) {
        
        // Convert double[] distances to float[]
        float[] distances = new float[distancesDouble.length];
        for (int i = 0; i < distancesDouble.length; i++) {
            distances[i] = (float)distancesDouble[i];
        }
        
        // Create a Switch to hold the different detail levels
        Switch detailSwitch = new Switch();
        detailSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
        
        // Add the detail levels to the switch
        detailSwitch.addChild(highDetailNode);
        detailSwitch.addChild(mediumDetailNode);
        detailSwitch.addChild(lowDetailNode);
        
        // Create a DistanceLOD behavior with correct types (float[] and Point3f)
        DistanceLOD lod = new DistanceLOD(distances, new Point3f(0.0f, 0.0f, 0.0f));
        lod.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
        lod.addSwitch(detailSwitch);
        
        // Create a BranchGroup to hold the Switch and LOD behavior
        BranchGroup lodBG = new BranchGroup();
        lodBG.addChild(detailSwitch);
        lodBG.addChild(lod);
        
        return lodBG;
    }
    
    /**
     * Updates the position of an LOD behavior to match the position of a moving object.
     * 
     * @param lodBehavior The DistanceLOD behavior to update
     * @param position The new position
     */
    public static void updateLODPosition(DistanceLOD lodBehavior, Vector3d position) {
        // Cast the double values to float for Point3f
        lodBehavior.setPosition(new Point3f((float)position.x, (float)position.y, (float)position.z));
    }
    
    /**
     * Creates a simplified version of a ghost model for use as lower detail levels.
     * 
     * @param isRedPlayer Whether this is the red player
     * @param scale Scale factor for the model
     * @return A simplified ghost model
     */
    public static Node createSimplifiedGhost(boolean isRedPlayer, double scale) {
        // Create a simple box to represent the ghost at a distance
        Appearance appearance = new Appearance();
        Material material = new Material();
        
        // Set color based on player
        if (isRedPlayer) {
            material.setDiffuseColor(new Color3f(1.0f, 0.2f, 0.2f));
            material.setAmbientColor(new Color3f(1.0f, 0.2f, 0.2f));
        } else {
            material.setDiffuseColor(new Color3f(0.2f, 0.2f, 1.0f));
            material.setAmbientColor(new Color3f(0.2f, 0.2f, 1.0f));
        }
        
        material.setEmissiveColor(new Color3f(0.1f, 0.1f, 0.1f));
        material.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
        material.setShininess(64.0f);
        appearance.setMaterial(material);
        
        // Create a simple shape with the appearance
        TransformGroup tg = new TransformGroup();
        org.jogamp.java3d.utils.geometry.Box box = 
            new org.jogamp.java3d.utils.geometry.Box(0.05f, 0.05f, 0.05f, appearance);
        tg.addChild(box);
        
        // Scale the model
        Transform3D transform = new Transform3D();
        transform.setScale(scale);
        TransformGroup scaleTG = new TransformGroup(transform);
        scaleTG.addChild(tg);
        
        return scaleTG;
    }
    
    /**
     * Creates a simplified version of an NPC for use as lower detail levels.
     * 
     * @param scale Scale factor for the model
     * @return A simplified NPC model
     */
    public static Node createSimplifiedNPC(double scale) {
        // Create a simple green box for NPCs at a distance
        Appearance appearance = new Appearance();
        Material material = new Material();
        material.setDiffuseColor(new Color3f(0.0f, 1.0f, 0.0f));
        material.setAmbientColor(new Color3f(0.0f, 1.0f, 0.0f));
        material.setEmissiveColor(new Color3f(0.1f, 0.1f, 0.1f));
        material.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
        material.setShininess(64.0f);
        appearance.setMaterial(material);
        
        // Create a simple shape with the appearance
        TransformGroup tg = new TransformGroup();
        org.jogamp.java3d.utils.geometry.Box box = 
            new org.jogamp.java3d.utils.geometry.Box(0.05f, 0.05f, 0.05f, appearance);
        tg.addChild(box);
        
        // Scale the model
        Transform3D transform = new Transform3D();
        transform.setScale(scale);
        TransformGroup scaleTG = new TransformGroup(transform);
        scaleTG.addChild(tg);
        
        return scaleTG;
    }
}