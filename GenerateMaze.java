package ShapeShifters;

import java.util.*;

public class GenerateMaze {
    // Grid representing the maze: 0 = passage, 1 = wall
    public static ArrayList<ArrayList<Integer>> grid;
    public static int m; // height of grid (excluding border)
    public static int n; // width of grid (excluding border)

    // possible moves
    public static int[][] moves = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    // Generates and returns the maze with outer walls
    public static ArrayList<ArrayList<Integer>> getMaze(int height, int width) {
        height -= 2; // exclude border
        width -= 2;

        grid = new ArrayList<>();
        m = height;
        n = width;

        // Initialize grid with all walls
        for (int i = 0; i < m; i++) {
            grid.add(new ArrayList<Integer>());
            for (int j = 0; j < n; j++) {
                grid.get(i).add(1);
            }
        }

        dfs(0, 0); // Start DFS from top-left

        // Add top and bottom border rows
        grid.add(new ArrayList<Integer>());
        grid.addFirst(new ArrayList<Integer>());

        // Add left and right wall to each row
        for (int i = 0; i < grid.size(); i++) {
            grid.get(i).add(0, 1);
            grid.get(i).add(1);
        }

        // Fill in top and bottom borders with wall
        for (int i = 0; i < n; i++) {
            grid.get(0).add(1);
            grid.get(grid.size() - 1).add(1);
        }

        return grid;
    }

    // Recursive DFS to carve paths in the maze
    public static void dfs(int i, int j) {
        grid.get(i).set(j, 0); // Mark current cell as path

        for (int[] neighbor : neighbors(i, j)) {
            // Skip if carving here would connect multiple paths
            if (numPathsBordering(neighbor[0], neighbor[1]) > 1) continue;
            dfs(neighbor[0], neighbor[1]);
        }
    }

    // Returns shuffled list of valid neighboring cells
    public static ArrayList<int[]> neighbors(int i, int j) {
        ArrayList<int[]> ans = new ArrayList<>();
        for (int[] move : moves) {
            int newI = i + move[0];
            int newJ = j + move[1];
            if (newI >= 0 && newI < m && newJ >= 0 && newJ < n) {
                ans.add(new int[] { newI, newJ });
            }
        }
        Collections.shuffle(ans); // randomize neighbor order
        return ans;
    }

    // Counts how many neighboring cells are already paths
    public static int numPathsBordering(int i, int j) {
        int ans = 0;
        for (int[] cell : neighbors(i, j)) {
            ans += 1 - grid.get(cell[0]).get(cell[1]);
        }
        return ans;
    }

    // Generates and prints a 20x20 maze
    public static void main(String[] args) {
        var g = GenerateMaze.getMaze(20, 20);
        for (ArrayList<Integer> row : grid) {
            for (Integer x : row) {
                System.out.printf("%d", x);
            }
            System.out.println();
        }
    }
}