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

// Sound imports for Option A
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class BasicScene extends JPanel {
    private static final long serialVersionUID = 1L;
    private static JFrame frame;

    // Two boxes for two players (one controlled locally; the other updated via network)
    private TransformGroup redBoxTG;
    private TransformGroup blueBoxTG;

    // Positions (moving only in x and z; y remains constant at 0.1)
    private Vector3d redBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private Vector3d blueBoxPos = new Vector3d(0.0, 0.1, 0.0);
    private final double STEP = 0.025;
    private List<Rectangle2D.Double> wallBounds = new ArrayList<>();
    private static final double BOX_HALF = 0.03;

    // Maze variables
    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;
    private static int[][] walls = new int[MAZE_HEIGHT][MAZE_WIDTH];

    // Networking variables
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int playerId = 0; // Assigned by the server

    // Reference to the universe for camera updates
    private SimpleUniverse universe;

    // Spotlight that follows the player
    private SpotLight spotlight;
    private TransformGroup spotlightTG;
    private static final float SPOTLIGHT_RADIUS = 0.5f; // Radius of spotlight
    private static final float SPOTLIGHT_CONCENTRATION = 50.0f; // Increased concentration (was 20.0f)
    private static final float SPOTLIGHT_SPREAD_ANGLE = (float)Math.PI/6; // Reduced spread angle (was PI/4)

    // Bounds for the light influence
    private BoundingSphere lightBounds;

    // Sound cooldown variables
    private long lastFootstepTime = 0;
    private long lastCollisionTime = 0;
    private static final long FOOTSTEP_COOLDOWN = 250; // milliseconds between footstep sounds
    private static final long COLLISION_COOLDOWN = 400; // milliseconds between collision sounds

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

            // Read maze data from server
            String maze = in.readLine();
            int index = 0;
            if (maze != null) {
                for (int i = 0; i < MAZE_HEIGHT; i++) {
                    for (int j = 0; j < MAZE_WIDTH; j++) {
                        walls[i][j] = maze.charAt(index) - '0';
                        index++;
                    }
                }
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

        // Define light bounds
        lightBounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);

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

        // Clear a central area (optional)
        for (int i = 5; i < 15; i++) {
            for (int j = 5; j < 15; j++) {
                walls[i][j] = 0;
            }
        }

        // Add walls to the scene
        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (walls[i][j] == 1) {
                    addWall(sceneBG, -1 + i * 0.103f, 0.1f, -1 + j * 0.103f,
                            0.055f, 0.05f, 0.055f, wallAppearance);
                }
            }
        }

        // Create a spotlight that follows the player
        createSpotlight(sceneBG);

        // Set the scene to be very dark overall
        setDarkAmbientLight(sceneBG);

        return sceneBG;
    }

    // Create and add a spotlight that will follow the player
    private void createSpotlight(BranchGroup sceneBG) {
        // Create a transform group for the spotlight
        spotlightTG = new TransformGroup();
        spotlightTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        // Initial position and direction for the spotlight
        Point3f initialPosition = new Point3f(0.0f, 0.3f, 0.0f);
        Vector3f initialDirection = new Vector3f(0.0f, -1.0f, 0.0f);

        // Create the spotlight with initial position and direction
        Color3f lightColor = new Color3f(1.0f, 1.0f, 1.0f);
        spotlight = new SpotLight(
                lightColor,                     // Light color
                initialPosition,                // Initial position
                new Point3f(1.0f, 0.5f, 0.1f),  // Stronger attenuation
                initialDirection,               // Initial direction
                SPOTLIGHT_SPREAD_ANGLE,         // Spread angle
                SPOTLIGHT_CONCENTRATION         // Concentration
        );

        // Set capability to change the light's state
        spotlight.setCapability(Light.ALLOW_STATE_WRITE);

        // Unlike the previous code, we'll use a transform group to move the light
        // First, create a transform to position the light
        Transform3D lightTransform = new Transform3D();
        lightTransform.setTranslation(new Vector3d(initialPosition));

        // Create a transform group for the spotlight that can be updated
        TransformGroup spotlightTransformGroup = new TransformGroup(lightTransform);
        spotlightTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        // Add the spotlight to the transform group
        spotlightTransformGroup.addChild(spotlight);

        // Set the bounds for the light
        spotlight.setInfluencingBounds(lightBounds);

        // Add the transform group to the scene
        sceneBG.addChild(spotlightTransformGroup);

        // Store the transform group for later updates
        this.spotlightTG = spotlightTransformGroup;
    }

    // Set the ambient light to complete darkness
    private void setDarkAmbientLight(BranchGroup sceneBG) {
        // No ambient light (complete darkness)
        Color3f ambientColor = new Color3f(0.0f, 0.0f, 0.0f);
        AmbientLight ambientLight = new AmbientLight(ambientColor);
        ambientLight.setInfluencingBounds(lightBounds);
        sceneBG.addChild(ambientLight);
    }

    // Update the spotlight position to match the player's position
    private void updateSpotlight() {
        Vector3d localPos = (playerId == 1) ? redBoxPos : blueBoxPos;

        // Create a new transform for the updated position
        Transform3D spotlightTransform = new Transform3D();

        // Position slightly above the player
        Vector3d spotlightPos = new Vector3d(
                localPos.x,
                0.3,           // Position above the player
                localPos.z
        );

        // Set the translation for the spotlight
        spotlightTransform.setTranslation(spotlightPos);

        // Apply the new transform to the spotlight's transform group
        spotlightTG.setTransform(spotlightTransform);
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
                    default:
                        return;
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
                    // Update camera to follow the local player's box
                    updateCamera();
                    // Update spotlight to follow the player
                    updateSpotlight();
                    // Play footstep sound effect for local player's move, with cooldown
                    if (System.currentTimeMillis() - lastFootstepTime > FOOTSTEP_COOLDOWN) {
                        playFootstepSound();
                        lastFootstepTime = System.currentTimeMillis();
                    }
                } else {
                    // Play wall collision sound when the player tries to move into a wall, with cooldown
                    if (System.currentTimeMillis() - lastCollisionTime > COLLISION_COOLDOWN) {
                        playWallCollisionSound();
                        lastCollisionTime = System.currentTimeMillis();
                    }
                }
            }
        });

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        // Create and store the universe for later camera updates
        universe = new SimpleUniverse(canvas);
        updateCamera(); // Set an initial camera view
        updateSpotlight(); // Set initial spotlight position
        sceneBG.compile();
        universe.addBranchGraph(sceneBG);

        setLayout(new BorderLayout());
        add("Center", canvas);
    }

    // Update the camera to follow (or "zoom in on") the local player's box.
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

    // Simple collision detection using bounding rectangles.
    private boolean collidesWithWall(double x, double z) {
        double half = BOX_HALF;
        double side = 2 * half;
        Rectangle2D.Double boxRect = new Rectangle2D.Double(x - half, z - half, side, side);

        for (Rectangle2D.Double wallRect : wallBounds) {
            if (wallRect.intersects(boxRect)) {
                return true;
            }
        }
        return false;
    }

    // Play footstep sound effect.
    private void playFootstepSound() {
        try {
            // Adjust path if needed. This expects footsteps.wav in src/ShapeShifters.
            File soundFile = new File("src/ShapeShifters/sounds/footsteps.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Play wall collision sound effect
    private void playWallCollisionSound() {
        try {
            // Path to the wall collision sound file
            File soundFile = new File("src/ShapeShifters/sounds/wallCollide.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        frame = new JFrame("Basic Scene: Maze View (Networking, Camera, Sound, Spotlight)");
        BasicScene basicScene = new BasicScene();
        BranchGroup sceneBG = basicScene.createScene();
        basicScene.setupUniverse(sceneBG);
        frame.getContentPane().add(basicScene);
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}