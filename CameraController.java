package ShapeShifters;

import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.java3d.Transform3D;

// camera controller to track target pos

public class CameraController {
    private TransformGroup cameraTG;

    public CameraController(TransformGroup cameraTG) {
        this.cameraTG = cameraTG;
    }

    public void updateCameraPosition(Vector3d targetPos) {
        // Example: "behind and above" offset
        double offsetY = 2.0;
        double offsetZ = 2.5;

        Point3d eye = new Point3d(
                targetPos.x,
                offsetY,
                targetPos.z + offsetZ
        );
        Point3d center = new Point3d(
                targetPos.x,
                0.0,
                targetPos.z
        );
        Vector3d up = new Vector3d(0.0, 1.0, 0.0);

        Transform3D viewTransform = new Transform3D();
        viewTransform.lookAt(eye, center, up);
        viewTransform.invert();

        cameraTG.setTransform(viewTransform);
    }
}
