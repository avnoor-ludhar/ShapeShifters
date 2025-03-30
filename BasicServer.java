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
        // print local IP for reference
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String serverIP = "172.20.10.3";
            System.out.println("Server IP address: " + serverIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // generate maze and moving wall data
        MazeManager mazeManager = new MazeManager(MAZE_HEIGHT, MAZE_WIDTH);
        maze = mazeManager.getMaze();
        movingWalls = mazeManager.getMovingWalls();

        // find valid positions from open cells
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

        Random rand = new Random();
        // pick treasure position randomly
        Vector3d treasurePos = validPositions.get(rand.nextInt(validPositions.size()));
        treasureMsg = "TREASURE " + treasurePos.x + " " + treasurePos.y + " " + treasurePos.z;
        validPositions.remove(treasurePos);

        // create green NPCs from valid positions
        Appearance npcAppearance = new Appearance();
        npcAppearance.setMaterial(new Material(
                new Color3f(0.0f, 1.0f, 0.0f),
                new Color3f(0.0f, 0.0f, 0.0f),
                new Color3f(0.0f, 1.0f, 0.0f),
                new Color3f(1.0f, 1.0f, 1.0f),
                64.0f));
        int npcCount = 3;
        for (int i = 0; i < npcCount; i++) {
            if (validPositions.isEmpty()) break;
            NPC npc = NPC.generateRandomNPC(validPositions, npcAppearance, 0.005);
            npcs.add(npc);
        }

        // set up user ghost model
        userGhost = new GhostModel(true, new Vector3d(0.0, 0.1, 0.0));

        // npc update loop with collisions
        new Thread(() -> {
            while (true) {
                for (NPC npc : npcs) {
                    // check collision with maze walls
                    npc.update((x, z) -> {
                        double half = 0.03;
                        double side = 2 * half;

                        // create bounding box around NPC at (x, z)
                        Rectangle2D.Double npcRect = new Rectangle2D.Double(x - half, z - half, side, side);

                        for (int i = 0; i < MAZE_HEIGHT; i++) {
                            for (int j = 0; j < MAZE_WIDTH; j++) {
                                if (maze.get(i).get(j) == 1) { // if cell is a wall
                                    double wx = -1 + i * 0.103; // convert grid to world x
                                    double wz = -1 + j * 0.103; // convert grid to world z

                                    double left = wx - 0.055; // wall left edge in world coords
                                    double top = wz + 0.055;  // wall top edge in world coords

                                    // define wall's bounding box in world space
                                    Rectangle2D.Double wallRect = new Rectangle2D.Double(left, top - 0.11, 0.11, 0.11);

                                    // check for collision between NPC and wall
                                    if (npcRect.intersects(wallRect)) return true;
                                }
                            }
                        }
                        return false; // no collision
                    }, userGhost);

                    // check collision with players
                    Vector3d npcPos = npc.getPosition();
                    double npcHalf = NPC.getCharacterHalf();
                    for (Map.Entry<Integer, Vector3d> entry : playerPositions.entrySet()) {
                        Vector3d playerPos = entry.getValue();
                        double playerHalf = GhostModel.getCharacterHalf();

                        if (CollisionDetector.isColliding(npcPos.x, npcPos.z, npcHalf,
                                playerPos.x, playerPos.z, playerHalf)) {

                            // get normalized direction from player to npc
                            Vector3d collisionNormal = new Vector3d(
                                    npcPos.x - playerPos.x,
                                    0,
                                    npcPos.z - playerPos.z
                            );
                            collisionNormal.normalize();

                            double bounceFactor = 1.5; // scale amount of displacement

                            // compute new npc position pushed away from player
                            Vector3d newNPCPos = new Vector3d(
                                    npcPos.x + collisionNormal.x * npc.getStep() * bounceFactor,
                                    0.1,
                                    npcPos.z + collisionNormal.z * npc.getStep() * bounceFactor
                            );

                            // reflect and randomize npc direction slightly
                            Vector3d newDirection = new Vector3d(
                                    -npc.getDirection().x + (Math.random() * 0.2 - 0.1),
                                    0,
                                    -npc.getDirection().z + (Math.random() * 0.2 - 0.1)
                            );
                            newDirection.normalize();

                            npc.setDirection(newDirection); // apply new direction
                            npc.setPosition(newNPCPos);     // apply new position
                        }
                    }
                }

                // check collision between npcs
                for (int i = 0; i < npcs.size(); i++) {
                    for (int j = i + 1; j < npcs.size(); j++) {
                        NPC npc1 = npcs.get(i);
                        NPC npc2 = npcs.get(j);
                        Vector3d pos1 = npc1.getPosition();
                        Vector3d pos2 = npc2.getPosition();

                        // check if npc1 and npc2 are colliding
                        if (CollisionDetector.isColliding(pos1.x, pos1.z, NPC.getCharacterHalf(),
                                pos2.x, pos2.z, NPC.getCharacterHalf())) {

                            // get direction vector from npc1 to npc2
                            Vector3d dir1To2 = new Vector3d();
                            dir1To2.sub(pos2, pos1);
                            dir1To2.normalize();

                            // move npc1 slightly away from npc2
                            Vector3d newPos1 = new Vector3d(
                                    pos1.x - dir1To2.x * npc1.getStep(),
                                    0.1,
                                    pos1.z - dir1To2.z * npc1.getStep()
                            );

                            // move npc2 slightly away from npc1
                            Vector3d newPos2 = new Vector3d(
                                    pos2.x + dir1To2.x * npc2.getStep(),
                                    0.1,
                                    pos2.z + dir1To2.z * npc2.getStep()
                            );

                            // reverse directions of both npcs
                            npc1.setDirection(new Vector3d(-npc1.getDirection().x, 0, -npc1.getDirection().z));
                            npc2.setDirection(new Vector3d(-npc2.getDirection().x, 0, -npc2.getDirection().z));

                            // apply new positions
                            npc1.setPosition(newPos1);
                            npc2.setPosition(newPos2);
                        }
                    }
                }


                broadcastNPCPositions(); // send npc data to clients

                try {
                    Thread.sleep(50); // wait before next update
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("Server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // wait for client
                System.out.println("New client connected: " + clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket, nextPlayerId++);
                clients.add(handler);
                new Thread(handler).start(); // start client thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // send npc positions to all clients
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

    // broadcast a message to all clients
    public static synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // client handler logic
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
                out.println("ID " + playerId); // send player id

                StringBuilder mazeStr = new StringBuilder();
                for (ArrayList<Integer> row : maze) {
                    for (Integer cell : row) {
                        mazeStr.append(cell);
                    }
                }
                out.println(mazeStr.toString()); // send maze

                for (int[] coords : movingWalls) {
                    out.println(coords[0] + " " + coords[1]); // send moving wall
                }

                out.println("NPC_COUNT " + npcs.size());
                for (NPC npc : npcs) {
                    Vector3d pos = npc.getPosition();
                    out.println("NPC_INIT " + pos.x + " " + pos.z + " 0 0");
                }

                out.println(treasureMsg); // send treasure info
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        // run server
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("TREASURE_ACTIVATE")) {
                        broadcast("TREASURE_MORPH", this);
                        continue;
                    }
                    if (line.startsWith("GAME_END")) {
                        broadcast(line, this);
                        continue;
                    }
                    if (line.startsWith("GREEN") || line.startsWith("BLUE")) {
                        broadcast(line, this);
                        continue;
                    }

                    String[] tokens = line.split(" ");
                    if (tokens.length < 4) continue;

                    int id = Integer.parseInt(tokens[0]);
                    double x = Double.parseDouble(tokens[1]);
                    double y = Double.parseDouble(tokens[2]);
                    double z = Double.parseDouble(tokens[3]);
                    playerPositions.put(id, new Vector3d(x, y, z)); // update player position

                    broadcast(line, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                playerPositions.remove(playerId); // remove player on disconnect
                try {
                    socket.close();
                } catch (IOException e) { /* ignore */ }
            }
        }
    }
}