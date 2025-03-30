package ShapeShifters;

import java.awt.Font;
import org.jogamp.java3d.*;
import org.jogamp.vecmath.*;

public class ColorString {
    private String str;
    private Color3f clr;
    private double scl;
    private Point3f pos;
    private TransformGroup tg;

    // constructor
    public ColorString(String str_ltrs, Color3f str_clr, double s, Point3f p) {
        this.str = str_ltrs;
        this.clr = str_clr;
        this.scl = s;
        this.pos = p;

        Transform3D combined = new Transform3D();
        combined.setScale(scl); // apply scaling to the transform

        tg = new TransformGroup(combined);
        tg.addChild(createObject());
    }

    // Creates the 3D text shape node
    private Node createObject() {
        Font font2D = new Font("Arial", Font.BOLD, 1);
        FontExtrusion extrusion = new FontExtrusion();
        Font3D font3D = new Font3D(font2D, extrusion);

        Text3D text3D = new Text3D(font3D, str, pos);
        text3D.setAlignment(Text3D.ALIGN_CENTER);

        Appearance app = createAppearance(clr);
        return new Shape3D(text3D, app);
    }

    // Creates material appearance with color
    private Appearance createAppearance(Color3f color) {
        Appearance app = new Appearance();
        Material mat = new Material();

        mat.setAmbientColor(new Color3f(0.2f, 0.2f, 0.2f));
        mat.setDiffuseColor(color);
        mat.setSpecularColor(new Color3f(0.7f, 0.7f, 0.7f));
        mat.setShininess(64.0f);
        mat.setLightingEnable(true);

        app.setMaterial(mat);
        return app;
    }

    // Returns full TransformGroup with the 3D text
    public TransformGroup getTransformGroup() {
        return tg;
    }
}
