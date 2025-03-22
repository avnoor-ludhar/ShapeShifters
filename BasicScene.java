package ShapeShifters;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Point;
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
import java.util.*;
import org.jogamp.java3d.PositionInterpolator;


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
    private HashMap<Rectangle2D.Double, Point> wallBounds = new HashMap<Rectangle2D.Double, Point>();
//    private List<Rectangle2D.Double> wallBounds = new ArrayList<>();
    private static final double BOX_HALF = 0.03;

    // Maze variables
    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;
    private static int[][] walls = new int[MAZE_HEIGHT][MAZE_WIDTH];
    private static HashSet<Point> movingWalls = new HashSet<Point>(); //walls that move up and down periodically
    private static HashMap<Point, Alpha> movingWallAlphas = new HashMap<Point, Alpha>();

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
            //CONNECTING AND GETTING PLAYER ID
            socket = new Socket("localhost", 5001);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Read assigned player ID (e.g., "ID 1")
            String idLine = in.readLine();
            if (idLine != null && idLine.startsWith("ID ")) {
                playerId = Integer.parseInt(idLine.substring(3).trim());
                System.out.println("Assigned player ID: " + playerId);
            }

            //READING IN MAZE
            String maze = in.readLine();
            int index = 0;
            if (maze != null) {
                for (int i = 0; i < MAZE_HEIGHT; i++) {
                    for (int j = 0; j < MAZE_WIDTH; j++) {
                        walls[i][j] = maze.charAt(index)-'0';
                        index += 1;
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                String coords = in.readLine();
                String[] coordsSplit = coords.split(" ");
                Point p = new Point(Integer.parseInt(coordsSplit[0]), Integer.parseInt(coordsSplit[1]));
                movingWalls.add(p);
                //Set all the alphas to have the same original start time so that they're synced up
//                long base = new GregorianCalendar(2020, Calendar.JANUARY, 1).getTimeInMillis();
                long offset = System.currentTimeMillis() % 19000;
                Alpha a = new Alpha(-1, Alpha.INCREASING_ENABLE | Alpha.DECREASING_ENABLE, 0, 19000-offset, (long)(2000), (long)0, (long)5000, (long)(2000), (long)0, (long)10000);
//                a.setStartTime(base);

                movingWallAlphas.put(p, a);
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

        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (walls[i][j] == 1) {
                    TransformGroup tg = addWall(sceneBG, -1 + i * .103f, .1f, -1 + j*.103f, .055f, .05f, .055f, wallAppearance, i, j);
                    if (movingWalls.contains(new Point(i, j))) {
//                        System.out.println("THIS HAPPENSNSNSNS");
                        System.out.printf("%d %d\n", i, j);
                        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

                        Transform3D axis = new Transform3D();
                        axis.rotZ(Math.PI/2); //pointing vertically
                        Alpha a = movingWallAlphas.get(new Point(i, j));
                        PositionInterpolator interpolator = new PositionInterpolator(
                                a,
                                tg,    // The TransformGroup to animate
                                axis,
                                0f,     //start
                                -.101f               //end
                        );
                        interpolator.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
                        tg.addChild(interpolator);
                        sceneBG.addChild(tg);
                    } else {
                        sceneBG.addChild(tg);
                    }
                }
            }
        }
        return sceneBG;
    }

    // Helper to add a wall and store its bounding rectangle for collision detection.
    private TransformGroup addWall(BranchGroup sceneBG, double x, double y, double z,
                         double width, double height, double depth,
                         Appearance appearance, int i, int j) {
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(x, y, z));
        TransformGroup tg = new TransformGroup(transform);
        TransformGroup tg2 = new TransformGroup();
        tg2.addChild(tg);
        Box wall = new Box((float) width, (float) height, (float) depth,
                Box.GENERATE_NORMALS, appearance);
        tg.addChild(wall);

        double left = x - width;
        double top = z + depth;
        double rectWidth = 2 * width;
        double rectHeight = 2 * depth;
        double bottom = top - rectHeight;
        Rectangle2D.Double wallRect = new Rectangle2D.Double(left, bottom, rectWidth, rectHeight);
        wallBounds.put(wallRect, new Point(i, j));

        return tg2;
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

        for (Rectangle2D.Double wallRect : wallBounds.keySet()) {
            Point coords = wallBounds.get(wallRect);
            if (movingWallAlphas.containsKey(coords) && movingWallAlphas.get(coords).value() == 1) {
                return false;
            }
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