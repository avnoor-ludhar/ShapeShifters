package ShapeShifters;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Random;
import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.vecmath.*;

public class NPC {
    private TransformGroup npcTG;
    private Vector3d position;
    private Vector3d direction;
    private double step;

    /**
     * Interface for collision detection.
     */
    public interface CollisionChecker {
        boolean collides(double x, double z);
    }

    public NPC(Vector3d pos, Vector3d dir, double step, Appearance appearance) {
        this.position = new Vector3d(pos);
        this.direction = new Vector3d(dir);
        this.step = step;

        Transform3D transform = new Transform3D();
        transform.setTranslation(this.position);
        npcTG = new TransformGroup(transform);
        npcTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Box npcBox = new Box(0.03f, 0.03f, 0.03f, Box.GENERATE_NORMALS, appearance);
        npcTG.addChild(npcBox);
    }

    public Vector3d getDirection() {
        return new Vector3d(direction);
    }

    public Vector3d getPosition() {
        return new Vector3d(position);
    }

    public TransformGroup getTransformGroup() {
        return npcTG;
    }

    /**
     * Update the NPC’s position. Reverse direction if a collision occurs.
     */
    public void update(CollisionChecker checker) {
        double newX = position.x + direction.x * step;
        double newZ = position.z + direction.z * step;

        // Grid-based collision detection
        int gridX = (int) Math.floor((newX + 1) / 0.103);
        int gridZ = (int) Math.floor((newZ + 1) / 0.103);

        // Check both grid-based and rectangle-based collision
        if (checker.collides(newX, newZ)) {
            // If collision detected, randomly change direction
            direction = randomizeDirection();
            return;
        }

        position.x = newX;
        position.z = newZ;

        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(position.x, 0.1, position.z));
        npcTG.setTransform(transform);
    }

    // Helper method to randomize direction when collision occurs
    private Vector3d randomizeDirection() {
        Random rand = new Random();
        int choice = rand.nextInt(4);
        switch (choice) {
            case 0: return new Vector3d(1, 0, 0);
            case 1: return new Vector3d(-1, 0, 0);
            case 2: return new Vector3d(0, 0, 1);
            default: return new Vector3d(0, 0, -1);
        }
    }

    /**
     * Creates an NPC at a random valid position with a random cardinal direction.
     */
    public static NPC generateRandomNPC(List<Vector3d> validPositions, Appearance appearance, double step) {
        if (validPositions.isEmpty()) {
            throw new IllegalArgumentException("No valid positions available");
        }

        int index = (int) (Math.random() * validPositions.size());
        Vector3d pos = validPositions.remove(index);

        Random rand = new Random();
        int choice = rand.nextInt(4);
        Vector3d dir;
        switch (choice) {
            case 0:
                dir = new Vector3d(1, 0, 0);
                break;
            case 1:
                dir = new Vector3d(-1, 0, 0);
                break;
            case 2:
                dir = new Vector3d(0, 0, 1);
                break;
            default:
                dir = new Vector3d(0, 0, -1);
                break;
        }
        return new NPC(pos, dir, step, appearance);
    }
}
