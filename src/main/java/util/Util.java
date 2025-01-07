package util;

public class Util {
    public static int[][] reversePlayer(int[][] state) {
        int rows = state.length;
        int columns = state[0].length;

        int[][] reversedState = new int[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                reversedState[rows - 1 - i][columns - 1 - j] = state[i][j];
            }
        }

        return reversedState;
    }
}
