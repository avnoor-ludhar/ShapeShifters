package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;

// utility class for creating and updating LOD (level of detail)
public class LODHelper {

    // creates LOD node with three levels of detail based on distance
    public static BranchGroup createLOD(Node highDetailNode, Node mediumDetailNode,
                                        Node lowDetailNode, double[] distancesDouble) {

        // convert distances to float
        float[] distances = new float[distancesDouble.length];
        for (int i = 0; i < distancesDouble.length; i++) {
            distances[i] = (float)distancesDouble[i];
        }

        // create switch node for detail levels
        Switch detailSwitch = new Switch();
        detailSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);

        // add detail nodes
        detailSwitch.addChild(highDetailNode);
        detailSwitch.addChild(mediumDetailNode);
        detailSwitch.addChild(lowDetailNode);

        // create LOD behavior
        DistanceLOD lod = new DistanceLOD(distances, new Point3f(0.0f, 0.0f, 0.0f));
        lod.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
        lod.addSwitch(detailSwitch);

        // attach switch and LOD to branch group
        BranchGroup lodBG = new BranchGroup();
        lodBG.addChild(detailSwitch);
        lodBG.addChild(lod);

        return lodBG;
    }

    // updates LOD position to match object
    public static void updateLODPosition(DistanceLOD lodBehavior, Vector3d position) {
        lodBehavior.setPosition(new Point3f((float)position.x, (float)position.y, (float)position.z));
    }

    // creates simplified ghost model for low detail rendering
    public static Node createSimplifiedGhost(boolean isRedPlayer, double scale) {
        Appearance appearance = new Appearance();
        Material material = new Material();

        // set color by player type
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

        // create box shape with appearance
        TransformGroup tg = new TransformGroup();
        org.jogamp.java3d.utils.geometry.Box box =
                new org.jogamp.java3d.utils.geometry.Box(0.05f, 0.05f, 0.05f, appearance);
        tg.addChild(box);

        // apply scale
        Transform3D transform = new Transform3D();
        transform.setScale(scale);
        TransformGroup scaleTG = new TransformGroup(transform);
        scaleTG.addChild(tg);

        return scaleTG;
    }

    // creates simplified NPC model for low detail rendering
    public static Node createSimplifiedNPC(double scale) {
        Appearance appearance = new Appearance();
        Material material = new Material();
        material.setDiffuseColor(new Color3f(0.0f, 1.0f, 0.0f));
        material.setAmbientColor(new Color3f(0.0f, 1.0f, 0.0f));
        material.setEmissiveColor(new Color3f(0.1f, 0.1f, 0.1f));
        material.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
        material.setShininess(64.0f);
        appearance.setMaterial(material);

        // create box shape with appearance
        TransformGroup tg = new TransformGroup();
        org.jogamp.java3d.utils.geometry.Box box =
                new org.jogamp.java3d.utils.geometry.Box(0.05f, 0.05f, 0.05f, appearance);
        tg.addChild(box);

        // apply scale
        Transform3D transform = new Transform3D();
        transform.setScale(scale);
        TransformGroup scaleTG = new TransformGroup(transform);
        scaleTG.addChild(tg);

        return scaleTG;
    }
}
