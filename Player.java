import java.time.format.TextStyle;
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

class Block {
    int positionA;
    int positionB;

    public Block(int row1, int column1, int row2, int column2) {
        this.positionA = row1 * Player.GRID_WIDTH + column1;
        this.positionB = row2 * Player.GRID_WIDTH + column2;
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
            state = nextState(state, 0, 0, State.nextBlocks[0]);
            printGrid(state);
            state = nextState(state, 2, 0, State.nextBlocks[1]);
            printGrid(state);
            state = nextState(state, 1, 3, State.nextBlocks[2]);
            printGrid(state);
            state = nextState(state, 2, 3, State.nextBlocks[3]);
            printGrid(state);
            state = nextState(state, 1, 3, State.nextBlocks[4]);
            printGrid(state);
            state = nextState(state, 3, 3, State.nextBlocks[0]);
            printGrid(state);
            state = nextState(state, 2, 3, State.nextBlocks[5]);
            printGrid(state);
        }
    }

    public void readInput(Scanner in) {
        for (int i = 0; i < 8; i++) {
            State.nextBlocks[i].colorA = in.next().charAt(0);
            State.nextBlocks[i].colorB = in.next().charAt(0);
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

    private Block getBlock(State state, int column, int rotation) {
        int row1, column1;
        int row2, column2;
        column1 = column;
        switch (rotation) {
            case 0:
                row1 = findFreeRow(state.myGrid, column1);
                column2 = column1 + 1;
                row2 = findFreeRow(state.myGrid, column2);
                break;
            case 1:
                row1 = findFreeRow(state.myGrid, column1);
                column2 = column1;
                row2 = row1 - 1;
                break;
            case 2:
                row1 = findFreeRow(state.myGrid, column1);
                column2 = column1 - 1;
                if (column2 < 0) return null;
                row2 = findFreeRow(state.myGrid, column2);
                break;
            case 3:
                column2 = column1;
                row2 = findFreeRow(state.myGrid, column2);
                row1 = row2 - 1;
                break;
            default:
                return null;
        }

        if (column2 < 0 || column2 > 5 || row1 < 0 || row2 < 0) {
            return null;
        }

        return new Block(row1, column1, row2, column2);
    }

    public int getRow(int position) {
        return position / GRID_WIDTH;
    }

    public int getColumn(int position) {
        return position % GRID_WIDTH;
    }

    private State nextState(State oldState, int column, int rotation, Color blockColor) {
        Block block = getBlock(oldState, column, rotation);
        if (block == null) {
            return null;
        }

        State state = new State();
        // place block
        state.myGrid = Arrays.copyOf(oldState.myGrid, oldState.myGrid.length);
        state.myGrid[block.positionA] = blockColor.colorA;
        state.myGrid[block.positionB] = blockColor.colorB;

        state.heights = Arrays.copyOf(oldState.heights, oldState.heights.length);
        state.heights[getColumn(block.positionA)]++;
        state.heights[getColumn(block.positionB)]++;
        printGrid(state);

        TreeSet<Integer> toCheck = new TreeSet<Integer>(); // values are positions
        toCheck.add(block.positionA);
        toCheck.add(block.positionB);
        while (!toCheck.isEmpty()) {
            // get position of block to check
            int beingChecked = toCheck.pollFirst();
            int row = getRow(beingChecked);
            column = getColumn(beingChecked);
            if (!insideGrid(row, column)) {
                throw new RuntimeException("SHOULD NOT HAPPEN");
            }

            char color = state.myGrid[beingChecked];
            if (color == '.') {
                continue;
            }

            // count size of blocks
            HashSet<Integer> group = new HashSet<Integer>();
            int size = countBlocks(state.myGrid, beingChecked, color, group);

            if (size >= 4) {
                // delete blocks
                for (Integer position : group) {
                    state.myGrid[position] = '.';
                    state.heights[getColumn(position)]--;
                }

                // obtain positions that needs to be checked (column -> row)
                printGrid(state);
                HashMap<Integer, Integer> suspiciousPositions = findSuspicious(group);

                // apply gravity
                HashSet<Integer> movedPositions = gravity(state.myGrid, suspiciousPositions);

                // check whether another deletion is possible within column
                toCheck.addAll(movedPositions);
            }
        }

        return state;
    }

    private HashMap<Integer, Integer> findSuspicious(HashSet<Integer> block) {
        // column -> row
        HashMap<Integer, Integer> suspicious = new HashMap<Integer, Integer>();
        for (Integer position : block) {
            int row = getRow(position);
            int column = getColumn(position);
            if (suspicious.containsKey(column)) {
                if (suspicious.get(column) < row) {
                    suspicious.put(column, row);
                }

            } else {
                suspicious.put(column, row);
            }
        }
        return suspicious;
    }

    private boolean insideGrid(int row, int column) {
        return row >= 0 && column >= 0 && row < GRID_HEIGHT && column < GRID_WIDTH;
    }

    private int findFreeRow(char[] grid, int column) {
        for (int i = 0; i < GRID_HEIGHT; i++) {
            if (grid[i * GRID_WIDTH + column] != '.') {
                return i - 1; // free row is above first occupied
            }
        }
        return GRID_HEIGHT - 1;
    }

    private int countBlocks(char[] grid, int position, char color, HashSet<Integer> visited) {
        int row = getRow(position);
        int column = getColumn(position);
        if (!insideGrid(row, column) ||
            grid[row * GRID_WIDTH + column] != color ||
            visited.contains(row * GRID_WIDTH + column)) {
                return 0;
        }

        visited.add(row * GRID_WIDTH + column);
        int left = countBlocks(grid, row * GRID_WIDTH + column - 1, color, visited);
        int right = countBlocks(grid, row * GRID_WIDTH + column  + 1, color, visited);
        int top = countBlocks(grid, (row - 1) * GRID_WIDTH + column, color, visited);
        int bottom = countBlocks(grid, (row + 1) * GRID_WIDTH + column, color, visited);
        return 1 + left + right + top + bottom;
    }

    private HashSet<Integer> gravity(char[] grid, HashMap<Integer, Integer> positions) {
        HashSet<Integer> moved = new HashSet<Integer>();
        for (Map.Entry<Integer, Integer> position : positions.entrySet()) {
            int row = position.getValue();
            int column = position.getKey();

            boolean found = false;
            for (int i = row - 1; i >= 0; i--) {
                if (grid[i * GRID_WIDTH + column] != '.') {
                    found = true;
                    grid[row * GRID_WIDTH + column] = grid[i * GRID_WIDTH + column];
                    grid[i * GRID_WIDTH + column] = '.';
                    moved.add(row * GRID_WIDTH + column);
                    row--;
                } else if (found) {
                    break;

                }
            }
        }

        return moved;
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
            char color = State.nextBlocks[depth].colorA;
            /*State nextState = nextState(state, i, color);
            if (nextState == null) {
                continue;
            }
            ActionValuePair child = DFS(nextState, depth + 1, maxDepth);
            child.action = i;
            if (child.value < bestActionValuePair.value) {
                bestActionValuePair = child;
            }*/
        }
        return bestActionValuePair;
    }

    public void mainLoop(Scanner in) {
        while (true) {
            ActionValuePair best = DFS(state, 0, 3);
            //state = nextState(state, best.action, State.nextBlocks[0].colorA);

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
        //P.mainLoop(in);
    }
}
