import java.util.*;
import java.io.*;

class Color {
    int colorA, colorB;
    public Color(int colorA, int colorB) {
        this.colorA = colorA;
        this.colorB = colorB;
    }
}

class Player {

    final int GRID_WIDTH = 6;
    final int GRID_HEGIHT = 12;
    final int GRID_SIZE = GRID_WIDTH * GRID_HEGIHT;

    ArrayList<Color> nextBlocks = new ArrayList<Color>();

    public Player() {
        // Scanner in = new Scanner(System.in);
        Scanner in = null;
        try {
            in = new Scanner(new File("map.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        char[] myGrid = new char[GRID_SIZE];
        char[] opponentsGrid = new char[GRID_SIZE];

        for (int i = 0; i < 8; i++) {
            nextBlocks.add(new Color(in.nextInt(), in.nextInt()));
        }

        for (int i = 0; i < 12; i++) {
            String row = in.next();
            for (int j = 0; j < row.length(); j++) {
                myGrid[i * GRID_WIDTH + j] = row.charAt(j);
            }
        }

        for (int i = 0; i < 12; i++) {
            String row = in.next();
            for (int j = 0; j < row.length(); j++) {
                opponentsGrid[i * GRID_WIDTH + j] = row.charAt(j);
            }
        }
        /*
        myGrid = nextState(myGrid, 0, '7');
        myGrid = nextState(myGrid, 1, '2');
        myGrid = nextState(myGrid, 2, '9');

        myGrid = nextState(myGrid, 0, '1');
        myGrid = nextState(myGrid, 0, '5');
        myGrid = nextState(myGrid, 0, '3');
        myGrid = nextState(myGrid, 1, '4');
        myGrid = nextState(myGrid, 1, '1');
        myGrid = nextState(myGrid, 1, '2');
        myGrid = nextState(myGrid, 1, '3');
        myGrid = nextState(myGrid, 2, '5');
        myGrid = nextState(myGrid, 2, '4');
        myGrid = nextState(myGrid, 2, '5');
        myGrid = nextState(myGrid, 3, '5');
        printGrid(myGrid);
        myGrid = nextState(myGrid, 1, '5');
        myGrid = nextState(myGrid, 3, '5');
        printGrid(myGrid);
        */
        myGrid = nextState(myGrid, 0, '5');
        myGrid = nextState(myGrid, 0, '2');
        myGrid = nextState(myGrid, 0, '5');
        myGrid = nextState(myGrid, 1, '3');
        myGrid = nextState(myGrid, 1, '2');
        printGrid(myGrid);
    }

    private char[] nextState(char[] grid, int column, char color) {
        int row = findFreeRow(grid, column);
        grid = placeBlock(grid, row, column, color);

        TreeSet<Integer> columnsToCheck = new TreeSet<Integer>();
        // check column where block was placed
        columnsToCheck.add(column);
        while (!columnsToCheck.isEmpty()) {
            // get position of block to check
            column = columnsToCheck.pollFirst();
            row = findFreeRow(grid, column) + 1;
            int lastFreeRow = GRID_HEGIHT - 1;
            // need to check all rows
            while (row <= lastFreeRow) {
                if (!insideGrid(row, column)) {
                    break;
                }

                color = grid[row * GRID_WIDTH + column];
                if (color == '.') {
                    break;
                }

                // count size of blocks
                HashSet<Integer> idxs = new HashSet<Integer>();
                int size = countBlocks(grid, row, column, color, idxs);

                if (size >= 4) {
                    // delete blocks
                    for (Integer idx : idxs) {
                        grid[idx] = '.';
                    }

                    // obtain columns that needs to be checked
                    HashSet<Integer> affectedColumns = getColumns(idxs);

                    // let blocks fall in columns with deleted blocks
                    lastFreeRow = gravity(grid, affectedColumns);

                    // check whether another deletion is possible within column
                    columnsToCheck.addAll(affectedColumns);
                }
                row++;
            }
        }

        return grid;
    }

    private HashSet<Integer> getColumns(HashSet<Integer> idxs) {
        HashSet<Integer> columns = new HashSet<Integer>();
        for (Integer idx : idxs) {
            columns.add(idx % GRID_WIDTH);
        }
        return columns;
    }

    private boolean insideGrid(int row, int column) {
        int index = row * GRID_WIDTH + column;
        return index >= 0 && index < GRID_SIZE;
    }

    private char[] placeBlock(char[] grid, int row, int column, char color) {
        char[] result = Arrays.copyOf(grid, grid.length);
        result[row * GRID_WIDTH + column] = color;
        result[(row - 1) * GRID_WIDTH + column] = color;
        return result;
    }

    private int findFreeRow(char[] grid, int column) {
        for (int i = 1; i < GRID_HEGIHT; i++) {
            if (grid[i * GRID_WIDTH + column] != '.') {
                return i - 1; // free row is above first occupied
            }
        }
        return GRID_HEGIHT - 1;
    }

    private int countBlocks(char[] grid, int row, int column, char color, HashSet<Integer> visited) {
        if (row < 0 ||
            column < 0 ||
            row >= GRID_HEGIHT ||
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
            for (int i = GRID_HEGIHT - 1; i > 0; i--) {
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
                } else {
                    if (found) {
                        break;
                    }
                }
            }
        }
        return lastFreeRow;
    }

    private void printGrid(char[] grid) {
        for (int i = 0; i < grid.length; i++) {
            System.out.print(grid[i]);
            if ((i + 1) % GRID_WIDTH == 0) {
                System.out.println();
            }
        }
    }

    public static void main(String args[]) {
        Player P = new Player();

    }
}
