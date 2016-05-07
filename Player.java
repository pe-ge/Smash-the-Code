import java.util.*;
import java.io.*;

class Color {
    char colorA, colorB;
}

class ActionValuePair {
    int column, rotation;
    double value;

    ActionValuePair(double value) {
        this.value = value;
    }
}

class State {
    public static final int GRID_WIDTH = 6;
    public static final int GRID_HEIGHT = 12;

    public static Color[] nextBlocks = new Color[8];

    public char[][] myGrid = new char[GRID_HEIGHT][GRID_WIDTH];
    public int[] heights = new int[GRID_WIDTH];
    public int total;

    public char[][] opponentsGrid = new char[GRID_HEIGHT][GRID_WIDTH];

    static {
        for (int i = 0; i < 8; i++) {
            nextBlocks[i] = new Color();
        }
    }
}

class Position {

    public static final Position[][] positions;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (row != position.row) return false;
        return column == position.column;

    }

    @Override
    public int hashCode() {
        int result = row;
        result = 31 * result + column;
        return result;
    }

    static {
        positions = new Position[State.GRID_HEIGHT][State.GRID_WIDTH];
        for (int row = 0; row < positions.length; row++) {
            for (int column = 0; column < positions[row].length; column++) {
                positions[row][column] = new Position(row, column);
            }
        }
    }

    public int row, column;

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

}

class BlockPair {
    Position first;
    Position second;

    public BlockPair(int row1, int column1, int row2, int column2) {
        this.first = new Position(row1, column1);
        this.second = new Position(row2, column2);
    }
}

class Player {

    public static final boolean TESTING = true;


    // maps column number to possible rotations
    public static final HashMap<Integer, ArrayList<Integer>> allActions = new HashMap<Integer, ArrayList<Integer>>();

    static {
        for (int column = 0; column < 6; column++) {
            ArrayList<Integer> rotations = new ArrayList<Integer>();
            allActions.put(column, rotations);
            for (int rotation = 0; rotation < 4; rotation++) {
                if ((column == 0 && rotation == 2) || (column == 5 && rotation == 0)) {
                    continue;
                }
                rotations.add(rotation);
            }
        }
    }

    public State readInput(Scanner in) {
        State state = new State();

        // reading next 8 blocks
        for (int i = 0; i < 8; i++) {
            State.nextBlocks[i].colorA = in.next().charAt(0);
            State.nextBlocks[i].colorB = in.next().charAt(0);
            //System.err.println(State.nextBlocks[i].colorA + " " + State.nextBlocks[i].colorB);
        }

        // reading my grid
        for (int r = 0; r < State.GRID_HEIGHT; r++) {
            String row = in.next();
            for (int c = 0; c < row.length(); c++) {
                char color = row.charAt(c);
                state.myGrid[r][c] = color;

                if (color != '.') {
                    state.total++;
                    if(state.heights[c] == 0) {
                        state.heights[c] = State.GRID_HEIGHT - r;
                    }
                }
            }
            //System.err.println(row);
        }

        // reading opponents grid
        for (int r = 0; r < State.GRID_HEIGHT; r++) {
            String row = in.next();
            for (int c = 0; c < row.length(); c++) {
                state.opponentsGrid[r][c] = row.charAt(c);
            }

            //System.err.println(row);
        }

        return state;
    }
    private BlockPair getBlockPair(State state, int column, int rotation) {
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

        if (row1 < 0 || row2 < 0) return null;

        return new BlockPair(row1, column1, row2, column2);
    }

    private char[][] deepCopy(char[][] array) {
        char[][] deepCopy = new char[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                deepCopy[i][j] = array[i][j];
            }
        }
        return deepCopy;
    }

    private State nextState(State oldState, int column, int rotation, Color blockColor) {
        BlockPair block = getBlockPair(oldState, column, rotation);
        if (block == null) {
            return null;
        }

        State state = new State();
        // place block
        state.myGrid = deepCopy(oldState.myGrid);
        state.myGrid[block.first.row][block.first.column] = blockColor.colorA;
        state.myGrid[block.second.row][block.second.column] = blockColor.colorB;

        state.heights = Arrays.copyOf(oldState.heights, oldState.heights.length);
        state.heights[block.first.column]++;
        state.heights[block.second.column]++;

        state.total = oldState.total + 2;

        HashSet<Position> toCheck = new HashSet<>(); // values are positions
        toCheck.add(block.first);
        toCheck.add(block.second);
        while (!toCheck.isEmpty()) {
            // get position of block to check
            Position beingChecked = toCheck.iterator().next();
            toCheck.remove(beingChecked);

            char color = state.myGrid[beingChecked.row][beingChecked.column];
            if (color == '.' || color == '0') {
                continue;
            }

            // count size of blocks
            HashSet<Position> group = new HashSet<>();
            int size = countBlocks(state.myGrid, beingChecked.row, beingChecked.column, color, group);

            if (size >= 4) {
                // delete blocks
                HashSet<Position> shouldFall = new HashSet<>();
                for (Position position : group) {
                    state.myGrid[position.row][position.column] = '.';
                    state.heights[position.column]--;
                    state.total--;

                    if (insideGrid(position.row - 1, position.column) &&
                        state.myGrid[position.row - 1][position.column] != '.' &&
                        state.myGrid[position.row - 1][position.column] != color) {
                            shouldFall.add(Position.positions[position.row - 1][position.column]);
                    }
                }

                // apply gravity
                HashSet<Position> movedPositions = gravity(state.myGrid, shouldFall);

                // check whether another deletion is possible within column
                toCheck.addAll(movedPositions);
            }
        }

        return state;
    }

    private boolean insideGrid(int row, int column) {
        return row >= 0 && column >= 0 && row < State.GRID_HEIGHT && column < State.GRID_WIDTH;
    }

    private int findFreeRow(char[][] grid, int column) {
        for (int row = 0; row < State.GRID_HEIGHT; row++) {
            if (grid[row][column] != '.') {
                return row - 1; // free row is above first occupied
            }
        }
        return State.GRID_HEIGHT - 1;
    }

    private int countBlocks(char[][] grid, int row, int column, char color, HashSet<Position> visited) {
        if (!insideGrid(row, column) ||
            grid[row][column] != color ||
            visited.contains(Position.positions[row][column])) {
                return 0;
        }

        visited.add(Position.positions[row][column]);
        int left = countBlocks(grid, row, column - 1, color, visited);
        int right = countBlocks(grid, row, column + 1, color, visited);
        int top = countBlocks(grid, row - 1, column, color, visited);
        int bottom = countBlocks(grid, row + 1, column, color, visited);
        return 1 + left + right + top + bottom;
    }

    private HashSet<Position> gravity(char[][] grid, HashSet<Position> positions) {
        HashSet<Position> moved = new HashSet<>();
        for (Position position : positions) {
            int row = position.row;
            int column = position.column;

            int bottomRow = State.GRID_HEIGHT - 1;
            for (int r = row + 1; r < State.GRID_HEIGHT; r++) {
                if (grid[r][column] != '.') {
                    bottomRow = r - 1;
                    break;
                }
            }

            for (int r = bottomRow; r >= 0; r--) {
                if (grid[r][column] == '.' && grid[row][column] != '.') {
                    grid[r][column] = grid[row][column];
                    grid[row][column] = '.';
                    moved.add(Position.positions[r][column]);
                    row--;
                } else {
                    break;
                }
            }
        }

        return moved;
    }

    private void printGrid(State state) {
        for (int row = 0; row < state.myGrid.length; row++) {
            for (int column = 0; column < state.myGrid[row].length; column++) {
                System.out.print(state.myGrid[row][column]);
            }
            System.out.println();
        }
        for (int i = 0; i < state.heights.length; i++) {
            System.out.print(state.heights[i]);
            if (state.heights[i] < 0 || state.heights[i] > State.GRID_HEIGHT) {
                throw new RuntimeException("HEIGHT IS NOT CORRECT");
            }
        }
        System.out.println();
        System.out.println("Total: " + state.total);
        if (state.total < 0 || state.total > State.GRID_HEIGHT * State.GRID_WIDTH) {
            throw new RuntimeException("TOTAL IS NOT CORRECT");
        }
    }

    private ActionValuePair DFS(State state, int depth, int maxDepth) {
        if (depth == maxDepth) {
            int maxHeight = 0;
            for (int i = 0; i < state.heights.length; i++) {
                maxHeight = Math.max(maxHeight, state.heights[i]);
            }

            if (TESTING) printGrid(state);
            return new ActionValuePair(2 * maxHeight + state.total);
        }

        ActionValuePair bestActionValuePair = new ActionValuePair(Double.MAX_VALUE);
        for (Map.Entry<Integer, ArrayList<Integer>> actions : allActions.entrySet()) {
            int column = actions.getKey();
            ArrayList<Integer> rotations = actions.getValue();
            for (Integer rotation : rotations) {
                Color color = State.nextBlocks[depth];
                State nextState = nextState(state, column, rotation, color);
                if (nextState == null) {
                    continue;
                }
                ActionValuePair child = DFS(nextState, depth + 1, maxDepth);
                child.column = column;
                child.rotation = rotation;
                if (child.value < bestActionValuePair.value) {
                    bestActionValuePair = child;
                }
            }
        }
        return bestActionValuePair;
    }

    public void mainLoop(Scanner in) {
        while (true) {
            State state = readInput(in);
            ActionValuePair bestAction = DFS(state, 0, 1);
            System.out.println(bestAction.column + " " + bestAction.rotation);
        }
    }

    public void testing() {
        Scanner in = new Scanner(System.in);
        State s = new State();
        for (int row = 0; row < s.myGrid.length; row++) {
            for (int column = 0; column < s.myGrid[row].length; column++) {
                s.myGrid[row][column] = '.';
            }
        }

        while (true) {
            Color nextBlock = new Color();
            nextBlock.colorA = in.next().charAt(0);
            nextBlock.colorB = in.next().charAt(0);
            int column = in.nextInt();
            int rotation = in.nextInt();

            s = nextState(s, column, rotation, nextBlock);

            printGrid(s);

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

        Player P = new Player();

        P.testing();
        P.mainLoop(in);
    }
}
