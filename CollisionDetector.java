package ShapeShifters;

import java.util.List;
import org.jogamp.vecmath.Vector3d;


public class CollisionDetector {

    public static boolean isColliding(double x1, double z1, double half1,
                                      double x2, double z2, double half2) {
        // Use precise AABB collision
        return (x1 - half1 < x2 + half2) &&
                (x1 + half1 > x2 - half2) &&
                (z1 - half1 < z2 + half2) &&
                (z1 + half1 > z2 - half2);
    }

    public static boolean collidesWithAnyNPC(double x, double z, double ghostHalf, List<NPC> npcs) {
        for (NPC npc : npcs) {
            Vector3d npcPos = npc.getPosition();
            double npcHalf = NPC.getCharacterHalf();
            if (isColliding(x, z, ghostHalf, npcPos.x, npcPos.z, npcHalf)) {
                return true;
            }
        }
        return false;
    }

    public static boolean collidesWithUser(double x, double z, double npcHalf, GhostModel user) {
        Vector3d userPos = user.getPosition();
        double ghostHalf = GhostModel.getCharacterHalf();
        return isColliding(x, z, npcHalf, userPos.x, userPos.z, ghostHalf);
    }
}
