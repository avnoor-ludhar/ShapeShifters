package ShapeShifters;

import org.jogamp.java3d.BranchGroup;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Class that has the main GUI for the game menu
public class GameMenu extends JFrame {
    private JTextField ipAddressField;
    private JTextField usernameField;
    private JButton playButton;
    private JButton exitButton;
    private Font titleFont;
    private Font labelFont;
    private Font buttonFont;
    private Color backgroundColor = new Color(30, 30, 40);
    private Color highlightColor = new Color(75, 105, 255); // blueish for PLAY background
    private Color hoverHighlightColor = highlightColor.brighter(); // hover blue for PLAY
    private Color exitColor = new Color(200, 60, 60); // red for EXIT background
    private Color exitHoverColor = exitColor.brighter(); // hover red for EXIT
    private Color textColor = new Color(230, 230, 250);
    private Point dragStart = null;
    private JLabel imageLabel; // For the game logo/image

    // Constructor to initialize and display the full menu UI
    public GameMenu() {
        // Load custom fonts with larger sizes
        try {
            titleFont = new Font("Arial", Font.BOLD, 36);
            labelFont = new Font("Tahoma", Font.BOLD, 18);
            buttonFont = new Font("Arial", Font.BOLD, 20);
        } catch (Exception e) {
            System.out.println("Error loading fonts: " + e.getMessage());
            titleFont = new Font("SansSerif", Font.BOLD, 36);
            labelFont = new Font("SansSerif", Font.BOLD, 18);
            buttonFont = new Font("SansSerif", Font.BOLD, 20);
        }

        // Set up the JFrame with larger dimensions and no window decorations
        setTitle("Shape Shifters");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0, 0, 700, 600, 25, 25));

        // Main panel with background color and padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(backgroundColor);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(backgroundColor);
        titlePanel.setBorder(new EmptyBorder(10, 0, 20, 0));
        JLabel titleLabel = new JLabel("SHAPE SHIFTERS");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(highlightColor);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitleLabel = new JLabel("Multiplayer Maze Adventure");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 20));
        subtitleLabel.setForeground(textColor);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Image label for the game logo/image
        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(300, 200));
        imageLabel.setMaximumSize(new Dimension(300, 200));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Load the game logo image immediately
        loadGameLogo();
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        titlePanel.add(subtitleLabel);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        titlePanel.add(imageLabel);

        // Form panel for IP and username
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(2, 1, 15, 25));
        formPanel.setBackground(backgroundColor);
        formPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // IP Address panel
        JPanel ipPanel = new JPanel(new BorderLayout(15, 0));
        ipPanel.setBackground(backgroundColor);
        JLabel ipLabel = new JLabel("IP Address:");
        ipLabel.setFont(labelFont);
        ipLabel.setForeground(textColor);
        ipLabel.setPreferredSize(new Dimension(120, 30));
        ipAddressField = createStyledTextField();
        ipPanel.add(ipLabel, BorderLayout.WEST);
        ipPanel.add(ipAddressField, BorderLayout.CENTER);

        // Username panel
        JPanel usernamePanel = new JPanel(new BorderLayout(15, 0));
        usernamePanel.setBackground(backgroundColor);
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(labelFont);
        usernameLabel.setForeground(textColor);
        usernameLabel.setPreferredSize(new Dimension(120, 30));
        usernameField = createStyledTextField();
        usernamePanel.add(usernameLabel, BorderLayout.WEST);
        usernamePanel.add(usernameField, BorderLayout.CENTER);

        formPanel.add(ipPanel);
        formPanel.add(usernamePanel);

        // Button panel for Play and Exit buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        // Set both buttons to the same size
        // (250x60)
        Dimension buttonSize = new Dimension(250, 60);
        playButton = new JButton("PLAY");
        playButton.setFont(buttonFont);
        playButton.setForeground(Color.WHITE);
        playButton.setBackground(highlightColor);
        playButton.setFocusPainted(false);
        playButton.setOpaque(true);
        playButton.setContentAreaFilled(true);
        // Remove border so that it doesn't blend in
        playButton.setBorder(BorderFactory.createEmptyBorder());
        playButton.setPreferredSize(buttonSize);
        exitButton = new JButton("EXIT");
        exitButton.setFont(buttonFont);
        exitButton.setForeground(Color.WHITE);
        exitButton.setBackground(exitColor);
        exitButton.setFocusPainted(false);
        exitButton.setOpaque(true);
        exitButton.setContentAreaFilled(true);
        // Remove border so that it doesn't blend in
        exitButton.setBorder(BorderFactory.createEmptyBorder());
        exitButton.setPreferredSize(buttonSize);
        exitButton.addActionListener(e -> System.exit(0));

        // Add hover effects for PLAY button
        playButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                playButton.setBackground(hoverHighlightColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                playButton.setBackground(highlightColor);
            }
        });

        // Add hover effects for EXIT button.
        exitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                exitButton.setBackground(exitHoverColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                exitButton.setBackground(exitColor);
            }
        });

        buttonPanel.add(playButton);
        buttonPanel.add(exitButton);

        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setOpaque(false);
        // Wrap the original main panel inside our background panel that paints shooting stars
        StarBackgroundPanel backgroundPanel = new StarBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        backgroundPanel.add(mainPanel, BorderLayout.CENTER);
        setContentPane(backgroundPanel);

        // Make the frame draggable
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    Point currentPoint = e.getLocationOnScreen();
                    setLocation(currentPoint.x - dragStart.x, currentPoint.y - dragStart.y);
                }
            }
        });
    }

    // Immediately loads the game logo image from the provided file path
    private void loadGameLogo() {
        try {
            String imagePath = "src/ShapeShifters/Textures/LogoGenerated.png";
            ImageIcon imageIcon = new ImageIcon(imagePath);
            if (imageIcon.getIconWidth() > 0) {
                Image img = imageIcon.getImage();
                Image resizedImg = img.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(resizedImg);
                imageLabel.setIcon(imageIcon);
                imageLabel.setText("");
                imageLabel.setBorder(null);
            } else {
                System.out.println("Menu texture failed to load from: " + imagePath);
            }
        } catch (Exception e) {
            System.out.println("Error loading menu image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Creates a styled JTextField with consistent look
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setForeground(Color.WHITE);
        field.setBackground(new Color(45, 45, 55));
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 75, 85)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        return field;
    }
    // Attempts to start the game session
        // Shows a loading dialog and launches the game scene
    private void startGame() {
        String ipAddress = ipAddressField.getText().trim();
        String username = usernameField.getText().trim();

        // Error handling
        if (ipAddress.isEmpty() || username.isEmpty()) {
            showErrorDialog("Please enter both IP address and username.");
            return;
        }

        JDialog loadingDialog = new JDialog(this, "Loading...", false);
        loadingDialog.setSize(350, 120);
        loadingDialog.setLocationRelativeTo(this);
        JLabel loadingLabel = new JLabel("Connecting to server...", JLabel.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        loadingDialog.add(loadingLabel);

        new Thread(() -> {
            try {
                loadingDialog.setVisible(true);
                dispose();

                BasicScene game = new BasicScene(ipAddress, username);
                BranchGroup sceneBG = game.createScene();
                game.setupUniverse(sceneBG);

                JFrame gameFrame = new JFrame("Shape Shifters - Maze Adventure");
                gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                gameFrame.setSize(800, 800);
                gameFrame.getContentPane().add(game);

                SwingUtilities.invokeLater(() -> loadingDialog.dispose());
                gameFrame.setVisible(true);
            } catch (Exception e) {
                loadingDialog.dispose();
                showErrorDialog("Failed to connect: " + e.getMessage());
            }
        }).start();
    }

    // Displays an error message in a modal dialog
    private void showErrorDialog(String message) {
        JDialog errorDialog = new JDialog(this, "Error", true);
        errorDialog.setSize(400, 180);
        errorDialog.setLocationRelativeTo(this);
        errorDialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(backgroundColor);

        JLabel errorLabel = new JLabel(message, JLabel.CENTER);
        errorLabel.setForeground(Color.WHITE);
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(120, 40));
        okButton.setBackground(highlightColor);
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setOpaque(true);
        okButton.setContentAreaFilled(true);
        // Add a white border to ensure the text stands out.
        okButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        okButton.addActionListener(e -> errorDialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.add(okButton);

        contentPanel.add(errorLabel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        errorDialog.add(contentPanel);
        errorDialog.setVisible(true);
    }

    // Entry point to launch the game menu
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new GameMenu().setVisible(true));
    }
}

// Custom JPanel that animates shooting stars
// Used as the visual background for the GameMenu
class StarBackgroundPanel extends JPanel {
    private List<ShootingStar> stars = new ArrayList<>();
    private Timer timer;
    // Increase the number of stars for a more frequent effect.
    private final int STAR_COUNT = 60;

    // Constructor for stars and starts the animation timer
    public StarBackgroundPanel() {
        setBackground(new Color(30, 30, 40));
        // Initialize stars when the panel is first displayed
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                initializeStars();
            }
        });
        // Timer to update the stars
        timer = new Timer(30, e -> {
            updateStars();
            repaint();
        });
        timer.start();
    }

    // Initializes random shooting stars across the panel
    private void initializeStars() {
        stars.clear();
        int width = getWidth();
        int height = getHeight();
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new ShootingStar(width, height));
        }
    }

    // Updates the position of each star
    private void updateStars() {
        int width = getWidth();
        int height = getHeight();
        for (ShootingStar star : stars) {
            star.update(width, height);
        }
    }

    @Override
    // Paints all the stars
    protected void paintComponent(Graphics g) {
        // Call super.paintComponent to clear the background first.
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (ShootingStar star : stars) {
            star.draw(g2d);
        }
        g2d.dispose();
    }
}

// Class that represents a single animated star
class ShootingStar {
    private int x, y;
    private int dx, dy;
    private int length;
    private Color color;
    private Random rand = new Random();

    // Initializes star with random direction and speed
    public ShootingStar(int panelWidth, int panelHeight) {
        reset(panelWidth, panelHeight);
    }

    // Updates star position and resets if it is off the screen
    public void update(int panelWidth, int panelHeight) {
        x += dx;
        y += dy;
        // When the star moves off-screen, reset it.
        if (x > panelWidth || x < 0 || y > panelHeight) {
            reset(panelWidth, panelHeight);
        }
    }

    // Resets star to a random position above screen with new velocity
    private void reset(int panelWidth, int panelHeight) {
        x = rand.nextInt(panelWidth);
        y = -rand.nextInt(50); // start off-screen at the top
        dx = (rand.nextBoolean() ? 1 : -1) * (3 + rand.nextInt(5));
        dy = 3 + rand.nextInt(5);
        length = 10 + rand.nextInt(20);
        color = Color.WHITE;
    }

    // Draws the star as a slanted line
        // Added effects to get the streaks across the stars
    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.drawLine(x, y, x - dx * length / 10, y - dy * length / 10);
    }
}