package ShapeShifters;

import java.util.*;


public class GenerateMaze {
    //given height & width of maze, returns an m x n grid
    //containing 0 and 1 (0 for no wall, 1 for wall)
    public static ArrayList<ArrayList<Integer>> grid;
    public static int m;
    public static int n;
    public static int[][] moves = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    public static ArrayList<ArrayList<Integer>> getMaze(int height, int width) {
        //the actual height will be 2 larger than the height of the generated maze, same for width
        height -= 2;
        width -= 2;
        grid = new ArrayList<ArrayList<Integer>>();
        m = height;
        n = width;
        for (int i = 0; i < m; i++) {
            grid.add(new ArrayList<Integer>());
            for (int j = 0; j < n; j++) {
                grid.get(i).add(1); //everything's a wall at first and we then "carve out" paths
            }
        }
        dfs(0, 0);
        grid.add(new ArrayList<Integer>());
        grid.addFirst(new ArrayList<Integer>());
        for (int i = 0; i < grid.size(); i++) {
            grid.get(i).addFirst(1);
            grid.get(i).add(1);
        }
        for (int i = 0; i < n; i++) {
            grid.get(0).add(1);
            grid.get(grid.size()-1).add(1);
        }
        return grid;
    }

    public static void dfs(int i, int j) {
        grid.get(i).set(j, 0);
        for (int[] neighbor: neighbors(i, j)) {
            if (numPathsBordering(neighbor[0], neighbor[1]) > 1) {
                continue;
            }
            dfs(neighbor[0], neighbor[1]);
        }


    }
    //So, how it'll work:
    //Find all neighbors of the current square
    //If more than 1 of them already contain a path, don't do anything
    //Otherwise, check which of those neighbors don't have more than 1 connecting path
    //And out of those, randomly choose one, knock down its wall, and explore

    public static ArrayList<int[]> neighbors(int i, int j) {
        ArrayList<int[]> ans = new ArrayList<int[]>();
        for (int[] move: moves) {
            int newI = i + move[0];
            int newJ = j + move[1];
            if (newI >= 0 && newI < m && newJ >= 0 && newJ < n) {
                ans.add(new int[]{newI, newJ});
            }
        }
        Collections.shuffle(ans);
        return ans;
    }

    public static int numPathsBordering(int i, int j) {
        int ans = 0;
        for (int[] cell: neighbors(i, j)) {
            ans += 1-grid.get(cell[0]).get(cell[1]);
        }
        return ans;
    }

    public static void main(String[] args) {
        //just for testing
        var g = GenerateMaze.getMaze(20, 20);
        for (ArrayList<Integer> row: grid) {
            for (Integer x: row) {
                System.out.printf("%d", x);
            }
            System.out.println();
        }
    }
}