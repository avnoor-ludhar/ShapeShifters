package ShapeShifters;

import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.vecmath.*;

// Class that initializes the animation tht shows up when the game is over
public class GameEndAnimation {
    private SimpleUniverse universe;
    private BranchGroup sceneBG;

    // Constructor for the universe and the scenes root BG
    public GameEndAnimation(SimpleUniverse universe, BranchGroup sceneBG) {
        this.universe = universe;
        this.sceneBG = sceneBG;
    }

    // Triggers the end of game animation
    public void triggerGameEnd(String winningTeam) {
        // Camera setup (unchanged)
        Transform3D viewTransform = new Transform3D();
        Point3d eye = new Point3d(0.0, 4.0, 0.0);
        Point3d center = new Point3d(0.0, 0.0, 0.0);
        Vector3d up = new Vector3d(0, 0, -1);
        viewTransform.lookAt(eye, center, up);
        viewTransform.invert();
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);

        // Lighting adjustments
        BranchGroup endLightBG = new BranchGroup();
        endLightBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        endLightBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

        // Reduced ambient light
        AmbientLight brightAmbient = new AmbientLight(new Color3f(0.1f, 0.1f, 0.1f)); // Reduced from 0.2f
        brightAmbient.setInfluencingBounds(new BoundingSphere(new Point3d(0, 0, 0), 100.0));
        endLightBG.addChild(brightAmbient);

        // Adjusted directional light (less intense, better angle)
        DirectionalLight endDirectional = new DirectionalLight(new Color3f(0.5f, 0.5f, 0.5f), new Vector3f(-0.5f, -1.0f, -0.5f));
        endDirectional.setInfluencingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        endLightBG.addChild(endDirectional);

        sceneBG.addChild(endLightBG);

        Color3f textColor = new Color3f(1.0f, 1.0f, 1.0f);

        // Sets up the text for the "Win" label at the end of the game
        Transform3D labelTransform = new Transform3D();
        labelTransform.setTranslation(new Vector3d(0.0, 3.0, 0.0));
        Transform3D orient = new Transform3D();
        orient.rotX(-Math.PI / 2);
        labelTransform.mul(orient);
        TransformGroup labelTG = new TransformGroup(labelTransform);
        String labelText = winningTeam + " Wins";
        ColorString label = new ColorString(labelText, textColor, 0.1, new Point3f(0, 0, 0));
        labelTG.addChild(label.getTransformGroup());

        BranchGroup labelBG = new BranchGroup();

        // Allows for dynamic changes to this group
        labelBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        labelBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

        labelBG.addChild(labelTG);

        sceneBG.addChild(labelBG);
    }
}
