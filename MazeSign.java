package ShapeShifters;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.vecmath.*;

// sign model that always faces viewer
public class MazeSign {

    // constants
    private static final float SIGN_WIDTH = 0.2f;
    private static final float SIGN_HEIGHT = 0.1f;
    private static final Color TEXT_COLOR = new Color(220, 220, 255);
    private static final Color BACKGROUND_COLOR = new Color(40, 40, 80, 200);
    private static final Font TEXT_FONT = new Font("Arial", Font.BOLD, 24);

    // sign transform group
    private TransformGroup signTG;

    // setup sign with position and text
    public MazeSign(Vector3d position, String text) {
        Transform3D transform = new Transform3D();
        transform.setTranslation(position);
        signTG = new TransformGroup(transform);
        createOrientedSign(text);
    }

    // creates oriented shape that faces viewer
    private void createOrientedSign(String text) {
        // generate image with text and background
        BufferedImage textImage = createTextImage(256, 128, text);

        try {
            // set up appearance and texture from image
            Appearance appearance = new Appearance();
            TextureLoader loader = new TextureLoader(textImage, "RGBA");
            Texture texture = loader.getTexture();
            texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
            texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
            appearance.setTexture(texture);

            // enable transparency for background
            TransparencyAttributes ta = new TransparencyAttributes();
            ta.setTransparencyMode(TransparencyAttributes.BLENDED);
            ta.setTransparency(0.0f);
            appearance.setTransparencyAttributes(ta);

            // modulate texture with lighting
            TextureAttributes texAttr = new TextureAttributes();
            texAttr.setTextureMode(TextureAttributes.MODULATE);
            appearance.setTextureAttributes(texAttr);

            // add lighting material for visibility
            Material material = new Material();
            material.setLightingEnable(true);
            material.setAmbientColor(new Color3f(1.0f, 1.0f, 1.0f));
            material.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
            material.setEmissiveColor(new Color3f(0.5f, 0.5f, 0.5f));
            appearance.setMaterial(material);

            // create quad geometry for sign surface
            QuadArray quad = new QuadArray(4, QuadArray.COORDINATES | QuadArray.TEXTURE_COORDINATE_2);
            quad.setCoordinate(0, new Point3f(-SIGN_WIDTH/2, -SIGN_HEIGHT/2, 0.0f));
            quad.setCoordinate(1, new Point3f(SIGN_WIDTH/2, -SIGN_HEIGHT/2, 0.0f));
            quad.setCoordinate(2, new Point3f(SIGN_WIDTH/2, SIGN_HEIGHT/2, 0.0f));
            quad.setCoordinate(3, new Point3f(-SIGN_WIDTH/2, SIGN_HEIGHT/2, 0.0f));

            // set texture coordinates to map image to quad
            quad.setTextureCoordinate(0, 0, new TexCoord2f(0.0f, 0.0f));
            quad.setTextureCoordinate(0, 1, new TexCoord2f(1.0f, 0.0f));
            quad.setTextureCoordinate(0, 2, new TexCoord2f(1.0f, 1.0f));
            quad.setTextureCoordinate(0, 3, new TexCoord2f(0.0f, 1.0f));

            // create oriented shape that rotates to face camera
            OrientedShape3D orientedShape = new OrientedShape3D(
                    quad,
                    appearance,
                    OrientedShape3D.ROTATE_ABOUT_POINT,
                    new Point3f(0.0f, 0.0f, 0.0f)
            );

            // align rotation around vertical axis to stay upright
            orientedShape.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_AXIS);
            orientedShape.setAlignmentAxis(0.0f, 1.0f, 0.0f);

            // add oriented shape to scene graph
            signTG.addChild(orientedShape);

        } catch (Exception e) {
            System.err.println("Error creating sign: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // creates image with styled text
    private BufferedImage createTextImage(int width, int height, String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRoundRect(0, 0, width, height, 15, 15);

        g2d.setColor(new Color(160, 160, 255));
        g2d.drawRoundRect(3, 3, width-6, height-6, 12, 12);

        g2d.setColor(TEXT_COLOR);
        g2d.setFont(TEXT_FONT);

        java.awt.FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        int x = (width - textWidth) / 2;
        int y = ((height - textHeight) / 2) + metrics.getAscent();

        g2d.drawString(text, x, y);
        g2d.dispose();

        return image;
    }

    // returns sign transform group
    public TransformGroup getTransformGroup() {
        return signTG;
    }
}
