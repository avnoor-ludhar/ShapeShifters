package ShapeShifters;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import org.jogamp.java3d.*;
import org.jogamp.java3d.loaders.Scene;
import org.jogamp.java3d.loaders.objectfile.ObjectFile;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.java3d.utils.geometry.Primitive;
import org.jogamp.java3d.utils.geometry.Cylinder;
import org.jogamp.java3d.utils.geometry.Cylinder;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.java3d.utils.picking.PickTool;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.vecmath.*;
import org.jogamp.java3d.utils.picking.PickTool;

public class AppearanceCycleBehavior extends Behavior {
    private WakeupCondition wakeupCondition;
    private Node targetShape;
    public Appearance originalAppearance;
    public Appearance newAppearance;
    private long changeTime;
    private int state; // 0: eligible, 1: new appearance active, 2: cooldown
    private GhostModel ghost;
    public PrintWriter pw;
    public AppearanceCycleBehavior(Node targetShape, GhostModel g, PrintWriter pw) {
        this.targetShape = targetShape;
        ghost = g;
        this.originalAppearance = new Appearance();
        Color3f modelColor = new Color3f(0.2f, 0.2f, 1.0f);
        Material material = new Material(
                modelColor,                      // Ambient color
                new Color3f(0.1f, 0.1f, 0.1f),   // Emissive color
                modelColor,                      // Diffuse color
                new Color3f(1.0f, 1.0f, 1.0f),   // Specular color
                64.0f                            // Shininess
        );
        this.originalAppearance.setMaterial(material);
        this.pw = pw;
        Appearance greenAppearance = new Appearance();
        Color3f greenColor = new Color3f(0.0f, 1.0f, 0.0f); // Green for NPCs
        Material material2 = new Material(
                greenColor,                     // Ambient color
                new Color3f(0.1f, 0.1f, 0.1f),  // Emissive color
                greenColor,                     // Diffuse color
                new Color3f(1.0f, 1.0f, 1.0f),  // Specular color
                64.0f                           // Shininess
        );
        greenAppearance.setMaterial(material2);

        this.newAppearance = greenAppearance;

        this.state = 0; // initially eligible for change
    }

    private void updateAppearance(Node node, Appearance app) {
        if (node instanceof Shape3D) {
//            ((Shape3D)node).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
            ((Shape3D) node).setAppearance(app);
        } else if (node instanceof Group) {
            Group group = (Group) node;
            for (int i = 0; i < group.numChildren(); i++) {
                updateAppearance(group.getChild(i), app);
            }
        }
    }

    @Override
    public void initialize() {
        // Wake up every 100 ms to check time.
        wakeupCondition = new WakeupOnElapsedTime(100);
        wakeupOn(wakeupCondition);
    }

    // Call this method when the stimulus happens.
    public void triggerChange() {
        if (state == 0) { // eligible
            pw.println("GREEN");
            updateAppearance(targetShape, newAppearance);
            ghost.step = .002;
            changeTime = System.currentTimeMillis();
            state = 1; // new appearance active
        }
    }

    @Override
    public void processStimulus(Iterator<WakeupCriterion> criteria) {
        long now = System.currentTimeMillis();
        if (state == 1 && (now - changeTime >= 5000)) {
            // 10 seconds passed: revert to original appearance.
            pw.println("BLUE");
            updateAppearance(targetShape, originalAppearance);
            ghost.step = .01;
//            targetShape.setAppearance(originalAppearance);
            changeTime = now;
            state = 2; // now in cooldown
        } else if (state == 2 && (now - changeTime >= 20000)) {
            // 20 seconds cooldown passed: eligible for new change.
            state = 0;
        }
        wakeupOn(new WakeupOnElapsedTime(100));
    }
}
