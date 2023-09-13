package connectx.Spycesar;

import connectx.*;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

public class Spycesar implements CXPlayer {

    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private CXCellState spycesar;
    private CXCellState opponent;
    private int  playerScore;
    private int  opponentScore;
    private int  TIMEOUT;
    private long START;

    // CONSTRUCTOR
    public Spycesar(){}

    @Override
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

        rand    = new Random(System.currentTimeMillis());
        myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;

        playerScore = 0;
        opponentScore = 0;

    }

    @Override
    public int selectColumn(CXBoard B) {

        START = System.currentTimeMillis(); // Save starting time
        spycesar = (B.numOfMarkedCells() %2 == 0) ? CXCellState.P1 : CXCellState.P2;
        opponent = (B.numOfMarkedCells() %2 == 0) ? CXCellState.P2 : CXCellState.P1;
        //System.out.println("Spycesar = " + spycesar + " AND " + " Opponent = " + opponent);

        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)];

        CXCell a = B.getLastMove();
        //System.out.println(a.i +" "+ a.j +" "+                      a.state);

        try {

                // Logica semi-random per le prime k mosse
                // B.X >= 4
                if (B.numOfMarkedCells() < (B.X * 2) - 3) {

                    // first move in the center column if spycesar move first in the "1° round"
                    // first move above the player or in the center column if spycesar move second in the "1° round"
                    if (B.numOfMarkedCells() == 0 || B.numOfMarkedCells() == 1) return B.N / 2;

                    //definire una logica semirandom per le restanti prime k - 1 mosse
                    else return save; // da togliere
                } else {

                    int col = singleMoveWin(B, L);
                    if (col != -1) return col;
                    else return singleMoveBlock(B, L);
                }


        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected");
            return save;
        }
    }



    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    /**
     * Check if we can win in a single move
     *
     * Returns the winning column if there is one, otherwise -1
     */

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for(int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin) {
                System.out.println("colonna vincente : " + i);
                return i; // Winning column found: return immediately
            }
            B.unmarkColumn();
        }
        System.out.println("colonna vincente non trovata. vado a SingleMoveBlock() ");
        return -1;
    }



    /**
     * Check if we can block adversary's victory
     *
     * Returns a blocking column if there is one, otherwise a random one
     */
//TODO: Fare log della funzione singleMoveBlock per vedere dove non trova la mossa bloccante
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

        //int i;
        //boolean stop2;
        for(int i : L) {
        //for(i = 0, stop2=false; i < L.length && !stop2; i++) {
            checktime();
            T.add(i); // We consider column i as a possible move
            B.markColumn(i);

            int j;
            boolean stop;

            for(j = 0, stop=false; j < L.length && !stop; j++) {
                //try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
                checktime();
                if(!B.fullColumn(L[j])) {
                    CXGameState state = B.markColumn(L[j]);
                    if (state == yourWin) {
                        T.remove(i); // We ignore the i-th column as a possible move
                        stop = true; // We don't need to check more
                        System.out.println("SingleMoveBlock trovato, colonna : " + i + " e mossa adv in " + L[j]);
                    /*
                        if(i != L[j]){
                            System.out.println("retrun L[j] = " + L[j]);
                            return L[j];
                        }
                    */

                    }
                    B.unmarkColumn();
                }
            }
            B.unmarkColumn();
            System.out.println("SingleMoveBlock non trovato per colonna "+ i);

        }

        //TODO : chiamare qui la func(). minimax
        if (T.size() > 0) {

            Integer[] X = T.toArray(new Integer[T.size()]);
            int p = rand.nextInt(X.length);
            System.out.println("T.size() > 0 : " + p);
            return X[p];
        } else {
            System.out.println("Else random");
            return L[rand.nextInt(L.length)];
        }
    }


/*

    // This is the minimax function. It considers all
    // the possible ways the game can go and returns
    // the value of the board
    public int minimax(CXBoard B, int depth, Boolean isMax, int alpha, int beta, long start) {

        //eval function
        int score = evaluate(B);
        // If Maximizer has won the game
        // return his/her evaluated score
        if (score == 10) return score;
            // If Minimizer has won the game
            // return his/her evaluated score
        else if (score == -10) return score;
        // If there are no more moves and
        // no winner then it is a tie
        if (B.numOfFreeCells() == 0) return 0;

        // If this maximizer's move
        if (isMax) {
            int eval = -1000; // -INFINITO
            // Traverse all cells
            for(int i = 0; i<B.M; i++)
                for (int j = 0; j < B.N; j++) {
                    if (B.cellState(i, j) == CXCellState.FREE) {
                        B.markColumn(j);
                        // Call minimax recursively and choose
                        // the maximum value
                        eval = Math.max(eval, minimax(B, depth + 1, !isMax, alpha, beta, start));
                        if(eval > alpha)
                            alpha = eval;
                        // CHECK ALPHABETA PRUNING
                        if (beta <= alpha)
                            return beta;
                        //break;
                        B.unmarkColumn();
                        //timeout
                        if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) break;
                    }
                }

            return eval;
        }
        else {
            // If this minimizer>'s move
            int eval = 1000; // +INFINITO
            // Traverse all cells
            for (int i = 0; i < B.M; i++) {
                for (int j = 0; j < B.N; j++) {

                    if (B.cellState(i, j) == CXCellState.FREE) {
                        //B.cellState(i, j) = opponent;

                        // Call minimax recursively and choose
                        // the minimum value
                        eval = Math.min(eval, minimax(B, depth + 1, !isMax, alpha, beta, start));
                        if(eval < beta)
                            beta = eval;
                        // CHECK ALPHABETA PRUNING
                        if (beta <= alpha)
                            return beta;
                        //break;
                        //B.cellState(i, j) = CXCellState.FREE;
                        //timeout
                        if ((System.currentTimeMillis() - start) / 1000.0 > TIMEOUT * (99.0 / 100.0)) break;
                    }
                }
            }
            return eval;
        }
    }



    //a function that calculates the value of
    // the board depending on the placement of
    // pieces on the board.

    public int evaluate(CXBoard B) {

        // Checking for Rows for X or O victory.
        for (int row = 0; row < B.M; row++){
            for(int col = 0; col < B.N; col++){

                if(board[row][col] == spycesar) playerScore++;
                else playerScore = 0;

                if(playerScore == B.K) return +10;
            }
            for(int col = 0; col < B.N; col++){

                if(board[row][col] == myOpponent) opponentScore++;
                else opponentScore = 0;

                if(opponentScore == B.K) return -10;
            }
        }
        // Checking for Columns for X or O victory.
        for (int col = 0; col < B.N; col++){
            playerScore = 0;
            opponentScore = 0;

            for(int row = 0; row < B.M; row++){

                if(board[row][col] == myPlayer) playerScore++;
                else playerScore = 0;
                if(playerScore == B.K) return +10;
            }
            for(int row = 0; row < B.M; row++){

                if(board[row][col] == myOpponent) opponentScore++;
                else opponentScore = 0;
                if(opponentScore == B.K) return -10;
            }
        }

        // Checking for Diagonals for X or O victory.

        int count1;

        for (int row = 0; row < B.M; row++){
            for (int col = 0; col < B.N; col++){

                playerScore = 0;
                count1 = 0;

                while(count1+col < B.N && count1+row < B.M){

                    if (board[count1+row][count1+col] == myPlayer) playerScore++;
                    else playerScore = 0;

                    if(playerScore == B.K) return +10;

                    count1++;
                }
            }
            for (int col = 0; col < B.N; col++){

                opponentScore = 0;
                count1 = 0;

                while(count1+col < B.N && count1+row < B.M){

                    if (board[count1+row][count1+col] == myOpponent) opponentScore++;
                    else opponentScore = 0;

                    if(opponentScore == B.K) return -10;

                    count1++;
                }
            }
        }

        int playerScoreInv = 0, opponentScoreInv = 0, count2 = 0;

        // Checking for Diagonals for X or O victory.
        for (int row = 0; row < B.M; row++){
            for (int col = 0; col < B.N; col++){

                playerScoreInv = 0;
                count1 = B.N;
                count2 = 0;

                while(row + count2 < B.M && count1-col-1 > 0){

                    if (board[count2+row][count1-col-1] == myPlayer) playerScoreInv++;
                    else playerScoreInv = 0;

                    if(playerScoreInv == B.K) return +10;

                    count1--;
                    count2++;
                }
            }
            for (int col = 0; col < B.N; col++){

                opponentScoreInv = 0;
                count1 = B.N;
                count2 = 0;

                while(count2+row < B.M && count1-col-1 > 0){

                    if (board[row + count2][count1-col-1] == myOpponent) opponentScoreInv++;
                    else opponentScoreInv = 0;

                    if(opponentScoreInv == B.K) return -10;

                    count1--;
                    count2++;
                }
            }
        }
        return 0;
    }
*/


    @Override
    public String playerName() {

        return "Spycesar";
    }
}

