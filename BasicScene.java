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
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.java3d.utils.picking.PickTool;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.vecmath.*;

public class BasicScene extends JPanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;

    // Ghost models for players (using GhostModel)
    private GhostModel redGhost;
    private GhostModel blueGhost;
    private Canvas3D canvas;
    private PickTool pickTool;
    // Player positions (x, y, z) – y remains constant at 0.1.
    private Vector3d redBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private Vector3d blueBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private final double STEP = 0.010;

    // Maze collision data
    private HashMap<Rectangle2D.Double, Point> wallBounds = new HashMap<>();

    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;
    private static int[][] walls = new int[MAZE_HEIGHT][MAZE_WIDTH];

    private static HashSet<Point> movingWalls = new HashSet<>();
    private static HashMap<Point, Alpha> movingWallAlphas = new HashMap<>();

    // NPC integration
    private List<NPC> npcs = new ArrayList<>();
    private final double NPC_STEP = 0.01;
    private Appearance npcAppearance;

    // Networking variables
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int playerId = 0; // Assigned by the server

    // 3D universe reference
    private SimpleUniverse universe;

    // Sound effect timing
    private long lastFootstepTime = 0;
    private long lastCollisionTime = 0;
    private static final long FOOTSTEP_COOLDOWN = 250; // ms
    private static final long COLLISION_COOLDOWN = 400;  // ms

    // Spotlight variables
    private SpotLight spotlight;
    private TransformGroup spotlightTG;
    private BoundingSphere lightBounds;
    private static final float SPOTLIGHT_RADIUS = 0.5f;
    private static final float SPOTLIGHT_CONCENTRATION = 50.0f;
    private static final float SPOTLIGHT_SPREAD_ANGLE = (float) Math.PI / 6;

    // Movement state booleans for smooth movement
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // Star system variables
    private static final int STAR_COUNT = 15000;
    private static final int SHOOTING_STAR_COUNT = 150;
    private static final float STAR_FIELD_RADIUS = 10.0f;
    private TransformGroup starSystemTG;
    private Shape3D shootingStarShape;
    private PointArray shootingStarPoints;
    private Random random = new Random();

    // Treasure (coin/star) variables
    private BranchGroup treasureBranchGroup;
    private TransformGroup treasureGroup; // Reference to the treasure's TransformGroup
    private Appearance treasureAppearance; // Appearance for the treasure
    // NEW: Define a constant for interaction distance and a flag to track state.
    private static final double TREASURE_INTERACT_DISTANCE = 0.15;
    private boolean treasureIsCoin = true;

    // Fields for IP address and username
    private String ipAddress;
    private String username;
    private BranchGroup rootBG;
    private TreasureKeyBehavior treasureKeyBehavior;

    // Add these field declarations to the class
    private MazeSign mazeSign; // Renamed to be more generic since we only have one sign

    private static boolean gameEnded = false;

    // --- Constructors ---
    public BasicScene() {
        this("localhost", "Player");
    }

    public BasicScene(String ipAddress, String username) {
        this.ipAddress = ipAddress;
        this.username = username;
        try {
            socket = new Socket(ipAddress, 5001);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String idLine = in.readLine();
            if (idLine != null && idLine.startsWith("ID ")) {
                playerId = Integer.parseInt(idLine.substring(3).trim());
                System.out.println("Assigned player ID: " + playerId);
            }

            String mazeStr = in.readLine();
            int index = 0;
            if (mazeStr != null) {
                for (int i = 0; i < MAZE_HEIGHT; i++) {
                    for (int j = 0; j < MAZE_WIDTH; j++) {
                        walls[i][j] = mazeStr.charAt(index) - '0';
                        index++;
                    }
                }
            }

            long offset = System.currentTimeMillis() % 19000;
            Alpha a = new Alpha(-1, Alpha.INCREASING_ENABLE | Alpha.DECREASING_ENABLE,
                    0, 19000 - offset, 2000, 0, 5000, 2000, 0, 10000);
            for (int i = 0; i < 4; i++) {
                String coords = in.readLine();
                if (coords != null) {
                    String[] split = coords.split(" ");
                    Point p = new Point(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                    movingWalls.add(p);
                    movingWallAlphas.put(p, a);
                }
            }

            npcAppearance = new Appearance();
            npcAppearance.setMaterial(new Material(
                    new Color3f(0.0f, 1.0f, 0.0f),
                    new Color3f(0.0f, 0.0f, 0.0f),
                    new Color3f(0.0f, 1.0f, 0.0f),
                    new Color3f(1.0f, 1.0f, 1.0f),
                    64.0f));
            String npcCountLine = in.readLine();
            if (npcCountLine != null && npcCountLine.startsWith("NPC_COUNT ")) {
                int npcCount = Integer.parseInt(npcCountLine.split(" ")[1]);
                for (int i = 0; i < npcCount; i++) {
                    String npcData = in.readLine();
                    if (npcData != null && npcData.startsWith("NPC_INIT ")) {
                        String[] tokens = npcData.split(" ");
                        double x = Double.parseDouble(tokens[1]);
                        double z = Double.parseDouble(tokens[2]);
                        double dirX = Double.parseDouble(tokens[3]);
                        double dirZ = Double.parseDouble(tokens[4]);
                        Vector3d pos = new Vector3d(x, 0.1, z);
                        Vector3d dir = new Vector3d(dirX, 0, dirZ);
                        NPC npc = new NPC(pos, dir, NPC_STEP, npcAppearance);
                        npcs.add(npc);
                    }
                }
            }

            String treasureCoordsLine = in.readLine();
            if (treasureCoordsLine != null && treasureCoordsLine.startsWith("TREASURE")) {
                String[] parts = treasureCoordsLine.split(" ");
                double tx = Double.parseDouble(parts[1]);
                double ty = Double.parseDouble(parts[2]);
                double tz = Double.parseDouble(parts[3]);
                treasureBranchGroup = createTreasure(tx, ty, tz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("NPC_UPDATE")) {
                        String[] tokens = line.split(" ");
                        for (int i = 1; i < tokens.length; i += 4) {
                            int npcId = Integer.parseInt(tokens[i]);
                            double x = Double.parseDouble(tokens[i + 1]);
                            double y = Double.parseDouble(tokens[i + 2]);
                            double z = Double.parseDouble(tokens[i + 3]);
                            NPC npc = npcs.get(npcId);
                            Transform3D transform = new Transform3D();
                            transform.setTranslation(new Vector3d(x, y, z));
                            npc.getTransformGroup().setTransform(transform);
                        }
                        continue;
                    }
                    String[] tokens = line.split(" ");
                    if (tokens.length < 4)
                        continue;
                    int id = Integer.parseInt(tokens[0]);
                    double x = Double.parseDouble(tokens[1]);
                    double y = Double.parseDouble(tokens[2]);
                    double z = Double.parseDouble(tokens[3]);

                    int direction = GhostModel.DIRECTION_DOWN;
                    if (tokens.length >= 5) {
                        direction = Integer.parseInt(tokens[4]);
                    }

                    if (id == 1 && redGhost != null) {
                        redGhost.updatePositionAndRotation(x, z, direction);
                    } else if (id == 2 && blueGhost != null) {
                        blueGhost.updatePositionAndRotation(x, z, direction);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- Scene Creation ---
    public BranchGroup createScene() {
        BranchGroup sceneBG = new BranchGroup();

        Background background = new Background(new Color3f(0.01f, 0.01f, 0.01f));
        BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 100.0);
        background.setApplicationBounds(bounds);
        sceneBG.addChild(background);

        createStarSystem(sceneBG);

        Appearance platformAppearance = new Appearance();
        platformAppearance.setMaterial(new Material(
                new Color3f(0.8f, 0.8f, 0.8f),  // Increased ambient reflection
                new Color3f(0.2f, 0.2f, 0.2f),  // Dark emission
                new Color3f(1.0f, 1.0f, 1.0f),  // Diffuse color
                new Color3f(1.0f, 1.0f, 1.0f),  // Specular color
                64.0f));  // Shininess

        String floorTexturePath = "src/Shapeshifters/Textures/QuartzFloorTexture.jpg";
        try {
            URL floorTextureURL = new File(floorTexturePath).toURI().toURL();
            Texture floorTexture = new TextureLoader(floorTextureURL, "RGB", new java.awt.Container()).getTexture();
            if (floorTexture != null) {
                platformAppearance.setTexture(floorTexture);
                TextureAttributes texAttr = new TextureAttributes();
                texAttr.setTextureMode(TextureAttributes.MODULATE);
                platformAppearance.setTextureAttributes(texAttr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        TransformGroup platformTG = new TransformGroup();
        Box platform = new Box(1.0f, 0.05f, 1.0f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, platformAppearance);
        platformTG.addChild(platform);
        sceneBG.addChild(platformTG);

        // Add four corner signs
        createMazeSigns(sceneBG);

        // Add four corner signs
        createMazeSigns(sceneBG);

        redGhost = new GhostModel(true, redBoxPos);
        blueGhost = new GhostModel(false, blueBoxPos);
        sceneBG.addChild(redGhost.getTransformGroup());
        //blue ghost added farther down to PickTool

        Appearance wallAppearance = new Appearance();
        String wallTexturePath = "src/ShapeShifters/Textures/WhiteWallTexture.jpg";
        try {
            URL wallTextureURL = new File(wallTexturePath).toURI().toURL();
            Texture wallTexture = new TextureLoader(wallTextureURL, "RGB", new java.awt.Container()).getTexture();
            if (wallTexture != null) {
                wallAppearance.setTexture(wallTexture);
                TextureAttributes wallTexAttr = new TextureAttributes();
                wallTexAttr.setTextureMode(TextureAttributes.MODULATE);
                wallAppearance.setTextureAttributes(wallTexAttr);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        Material wallMat = new Material();
        wallMat.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
        wallAppearance.setMaterial(wallMat);

        AmbientLight ambientLight = new AmbientLight(new Color3f(0.05f, 0.05f, 0.05f));
        ambientLight.setInfluencingBounds(bounds);
        sceneBG.addChild(ambientLight);
        DirectionalLight directionalLight = new DirectionalLight(
                new Color3f(0.0f, 0.0f, 0.0f),
                new Vector3f(-1.0f, -1.0f, -1.0f));
        directionalLight.setInfluencingBounds(bounds);
        sceneBG.addChild(directionalLight);

        for (int i = 5; i < 15; i++) {
            for (int j = 5; j < 15; j++) {
                walls[i][j] = 0;
            }
        }
        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (walls[i][j] == 1) {
                    TransformGroup tg = addWall(sceneBG,
                            -1 + i * 0.103f, 0.1f, -1 + j * 0.103f,
                            0.055f, 0.05f, 0.055f,
                            wallAppearance, i, j);
                    if (movingWalls.contains(new Point(i, j))) {
                        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                        Transform3D axis = new Transform3D();
                        axis.rotZ(Math.PI / 2);
                        Alpha a = movingWallAlphas.get(new Point(i, j));
                        PositionInterpolator interpolator = new PositionInterpolator(a, tg, axis, 0f, -0.101f);
                        interpolator.setSchedulingBounds(new BoundingSphere(new Point3d(0, 0, 0), 100.0));
                        tg.addChild(interpolator);
                    }
                    sceneBG.addChild(tg);
                }
            }
        }

        for (NPC npc : npcs) {
            sceneBG.addChild(npc.getTransformGroup());
        }


        if (treasureBranchGroup != null) {
            sceneBG.addChild(treasureBranchGroup);

            // Create and add treasure behavior
            treasureKeyBehavior = new TreasureKeyBehavior(treasureBranchGroup, treasureGroup, redBoxPos, blueBoxPos, playerId, sceneBG);
            treasureKeyBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
            sceneBG.addChild(treasureKeyBehavior);
        }

        createSpotlight(sceneBG);

        TransformGroup blueGhostTransform = blueGhost.getTransformGroup();
        BranchGroup blueGhostBranch = new BranchGroup();
        blueGhostBranch.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        blueGhostBranch.addChild(blueGhostTransform);
        sceneBG.addChild(blueGhostBranch);
        pickTool = new PickTool(blueGhostBranch);                 // initialize 'pickTool' and allow 'cubeBG' pickable
        pickTool.setMode(PickTool.BOUNDS);

        Cylinder base = new Cylinder(0.1f, .2f);
        Transform3D baseTransform = new Transform3D();
        baseTransform.setTranslation(new Vector3f(.3f, 0.1f, .3f)); // upright
        baseTransform.setScale(.6f);
        TransformGroup baseTG = new TransformGroup();
        baseTG.setTransform(baseTransform);
        baseTG.addChild(base);
        sceneBG.addChild(baseTG);

        // Middle horizontal rectangle (Box)
        Box middleBox = new Box(0.3f, 0.005f, 0.02f, null); // thin flat rectangle
        TransformGroup spinMiddleTG = createSpinner(2000, 'x'); // spins once/sec

        Transform3D midBoxTrans = new Transform3D();
        midBoxTrans.setTranslation(new Vector3f(0f, 0.09f, 0f));
        TransformGroup midBoxTG = new TransformGroup(midBoxTrans);
        midBoxTG.addChild(middleBox);
        spinMiddleTG.addChild(midBoxTG);
        baseTG.addChild(spinMiddleTG);

        // Ends of the middle box (small boxes spinning fast)
        float offset = 0.3f; // match middleBox width



        ObjectFile f = new ObjectFile(ObjectFile.RESIZE, (float) (60 * Math.PI / 180.0));
        Scene s1 = null;
        Scene s2 = null;
        try {
            s1 = f.load("src/ShapeShifters/assets/FanBlades.obj");
            s2 = f.load("src/ShapeShifters/assets/FanBlades.obj");
        } catch (Exception e) {}
        if (s1 == null || s2 == null) {
            System.exit(1); //this won't happen dw
        }
        BranchGroup b1 = s1.getSceneGroup();
        BranchGroup b2 = s2.getSceneGroup();

        TransformGroup tg1 = new TransformGroup();
        tg1.addChild(b1);
        Transform3D transform1 = new Transform3D();
        transform1.rotY(Math.PI/2);
        transform1.setScale(.1);
        tg1.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg1.setTransform(transform1);

        TransformGroup tg2 = new TransformGroup();
        tg2.addChild(b2);
        Transform3D transform2 = new Transform3D();
        transform2.rotY(Math.PI/2);
        transform2.setScale(.1);
        tg2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg2.setTransform(transform2);


//        Box endBoxLeft = new Box(0.005f, 0.01f, 0.05f, null);
        TransformGroup spinLeft = createSpinner(200, 'z'); // faster spin

        Transform3D leftTrans = new Transform3D();
        leftTrans.setTranslation(new Vector3f(-offset, 0f, 0f));
        TransformGroup leftTG = new TransformGroup(leftTrans);
        leftTG.addChild(tg1);
        spinLeft.addChild(leftTG);
        midBoxTG.addChild(spinLeft); // attach to midBoxTG so it spins with it




//        Box endBoxRight = new Box(0.01f, 0.01f, 0.05f, null);
        TransformGroup spinRight = createSpinner(200, 'z');

        Transform3D rightTrans = new Transform3D();
        rightTrans.setTranslation(new Vector3f(offset, 0f, 0f));
        TransformGroup rightTG = new TransformGroup(rightTrans);
        rightTG.addChild(tg2);
        spinRight.addChild(rightTG);
        midBoxTG.addChild(spinRight);

        this.rootBG = sceneBG;
        rootBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        rootBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

        sceneBG.compile();
        return sceneBG;
    }

    public static void setGameEnded(boolean ended) {
        gameEnded = ended;
    }

    private TransformGroup createSpinner(long durationMillis, char axis) {
        TransformGroup spinner = new TransformGroup();
        spinner.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Alpha spinAlpha = new Alpha(-1, durationMillis);

        Transform3D rotationAxis = new Transform3D();
        switch (axis) {
            case 'x':
//                rotationAxis.rotX(Math.PI / 2); break;
                break; //rotating around x by default
            case 'z':
                rotationAxis.rotZ(Math.PI / 2); break;
            // default is Y-axis (identity)
        }

        RotationInterpolator rotator = new RotationInterpolator(spinAlpha, spinner, rotationAxis, 0f, (float)Math.PI * 2);
        rotator.setSchedulingBounds(new BoundingSphere());
        spinner.addChild(rotator);
        return spinner;
    }

    private TransformGroup addWall(BranchGroup sceneBG, double x, double y, double z,
                                   double width, double height, double depth,
                                   Appearance appearance, int i, int j) {
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(x, y, z));
        TransformGroup tg = new TransformGroup(transform);
        TransformGroup container = new TransformGroup();
        container.addChild(tg);
        Box wall = new Box((float) width, (float) height, (float) depth,
                Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, appearance);
        tg.addChild(wall);
        double left = x - width;
        double top = z + depth;
        double rectWidth = 2 * width;
        double rectHeight = 2 * depth;
        double bottom = top - rectHeight;
        Rectangle2D.Double wallRect = new Rectangle2D.Double(left, bottom, rectWidth, rectHeight);
        wallBounds.put(wallRect, new Point(i, j));
        return container;
    }

    private void createSpotlight(BranchGroup sceneBG) {
        spotlightTG = new TransformGroup();
        spotlightTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Point3f initialPosition = new Point3f(0.0f, 0.2f, 0.0f);
        Vector3f initialDirection = new Vector3f(0.0f, -1.0f, 0.0f);
        Color3f lightColor = new Color3f(1.0f, 0.95f, 0.6f);
        spotlight = new SpotLight(lightColor, initialPosition, new Point3f(1.0f, 0.05f, 0.01f),
                initialDirection,
                (float) Math.PI / 2,
                50.0f);
        spotlight.setCapability(Light.ALLOW_STATE_WRITE);
        Transform3D lightTransform = new Transform3D();
        lightTransform.setTranslation(new Vector3d(initialPosition));
        TransformGroup spotlightTransformGroup = new TransformGroup(lightTransform);
        spotlightTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        spotlightTransformGroup.addChild(spotlight);
        lightBounds = new BoundingSphere(new Point3d(0, 0, 0), 100.0);
        spotlight.setInfluencingBounds(lightBounds);
        sceneBG.addChild(spotlightTransformGroup);
        spotlightTG = spotlightTransformGroup;
    }

    // Universe and input setup.
    public void setupUniverse(BranchGroup sceneBG) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas = new Canvas3D(config);
        canvas.addMouseListener(this);
        // Set up key listener to update movement state.
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'w': upPressed = true; break;
                    case 's': downPressed = true; break;
                    case 'a': leftPressed = true; break;
                    case 'd': rightPressed = true; break;
                    default:
                        break;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'w': upPressed = false; break;
                    case 's': downPressed = false; break;
                    case 'a': leftPressed = false; break;
                    case 'd': rightPressed = false; break;
                    default:
                        break;
                }
            }
        });

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        Timer movementTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMovement();
            }
        });
        movementTimer.start();

        universe = new SimpleUniverse(canvas);
        updateCamera();
        updateSpotlight();
        universe.addBranchGraph(sceneBG);
        setLayout(new BorderLayout());
        add("Center", canvas);

        // In setupUniverse method:
        javax.swing.Timer endTimer = new javax.swing.Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GameEndAnimation gameEnd = new GameEndAnimation(universe, rootBG);
                gameEnd.triggerGameEnd("Blue");
            }
        });
        endTimer.setRepeats(false); // Ensure the timer only fires once
        endTimer.start();
    }

    private void updateMovement() {
        //first try both x and z, then x, then z. If any of them happen then return
        double dx = 0;
        double dz = 0;
        double oldX, oldZ, newX, newZ;
        oldX = 0;
        oldZ = 0;
        newX = 0;
        newZ = 0;
        if (playerId == 1) {
            oldX = redBoxPos.x;
            oldZ = redBoxPos.z;
        } else {
            oldX = blueBoxPos.x;
            oldZ = blueBoxPos.z;
        }

        int direction = -1;
        boolean[][] combos = {{true, true}, {true, false}, {false, true}, {false, false}};
        for (boolean[] combo: combos) {
            boolean changeX = combo[0];
            boolean changeZ = combo[1];
            dx = 0;
            dz = 0;
            newX = oldX;
            newZ = oldZ;
            direction = -1; // Track which direction we're moving

            if (upPressed && changeZ) {
                dz -= STEP;
                direction = GhostModel.DIRECTION_UP;
            }
            if (downPressed && changeZ) {
                dz += STEP;
                direction = GhostModel.DIRECTION_DOWN;
            }
            if (leftPressed && changeX) {
                dx -= STEP;
                direction = GhostModel.DIRECTION_LEFT;
            }
            if (rightPressed && changeX) {
                dx += STEP;
                direction = GhostModel.DIRECTION_RIGHT;
            }

            // Normalize diagonal movement to keep speed consistent.
            if (dx != 0 || dz != 0) {
                double length = Math.sqrt(dx * dx + dz * dz);
                dx = dx / length * STEP;
                dz = dz / length * STEP;

                // For diagonal movement, prioritize the last key pressed
                // If multiple keys are pressed simultaneously, we'll use the horizontal direction
                if (dx != 0 && dz != 0) {
                    if (dx < 0 && dz > 0) {
                        direction = GhostModel.DIRECTION_DOWNLEFT;
                    } else if (dx < 0 && dz < 0) {
                        direction = GhostModel.DIRECTION_UPLEFT;
                    } else if (dx > 0 && dz > 0) {
                        direction = GhostModel.DIRECTION_DOWNRIGHT;
                    } else {
                        direction = GhostModel.DIRECTION_UPRIGHT;
                    }
                }
            } else {
                // If no keys are pressed, return without updating
                return;
            }

            newX += dx;
            newZ += dz;
            if (collidesWithWall(newX, newZ)) {
                continue;
            } else {
                break;
            }
        }

        if (!collidesWithWall(newX, newZ)) {
            if (playerId == 1) {
                redBoxPos.x = newX;
                redBoxPos.z = newZ;
                treasureKeyBehavior.updateRedPosition(redBoxPos);
                redGhost.updatePositionAndRotation(newX, newZ, direction);
            } else {
                blueBoxPos.x = newX;
                blueBoxPos.z = newZ;
                treasureKeyBehavior.updateBluePosition(blueBoxPos);
                blueGhost.updatePositionAndRotation(newX, newZ, direction);
            }

            if (out != null) {
                out.println(playerId + " " + newX + " " + 0.1 + " " + newZ + " " + direction);
            }

            updateCamera();
            updateSpotlight();
            if ((dx != 0 || dz != 0) && (System.currentTimeMillis() - lastFootstepTime > FOOTSTEP_COOLDOWN)) {
                playFootstepSound();
                lastFootstepTime = System.currentTimeMillis();
            }
        } else {
            if (System.currentTimeMillis() - lastCollisionTime > COLLISION_COOLDOWN) {
                playWallCollisionSound();
                lastCollisionTime = System.currentTimeMillis();
            }
        }
    }

    private void updateCamera() {
        if(gameEnded) return;

        Vector3d localPos = (playerId == 1) ? redBoxPos : blueBoxPos;
        Point3d eye = new Point3d(localPos.x, localPos.y + 0.6, localPos.z + 0.5);
        Point3d center = new Point3d(localPos.x, localPos.y, localPos.z);
        Vector3d up = new Vector3d(0, 0, -1);
        Transform3D viewTransform = new Transform3D();
        viewTransform.lookAt(eye, center, up);
        viewTransform.invert();
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);
    }

    private void updateSpotlight() {
        Vector3d localPos = (playerId == 1) ? redBoxPos : blueBoxPos;
        Transform3D spotlightTransform = new Transform3D();
        Vector3d spotlightPos = new Vector3d(localPos.x, 0.8, localPos.z);
        spotlightTransform.setTranslation(spotlightPos);
        spotlightTG.setTransform(spotlightTransform);
    }

    private boolean collidesWithWall(double x, double z) {
        double half = GhostModel.getCharacterHalf();
        double side = 2 * half;
        Rectangle2D.Double boxRect = new Rectangle2D.Double(x - half, z - half, side, side);
        for (Rectangle2D.Double wallRect : wallBounds.keySet()) {
            Point coords = wallBounds.get(wallRect);
            if (movingWallAlphas.containsKey(coords)) {
                float alphaValue = movingWallAlphas.get(coords).value();
                if (alphaValue > 0.95f) continue;
            }
            if (wallRect.intersects(boxRect))
                return true;
        }
        return false;
    }

    private void playFootstepSound() {
        try {
            File soundFile = new File("src/ShapeShifters/sounds/footsteps.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            javax.sound.sampled.Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playWallCollisionSound() {
        try {
            File soundFile = new File("src/ShapeShifters/sounds/wallCollide.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            javax.sound.sampled.Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createStarSystem(BranchGroup sceneBG) {
        Transform3D starSystemTransform = new Transform3D();
        starSystemTG = new TransformGroup(starSystemTransform);
        starSystemTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        PointArray starPoints = new PointArray(STAR_COUNT, GeometryArray.COORDINATES | GeometryArray.COLOR_3);
        for (int i = 0; i < STAR_COUNT; i++) {
            double theta = 2.0 * Math.PI * random.nextDouble();
            double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
            float x = (float)(STAR_FIELD_RADIUS * Math.sin(phi) * Math.cos(theta));
            float y = (float)(STAR_FIELD_RADIUS * Math.sin(phi) * Math.sin(theta));
            float z = (float)(STAR_FIELD_RADIUS * Math.cos(phi));
            starPoints.setCoordinate(i, new Point3f(x, y, z));
            float brightness = 0.5f + random.nextFloat() * 0.5f;
            starPoints.setColor(i, new Color3f(brightness, brightness, brightness));
        }
        Appearance starAppearance = new Appearance();
        PointAttributes starPointAttributes = new PointAttributes();
        starPointAttributes.setPointSize(2.0f);
        starAppearance.setPointAttributes(starPointAttributes);
        Shape3D starShape = new Shape3D(starPoints, starAppearance);
        starSystemTG.addChild(starShape);

        shootingStarPoints = new PointArray(SHOOTING_STAR_COUNT, GeometryArray.COORDINATES | GeometryArray.COLOR_3);
        shootingStarPoints.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        shootingStarPoints.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
        for (int i = 0; i < SHOOTING_STAR_COUNT; i++) {
            initializeShootingStar(i);
        }
        Appearance shootingStarAppearance = new Appearance();
        PointAttributes shootingStarPointAttributes = new PointAttributes();
        shootingStarPointAttributes.setPointSize(5.0f);
        shootingStarAppearance.setPointAttributes(shootingStarPointAttributes);
        shootingStarShape = new Shape3D(shootingStarPoints, shootingStarAppearance);
        shootingStarShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        starSystemTG.addChild(shootingStarShape);
        sceneBG.addChild(starSystemTG);

        startShootingStarAnimation();
    }

    private void initializeShootingStar(int index) {
        double angle = random.nextDouble() * 2.0 * Math.PI;
        float x = STAR_FIELD_RADIUS * 0.9f;
        float y = (float)(STAR_FIELD_RADIUS * 0.7f * Math.sin(angle));
        float z = (float)(STAR_FIELD_RADIUS * 0.7f * Math.cos(angle));
        shootingStarPoints.setCoordinate(index, new Point3f(x, y, z));
        shootingStarPoints.setColor(index, new Color3f(1.0f, 0.9f, 0.5f));
    }

    private void startShootingStarAnimation() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                while (true) {
                    for (int i = 0; i < SHOOTING_STAR_COUNT; i++) {
                        Point3f pos = new Point3f();
                        shootingStarPoints.getCoordinate(i, pos);
                        pos.x -= 0.15f;
                        pos.y -= 0.05f;
                        if (pos.x < -STAR_FIELD_RADIUS || Math.abs(pos.y) > STAR_FIELD_RADIUS) {
                            initializeShootingStar(i);
                        } else {
                            shootingStarPoints.setCoordinate(i, pos);
                        }
                    }
                    Thread.sleep(16);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Update the createMazeSigns method
    private void createMazeSigns(BranchGroup sceneBG) {
        // Calculate position for the North West sign
        double cornerX = 0.9;  // Adjust based on your maze size
        double cornerZ = 0.9;  // Adjust based on your maze size
        double signHeight = 0.3;  // Height above the ground
        
        // Create only the North West sign with "The Maze" text
        mazeSign = new MazeSign(
            new Vector3d(-cornerX, signHeight, -cornerZ), 
            "The Maze"
        );
        
        // Add the sign to the scene
        sceneBG.addChild(mazeSign.getTransformGroup());
    }


    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e){}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {
        Point3d pixelPos = new Point3d();
        Point3d eyePos = new Point3d();
        canvas.getPixelLocationInImagePlate(e.getX(), e.getY(), pixelPos);
        canvas.getCenterEyeInImagePlate(eyePos);

        Transform3D ip2vw = new Transform3D();
        canvas.getImagePlateToVworld(ip2vw);
        ip2vw.transform(pixelPos);
        ip2vw.transform(eyePos);

        Vector3d rayDirection = new Vector3d();
        rayDirection.sub(pixelPos, eyePos);
        rayDirection.normalize();

        pickTool.setShapeRay(eyePos, rayDirection);
//        System.out.println(pickTool.pickClosest());
        if (pickTool.pickClosest() != null) {
            double dist = Math.pow((Math.pow(redBoxPos.x - blueBoxPos.x, 2) + Math.pow(redBoxPos.z - blueBoxPos.z, 2)), .5);
            if (dist < .5f && playerId == 1) {
                blueBoxPos = new Vector3d(0.0, 0.1, 0.0);
                blueGhost.updatePositionAndRotation(blueBoxPos.x, blueBoxPos.z, GhostModel.DIRECTION_DOWN);
            }
        }

        return;
    }


    private BranchGroup createTreasure(double x, double y, double z) {
        if (treasureAppearance == null) {
            treasureAppearance = new Appearance();
            treasureAppearance.setMaterial(new Material(
                    new Color3f(1.0f, 0.84f, 0.0f),
                    new Color3f(0.0f, 0.0f, 0.0f),
                    new Color3f(1.0f, 0.84f, 0.0f),
                    new Color3f(1.0f, 1.0f, 1.0f),
                    64.0f));
        }

        // Create the coin shape (same as before)
        float radius = 0.025f;
        float height = 0.005f;
        Cylinder treasureDisk = new Cylinder(radius, height,
                Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS,
                treasureAppearance);

        treasureDisk.setCapability(Shape3D.ALLOW_PICKABLE_READ);
        treasureDisk.setCapability(Shape3D.ALLOW_PICKABLE_WRITE);
        treasureDisk.setPickable(true);
        treasureDisk.setUserData("treasure");

        // Create the transformation hierarchy (same as before)
        Transform3D coinRotation = new Transform3D();
        coinRotation.rotZ(Math.PI/2);

        TransformGroup coinOrientationTG = new TransformGroup(coinRotation);
        coinOrientationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        coinOrientationTG.addChild(treasureDisk);
        coinOrientationTG.setUserData("treasure");

        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rotationTG.addChild(coinOrientationTG);

        Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 4000, 0, 0, 0, 0, 0);
        RotationInterpolator rotator = new RotationInterpolator(
                rotationAlpha, rotationTG, new Transform3D(), 0.0f, (float)(Math.PI*2));
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        rotationTG.addChild(rotator);

        Transform3D position = new Transform3D();
        position.setTranslation(new Vector3d(x, y, z));
        TransformGroup positionedTG = new TransformGroup(position);
        positionedTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        positionedTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        positionedTG.addChild(rotationTG);
        positionedTG.setUserData("treasure");

        this.treasureGroup = positionedTG;

        // Wrap the TransformGroup in a BranchGroup
        BranchGroup treasureBG = new BranchGroup();
        treasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        treasureBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        treasureBG.setCapability(BranchGroup.ALLOW_DETACH);
        treasureBG.addChild(positionedTG);

        return treasureBG;
    }

    public static void main(String[] args) {
        frame = new JFrame("Basic Scene: Maze View (Networking, Ghosts, Star System, Sound & Spotlight)");
        BasicScene basicScene = new BasicScene("localhost", "Player1");
        BranchGroup sceneBG = basicScene.createScene();
        basicScene.setupUniverse(sceneBG);
        frame.getContentPane().add(basicScene);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}