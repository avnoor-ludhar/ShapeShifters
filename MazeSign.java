package ShapeShifters;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.vecmath.*;

/**
 * Creates a sign that always faces the viewer, using OrientedShape3D.
 */
public class MazeSign {
    // Sign configuration
    private static final float SIGN_WIDTH = 0.2f;
    private static final float SIGN_HEIGHT = 0.1f;
    private static final Color TEXT_COLOR = new Color(220, 220, 255);
    private static final Color BACKGROUND_COLOR = new Color(40, 40, 80, 200);
    private static final Font TEXT_FONT = new Font("Arial", Font.BOLD, 24);
    
    // Transform group to position the sign
    private TransformGroup signTG;
    
    /**
     * Creates a new maze sign at the specified position with the given text.
     * 
     * @param position The 3D position for the sign
     * @param text The text to display on the sign
     */
    public MazeSign(Vector3d position, String text) {
        // Create transform group for positioning
        Transform3D transform = new Transform3D();
        transform.setTranslation(position);
        signTG = new TransformGroup(transform);
        
        // Create an OrientedShape3D that will always face the viewer
        createOrientedSign(text);
    }
    
    /**
     * Creates an OrientedShape3D that always faces the viewer.
     * 
     * @param text The text to display on the sign
     */
    private void createOrientedSign(String text) {
        // Create a texture with the text
        BufferedImage textImage = createTextImage(256, 128, text);
        
        try {
            // Create appearance with texture
            Appearance appearance = new Appearance();
            
            // Load texture from BufferedImage
            TextureLoader loader = new TextureLoader(textImage, "RGBA");
            Texture texture = loader.getTexture();
            texture.setMinFilter(Texture.BASE_LEVEL_LINEAR);
            texture.setMagFilter(Texture.BASE_LEVEL_LINEAR);
            appearance.setTexture(texture);
            
            // Enable transparency
            TransparencyAttributes ta = new TransparencyAttributes();
            ta.setTransparencyMode(TransparencyAttributes.BLENDED);
            ta.setTransparency(0.0f);
            appearance.setTransparencyAttributes(ta);
            
            // Enable texture blending
            TextureAttributes texAttr = new TextureAttributes();
            texAttr.setTextureMode(TextureAttributes.MODULATE);
            appearance.setTextureAttributes(texAttr);
            
            // Add material to ensure good visibility
            Material material = new Material();
            material.setLightingEnable(true);
            material.setAmbientColor(new Color3f(1.0f, 1.0f, 1.0f));
            material.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
            material.setEmissiveColor(new Color3f(0.5f, 0.5f, 0.5f)); // Make it glow slightly
            appearance.setMaterial(material);
            
            // Create a quad for the sign
            QuadArray quad = new QuadArray(4, QuadArray.COORDINATES | QuadArray.TEXTURE_COORDINATE_2);
            
            // Set coordinates for the quad (centered at origin)
            quad.setCoordinate(0, new Point3f(-SIGN_WIDTH/2, -SIGN_HEIGHT/2, 0.0f));
            quad.setCoordinate(1, new Point3f(SIGN_WIDTH/2, -SIGN_HEIGHT/2, 0.0f));
            quad.setCoordinate(2, new Point3f(SIGN_WIDTH/2, SIGN_HEIGHT/2, 0.0f));
            quad.setCoordinate(3, new Point3f(-SIGN_WIDTH/2, SIGN_HEIGHT/2, 0.0f));
            
            // Fix texture coordinates to correct upside-down text
            quad.setTextureCoordinate(0, 0, new TexCoord2f(0.0f, 0.0f));
            quad.setTextureCoordinate(0, 1, new TexCoord2f(1.0f, 0.0f));
            quad.setTextureCoordinate(0, 2, new TexCoord2f(1.0f, 1.0f));
            quad.setTextureCoordinate(0, 3, new TexCoord2f(0.0f, 1.0f));
            
            // Create an OrientedShape3D (always faces viewer)
            OrientedShape3D orientedShape = new OrientedShape3D(
                quad,                      // Geometry
                appearance,                // Appearance
                OrientedShape3D.ROTATE_ABOUT_POINT, // Rotation mode
                new Point3f(0.0f, 0.0f, 0.0f)       // Rotation point
            );
            
            // Set alignment mode to ensure sign stays upright
            orientedShape.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_AXIS);
            orientedShape.setAlignmentAxis(0.0f, 1.0f, 0.0f);
            
            // Add the oriented shape to the transform group
            signTG.addChild(orientedShape);
            
        } catch (Exception e) {
            System.err.println("Error creating sign: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a BufferedImage with text on a semi-transparent background.
     * 
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param text Text to display on the sign
     * @return BufferedImage with the text and background
     */
    private BufferedImage createTextImage(int width, int height, String text) {
        // Create a BufferedImage with alpha channel
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Enable anti-aliasing for smooth text
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                           java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                           java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Clear the image with the background color
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        // Add a subtle border
        g2d.setColor(new Color(160, 160, 255));
        g2d.drawRoundRect(3, 3, width-6, height-6, 12, 12);
        
        // Draw the text
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(TEXT_FONT);
        
        // Center the text
        java.awt.FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        int x = (width - textWidth) / 2;
        int y = ((height - textHeight) / 2) + metrics.getAscent();
        
        g2d.drawString(text, x, y);
        
        // Cleanup
        g2d.dispose();
        
        return image;
    }
    
    /**
     * Gets the transform group containing the sign.
     * 
     * @return The sign's transform group
     */
    public TransformGroup getTransformGroup() {
        return signTG;
    }
}