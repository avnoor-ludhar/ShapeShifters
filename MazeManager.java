package ShapeShifters;

import java.util.ArrayList;
import java.util.Random;

// Handles maze creation and customization
public class MazeManager {
    private final int mazeHeight;
    private final int mazeWidth;
    private ArrayList<ArrayList<Integer>> maze;
    private int[][] movingWalls;

    // Initializes the maze
    // Modifies the maze
    // Sets moving walls within the maze
    public MazeManager(int height, int width) {
        // Use the same dimensions as before
        this.mazeHeight = height;
        this.mazeWidth = width;
        // Get the maze grid from GenerateMaze
        maze = GenerateMaze.getMaze(height, width);
        clearCenter();
        removeRandomWalls();
        designateMovingWalls();
    }

    // Clears a 3x3 region in the center of the maze
        // So the fan in the middle has a guaranteed open space at all times
    private void clearCenter() {
        // Clear a central area of the maze (positions 9 to 11)
        for (int i = 9; i < 12; i++) {
            for (int j = 9; j < 12; j++) {
                maze.get(i).set(j, 0);
            }
        }
    }

    // Randomly removes walls in the maze
    // Adds unpredictability to the generated maze
    private void removeRandomWalls() {
        // Remove walls randomly (20% chance)
        for (int i = 1; i < mazeHeight - 1; i++) {
            for (int j = 1; j < mazeWidth - 1; j++) {
                if (Math.random() < 0.2) {
                    maze.get(i).set(j, 0);
                }
            }
        }
    }

    // Randomly selects 4 wall positions which can later be animated or modified during gameplay
    private void designateMovingWalls() {
        movingWalls = new int[4][2];
        Random rand = new Random();
        int movingWallsIndex = 0;
        while (movingWallsIndex < 4) {
            int i = rand.nextInt(mazeHeight - 2) + 1;
            int j = rand.nextInt(mazeWidth - 2) + 1;
            if (maze.get(i).get(j) == 1 && !alreadyChosen(i, j, movingWallsIndex)) {
                movingWalls[movingWallsIndex][0] = i;
                movingWalls[movingWallsIndex][1] = j;
                movingWallsIndex++;
            }
        }
    }

    // Helper method to avoid selecting duplicate wall positions
    private boolean alreadyChosen(int i, int j, int count) {
        for (int n = 0; n < count; n++) {
            if (movingWalls[n][0] == i && movingWalls[n][1] == j) {
                return true;
            }
        }
        return false;
    }

    // Returns the generated and modified maze grid
    public ArrayList<ArrayList<Integer>> getMaze() {
        return maze;
    }

    // Returns the list of moving wall coordinates
    public int[][] getMovingWalls() {
        return movingWalls;
    }
}