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
import java.io.*;
import java.net.*;

public class BasicScene extends JPanel {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;

    // Two boxes for two players (one controlled locally; the other updated via network)
    private TransformGroup redBoxTG;
    private TransformGroup blueBoxTG;

    // Positions (moving only in x and z; y remains constant at 0.1)
    private Vector3d redBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private Vector3d blueBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private final double STEP = 0.05;
    private List<Rectangle2D.Double> wallBounds = new ArrayList<>();
    private static final double BOX_HALF = 0.03;

    // Maze variables
    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;
    private static int[][] walls;

    // Networking variables
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int playerId = 0; // Assigned by the server

    // Reference to the universe for camera updates
    private SimpleUniverse universe;

    public BasicScene() {
        // Connect to the server
        try {
            socket = new Socket("localhost", 5001);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Read assigned player ID (e.g., "ID 1")
            String idLine = in.readLine();
            if (idLine != null && idLine.startsWith("ID ")) {
                playerId = Integer.parseInt(idLine.substring(3).trim());
                System.out.println("Assigned player ID: " + playerId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Start a thread to listen for messages from the server.
        new Thread(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    // Expected format: "id x y z"
                    String[] tokens = line.split(" ");
                    if (tokens.length < 4) continue;
                    int id = Integer.parseInt(tokens[0]);
                    double x = Double.parseDouble(tokens[1]);
                    double y = Double.parseDouble(tokens[2]);
                    double z = Double.parseDouble(tokens[3]);
                    // Only update the box for the remote player.
                    if (id != playerId) {
                        Transform3D update = new Transform3D();
                        Vector3d pos = new Vector3d(x, y, z);
                        update.setTranslation(pos);
                        if (id == 1) {
                            redBoxTG.setTransform(update);
                        } else if (id == 2) {
                            blueBoxTG.setTransform(update);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public BranchGroup createScene() {
        BranchGroup sceneBG = new BranchGroup();

        // Create the platform
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

        // Create red and blue boxes.
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

        Transform3D blueBoxTransform = new Transform3D();
        blueBoxTransform.setTranslation(blueBoxPos);
        blueBoxTG = new TransformGroup(blueBoxTransform);
        blueBoxTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Appearance blueAppearance = new Appearance();
        blueAppearance.setMaterial(new Material(
                new Color3f(0.0f, 0.0f, 1.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(0.0f, 0.0f, 1.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
        Box blueBox = new Box(0.03f, 0.03f, 0.03f, Box.GENERATE_NORMALS, blueAppearance);
        blueBoxTG.addChild(blueBox);
        sceneBG.addChild(blueBoxTG);

        // Create maze walls.
        Appearance wallAppearance = new Appearance();
        wallAppearance.setMaterial(new Material(
                new Color3f(0.5f, 0.5f, 0.5f),
                new Color3f(0.1f, 0.1f, 0.1f),
                new Color3f(0.5f, 0.5f, 0.5f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));

//        float wallThickness = 0.01f;
//        float wallHeight = 0.15f;
//        float innerWallThickness = 0.005f;

//        addWall(sceneBG, -1.0, wallHeight / 2, 0.0, wallThickness, wallHeight, 1.0f, wallAppearance);
//        addWall(sceneBG, 1.0, wallHeight / 2, 0.0, wallThickness, wallHeight, 1.0f, wallAppearance);
//        addWall(sceneBG, 0.0, wallHeight / 2, -1.0, 1.0f, wallHeight, wallThickness, wallAppearance);
//        addWall(sceneBG, 0.0, wallHeight / 2, 1.0, 1.0f, wallHeight, wallThickness, wallAppearance);
//
//        double[][] mazeWalls = {
//                {-0.5, 0.75, 0.5, innerWallThickness}, {-0.5, 0.5, innerWallThickness, 0.25},
//                {0.5, 0.5, 0.5, innerWallThickness}, {0.1, 0.25, innerWallThickness, 0.25},
//                {-0.5, -0.25, 0.3, innerWallThickness}, {-0.5, 0, innerWallThickness, 0.25},
//                {0.5, -0.5, 0.5, innerWallThickness}, {0.5, -0.25, innerWallThickness, 0.25},
//                {-0.7, -0.65, 0.3, innerWallThickness}, {-0.4, -0.65, innerWallThickness, 0.15}
//        };

//        for (double[] wall : mazeWalls) {
//            addWall(sceneBG, wall[0], wallHeight / 2, wall[1], wall[2], wallHeight, wall[3], wallAppearance);
//        }

        ArrayList<ArrayList<Integer>> wallsArrayList = GenerateMaze.getMaze(MAZE_HEIGHT-2, MAZE_HEIGHT-2);
        walls = new int[MAZE_HEIGHT][MAZE_WIDTH];
        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {

                walls[i][j] = wallsArrayList.get(i).get(j);
            }
        }
        for (int i = 4; i < 16; i++) {
            for (int j = 4; j < 16; j++) {
                walls[i][j] = 0;
            }
        }

        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (walls[i][j] == 1) {
                    addWall(sceneBG, -1 + i * .103f, .1f, -1 + j*.103f, .055f, .05f, .055f, wallAppearance);
                }
            }
        }

        return sceneBG;
    }

    // Helper to add a wall and store its bounding rectangle for collision detection.
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
        double left = x - width;
        double top = z + depth;
        double rectWidth = 2 * width;
        double rectHeight = 2 * depth;
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
                double newX, newZ;
                // Determine which box this client controls based on its player ID.
                if (playerId == 1) {
                    newX = redBoxPos.x;
                    newZ = redBoxPos.z;
                } else {
                    newX = blueBoxPos.x;
                    newZ = blueBoxPos.z;
                }

                switch (e.getKeyChar()) {
                    case 'w': newZ -= STEP; break;
                    case 's': newZ += STEP; break;
                    case 'a': newX -= STEP; break;
                    case 'd': newX += STEP; break;
                }

                if (!collidesWithWall(newX, newZ)) {
                    if (playerId == 1) {
                        redBoxPos.x = newX;
                        redBoxPos.z = newZ;
                        Transform3D newTransform = new Transform3D();
                        newTransform.setTranslation(redBoxPos);
                        redBoxTG.setTransform(newTransform);
                    } else if (playerId == 2) {
                        blueBoxPos.x = newX;
                        blueBoxPos.z = newZ;
                        Transform3D newTransform = new Transform3D();
                        newTransform.setTranslation(blueBoxPos);
                        blueBoxTG.setTransform(newTransform);
                    }
                    // Send updated position in the format: "id x y z"
                    if (out != null) {
                        out.println(playerId + " " + newX + " " + 0.1 + " " + newZ);
                    }
                    // Update the camera so it follows the local player's box.
                    updateCamera();
                }
            }
        });

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        // Create and store the universe for later camera updates.
        universe = new SimpleUniverse(canvas);
        // Set an initial camera view based on the local player's position.
        updateCamera();

        sceneBG.compile();
        universe.addBranchGraph(sceneBG);

        setLayout(new BorderLayout());
        add("Center", canvas);
    }

    // Update the camera to follow (or "zoom in on") the local player's box.
    private void updateCamera() {
        Vector3d localPos = (playerId == 1) ? redBoxPos : blueBoxPos;
        // Define the camera position (eye) relative to the local player.
        Point3d eye = new Point3d(localPos.x, localPos.y + 2.0, localPos.z + 2.5);
        Point3d center = new Point3d(localPos.x, localPos.y, localPos.z);
        Vector3d up = new Vector3d(0, 1, 0);
        Transform3D viewTransform = new Transform3D();
        viewTransform.lookAt(eye, center, up);
        viewTransform.invert();
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);
    }

    // Simple collision detection using bounding rectangles.
    private boolean collidesWithWall(double x, double z) {
        double half = BOX_HALF;
        double side = 2 * half;
        Rectangle2D.Double boxRect = new Rectangle2D.Double(
                x - half,
                z - half,
                side,
                side
        );
        for (Rectangle2D.Double wallRect : wallBounds) {
            if (wallRect.intersects(boxRect)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        frame = new JFrame("Basic Scene: Maze View (Networking with Dynamic Camera)");
        BasicScene BasicScene = new BasicScene();
        BranchGroup sceneBG = BasicScene.createScene();
        BasicScene.setupUniverse(sceneBG);
        frame.getContentPane().add(BasicScene);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}