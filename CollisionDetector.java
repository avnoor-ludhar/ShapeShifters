package ShapeShifters;

import org.jogamp.vecmath.Vector3d;

// Initializes the CollisionDetector class
public class CollisionDetector {
    // Checks if 2 objects are colliding with AABB collision detection
    public static boolean isColliding(double x1, double z1, double half1, double x2, double z2, double half2) {
        // Checks overlaps on both X and Y axis'
        return (x1 - half1 < x2 + half2) &&
                (x1 + half1 > x2 - half2) &&
                (z1 - half1 < z2 + half2) &&
                (z1 + half1 > z2 - half2);
    }

    // Checks if an NPC collides with the player/user
    public static boolean collidesWithUser(double x, double z, double npcHalf, GhostModel user) {
        Vector3d userPos = user.getPosition();
        double ghostHalf = GhostModel.getCharacterHalf();
        return isColliding(x, z, npcHalf, userPos.x, userPos.z, ghostHalf);
    }
}