package ShapeShifters;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.Material;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Vector3d;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class BasicServer {
    private static final int PORT = 5001;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int nextPlayerId = 1;
    private static List<NPC> npcs = new ArrayList<>();
    private static ArrayList<ArrayList<Integer>> maze;
    private static int[][] movingWalls = new int[4][2];
    private static final int MAZE_HEIGHT = 20;
    private static final int MAZE_WIDTH = 20;
    private static String treasureMsg;
    private static GhostModel userGhost;
    private static Map<Integer, Vector3d> playerPositions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Print the server's IP address (hard-coded for testing)
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String serverIP = "10.72.47.254";
            System.out.println("Server IP address: " + serverIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // Initialize maze and designate moving walls.
        maze = GenerateMaze.getMaze(20, 20);
        // Clear a central area of the maze.
        for (int i = 9; i < 12; i++) {
            for (int j = 9; j < 12; j++) {
                maze.get(i).set(j, 0);
            }
        }
        // Randomly remove 10% of walls.
        for (int i = 1; i < MAZE_HEIGHT - 1; i++) {
            for (int j = 1; j < MAZE_WIDTH - 1; j++) {
                if (Math.random() < 0.2) {
                    maze.get(i).set(j, 0);
                }
            }
        }
        // Designate 4 moving wall positions.
        Random rand = new Random();
        int movingWallsIndex = 0;
        while (movingWallsIndex < 4) {
            int i = rand.nextInt(18) + 1;
            int j = rand.nextInt(18) + 1;
            boolean found = false;
            if (maze.get(i).get(j) == 1) {
                for (int n = 0; n < movingWallsIndex; n++) {
                    if (movingWalls[n][0] == i && movingWalls[n][1] == j) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    movingWalls[movingWallsIndex] = new int[]{i, j};
                    movingWallsIndex++;
                }
            }
        }

        // Generate valid positions for NPCs from open maze cells.
        List<Vector3d> validPositions = new ArrayList<>();
        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (maze.get(i).get(j) == 0) {
                    double x = -1 + i * 0.103;
                    double z = -1 + j * 0.103;
                    validPositions.add(new Vector3d(x, 0.1, z));
                }
            }
        }

        // Generate treasure coordinates from one valid position.
        Vector3d treasurePos = validPositions.get(rand.nextInt(validPositions.size()));
        treasureMsg = "TREASURE " + treasurePos.x + " " + treasurePos.y + " " + treasurePos.z;
        validPositions.remove(treasurePos);

        // Create 3 NPCs with a green appearance.
        Appearance npcAppearance = new Appearance();
        npcAppearance.setMaterial(new Material(
                new Color3f(0.0f, 1.0f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(0.0f, 1.0f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
        int npcCount = 3;
        for (int i = 0; i < npcCount; i++) {
            if (validPositions.isEmpty())
                break;
            NPC npc = NPC.generateRandomNPC(validPositions, npcAppearance, 0.005);
            npcs.add(npc);
        }

        userGhost = new GhostModel(true, new Vector3d(0.0, 0.1, 0.0));

        userGhost = new GhostModel(true, new Vector3d(0.0, 0.1, 0.0));

        // Periodically update NPC positions and broadcast their states.
        new Thread(() -> {
            while (true) {
                // Update each NPC with wall collisions and player collisions
                for (NPC npc : npcs) {
                    // Existing wall collision check
                    npc.update((x, z) -> {
                        double half = 0.03;
                        double side = 2 * half;
                        Rectangle2D.Double npcRect = new Rectangle2D.Double(x - half, z - half, side, side);
                        for (int i = 0; i < MAZE_HEIGHT; i++) {
                            for (int j = 0; j < MAZE_WIDTH; j++) {
                                if (maze.get(i).get(j) == 1) {
                                    double wx = -1 + i * 0.103;
                                    double wz = -1 + j * 0.103;
                                    double left = wx - 0.055;
                                    double top = wz + 0.055;
                                    Rectangle2D.Double wallRect = new Rectangle2D.Double(left, top - 0.11, 0.11, 0.11);
                                    if (npcRect.intersects(wallRect))
                                        return true;
                                }
                            }
                        }
                        return false;
                    }, userGhost);

                    // New: Check collisions with players
                    Vector3d npcPos = npc.getPosition();
                    double npcHalf = NPC.getCharacterHalf();
                    for (Map.Entry<Integer, Vector3d> entry : playerPositions.entrySet()) {
                        Vector3d playerPos = entry.getValue();
                        double playerHalf = GhostModel.getCharacterHalf();

                        if (CollisionDetector.isColliding(npcPos.x, npcPos.z, npcHalf,
                                playerPos.x, playerPos.z, playerHalf)) {

                            // Calculate push back direction (from NPC to player)
                            Vector3d collisionNormal = new Vector3d(
                                    npcPos.x - playerPos.x,
                                    0,
                                    npcPos.z - playerPos.z
                            );
                            collisionNormal.normalize();

                            // Set a stronger bounce factor
                            double bounceFactor = 1.5; // Adjust this value as needed

                            // Calculate new position that pushes the NPC away
                            Vector3d newNPCPos = new Vector3d(
                                    npcPos.x + collisionNormal.x * npc.getStep() * bounceFactor,
                                    0.1,
                                    npcPos.z + collisionNormal.z * npc.getStep() * bounceFactor
                            );

                            // Reverse and slightly randomize direction
                            Vector3d newDirection = new Vector3d(
                                    -npc.getDirection().x + (Math.random() * 0.2 - 0.1),
                                    0,
                                    -npc.getDirection().z + (Math.random() * 0.2 - 0.1)
                            );
                            newDirection.normalize();

                            npc.setDirection(newDirection);
                            npc.setPosition(newNPCPos);

                            // Debug logging (optional)
                            System.out.println("NPC collided with player " + entry.getKey() +
                                    " - New NPC pos: " + newNPCPos + " New direction: " + newDirection);
                        }
                    }
                }

                // Existing NPC-NPC collision check
                for (int i = 0; i < npcs.size(); i++) {
                    for (int j = i + 1; j < npcs.size(); j++) {
                        NPC npc1 = npcs.get(i);
                        NPC npc2 = npcs.get(j);
                        Vector3d pos1 = npc1.getPosition();
                        Vector3d pos2 = npc2.getPosition();

                        if (CollisionDetector.isColliding(pos1.x, pos1.z, NPC.getCharacterHalf(),
                                pos2.x, pos2.z, NPC.getCharacterHalf())) {

                            // Calculate push back direction
                            Vector3d dir1To2 = new Vector3d();
                            dir1To2.sub(pos2, pos1);
                            dir1To2.normalize();

                            // Adjust positions
                            Vector3d newPos1 = new Vector3d(
                                    pos1.x - dir1To2.x * npc1.getStep(),
                                    0.1,
                                    pos1.z - dir1To2.z * npc1.getStep()
                            );

                            Vector3d newPos2 = new Vector3d(
                                    pos2.x + dir1To2.x * npc2.getStep(),
                                    0.1,
                                    pos2.z + dir1To2.z * npc2.getStep()
                            );

                            // Reverse directions
                            npc1.setDirection(new Vector3d(-npc1.getDirection().x, 0, -npc1.getDirection().z));
                            npc2.setDirection(new Vector3d(-npc2.getDirection().x, 0, -npc2.getDirection().z));

                            // Update positions
                            npc1.setPosition(newPos1);
                            npc2.setPosition(newPos2);
                        }
                    }
                }

                broadcastNPCPositions();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("Server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket, nextPlayerId++);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void broadcastNPCPositions() {
        StringBuilder npcState = new StringBuilder("NPC_UPDATE");
        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            Vector3d pos = npc.getPosition();
            Vector3d dir = npc.getDirection();
            
            npcState.append(" ").append(i)
                    .append(" ").append(pos.x)
                    .append(" ").append(0.1)
                    .append(" ").append(pos.z)
                    .append(" ").append(dir.x)
                    .append(" ").append(dir.z);
        }
        for (ClientHandler client : clients) {
            client.sendMessage(npcState.toString());
        }
    }

    // Updated broadcast method: sends the message to all clients.
    public static synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int playerId;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Send the player's ID to the client.
                out.println("ID " + playerId);
                // Send maze data as a concatenated string.
                StringBuilder mazeStr = new StringBuilder();
                for (ArrayList<Integer> row : maze) {
                    for (Integer cell : row) {
                        mazeStr.append(cell);
                    }
                }
                out.println(mazeStr.toString());
                // Send moving wall coordinates.
                for (int[] coords : movingWalls) {
                    out.println(coords[0] + " " + coords[1]);
                }
                // Send NPC initialization information.
                out.println("NPC_COUNT " + npcs.size());
                for (NPC npc : npcs) {
                    Vector3d pos = npc.getPosition();
                    out.println("NPC_INIT " + pos.x + " " + pos.z + " 0 0");
                }
                // Send treasure information.
                out.println(treasureMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    // Handle treasure activation.
                    if (line.startsWith("TREASURE_ACTIVATE")) {
                        broadcast("TREASURE_MORPH", this);
                        continue;
                    }
                    // Handle ghost morph commands
                    if (line.startsWith("GREEN") || line.startsWith("BLUE")) {
                        broadcast(line, this);
                        continue;
                    }
                    // Process regular player position updates.
                    String[] tokens = line.split(" ");
                    if (tokens.length < 4)
                        continue;

                    // Update player position in the map
                    int id = Integer.parseInt(tokens[0]);
                    double x = Double.parseDouble(tokens[1]);
                    double y = Double.parseDouble(tokens[2]);
                    double z = Double.parseDouble(tokens[3]);
                    playerPositions.put(id, new Vector3d(x, y, z));

                    broadcast(line, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                playerPositions.remove(playerId);
                try {
                    socket.close();
                } catch (IOException e) { /* Ignore */ }
            }
        }
    }
}
