import java.util.*;
import java.io.*;

class Color {
    char colorA, colorB;
}

class ActionValuePair {
    int column, rotation, value;

    ActionValuePair(int value) {
        this.value = value;
    }
}

class State {
    public static final int GRID_WIDTH = 6;
    public static final int GRID_HEIGHT = 12;

    public static Color[] nextBlocks = new Color[8];

    public char[][] grid = new char[GRID_HEIGHT][GRID_WIDTH];
    public int[] heights = new int[GRID_WIDTH];
    public int total = 0;

    // calculating score points
    public int clearedBlocks = 0;
    public int chainPower = 0;

    static {
        for (int i = 0; i < 8; i++) {
            nextBlocks[i] = new Color();
        }
    }
}

class StatePair {
    public State me;
    public State opponent;

    public StatePair(State me, State opponent) {
        this.me = me;
        this.opponent = opponent;
    }
}

class Block {

    public static final Block[][] blocks;
    public int row, column;

    static {
        blocks = new Block[State.GRID_HEIGHT][State.GRID_WIDTH];
        for (int row = 0; row < blocks.length; row++) {
            for (int column = 0; column < blocks[row].length; column++) {
                blocks[row][column] = new Block(row, column);
            }
        }
    }

    public Block(int row, int column) {
        this.row = row;
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        if (row != block.row) return false;
        return column == block.column;

    }

    @Override
    public int hashCode() {
        int result = row;
        result = 31 * result + column;
        return result;
    }
}

class BlockPair {
    public Block first;
    public Block second;

    public BlockPair(int row1, int column1, int row2, int column2) {
        this.first = new Block(row1, column1);
        this.second = new Block(row2, column2);
    }
}

class Player {

    private static final boolean TESTING = false;

    // maps column number to possible rotations
    private static final HashMap<Integer, ArrayList<Integer>> allActions = new HashMap<Integer, ArrayList<Integer>>();

    // init possible rotations for every column
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

    public StatePair readInput(Scanner in) {
        // reading colors of blocks
        for (int i = 0; i < 8; i++) {
            State.nextBlocks[i].colorA = in.next().charAt(0);
            State.nextBlocks[i].colorB = in.next().charAt(0);
            //System.err.println(State.nextBlocks[i].colorA + " " + State.nextBlocks[i].colorB);
        }
        State me = readState(in);
        State opponent = readState(in);

        return new StatePair(me, opponent);
    }

    private State readState(Scanner in) {
        State state = new State();
        for (int r = 0; r < State.GRID_HEIGHT; r++) {
            String row = in.next();
            for (int c = 0; c < row.length(); c++) {
                char color = row.charAt(c);
                state.grid[r][c] = color;

                if (color != '.') {
                    state.total++;
                    if(state.heights[c] == 0) {
                        state.heights[c] = State.GRID_HEIGHT - r;
                    }
                }
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
                row1 = findFreeRow(state.grid, column1);
                column2 = column1 + 1;
                row2 = findFreeRow(state.grid, column2);
                break;
            case 1:
                row1 = findFreeRow(state.grid, column1);
                column2 = column1;
                row2 = row1 - 1;
                break;
            case 2:
                row1 = findFreeRow(state.grid, column1);
                column2 = column1 - 1;
                row2 = findFreeRow(state.grid, column2);
                break;
            case 3:
                column2 = column1;
                row2 = findFreeRow(state.grid, column2);
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
        BlockPair blockPair = getBlockPair(oldState, column, rotation);
        if (blockPair == null) {
            return null;
        }

        State state = new State();
        // place block
        state.grid = deepCopy(oldState.grid);
        state.grid[blockPair.first.row][blockPair.first.column] = blockColor.colorA;
        state.grid[blockPair.second.row][blockPair.second.column] = blockColor.colorB;

        // update heights
        state.heights = Arrays.copyOf(oldState.heights, oldState.heights.length);
        state.heights[blockPair.first.column]++;
        state.heights[blockPair.second.column]++;

        // update total count
        state.total = oldState.total + 2;

        HashSet<Block> toCheck = new HashSet<>();
        toCheck.add(blockPair.first);
        toCheck.add(blockPair.second);
        while (!toCheck.isEmpty()) {
            // get position of block to check
            Block beingChecked = toCheck.iterator().next();
            toCheck.remove(beingChecked);

            char color = state.grid[beingChecked.row][beingChecked.column];
            if (color == '.' || color == '0') {
                continue;
            }

            // count group size
            HashSet<Block> group = new HashSet<>();
            int size = countBlocks(state.grid, beingChecked.row, beingChecked.column, color, group);

            if (size >= 4) {
                /* =================== SCORE POINTS =================== */
                if (!beingChecked.equals(blockPair.first) && !beingChecked.equals(blockPair.second)) {
                    state.chainPower++;
                }
                /* =================== SCORE POINTS =================== */

                // delete blocks
                HashSet<Block> blocksToFall = new HashSet<>();
                for (Block blockToDelete : group) {
                    state.grid[blockToDelete.row][blockToDelete.column] = '.';
                    state.heights[blockToDelete.column]--;
                    state.total--;
                    state.clearedBlocks++;

                    // check whether '0' blocks are around
                    // above
                    if (haveColor(state.grid, blockToDelete.row - 1, blockToDelete.column, '0')) {
                        state.grid[blockToDelete.row - 1][blockToDelete.column] = '.';
                        if (blockAboveShouldFall(state.grid, blockToDelete.row - 1, blockToDelete.column, (char)0)) {
                            blocksToFall.add(Block.blocks[blockToDelete.row - 2][blockToDelete.column]);
                        }
                    }
                    // below
                    if (haveColor(state.grid, blockToDelete.row + 1, blockToDelete.column, '0')) {
                        state.grid[blockToDelete.row + 1][blockToDelete.column] = '.';
                    }
                    // to the left
                    if (haveColor(state.grid, blockToDelete.row, blockToDelete.column - 1, '0')) {
                        state.grid[blockToDelete.row][blockToDelete.column - 1] = '.';
                        if (blockAboveShouldFall(state.grid, blockToDelete.row, blockToDelete.column - 1, (char)0)) {
                            blocksToFall.add(Block.blocks[blockToDelete.row - 1][blockToDelete.column - 1]);
                        }
                    }
                    // to the right
                    if (haveColor(state.grid, blockToDelete.row, blockToDelete.column + 1, '0')) {
                        state.grid[blockToDelete.row][blockToDelete.column + 1] = '.';
                        if (blockAboveShouldFall(state.grid, blockToDelete.row, blockToDelete.column + 1, (char)0)) {
                            blocksToFall.add(Block.blocks[blockToDelete.row - 1][blockToDelete.column + 1]);
                        }
                    }

                    // check whether block above should fall
                    if (blockAboveShouldFall(state.grid, blockToDelete, color)) {
                        blocksToFall.add(Block.blocks[blockToDelete.row - 1][blockToDelete.column]);
                    }
                }

                // apply gravity
                HashSet<Block> fallenBlocks = gravity(state.grid, blocksToFall);

                // check whether another deletion is possible
                toCheck.addAll(fallenBlocks);
            }
        }

        return state;
    }

    private boolean blockAboveShouldFall(char[][] grid, int row, int column, char color) {
        return blockAboveShouldFall(grid, Block.blocks[row][column], color);
    }

    private boolean blockAboveShouldFall(char[][] grid, Block block, char color) {
        return insideGrid(block.row - 1, block.column) &&
                grid[block.row - 1][block.column] != '.' &&
                grid[block.row - 1][block.column] != color;
    }

    private int findFreeRow(char[][] grid, int column) {
        for (int row = 0; row < State.GRID_HEIGHT; row++) {
            if (grid[row][column] != '.') {
                return row - 1; // free row is above first occupied
            }
        }
        return State.GRID_HEIGHT - 1;
    }

    private boolean insideGrid(int row, int column) {
        return row >= 0 && column >= 0 && row < State.GRID_HEIGHT && column < State.GRID_WIDTH;
    }

    private boolean haveColor(char[][] grid, int row, int column, char color) {
        return insideGrid(row, column) && grid[row][column] == color;
    }

    private int countBlocks(char[][] grid, int row, int column, char color, HashSet<Block> visited) {
        if (!haveColor(grid, row, column, color) ||
                visited.contains(Block.blocks[row][column])) {
            return 0;
        }

        visited.add(Block.blocks[row][column]);
        int left = countBlocks(grid, row, column - 1, color, visited);
        int right = countBlocks(grid, row, column + 1, color, visited);
        int top = countBlocks(grid, row - 1, column, color, visited);
        int bottom = countBlocks(grid, row + 1, column, color, visited);
        return 1 + left + right + top + bottom;
    }

    private int countGrid(State state) {
        HashSet<Block> visited = new HashSet<>();
        int total = 0;
        for (int column = 0; column < State.GRID_WIDTH; column++) {
            int row = State.GRID_HEIGHT - state.heights[column];
            if (row == State.GRID_HEIGHT || row < 0 || state.grid[row][column] == '0' || state.grid[row][column] == '.') continue;
            total += Math.pow(10, countBlocks(state.grid, row, column, state.grid[row][column], visited));
        }
        return total;
    }

    private HashSet<Block> gravity(char[][] grid, HashSet<Block> blocksToFall) {
        HashSet<Block> moved = new HashSet<>();
        for (Block block : blocksToFall) {
            int row = block.row;
            int column = block.column;

            int bottomRow = State.GRID_HEIGHT - 1;
            for (int r = row + 1; r < State.GRID_HEIGHT; r++) {
                if (grid[r][column] != '.') {
                    bottomRow = r - 1;
                    break;
                }
            }

            for (int r = bottomRow; r >= 0; r--) {
                if (grid[r][column] == '.' && row >= 0 && grid[row][column] != '.') {
                    grid[r][column] = grid[row][column];
                    grid[row][column] = '.';
                    moved.add(Block.blocks[r][column]);
                    row--;
                } else {
                    break;
                }
            }
        }

        return moved;
    }

    private void printGrid(State state) {
        for (int row = 0; row < state.grid.length; row++) {
            for (int column = 0; column < state.grid[row].length; column++) {
                System.out.print(state.grid[row][column]);
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

            int total = 0;
            if (total < 40) {
                total = countGrid(state);

            } else {
                //state.total = 2 * state.total;
                //state.deletions = 2 * state.deletions;
                maxHeight = 0;
            }

            if (TESTING) printGrid(state);
            return new ActionValuePair(-2 * maxHeight - state.total + total);
            //return new ActionValuePair(total);
        }

        ActionValuePair bestActionValuePair = new ActionValuePair(Integer.MIN_VALUE);
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
                if (child.value > bestActionValuePair.value) {
                    bestActionValuePair = child;
                }
            }
        }
        return bestActionValuePair;
    }

    private ActionValuePair opponentDFS(State state, int depth, int maxDepth) {
        if (depth == maxDepth) {
            return new ActionValuePair((int)Math.pow(10, state.chainPower) + state.clearedBlocks);
        }

        ActionValuePair bestActionValuePair = new ActionValuePair(Integer.MIN_VALUE);
        for (Map.Entry<Integer, ArrayList<Integer>> actions : allActions.entrySet()) {
            int column = actions.getKey();
            ArrayList<Integer> rotations = actions.getValue();
            Color color = State.nextBlocks[depth];
            for (Integer rotation : rotations) {
                State nextState = nextState(state, column, rotation, color);
                if (nextState == null) {
                    continue;
                }
                ActionValuePair child = opponentDFS(nextState, depth + 1, maxDepth);
                child.column = column;
                child.rotation = rotation;
                if (child.value > bestActionValuePair.value) {
                    bestActionValuePair = child;
                }
            }
        }
        return bestActionValuePair;
    }

    public void mainLoop(Scanner in) {
        // ak opDFS maxDepth=1|2 vrati value > 10, robim DFS maxDepth=1 a idem tiez palit
        // inac ro
        while (true) {
            StatePair statePair = readInput(in);
            ActionValuePair bestAction = DFS(statePair.me, 0, 2);
            State state = nextState(statePair.me, bestAction.column, bestAction.rotation, State.nextBlocks[0]);
            System.out.println(bestAction.column + " " + bestAction.rotation);
        }
    }

    public void testing() {
        Scanner in = new Scanner(System.in);
        State s = new State();
        for (int row = 0; row < s.grid.length; row++) {
            for (int column = 0; column < s.grid[row].length; column++) {
                s.grid[row][column] = '.';
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

        //P.testing();
        P.mainLoop(in);
    }
}

