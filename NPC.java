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
     * Interface for collision detection. The caller should provide an implementation that
     * checks if a given (x,z) coordinate collides with a wall.
     */
    public interface CollisionChecker {
        boolean collides(double x, double z);
    }

    /**
     * Constructs an NPC given its starting position, direction, movement step, and appearance.
     */
    public NPC(Vector3d pos, Vector3d dir, double step, Appearance appearance) {
        this.position = new Vector3d(pos);
        this.direction = new Vector3d(dir);
        this.step = step;

        // Create the TransformGroup and add a box to represent the NPC.
        Transform3D transform = new Transform3D();
        transform.setTranslation(this.position);
        npcTG = new TransformGroup(transform);
        npcTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Box npcBox = new Box(0.03f, 0.03f, 0.03f, Box.GENERATE_NORMALS, appearance);
        npcTG.addChild(npcBox);
    }

    /**
     * Returns the TransformGroup for this NPC (to be added to the scene graph).
     */
    public TransformGroup getTransformGroup() {
        return npcTG;
    }

    /**
     * Update the NPC’s position. The provided CollisionChecker is used to determine whether
     * moving to the next position would hit a wall. If so, the NPC reverses its direction.
     */
    public void update(CollisionChecker checker) {
        double newX = position.x + direction.x * step;
        double newZ = position.z + direction.z * step;

        // If the new position collides with a wall, reverse the movement direction.
        if (checker.collides(newX, newZ)) {
            direction.scale(-1);
            newX = position.x + direction.x * step;
            newZ = position.z + direction.z * step;
        }

        position.x = newX;
        position.z = newZ;

        // Update the transform to reflect the new position (keeping y constant at 0.1).
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(position.x, 0.1, position.z));
        npcTG.setTransform(transform);
    }

    /**
     * Static factory method that creates an NPC at a random valid position (from the provided list)
     * with a random initial cardinal direction.
     *
     * @param validPositions A list of world positions (Vector3d) that are free (i.e. not occupied by walls).
     *                       The chosen position is removed from the list.
     * @param appearance The Appearance for the NPC.
     * @param step The movement step size.
     * @return An instance of NPC.
     */
    public static NPC generateRandomNPC(List<Vector3d> validPositions, Appearance appearance, double step) {
        if (validPositions.isEmpty()) {
            throw new IllegalArgumentException("No valid positions available");
        }

        int index = (int) (Math.random() * validPositions.size());
        Vector3d pos = validPositions.remove(index);

        // Choose a random cardinal direction.
        Random rand = new Random();
        int choice = rand.nextInt(4);
        Vector3d dir;
        switch (choice) {
            case 0: dir = new Vector3d(1, 0, 0); break;    // right
            case 1: dir = new Vector3d(-1, 0, 0); break;   // left
            case 2: dir = new Vector3d(0, 0, 1); break;    // down
            default: dir = new Vector3d(0, 0, -1); break;  // up
        }
        return new NPC(pos, dir, step, appearance);
    }
}
