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
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.java3d.utils.geometry.Primitive;
import org.jogamp.java3d.utils.geometry.Cylinder;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.vecmath.*;
import org.jogamp.java3d.utils.picking.PickTool;

public class BasicScene extends JPanel {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;

    // Ghost models for players (using GhostModel)
    private GhostModel redGhost;
    private GhostModel blueGhost;

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
    private TransformGroup treasureGroup; // Reference to the treasure's TransformGroup
    private Appearance treasureAppearance; // Appearance for the treasure
    // NEW: Define a constant for interaction distance and a flag to track state.
    private static final double TREASURE_INTERACT_DISTANCE = 0.15;
    private boolean treasureIsCoin = true;

    // Fields for IP address and username
    private String ipAddress;
    private String username;
    private Canvas3D canvas;
    private BranchGroup rootBG;
    private static PickTool pickTool = null;

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
                treasureGroup = createTreasure(tx, ty, tz);
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
                new Color3f(0.8f, 0.8f, 0.8f),
                new Color3f(0.2f, 0.2f, 0.2f),
                new Color3f(1.0f, 1.0f, 1.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        TransformGroup platformTG = new TransformGroup();
        Box platform = new Box(1.0f, 0.05f, 1.0f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, platformAppearance);
        platformTG.addChild(platform);
        sceneBG.addChild(platformTG);

        redGhost = new GhostModel(true, redBoxPos);
        blueGhost = new GhostModel(false, blueBoxPos);
        sceneBG.addChild(redGhost.getTransformGroup());
        sceneBG.addChild(blueGhost.getTransformGroup());

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

        if (treasureGroup != null) {
            sceneBG.addChild(treasureGroup);
        }

        createSpotlight(sceneBG);

        this.rootBG = sceneBG;
        rootBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        rootBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

        if (pickTool == null) {
            pickTool = new PickTool(rootBG);
            pickTool.setMode(PickTool.BOUNDS);
        }

        sceneBG.compile();
        return sceneBG;
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

    public void setupUniverse(BranchGroup sceneBG) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas = new Canvas3D(config);

        // Update key listener to handle movement and treasure toggle ('e').
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'w': upPressed = true; break;
                    case 's': downPressed = true; break;
                    case 'a': leftPressed = true; break;
                    case 'd': rightPressed = true; break;
                    case 'e':
                        // Check if player is close enough to interact with treasure.
                        if (treasureGroup != null) {
                            Transform3D treasureTransform = new Transform3D();
                            treasureGroup.getTransform(treasureTransform);
                            Vector3d treasurePos = new Vector3d();
                            treasureTransform.get(treasurePos);
                            Vector3d playerPos = (playerId == 1) ? redBoxPos : blueBoxPos;
                            Vector3d diff = new Vector3d();
                            diff.sub(treasurePos, playerPos);
                            if (diff.length() < TREASURE_INTERACT_DISTANCE && treasureIsCoin) {
                                morphTreasureToStar();
                                treasureIsCoin = false;
                            }
                        }
                        break;
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
    }

    private void updateMovement() {
        double newX, newZ;
        if (playerId == 1) {
            newX = redBoxPos.x;
            newZ = redBoxPos.z;
        } else {
            newX = blueBoxPos.x;
            newZ = blueBoxPos.z;
        }

        double dx = 0, dz = 0;
        int direction = -1;

        if (upPressed) {
            dz -= STEP;
            direction = GhostModel.DIRECTION_UP;
        }
        if (downPressed) {
            dz += STEP;
            direction = GhostModel.DIRECTION_DOWN;
        }
        if (leftPressed) {
            dx -= STEP;
            direction = GhostModel.DIRECTION_LEFT;
        }
        if (rightPressed) {
            dx += STEP;
            direction = GhostModel.DIRECTION_RIGHT;
        }

        if (dx != 0 || dz != 0) {
            double length = Math.sqrt(dx * dx + dz * dz);
            dx = dx / length * STEP;
            dz = dz / length * STEP;

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
            return;
        }

        newX += dx;
        newZ += dz;

        if (!collidesWithWall(newX, newZ)) {
            if (playerId == 1) {
                redBoxPos.x = newX;
                redBoxPos.z = newZ;
                redGhost.updatePositionAndRotation(newX, newZ, direction);
            } else {
                blueBoxPos.x = newX;
                blueBoxPos.z = newZ;
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

    // Create the treasure (coin) with an initial coin shape.
    private TransformGroup createTreasure(double x, double y, double z) {
        if (treasureAppearance == null) {
            treasureAppearance = new Appearance();
            treasureAppearance.setMaterial(new Material(
                    new Color3f(1.0f, 0.84f, 0.0f),
                    new Color3f(0.0f, 0.0f, 0.0f),
                    new Color3f(1.0f, 0.84f, 0.0f),
                    new Color3f(1.0f, 1.0f, 1.0f),
                    64.0f));
        }

        float radius = 0.025f;
        float height = 0.005f;
        Cylinder treasureDisk = new Cylinder(radius, height,
                Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS,
                treasureAppearance);

        treasureDisk.setCapability(Shape3D.ALLOW_PICKABLE_READ);
        treasureDisk.setCapability(Shape3D.ALLOW_PICKABLE_WRITE);
        treasureDisk.setPickable(true);
        treasureDisk.setUserData("treasure");

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
        // NEW: Enable both transform read and write for later interaction.
        positionedTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        positionedTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        positionedTG.addChild(rotationTG);
        positionedTG.setUserData("treasure");

        return positionedTG;
    }

    // Morph the coin treasure into a star.
    private void morphTreasureToStar() {
        if (treasureGroup == null) return;
        treasureGroup.removeAllChildren();

        TransformGroup rotationTG = new TransformGroup();
        rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Shape3D starShape = createStarShape(0.025f, 0.005f, treasureAppearance);
        starShape.setPickable(true);
        rotationTG.addChild(starShape);

        Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE,
                0, 0, 4000, 0, 0, 0, 0, 0);
        Transform3D yAxis = new Transform3D();
        RotationInterpolator rotator = new RotationInterpolator(
                rotationAlpha,
                rotationTG,
                yAxis,
                0.0f, (float) (Math.PI * 2.0f));
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0, 0, 0), 100.0));
        rotationTG.addChild(rotator);

        treasureGroup.addChild(rotationTG);
        treasureGroup.setUserData("treasure");
    }

    // Helper method to create a star shape.
    private Shape3D createStarShape(float radius, float height, Appearance appearance) {
        int numPoints = 5;
        int numVertices = numPoints * 2;
        Point3f[] frontVertices = new Point3f[numVertices + 1];
        frontVertices[0] = new Point3f(0.0f, 0.0f, height / 2);
        float innerFactor = 0.5f;
        for (int i = 0; i < numVertices; i++) {
            double angle = Math.PI / 2 + i * Math.PI / numPoints;
            float r = (i % 2 == 0) ? radius : radius * innerFactor;
            float x = (float)(r * Math.cos(angle));
            float y = (float)(r * Math.sin(angle));
            frontVertices[i + 1] = new Point3f(x, y, height / 2);
        }
        int[] stripCounts = { numVertices + 1 };
        TriangleFanArray frontFace = new TriangleFanArray(numVertices + 1,
                GeometryArray.COORDINATES, stripCounts);
        frontFace.setCoordinates(0, frontVertices);

        return new Shape3D(frontFace, appearance);
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