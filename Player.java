import java.util.*;
import java.io.*;

class Color {
    char colorA, colorB;
}

class ActionValuePair {
    int action;
    double value;

    ActionValuePair(double value) {
        this.value = value;
    }
}

class State {
    public static Color[] nextBlocks = new Color[8];

    public char[] myGrid = new char[Player.GRID_SIZE];
    public int[] heights = new int[Player.GRID_WIDTH];
    public char[] opponentsGrid = new char[Player.GRID_SIZE];

    static {
        for (int i = 0; i < 8; i++) {
            nextBlocks[i] = new Color();
        }
    }
}

class Player {

    public static final boolean TESTING = true;

    public static final int GRID_WIDTH = 6;
    public static final int GRID_HEIGHT = 12;
    public static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;

    private State state;

    public Player(Scanner in) {
        state = new State();

        readInput(in);

        if (TESTING) {
        }
    }

    public void readInput(Scanner in) {
        for (int i = 0; i < 8; i++) {
            state.nextBlocks[i].colorA = in.next().charAt(0);
            state.nextBlocks[i].colorB = in.next().charAt(0);
        }

        for (int i = 0; i < 12; i++) {
            String row = in.next();
            for (int j = 0; j < row.length(); j++) {
                state.myGrid[i * GRID_WIDTH + j] = row.charAt(j);
            }
        }

        for (int i = 0; i < 12; i++) {
            String row = in.next();
            for (int j = 0; j < row.length(); j++) {
                state.opponentsGrid[i * GRID_WIDTH + j] = row.charAt(j);
            }
        }
    }

    private State nextState(State oldState, int column, char color) {
        int row = findFreeRow(oldState.myGrid, column);
        if (row < 1) {
            return null;
        }

        State state = placeBlock(oldState, row, column, color);

        TreeSet<Integer> columnsToCheck = new TreeSet<Integer>();
        // check column where block was placed
        columnsToCheck.add(column);
        while (!columnsToCheck.isEmpty()) {
            // get position of block to check
            column = columnsToCheck.pollFirst();
            row = findFreeRow(state.myGrid, column) + 1;
            int lastFreeRow = GRID_HEIGHT - 1;
            // need to check all rows
            while (row <= lastFreeRow) {
                if (!insideGrid(row, column)) {
                    break;
                }

                color = state.myGrid[row * GRID_WIDTH + column];
                if (color == '.') {
                    break;
                }

                // count size of blocks
                HashSet<Integer> idxs = new HashSet<Integer>();
                int size = countBlocks(state.myGrid, row, column, color, idxs);

                if (size >= 4) {
                    // delete blocks
                    for (Integer idx : idxs) {
                        state.myGrid[idx] = '.';
                        state.heights[getColumn(idx)]--;
                    }

                    // obtain columns that needs to be checked
                    HashSet<Integer> affectedColumns = getColumns(idxs);

                    // let blocks fall in columns with deleted blocks
                    lastFreeRow = gravity(state.myGrid, affectedColumns);

                    // check whether another deletion is possible within column
                    columnsToCheck.addAll(affectedColumns);
                } else {
                    break;
                }
                row++;
            }
        }

        return state;
    }

    private int getColumn(int idx) {
        return idx % GRID_WIDTH;
    }

    private HashSet<Integer> getColumns(HashSet<Integer> idxs) {
        HashSet<Integer> columns = new HashSet<Integer>();
        for (Integer idx : idxs) {
            columns.add(getColumn(idx));
        }
        return columns;
    }

    private boolean insideGrid(int row, int column) {
        int index = row * GRID_WIDTH + column;
        return index >= 0 && index < GRID_SIZE;
    }

    private State placeBlock(State state, int row, int column, char color) {
        char[] grid = Arrays.copyOf(state.myGrid, state.myGrid.length);
        grid[row * GRID_WIDTH + column] = color;
        grid[(row - 1) * GRID_WIDTH + column] = color;

        int[] heights = Arrays.copyOf(state.heights, state.heights.length);
        heights[column] += 2;

        State result = new State();
        result.myGrid = grid;
        result.heights = heights;

        return result;
    }

    private int findFreeRow(char[] grid, int column) {
        for (int i = 0; i < GRID_HEIGHT; i++) {
            if (grid[i * GRID_WIDTH + column] != '.') {
                return i - 1; // free row is above first occupied
            }
        }
        return GRID_HEIGHT - 1;
    }

    private int countBlocks(char[] grid, int row, int column, char color, HashSet<Integer> visited) {
        if (row < 0 ||
            column < 0 ||
            row >= GRID_HEIGHT ||
            column >= GRID_WIDTH ||
            grid[row * GRID_WIDTH + column] != color ||
            visited.contains(row * GRID_WIDTH + column)) {
                return 0;
        }

        visited.add(row * GRID_WIDTH + column);
        int left = countBlocks(grid, row, column - 1, color, visited);
        int right = countBlocks(grid, row, column + 1, color, visited);
        int top = countBlocks(grid, row - 1, column, color, visited);
        int bottom = countBlocks(grid, row + 1, column, color, visited);
        return 1 + left + right + top + bottom;
    }

    private int gravity(char[] grid, HashSet<Integer> columns) {
        int lastFreeRow = 0;
        for (Integer column : columns) {
            int row = 0;
            // find first empty
            for (int i = GRID_HEIGHT - 1; i > 0; i--) {
                if (grid[i * GRID_WIDTH + column] == '.') {
                    row = i;
                    lastFreeRow = i;
                    break;
                }
            }
            // find first non-empty above firstEmptyRow
            boolean found = false;
            for (int i = row - 1; i >= 0; i--) {
                if (grid[i * GRID_WIDTH + column] != '.') {
                    found = true;
                    grid[row * GRID_WIDTH + column] = grid[i * GRID_WIDTH + column];
                    grid[i * GRID_WIDTH + column] = '.';
                    row--;
                } else if (found) {
                    break;
                }
            }
        }
        return lastFreeRow;
    }

    private void printGrid(State state) {
        for (int i = 0; i < state.myGrid.length; i++) {
            System.out.print(state.myGrid[i]);
            if ((i + 1) % GRID_WIDTH == 0) {
                System.out.println();
            }
        }
        for (int i = 0; i < state.heights.length; i++) {
            System.out.print(state.heights[i]);
        }
        System.out.println();
    }

    private ActionValuePair DFS(State state, int depth, int maxDepth) {
        if (depth == maxDepth) {
            int maxHeight = 0;
            int totalBlocks = 0;
            for (int i = 0; i < state.heights.length; i++) {
                maxHeight = Math.max(maxHeight, state.heights[i]);
                totalBlocks += state.heights[i];
            }

            return new ActionValuePair(2 * maxHeight + totalBlocks);
        }

        ActionValuePair bestActionValuePair = new ActionValuePair(Double.MAX_VALUE);
        for (int i = 0; i < 6; i++) {
            char color = state.nextBlocks[depth].colorA;
            State nextState = nextState(state, i, color);
            if (nextState == null) {
                continue;
            }
            ActionValuePair child = DFS(nextState, depth + 1, maxDepth);
            child.action = i;
            if (child.value < bestActionValuePair.value) {
                bestActionValuePair = child;
            }
        }
        return bestActionValuePair;
    }

    public void mainLoop(Scanner in) {
        while (true) {
            ActionValuePair best = DFS(state, 0, 3);
            state = nextState(state, best.action, State.nextBlocks[0].colorA);

            System.out.println(best.action);
            if (TESTING) {
                printGrid(state);
            }

            readInput(in);
        }
    }

    public static void main(String args[]) {
        Scanner in = null;
        if (Player.TESTING) {
            try {
                in = new Scanner(new File("map1"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            in = new Scanner(System.in);
        }

        Player P = new Player(in);
        P.mainLoop(in);
    }
}
