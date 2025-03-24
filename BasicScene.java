package ShapeShifters;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.vecmath.*;

public class BasicScene extends JPanel {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;

    // Player box transform groups and positions
    private TransformGroup redBoxTG;
    private TransformGroup blueBoxTG;
    private Vector3d redBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private Vector3d blueBoxPos = new Vector3d(0.0, 0.1, 0.0);
    // Movement step
    private final double STEP = 0.025;

    // Maze variables and wall collision data
    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;
    private static int[][] walls = new int[MAZE_HEIGHT][MAZE_WIDTH];
    private HashMap<Rectangle2D.Double, Point> wallBounds = new HashMap<>();

    // Moving wall data
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

    // Add the missing universe field
    private SimpleUniverse universe;

    // Sound effect timing
    private long lastFootstepTime = 0;
    private long lastCollisionTime = 0;
    private static final long FOOTSTEP_COOLDOWN = 250; // milliseconds
    private static final long COLLISION_COOLDOWN = 400; // milliseconds

    // Spotlight variables
    private SpotLight spotlight;
    private TransformGroup spotlightTG;
    private BoundingSphere lightBounds;
    private static final float SPOTLIGHT_RADIUS = 0.5f;
    private static final float SPOTLIGHT_CONCENTRATION = 50.0f;
    private static final float SPOTLIGHT_SPREAD_ANGLE = (float) Math.PI / 6;

    // Star system (from local version)
    private static final int STAR_COUNT = 15000;
    private static final int SHOOTING_STAR_COUNT = 150;
    private static final float STAR_FIELD_RADIUS = 10.0f;
    private TransformGroup starSystemTG;
    private Shape3D shootingStarShape;
    private PointArray shootingStarPoints;
    private Random random = new Random();

    // For IP address and username
    private String ipAddress;
    private String username;

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

            // Read maze data.
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

            // Read moving wall coordinates.
            for (int i = 0; i < 4; i++) {
                String coords = in.readLine();
                if (coords != null) {
                    String[] coordsSplit = coords.split(" ");
                    Point p = new Point(Integer.parseInt(coordsSplit[0]), Integer.parseInt(coordsSplit[1]));
                    movingWalls.add(p);
                    long offset = System.currentTimeMillis() % 19000;
                    Alpha a = new Alpha(-1, Alpha.INCREASING_ENABLE | Alpha.DECREASING_ENABLE,
                            0, 19000 - offset, 2000, 0, 5000, 2000, 0, 10000);
                    movingWallAlphas.put(p, a);
                }
            }

            // Set up NPC appearance and data.
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Listen for server messages.
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
                    Transform3D update = new Transform3D();
                    update.setTranslation(new Vector3d(x, y, z));
                    if (id == 1) {
                        redBoxTG.setTransform(update);
                    } else if (id == 2) {
                        blueBoxTG.setTransform(update);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- Scene creation ---
    public BranchGroup createScene() {
        BranchGroup sceneBG = new BranchGroup();

        // Background setup.
        Background background = new Background(new Color3f(0.0f, 0.0f, 0.0f));
        BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 100.0);
        background.setApplicationBounds(bounds);
        sceneBG.addChild(background);

        // Star system.
        createStarSystem(sceneBG);

        // Platform with texture.
        Appearance platformAppearance = new Appearance();
        platformAppearance.setMaterial(new Material(
                new Color3f(0.8f, 0.8f, 0.8f),
                new Color3f(0.2f, 0.2f, 0.2f),
                new Color3f(0.8f, 0.8f, 0.8f),
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

        // Player boxes.
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

        // Wall texture and appearance.
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

        // Lighting.
        lightBounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        createSpotlight(sceneBG);
        AmbientLight darkAmbient = new AmbientLight(new Color3f(0.0f, 0.0f, 0.0f));
        darkAmbient.setInfluencingBounds(lightBounds);
        sceneBG.addChild(darkAmbient);
        DirectionalLight directionalLight = new DirectionalLight(
                new Color3f(1.0f, 1.0f, 1.0f),
                new Vector3f(-1.0f, -1.0f, -1.0f));
        directionalLight.setInfluencingBounds(bounds);
        sceneBG.addChild(directionalLight);

        // Build maze walls.
        for (int i = 5; i < 15; i++) {
            for (int j = 5; j < 15; j++) {
                walls[i][j] = 0;
            }
        }
        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (walls[i][j] == 1) {
                    TransformGroup tg = addWall(sceneBG,
                            -1 + i * 0.103, 0.1, -1 + j * 0.103,
                            0.055, 0.05, 0.055,
                            wallAppearance, i, j);
                    if (movingWalls.contains(new Point(i, j))) {
                        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
                        Transform3D axis = new Transform3D();
                        axis.rotZ(Math.PI / 2);
                        Alpha a = movingWallAlphas.get(new Point(i, j));
                        PositionInterpolator interpolator = new PositionInterpolator(a, tg, axis, 0f, -0.101f);
                        interpolator.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
                        tg.addChild(interpolator);
                    }
                    sceneBG.addChild(tg);
                }
            }
        }

        // Add NPCs.
        for (NPC npc : npcs) {
            sceneBG.addChild(npc.getTransformGroup());
        }

        sceneBG.compile();
        return sceneBG;
    }

    // Adds a wall and stores its collision bounds.
    private TransformGroup addWall(BranchGroup sceneBG, double x, double y, double z,
                                   double width, double height, double depth,
                                   Appearance appearance, int i, int j) {
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(x, y, z));
        TransformGroup tg = new TransformGroup(transform);
        TransformGroup tgContainer = new TransformGroup();
        tgContainer.addChild(tg);
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

        return tgContainer;
    }

    // Create a spotlight following the player.
    private void createSpotlight(BranchGroup sceneBG) {
        spotlightTG = new TransformGroup();
        spotlightTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Point3f initialPosition = new Point3f(0.0f, 0.3f, 0.0f);
        Vector3f initialDirection = new Vector3f(0.0f, -1.0f, 0.0f);
        Color3f lightColor = new Color3f(1.0f, 1.0f, 1.0f);
        spotlight = new SpotLight(lightColor, initialPosition, new Point3f(1.0f, 0.5f, 0.1f),
                initialDirection, SPOTLIGHT_SPREAD_ANGLE, SPOTLIGHT_CONCENTRATION);
        spotlight.setCapability(Light.ALLOW_STATE_WRITE);
        Transform3D lightTransform = new Transform3D();
        lightTransform.setTranslation(new Vector3d(initialPosition));
        TransformGroup spotlightTransformGroup = new TransformGroup(lightTransform);
        spotlightTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        spotlightTransformGroup.addChild(spotlight);
        spotlight.setInfluencingBounds(lightBounds);
        sceneBG.addChild(spotlightTransformGroup);
        this.spotlightTG = spotlightTransformGroup;
    }

    // Star system creation.
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
            float brightness = 0.5f + (random.nextFloat() * 0.5f);
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

    // Initialize a shooting star.
    private void initializeShootingStar(int index) {
        double angle = random.nextDouble() * 2.0 * Math.PI;
        float x = STAR_FIELD_RADIUS * 0.9f;
        float y = (float)(STAR_FIELD_RADIUS * 0.7f * Math.sin(angle));
        float z = (float)(STAR_FIELD_RADIUS * 0.7f * Math.cos(angle));
        shootingStarPoints.setCoordinate(index, new Point3f(x, y, z));
        shootingStarPoints.setColor(index, new Color3f(1.0f, 0.9f, 0.5f));
    }

    // Animate shooting stars.
    private void startShootingStarAnimation() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                while (true) {
                    for (int i = 0; i < SHOOTING_STAR_COUNT; i++) {
                        Point3f position = new Point3f();
                        shootingStarPoints.getCoordinate(i, position);
                        position.x -= 0.15f;
                        position.y -= 0.05f;
                        if (position.x < -STAR_FIELD_RADIUS || Math.abs(position.y) > STAR_FIELD_RADIUS) {
                            initializeShootingStar(i);
                        } else {
                            shootingStarPoints.setCoordinate(i, position);
                        }
                    }
                    Thread.sleep(16);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Universe setup and input handling.
    public void setupUniverse(BranchGroup sceneBG) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas = new Canvas3D(config);
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                double newX, newZ;
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
                    default: return;
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
                    if (out != null) {
                        out.println(playerId + " " + newX + " " + 0.1 + " " + newZ);
                    }
                    updateCamera();
                    updateSpotlight();
                    if (System.currentTimeMillis() - lastFootstepTime > FOOTSTEP_COOLDOWN) {
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
        });
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();
        universe = new SimpleUniverse(canvas);
        updateCamera();
        universe.addBranchGraph(sceneBG);
        setLayout(new BorderLayout());
        add("Center", canvas);
    }

    // Update camera.
    private void updateCamera() {
        Vector3d localPos = (playerId == 1) ? redBoxPos : blueBoxPos;
        Point3d eye = new Point3d(localPos.x, localPos.y + 1.5, localPos.z + 1.0);
        Point3d center = new Point3d(localPos.x, localPos.y, localPos.z);
        Vector3d up = new Vector3d(0, 0, -1);
        Transform3D viewTransform = new Transform3D();
        viewTransform.lookAt(eye, center, up);
        viewTransform.invert();
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);
    }

    // Update spotlight.
    private void updateSpotlight() {
        Vector3d localPos = (playerId == 1) ? redBoxPos : blueBoxPos;
        Transform3D spotlightTransform = new Transform3D();
        Vector3d spotlightPos = new Vector3d(localPos.x, 0.3, localPos.z);
        spotlightTransform.setTranslation(spotlightPos);
        spotlightTG.setTransform(spotlightTransform);
    }

    // Collision detection.
    private boolean collidesWithWall(double x, double z) {
        double half = 0.03;
        double side = 2 * half;
        Rectangle2D.Double boxRect = new Rectangle2D.Double(x - half, z - half, side, side);
        for (Rectangle2D.Double wallRect : wallBounds.keySet()) {
            Point coords = wallBounds.get(wallRect);
            if (movingWallAlphas.containsKey(coords) && movingWallAlphas.get(coords).value() == 1)
                return false;
            if (wallRect.intersects(boxRect))
                return true;
        }
        return false;
    }

    // Play footstep sound using javax.sound.sampled.Clip.
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

    // Play wall collision sound using javax.sound.sampled.Clip.
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

    // --- Main ---
    public static void main(String[] args) {
        frame = new JFrame("Basic Scene: Maze View (Networking, Textures, Sound, Spotlight, NPCs & Star System)");
        BasicScene basicScene = new BasicScene();
        BranchGroup sceneBG = basicScene.createScene();
        basicScene.setupUniverse(sceneBG);
        frame.getContentPane().add(basicScene);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
