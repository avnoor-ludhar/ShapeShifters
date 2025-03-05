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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class BasicScene extends JPanel {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;
    private TransformGroup redBoxTG;
    private Vector3d redBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private final double STEP = 0.05; // lowered so walls cant be skipped over
    private List<Rectangle2D.Double> wallBounds = new ArrayList<>();
    private static final double RED_BOX_HALF = 0.03;


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

    private void addWall(BranchGroup sceneBG, double x, double y, double z,
                         double width, double height, double depth,
                         Appearance appearance) {
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(x, y, z));
        TransformGroup tg = new TransformGroup(transform);
        Box wall = new Box((float) width, (float) height, (float) depth,
                Box.GENERATE_NORMALS, appearance);
        tg.addChild(wall);
        sceneBG.addChild(tg);
        double left   = x - width;
        double top    = z + depth;     // top > bottom
        double rectWidth  = 2 * width; // total width in x
        double rectHeight = 2 * depth; // total height in z
        double bottom = top - rectHeight;
        Rectangle2D.Double wallRect = new Rectangle2D.Double(left, bottom, rectWidth, rectHeight);
        wallBounds.add(wallRect);
    }


    public void setupUniverse(BranchGroup sceneBG) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas = new Canvas3D(config);

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
//                System.out.println("Key pressed: " + e.getKeyCode());
                double newX = redBoxPos.x;
                double newZ = redBoxPos.z;

                switch (e.getKeyChar()) {
                    case 'w': newZ -= STEP; break;
                    case 's': newZ += STEP; break;
                    case 'a': newX -= STEP; break;
                    case 'd': newX += STEP; break;
                }

                if (!collidesWithWall(newX, newZ)) {
                    // if safe then move
                    redBoxPos.x = newX;
                    redBoxPos.z = newZ;

                    Transform3D newTransform = new Transform3D();
                    newTransform.setTranslation(redBoxPos);
                    redBoxTG.setTransform(newTransform);
                }
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

    private boolean collidesWithWall(double x, double z) {
        // bounding square of red box
        double half = RED_BOX_HALF;     // 0.03
        double side = 2 * half;         // 0.06

        Rectangle2D.Double redBoxRect = new Rectangle2D.Double(
                x - half,
                z - half,
                side,
                side
        );

        // check wall intersection
        for (Rectangle2D.Double wallRect : wallBounds) {
            if (wallRect.intersects(redBoxRect)) {
                return true;    // collision
            }
        }
        return false;           // no collisions
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

