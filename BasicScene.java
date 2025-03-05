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
    private TransformGroup redBoxTG;
    private Vector3d redBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private final double STEP = 0.1;

    public BasicScene() {}

    public BranchGroup createScene() {
        BranchGroup sceneBG = new BranchGroup();

        TransformGroup platformTG = new TransformGroup();
        Appearance platformAppearance = new Appearance();
        platformAppearance.setMaterial(new Material(
                new Color3f(0.8f, 0.8f, 0.8f),
                new Color3f(0.2f, 0.2f, 0.2f),
                new Color3f(0.8f, 0.8f, 0.8f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
        Box platform = new Box(1.0f, 0.05f, 1.0f, Box.GENERATE_NORMALS, platformAppearance);
        platformTG.addChild(platform);
        sceneBG.addChild(platformTG);

        Transform3D redBoxTransform = new Transform3D();
        redBoxTransform.setTranslation(redBoxPos);
        redBoxTG = new TransformGroup(redBoxTransform);
        redBoxTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Appearance redAppearance = new Appearance();
        redAppearance.setMaterial(new Material(
                new Color3f(1.0f, 0.0f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(1.0f, 0.0f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
        Box redBox = new Box(0.03f, 0.03f, 0.03f, Box.GENERATE_NORMALS, redAppearance);
        redBoxTG.addChild(redBox);
        sceneBG.addChild(redBoxTG);

        Appearance wallAppearance = new Appearance();
        wallAppearance.setMaterial(new Material(
                new Color3f(0.5f, 0.5f, 0.5f),
                new Color3f(0.1f, 0.1f, 0.1f),
                new Color3f(0.5f, 0.5f, 0.5f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));

        float wallThickness = 0.01f;
        float wallHeight = 0.15f;
        float innerWallThickness = 0.005f;

        addWall(sceneBG, -1.0, wallHeight / 2, 0.0, wallThickness, wallHeight, 1.0f, wallAppearance);
        addWall(sceneBG, 1.0, wallHeight / 2, 0.0, wallThickness, wallHeight, 1.0f, wallAppearance);
        addWall(sceneBG, 0.0, wallHeight / 2, -1.0, 1.0f, wallHeight, wallThickness, wallAppearance);
        addWall(sceneBG, 0.0, wallHeight / 2, 1.0, 1.0f, wallHeight, wallThickness, wallAppearance);

        double[][] mazeWalls = {
                {-0.5, 0.75, 0.5, innerWallThickness}, {-0.5, 0.5, innerWallThickness, 0.25},
                {0.5, 0.5, 0.5, innerWallThickness}, {0.1, 0.25, innerWallThickness, 0.25},
                {-0.5, -0.25, 0.3, innerWallThickness}, {-0.5, 0, innerWallThickness, 0.25},
                {0.5, -0.5, 0.5, innerWallThickness}, {0.5, -0.25, innerWallThickness, 0.25},
                {-0.7, -0.65, 0.3, innerWallThickness}, {-0.4, -0.65, innerWallThickness, 0.15}

        };

        for (double[] wall : mazeWalls) {
            addWall(sceneBG, wall[0], wallHeight / 2, wall[1], wall[2], wallHeight, wall[3], wallAppearance);
        }

        return sceneBG;
    }

    private void addWall(BranchGroup sceneBG, double x, double y, double z, double width, double height, double depth, Appearance appearance) {
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(x, y, z));
        TransformGroup tg = new TransformGroup(transform);
        Box wall = new Box((float) width, (float) height, (float) depth, Box.GENERATE_NORMALS, appearance);
        tg.addChild(wall);
        sceneBG.addChild(tg);
    }

    public void setupUniverse(BranchGroup sceneBG) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas = new Canvas3D(config);

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

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        SimpleUniverse universe = new SimpleUniverse(canvas);
        Point3d eye = new Point3d(0, 2.0, 2.5);
        Point3d center = new Point3d(0.0, 0.0, 0.0);
        Vector3d up = new Vector3d(0.0, 0.0, -1.0);
        Transform3D viewTransform = new Transform3D();
        viewTransform.lookAt(eye, center, up);
        viewTransform.invert();
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);

        sceneBG.compile();
        universe.addBranchGraph(sceneBG);

        setLayout(new BorderLayout());
        add("Center", canvas);
    }

    public static void main(String[] args) {
        frame = new JFrame("Basic Scene: Maze View");
        BasicScene basicScene = new BasicScene();
        BranchGroup sceneBG = basicScene.createScene();
        basicScene.setupUniverse(sceneBG);
        frame.getContentPane().add(basicScene);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
