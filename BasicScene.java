package ShapeShifters;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.vecmath.*;

public class BasicScene extends JPanel {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;

    // TransformGroup for the movable red box.
    private TransformGroup redBoxTG;
    // Current translation for the red box.
    private Vector3d redBoxPos = new Vector3d(0.0, 0.1, 0.0);
    // Movement step size.
    private final double STEP = 0.1;

    public BasicScene() {
        // (We no longer add a KeyListener to the panel here.)
    }

    // Instance method to create the scene graph.
    public BranchGroup createScene() {
        BranchGroup sceneBG = new BranchGroup();

        // Create platform.
        TransformGroup platformTG = new TransformGroup();
        Appearance platformAppearance = new Appearance();
        platformAppearance.setMaterial(new Material(
                new Color3f(0.8f, 0.8f, 0.8f),  // ambient
                new Color3f(0.2f, 0.2f, 0.2f),  // emissive
                new Color3f(0.8f, 0.8f, 0.8f),  // diffuse
                new Color3f(1.0f, 1.0f, 1.0f),  // specular
                64.0f));                       // shininess
        Box platform = new Box(1.0f, 0.05f, 1.0f, Box.GENERATE_NORMALS, platformAppearance);
        platformTG.addChild(platform);
        sceneBG.addChild(platformTG);

        // Create movable red box.
        Transform3D redBoxTransform = new Transform3D();
        redBoxTransform.setTranslation(redBoxPos);
        redBoxTG = new TransformGroup(redBoxTransform);
        redBoxTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Appearance redAppearance = new Appearance();
        redAppearance.setMaterial(new Material(
                new Color3f(1.0f, 0.0f, 0.0f),  // ambient: red
                new Color3f(0.0f, 0.0f, 0.0f),  // emissive: black
                new Color3f(1.0f, 0.0f, 0.0f),  // diffuse: red
                new Color3f(1.0f, 1.0f, 1.0f),  // specular: white
                64.0f));                       // shininess
        Box redBox = new Box(0.1f, 0.1f, 0.1f, Box.GENERATE_NORMALS, redAppearance);
        redBoxTG.addChild(redBox);
        sceneBG.addChild(redBoxTG);

        // Add lighting.
        Color3f lightColor = new Color3f(1.0f, 1.0f, 1.0f);
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        Vector3f lightDirection = new Vector3f(-1.0f, -1.0f, -1.0f);
        DirectionalLight directionalLight = new DirectionalLight(lightColor, lightDirection);
        directionalLight.setInfluencingBounds(bounds);
        sceneBG.addChild(directionalLight);

        return sceneBG;
    }

    public void setupUniverse(BranchGroup sceneBG) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas = new Canvas3D(config);

        // Attach the KeyListener to the Canvas3D.
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'w': redBoxPos.z -= STEP; break;
                    case 's': redBoxPos.z += STEP; break;
                    case 'a': redBoxPos.x -= STEP; break;
                    case 'd': redBoxPos.x += STEP; break;
                }
                Transform3D newTransform = new Transform3D();
                newTransform.setTranslation(redBoxPos);
                redBoxTG.setTransform(newTransform);
            }
        });

        // Request focus on the canvas to capture key events.
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        SimpleUniverse universe = new SimpleUniverse(canvas);
        Point3d eye = new Point3d(0, 1.75, 3);
        Point3d center = new Point3d(0.0, 0.0, 0.0);
        Vector3d up = new Vector3d(0.0, 1.0, 0.0);
        Transform3D viewTransform = new Transform3D();
        viewTransform.lookAt(eye, center, up);
        viewTransform.invert();
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);

        sceneBG.compile();
        universe.addBranchGraph(sceneBG);

        // Add the canvas to this panel.
        setLayout(new BorderLayout());
        add("Center", canvas);
    }

    public static void main(String[] args) {
        frame = new JFrame("Basic Scene: Platform View");
        BasicScene basicScene = new BasicScene();
        BranchGroup sceneBG = basicScene.createScene();
        basicScene.setupUniverse(sceneBG);
        frame.getContentPane().add(basicScene);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
