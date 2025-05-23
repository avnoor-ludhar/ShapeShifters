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
import org.jogamp.java3d.utils.geometry.Cylinder;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.java3d.utils.picking.PickTool;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.vecmath.*;

// The class which is responsible for
    // Rendering the 3D maze environment
    // Managing multiplayer game logic
    // Communicating with the game server
    // Handling player movement, NPCs, lighting, audio, treasure interaction, LOD, and GUI
public class BasicScene extends JPanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;

    // Ghost models for players (using GhostModel)
    private GhostModel redGhost;
    private GhostModel blueGhost;
    private Canvas3D canvas;
    private PickTool redPickTool;
    private PickTool bluePickTool;
    private AppearanceCycleBehavior blueGhostCycle;
    // Player positions (x, y, z) – y remains constant at 0.1
    private Vector3d redBoxPos = new Vector3d(0.3, 0.1, 0.0);
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

    // Fields for IP address and username
    private String ipAddress;
    private String username;
    private BranchGroup rootBG;
    private TreasureKeyBehavior treasureKeyBehavior;
    private TreasureManager treasureManager;

    // Add these field declarations to the class
    private MazeSign mazeSign;
    private static boolean gameEnded = false;

    // Default constructor
    public BasicScene() {
        this("localhost", "Player");
    }

    // Updates the visuals of the scene
    private void updateAppearance(Node node, Appearance app) {
        if (node instanceof Shape3D) {
            ((Shape3D) node).setAppearance(app);
        }
        else if (node instanceof Group) {
            Group group = (Group) node;
            for (int i = 0; i < group.numChildren(); i++) {
                updateAppearance(group.getChild(i), app);
            }
        }
    }

    // Full constructor
        // Connects to server, receives maze & NPC data, sets up networking
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
                this.treasureManager = new TreasureManager(tx,ty, tz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("GAME_END")) {
                        // Extract optional winner information.
                        String[] tokens = line.split(" ");
                        String winner = (tokens.length > 1) ? tokens[1] : "unknown";
                        triggerGameEnd(winner);
                        continue;
                    }

                    if (line != null && line.startsWith("GREEN")) {
                        updateAppearance(blueGhost.getTransformGroup(), blueGhostCycle.newAppearance);
                    }
                    if (line != null && line.startsWith("BLUE")) {
                        updateAppearance(blueGhost.getTransformGroup(), blueGhostCycle.originalAppearance);
                    }

                    // Handle treasure morph broadcast
                    if (line.startsWith("TREASURE_MORPH")) {
                        if (treasureKeyBehavior != null) {
                            treasureKeyBehavior.startMorphAnimation();
                            System.out.println("TREASURE_MORPH activated.");
                        }
                        continue;
                    }
                    // Handle NPC update messages
                    if (line.startsWith("NPC_UPDATE")) {
                        String[] tokens = line.split(" ");
                        for (int i = 1; i < tokens.length; i += 6) {
                            int npcId = Integer.parseInt(tokens[i]);
                            double x = Double.parseDouble(tokens[i + 1]);
                            double y = Double.parseDouble(tokens[i + 2]);
                            double z = Double.parseDouble(tokens[i + 3]);
                            double dirX = Double.parseDouble(tokens[i + 4]);
                            double dirZ = Double.parseDouble(tokens[i + 5]);

                            NPC npc = npcs.get(npcId);
                            Vector3d newPos = new Vector3d(x, y, z);
                            npc.setPosition(newPos);
                            Transform3D transform = new Transform3D();
                            transform.setTranslation(newPos);
                            npc.getTransformGroup().setTransform(transform);

                            // Update direction and rotation
                            npc.updateDirection(new Vector3d(dirX, 0, dirZ));
                        }
                        continue;
                    }

                    // Process regular player position updates
                    String[] tokens = line.split(" ");
                    if (tokens.length < 4) {
                        continue;
                    }

                    int id = Integer.parseInt(tokens[0]);
                    double x = Double.parseDouble(tokens[1]);
                    double y = Double.parseDouble(tokens[2]);
                    double z = Double.parseDouble(tokens[3]);
                    int direction = GhostModel.DIRECTION_DOWN;
                    if (tokens.length >= 5) {
                        direction = Integer.parseInt(tokens[4]);
                    }
                    if (id == 1 && redGhost != null) {
                        redBoxPos.x = x;
                        redBoxPos.z = z;
                        redGhost.updatePositionAndRotation(x, z, direction);

                        if (this.universe != null) {
                            updateCamera();
                            updateSpotlight();
                        }
                    }

                    else if (id == 2 && blueGhost != null) {
                        blueBoxPos.x = x;
                        blueBoxPos.z = z;
                        blueGhost.updatePositionAndRotation(x, z, direction);
                        if (this.universe != null) {
                            updateCamera();
                            updateSpotlight();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


    }

    // Builds and returns the entire 3D scene graph
    public BranchGroup createScene() {

        BranchGroup sceneBG = new BranchGroup();
        Background background = new Background(new Color3f(0.01f, 0.01f, 0.01f));
        BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 100.0);
        background.setApplicationBounds(bounds);
        sceneBG.addChild(background);
        ShootingStars shootingStars = new ShootingStars();
        sceneBG.addChild(shootingStars.getStarSystemTG());
        Appearance platformAppearance = new Appearance();
        platformAppearance.setMaterial(new Material(
                new Color3f(0.8f, 0.8f, 0.8f),
                new Color3f(0.2f, 0.2f, 0.2f),
                new Color3f(1.0f, 1.0f, 1.0f),
                new Color3f(1.0f, 1.0f, 1.0f), 64.0f));

        String floorTexturePath = "src/ShapeShifters/Textures/QuartzFloorTexture.jpg";
        try {
            URL floorTextureURL = new File(floorTexturePath).toURI().toURL();
            Texture floorTexture = new TextureLoader(floorTextureURL, "RGB", new java.awt.Container()).getTexture();
            if (floorTexture != null) {
                platformAppearance.setTexture(floorTexture);
                TextureAttributes texAttr = new TextureAttributes();
                texAttr.setTextureMode(TextureAttributes.MODULATE);
                platformAppearance.setTextureAttributes(texAttr);
            }
        }
        catch (Exception e) {
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

        if (playerId == 1) {
            Point2f redPosn = getUnfilledPosn();
            redBoxPos.x = redPosn.getX();
            redBoxPos.z = redPosn.getY();
            out.println(playerId + " " + redBoxPos.x + " " + 0.1 + " " + redBoxPos.z + " " + GhostModel.DIRECTION_DOWN);
        }
        else {
            Point2f bluePosn = getUnfilledPosn();
            blueBoxPos.x = bluePosn.getX();
            blueBoxPos.z = bluePosn.getY();
            out.println(playerId + " " + blueBoxPos.x + " " + 0.1 + " " + blueBoxPos.x + " " + GhostModel.DIRECTION_DOWN);
        }

        redGhost = new GhostModel(true, redBoxPos);
        blueGhost = new GhostModel(false, blueBoxPos);
        sceneBG.addChild(redGhost.getTransformGroup());

        // Blue ghost added to pickTool
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
        }
        catch(Exception e) {
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

        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (walls[i][j] == 1) {
                    TransformGroup tg = addWall(sceneBG,
                            -1 + i * 0.103f, 0.1f, -1 + j * 0.103f,
                            0.055f, 0.05f, 0.055f,
                            wallAppearance, i, j, true);
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

        BranchGroup npcBG = new BranchGroup();
        npcBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

        for (NPC npc: npcs) {
            npcBG.addChild(npc.getTransformGroup());
        }

        for (int i = 9; i < 12; i++) {
            for (int j = 9; j < 12; j++) {
                addWall(sceneBG,
                        -1 + i * 0.103f, 0.1f, -1 + j * 0.103f,
                        0.055f, 0.05f, 0.055f,
                        wallAppearance, i, j, false);
            }
        }

        bluePickTool = new PickTool(npcBG);
        bluePickTool.setMode(PickTool.BOUNDS);
        sceneBG.addChild(npcBG);


        if (treasureManager.getTreasureBranchGroup() != null) {
            sceneBG.addChild(treasureManager.getTreasureBranchGroup() );

            // Create and add treasure behavior
            treasureKeyBehavior = new TreasureKeyBehavior(treasureManager, redBoxPos, blueBoxPos, playerId, sceneBG, out);
            treasureKeyBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
            sceneBG.addChild(treasureKeyBehavior);
        }

        createSpotlight(sceneBG);

        TransformGroup blueGhostTransform = blueGhost.getTransformGroup();
        BranchGroup blueGhostBranch = new BranchGroup();
        blueGhostBranch.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        blueGhostBranch.addChild(blueGhostTransform);
        sceneBG.addChild(blueGhostBranch);
        redPickTool = new PickTool(blueGhostBranch);
        redPickTool.setMode(PickTool.BOUNDS);

        Cylinder base = new Cylinder(0.1f, .2f);
        Transform3D baseTransform = new Transform3D();
        baseTransform.setTranslation(new Vector3f(.03f, 0.1f, .03f));
        baseTransform.setScale(.45f);
        TransformGroup baseTG = new TransformGroup();
        baseTG.setTransform(baseTransform);
        baseTG.addChild(base);
        sceneBG.addChild(baseTG);

        // Middle horizontal rectangle (Box)
        Box middleBox = new Box(0.3f, 0.005f, 0.02f, null);
        TransformGroup spinMiddleTG = createSpinner(2000, 'x');

        Transform3D midBoxTrans = new Transform3D();
        midBoxTrans.setTranslation(new Vector3f(0f, 0.09f, 0f));
        TransformGroup midBoxTG = new TransformGroup(midBoxTrans);
        midBoxTG.addChild(middleBox);
        spinMiddleTG.addChild(midBoxTG);
        baseTG.addChild(spinMiddleTG);

        // Ends of the middle box (small boxes spinning fast)
        float offset = 0.3f;

        // Create LOD versions of the fan blades
        // Load the models separately for left and right
        ObjectFile f = new ObjectFile(ObjectFile.RESIZE, (float) (60 * Math.PI / 180.0));
        Scene s1 = null;
        Scene s2 = null;
        try {
            s1 = f.load("src/ShapeShifters/assets/FanBlades.obj");
            s2 = f.load("src/ShapeShifters/assets/FanBlades.obj");
        }
        catch (Exception e) {}
        if (s1 == null || s2 == null) {
            System.exit(1);
        }

        // LEFT FAN BLADE
        // Create high detail version for left fan
        TransformGroup tg1 = new TransformGroup();
        tg1.addChild(s1.getSceneGroup());
        Transform3D transform1 = new Transform3D();
        transform1.rotY(Math.PI/2);
        transform1.setScale(.1);
        tg1.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg1.setTransform(transform1);

        // Create a low detail version (small box) for the left fan blade
        TransformGroup lowDetailLeftTG = new TransformGroup();
        Appearance fanAppearance = new Appearance();
        Material fanMaterial = new Material(
                new Color3f(0.8f, 0.8f, 0.8f),
                new Color3f(0.1f, 0.1f, 0.1f),
                new Color3f(0.8f, 0.8f, 0.8f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f
        );

        fanAppearance.setMaterial(fanMaterial);
        Box lowDetailFan = new Box(0.01f, 0.01f, 0.01f, Box.GENERATE_NORMALS, fanAppearance);
        lowDetailLeftTG.addChild(lowDetailFan);

        // Create empty node with 2 levels
        TransformGroup emptyLeftTG = new TransformGroup();
        Box emptyBox = new Box(0.001f, 0.001f, 0.001f, Box.GENERATE_NORMALS, fanAppearance);
        emptyLeftTG.addChild(emptyBox);

        // Create LOD for left fan blade
        double[] fanDistances = {2.3, 2.4, 100.0};
        BranchGroup leftFanLODBG = LODHelper.createLOD(tg1, emptyLeftTG, lowDetailLeftTG, fanDistances);

        // RIGHT FAN BLADE
        // Create high detail version for right fan (using separate model instance)
        TransformGroup tg2 = new TransformGroup();
        tg2.addChild(s2.getSceneGroup());
        Transform3D transform2 = new Transform3D();
        transform2.rotY(Math.PI/2);
        transform2.setScale(.1);
        tg2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg2.setTransform(transform2);

        // Create a low detail version for the right fan blade
        TransformGroup lowDetailRightTG = new TransformGroup();
        Box lowDetailFanRight = new Box(0.01f, 0.01f, 0.01f, Box.GENERATE_NORMALS, fanAppearance);
        lowDetailRightTG.addChild(lowDetailFanRight);

        // Create empty node with 2 levels
        TransformGroup emptyRightTG = new TransformGroup();
        Box emptyRightBox = new Box(0.001f, 0.001f, 0.001f, Box.GENERATE_NORMALS, fanAppearance);
        emptyRightTG.addChild(emptyRightBox);

        // Create LOD for right fan blade (using same distances)
        BranchGroup rightFanLODBG = LODHelper.createLOD(tg2, emptyRightTG, lowDetailRightTG, fanDistances);

        // Add fan blades to the spinning transforms
        TransformGroup spinLeft = createSpinner(200, 'z'); // faster spin
        Transform3D leftTrans = new Transform3D();
        leftTrans.setTranslation(new Vector3f(-offset, 0f, 0f));
        TransformGroup leftTG = new TransformGroup(leftTrans);
        leftTG.addChild(leftFanLODBG);
        spinLeft.addChild(leftTG);
        midBoxTG.addChild(spinLeft);

        TransformGroup spinRight = createSpinner(200, 'z');
        Transform3D rightTrans = new Transform3D();
        rightTrans.setTranslation(new Vector3f(offset, 0f, 0f));
        TransformGroup rightTG = new TransformGroup(rightTrans);
        rightTG.addChild(rightFanLODBG);
        spinRight.addChild(rightTG);
        midBoxTG.addChild(spinRight);

        this.rootBG = sceneBG;
        rootBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        rootBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

        // When creating the fan components, add user data to identify them
        leftFanLODBG.setUserData("fan-left");
        rightFanLODBG.setUserData("fan-right");

        sceneBG.compile();
        return sceneBG;
    }

    // Game ended
    public static boolean getGameEnded(){
        return gameEnded;
    }

    // Function that triggers the game ended logic
    private void triggerGameEnd(String winner) {
        gameEnded = true;
        // Pause the game for 2 seconds before showing the end animation.
        new Thread(() -> {
            try {
                // 3 second delay
                Thread.sleep(3000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            GameEndAnimation gameEnd = new GameEndAnimation(universe, rootBG);
            gameEnd.triggerGameEnd(winner);
            System.out.println("Game Over! Winner: " + winner);
        }).start();
    }

    // Function to get the unfilled posn
    private Point2f getUnfilledPosn() {
        Random rand = new Random();
        float x;
        float z;
        while (true) {
            int randX = rand.nextInt(18) + 1;
            int randY = rand.nextInt(18) + 1;
            // Empty posn not filled by the spinny thingamajig
            if ((walls[randX][randY] == 0) && !(9 <= randX && randX < 12 && 9 <= randY && randY < 12)) {
                x = -1 + randX * 0.103f;
                z = -1 + randY * 0.103f;
                break;
            }
        }
        return new Point2f(x, z);
    }

    // Function to actually create the spinner in the middle of the scene
    private TransformGroup createSpinner(long durationMillis, char axis) {
        TransformGroup spinner = new TransformGroup();
        spinner.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Alpha spinAlpha = new Alpha(-1, durationMillis);
        Transform3D rotationAxis = new Transform3D();
        switch (axis) {
            case 'x':
                // Rotating around x by default
                break;
            case 'z':
                rotationAxis.rotZ(Math.PI / 2); break;
            // Default is Y-axis
        }

        RotationInterpolator rotator = new RotationInterpolator(spinAlpha, spinner, rotationAxis, 0f, (float)Math.PI * 2);
        rotator.setSchedulingBounds(new BoundingSphere());
        spinner.addChild(rotator);
        return spinner;
    }

    // Function to add walls to the scene of the maze once BasicScene.java is ran
    private TransformGroup addWall(BranchGroup sceneBG, double x, double y, double z,
                                   double width, double height, double depth,
                                   Appearance appearance, int i, int j, boolean render) {
        if (render) {
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

        double left = x - width;
        double top = z + depth;
        double rectWidth = 2 * width;
        double rectHeight = 2 * depth;
        double bottom = top - rectHeight;
        Rectangle2D.Double wallRect = new Rectangle2D.Double(left, bottom, rectWidth, rectHeight);
        wallBounds.put(wallRect, new Point(i, j));
        return null;
    }

    // Adds a yellow spotlight above the player
        // Follows the players movements as well
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

    // Sets up the 3D canvas, camera, controls, lighting, and real-time input listeners
    public void setupUniverse(BranchGroup sceneBG) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas = new Canvas3D(config);
        canvas.addMouseListener(this);
        // Set up key listener to update movement state
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'w':
                        upPressed = true;
                        break;
                    case 's':
                        downPressed = true;
                        break;
                    case 'a':
                        leftPressed = true;
                        break;
                    case 'd':
                        rightPressed = true;
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'w':
                        upPressed = false;
                        break;
                    case 's':
                        downPressed = false;
                        break;
                    case 'a':
                        leftPressed = false;
                        break;
                    case 'd':
                        rightPressed = false;
                        break;
                    default:
                        break;
                }
            }
        });

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        blueGhostCycle = new AppearanceCycleBehavior(blueGhost.getTransformGroup(), blueGhost, out);
        blueGhostCycle.setSchedulingBounds(new BoundingSphere(new Point3d(0,0,0), 100.0));
        BranchGroup behaviorBG = new BranchGroup();
        behaviorBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        behaviorBG.addChild(blueGhostCycle);
        sceneBG.addChild(behaviorBG);
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
    }

    // Movement called on timer
        // Applies key movement, handles collisions, updates ghost positions
    private void updateMovement() {
        double dx = 0, dz = 0;
        double oldX, oldZ, newX = 0, newZ = 0;
        if (playerId == 1) {
            oldX = redBoxPos.x;
            oldZ = redBoxPos.z;
        }
        else {
            oldX = blueBoxPos.x;
            oldZ = blueBoxPos.z;
        }

        int direction = -1;
        // Get the movement step from the corresponding ghost
        double step = (playerId == 1) ? redGhost.step : blueGhost.step;
        boolean moved = false;

        // Try various combinations: first change both axes, then only one axis
        boolean[][] combos = { {true, true}, {true, false}, {false, true}, {false, false} };
        for (boolean[] combo : combos) {
            boolean changeX = combo[0], changeZ = combo[1];
            dx = 0;
            dz = 0;
            newX = oldX;
            newZ = oldZ;
            direction = -1;
            // Apply key inputs conditionally
            if (upPressed && changeZ) {
                dz -= step;
                direction = GhostModel.DIRECTION_UP;
            }
            if (downPressed && changeZ) {
                dz += step;
                direction = GhostModel.DIRECTION_DOWN;
            }
            if (leftPressed && changeX) {
                dx -= step;
                direction = GhostModel.DIRECTION_LEFT;
            }
            if (rightPressed && changeX) {
                dx += step;
                direction = GhostModel.DIRECTION_RIGHT;
            }

            // Normalize diagonal movement
            if (dx != 0 || dz != 0) {
                double length = Math.sqrt(dx * dx + dz * dz);
                dx = dx / length * step;
                dz = dz / length * step;
                if (dx != 0 && dz != 0) {
                    if (dx < 0 && dz > 0) {
                        direction = GhostModel.DIRECTION_DOWNLEFT;
                    }
                    else if (dx < 0 && dz < 0) {
                        direction = GhostModel.DIRECTION_UPLEFT;
                    }
                    else if (dx > 0 && dz > 0) {
                        direction = GhostModel.DIRECTION_DOWNRIGHT;
                    }
                    else {
                        direction = GhostModel.DIRECTION_UPRIGHT;
                    }
                }
            }
            else {
                // No movement if no key pressed
                return;
            }

            newX += dx;
            newZ += dz;
            // If the new position doesnt collide with a wall
                // Then accept this move
            if (!collidesWithWall(newX, newZ)) {
                moved = true;
                break;
            }
        }

        if (!moved) {
            // If no valid movement, play collision sound and exit
            if (System.currentTimeMillis() - lastCollisionTime > COLLISION_COOLDOWN) {
                playWallCollisionSound();
                lastCollisionTime = System.currentTimeMillis();
            }
            return;
        }

        // Check for collisions with NPCs
        for (NPC npc : npcs) {
            Vector3d npcPos = npc.getPosition();
            if (CollisionDetector.isColliding(
                    newX, newZ, GhostModel.getCharacterHalf(),
                    npcPos.x, npcPos.z, NPC.getCharacterHalf())) {
                if (System.currentTimeMillis() - lastCollisionTime > COLLISION_COOLDOWN) {
                    playWallCollisionSound();
                    lastCollisionTime = System.currentTimeMillis();
                }
                return;
            }
        }

        // Check for collisions with the other player
        Vector3d otherPlayerPos = (playerId == 1) ? blueBoxPos : redBoxPos;
        if (CollisionDetector.isColliding(
                newX, newZ, GhostModel.getCharacterHalf(),
                otherPlayerPos.x, otherPlayerPos.z, GhostModel.getCharacterHalf())) {
            if (System.currentTimeMillis() - lastCollisionTime > COLLISION_COOLDOWN) {
                playWallCollisionSound();
                lastCollisionTime = System.currentTimeMillis();
            }
            return;
        }

        // Update position if no collisions occurred
        if (playerId == 1) {
            redBoxPos.x = newX;
            redBoxPos.z = newZ;
            treasureKeyBehavior.updateRedPosition(redBoxPos);
            redGhost.updatePositionAndRotation(newX, newZ, direction);
        }
        else {
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
    }

    // Moves the camera above and behind the current player
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

        // Update fan blade LOD positions
        updateFanLODPositions();
    }

    // Moves spotlight position to follow the current player
    private void updateSpotlight() {
        Vector3d localPos = (playerId == 1) ? redBoxPos : blueBoxPos;
        Transform3D spotlightTransform = new Transform3D();
        Vector3d spotlightPos = new Vector3d(localPos.x, 0.8, localPos.z);
        spotlightTransform.setTranslation(spotlightPos);
        spotlightTG.setTransform(spotlightTransform);
    }

    // Iterates through the scene to update LOD based on player distance
    private void updateFanLODPositions() {
        // Get the player position based on playerId
        Vector3d playerPos = (playerId == 1) ? redBoxPos : blueBoxPos;

        // Calculate the position of the fan (at center with cylinder)
        Vector3d fanPosition = new Vector3d(0.03, 0.1, 0.03);

        // Find only fan related DistanceLOD behaviors and update them
        if (rootBG != null) {
            // Look for fan LOD behaviors specifically
                // These will be in the midBoxTG hierarchy
            for (int i = 0; i < rootBG.numChildren(); i++) {
                Node child = rootBG.getChild(i);
                if (child instanceof TransformGroup) {
                    TransformGroup tg = (TransformGroup)child;
                    if (isFanComponent(tg)) {
                        updateFanLODInGroup(tg, playerPos);
                    }
                }
            }
        }
    }

    // Identifies if a node is a fan part using userData
    private boolean isFanComponent(Node node) {
        return node.getUserData() != null &&
               node.getUserData().toString().contains("fan");
    }

    // Recursively updates LOD for any fan related group
    private void updateFanLODInGroup(Node node, Vector3d viewerPosition) {
        if (node instanceof DistanceLOD) {
            // Update the LOD's position
            LODHelper.updateLODPosition((DistanceLOD) node, viewerPosition);
        } else if (node instanceof Group) {
            Group group = (Group) node;
            // Search all children
            for (int i = 0; i < group.numChildren(); i++) {
                updateFanLODInGroup(group.getChild(i), viewerPosition);
            }
        }
    }

    // Checks if player collides with maze wall
    // Ignores moving walls when fully 'open'
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

    //
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

    // Plays footstep sound when player moves around the maze
    private void playWallCollisionSound() {
        try {
            File soundFile = new File("src/ShapeShifters/sounds/wallCollide.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            javax.sound.sampled.Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Adds the "ShapeShifters" floating sign to the NorthWest corner
    private void createMazeSigns(BranchGroup sceneBG) {
        // Calculate position for the North West sign
        double cornerX = 0.9;
        double cornerZ = 0.9;
        double signHeight = 0.3;

        // Create only the North West sign with "ShapeShifters" text
        mazeSign = new MazeSign(
            new Vector3d(-cornerX, signHeight, -cornerZ),
            "ShapeShifters"
        );

        // Add the sign to the scene
        sceneBG.addChild(mazeSign.getTransformGroup());
    }

    // Unused MouseListener methods
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e){}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}


    @Override
    // Mouse interaction logic
    public void mouseClicked(MouseEvent e) {
        // Compute the pick ray from the click coordinates
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

        // Check for a collision using the red pick tool
        redPickTool.setShapeRay(eyePos, rayDirection);
        if (redPickTool.pickClosest() != null) {
            // Calculate distance between red and blue ghosts
            double dist = Math.sqrt(Math.pow(redBoxPos.x - blueBoxPos.x, 2) + Math.pow(redBoxPos.z - blueBoxPos.z, 2));
            System.out.println("Red pick detected. Distance: " + dist);
            System.out.printf("%f\n %f %f\n%f %f\n", dist, blueBoxPos.x, blueBoxPos.z, redBoxPos.x, redBoxPos.z);

            // If within a threshold and this is player 1, update blue ghost's position
            if (dist < 0.5f && playerId == 1 && !gameEnded) {
                System.out.println("Red player's action: updating blue ghost position.");
                Point2f p = getUnfilledPosn();
                blueBoxPos.x = p.getX();
                blueBoxPos.z = p.getY();

                // Broadcast new blue ghost position to all clients
                out.println("2 " + p.getX() + " " + 0.1 + " " + p.getY() + " " + GhostModel.DIRECTION_DOWN);
                blueGhost.updatePositionAndRotation(p.getX(), p.getY(), GhostModel.DIRECTION_DOWN);
                out.println("GAME_END Red");
            }
        }

        // Check for a collision using the blue pick tool
        bluePickTool.setShapeRay(eyePos, rayDirection);
        if (bluePickTool.pickClosest() != null && playerId == 2) {
            // Instead of directly changing the appearance, we call triggerChange() so the event is sent to the server
            blueGhostCycle.triggerChange();
        }
        return;
    }

    // Main method for launching the game scene
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