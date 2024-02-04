package connectx.Spycesar;

import connectx.*;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class Spycesar implements CXPlayer {

    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private CXCellState spycesar;
    private CXCellState opponent;
    private int TIMEOUT;
    private long START, START2;
    private final int alpha = -1000;
    private final int beta = 1000;
    private HashMap<String, Integer> evaluatedConfigurations;     // Nuova HashMap per memorizzare le valutazioni già calcolate
    private int countMosse; // Per evitare di chiamare evaluate quando devo allineare X simboli per vincere e ho fatto solo 4 mosse
    long timeout_in_millisecs;
    Integer[] freeCol;


    // CONSTRUCTOR
    public Spycesar() {

    }


    @Override
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        timeout_in_millisecs = timeout_in_secs * 1000L;
        evaluatedConfigurations = new HashMap<>();
        countMosse = 0;
        freeCol = new Integer[0];

    }



    @Override
    public int selectColumn(CXBoard B) {

        START = System.currentTimeMillis(); // Save starting time
        spycesar = (B.numOfMarkedCells() % 2 == 0) ? CXCellState.P1 : CXCellState.P2;
        opponent = (B.numOfMarkedCells() % 2 == 0) ? CXCellState.P2 : CXCellState.P1;
        freeCol = B.getAvailableColumns();
        int save = freeCol[rand.nextInt(freeCol.length)];

        try{

            // first move in the center column if spycesar move first in the "1° round"
            // first move above the player or in the center column if spycesar move second in the "1° round"
            if (B.numOfMarkedCells() == 0 || B.numOfMarkedCells() == 1) {
                for (int i = 0; i <= B.numOfMarkedCells(); i++) {
                    incrementCountMosse();
                }
                return B.N / 2;
            }

            else if(B.numOfMarkedCells() < (B.X * 2) - 2) {
                return findBestMove(B,freeCol);
            }
            else {

                int col = singleMoveWin(B, freeCol);
                if(col != - 1){
                    return col;
                }

                return singleMoveBlock(B,freeCol);
            }

             //    return findBestMove(B, freeCol);
            //}
        }
        catch (TimeoutException e){

            System.err.println("Timeout Exception !!!");
            return save;
        }
    }



    private int findBestMove(CXBoard B, Integer[] L) {

        int save = L[rand.nextInt(L.length)];

        try {

            //System.out.println("arrivato a findbestMove");
            int bestScore = -1000;
            int bestMove = 0;
            int j = 0;
            int[] a = new int[L.length]; // array of scores
            int[] b = new int[L.length]; // array of free col

            for (int i : L) {
                if (!B.fullColumn(i)) {
                    //try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout

                    System.err.println("Number of free cell: " + getNumberOfFreeCell(B));
                    checktime();
                    B.markColumn(i);
                    incrementCountMosse();
                    START2 = System.currentTimeMillis();
                    int score = minimax(B, false, alpha, beta, L.length);
                    B.unmarkColumn();
                    decrementCountMosse();
                    a[j] = score;
                    b[j] = i;
                    j++;
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = i;
                    }
/*
                    for (Integer integer : L) {
                        System.out.println("COLONNE DISPONIBILI: " + integer);
                    }
*/
                }
            }

            //System.out.println("BestMove = " + bestMove + " BestScore = " + bestScore);

            for (int i = 0; i < L.length; i++) {
                System.err.println("COLONNA " + b[i] + " SCORE " + a[i]);
            }
            System.err.println("-----------------------------------");

            incrementCountMosse();

            return bestMove;
        }
        catch (TimeoutException e) {

            System.err.println("Timeout!!! Random column selected");
            incrementCountMosse();

            return save;
        }
    }





    // This is the minimax function. It considers all
    // the possible ways the game can go and returns
    // the value of the board
    private int minimax(CXBoard B, Boolean isMax, int alpha, int beta, int nCol) throws TimeoutException {

        checktime();
        int score = evaluate(B);

        if (B.gameState() != CXGameState.OPEN || checktimeToCutDepth(B, nCol)) {
            //System.err.println("Score = " + score);
            return score;
        }

        Integer[] L = B.getAvailableColumns();

        // If this maximizer's move
        if (isMax) {
            int eval = -1000;
            for (int i : L) {
                if (!B.fullColumn(i)) {
                    B.markColumn(i);
                    incrementCountMosse();
                    eval = Math.max(eval,
                            minimax(B, !isMax, alpha, beta, nCol));

                    decrementCountMosse();
                    B.unmarkColumn();
                    // CHECK ALPHABETA PRUNING
                    alpha = Math.max(eval, alpha);
                    if (beta <= alpha) {
                        //System.err.println("prune");
                        break;
                    }

                }
            }
            return eval;
        }
        else {
            // If this minimizer's move
            int eval = 1000;
            for (int i : L) {
                if (!B.fullColumn(i)) {
                    B.markColumn(i);
                    eval = Math.min(eval,
                            minimax(B, !isMax, alpha, beta, nCol));

                    B.unmarkColumn();
                    // CHECK ALPHABETA PRUNING
                    beta = Math.min(eval, beta);
                    if (beta <= alpha) {
                        //System.err.println("prune");
                        break;
                    }

                }
            }
            return eval;
        }
    }





    private int evaluate(CXBoard B) {

        String boardHash = calculateBoardHash(B);
        String mirrorBoardHash = calculateMirrorBoardHash(B);

        //System.err.println("BOARD HASH = " + boardHash);

        // Controlla se la valutazione è già presente nella cache
        if (evaluatedConfigurations.containsKey(boardHash)) {
            //System.err.println(" VALUTAZIONE GIA PRESENTE NELLA CACHE: " + evaluatedConfigurations.get(boardHash));
            return evaluatedConfigurations.get(boardHash);
        }

        // Valuta la configurazione trasposta orizzontalmente della matrice
        if (evaluatedConfigurations.containsKey(mirrorBoardHash)) {
            //System.err.println(" VALUTAZIONE GIA PRESENTE NELLA CACHE: " + evaluatedConfigurations.get(boardHash));
            return evaluatedConfigurations.get(mirrorBoardHash);
        }


        // Se non è presente, calcola la valutazione normalmente
        int evaluation = calculateEvaluation(B);
        // Aggiungi la valutazione alla cache
        evaluatedConfigurations.put(boardHash, evaluation);

        return evaluation;
    }





    // Calculate the hash of the board state config.
    private String calculateBoardHash(CXBoard B) {

        CXCellState[][] board = B.getBoard();
        StringBuilder hashBuilder = new StringBuilder();

        // Aggiungi la posizione delle pedine sul tabellone
        for (int row = 0; row < B.M; row++) {
            for (int col = 0; col < B.N; col++) {
                hashBuilder.append(board[row][col].toString());
            }
        }

        return hashBuilder.toString();
    }




    // Calculate the hash of the mirror board state config.
    private String calculateMirrorBoardHash(CXBoard B) {

        CXCellState[][] board = B.getBoard();
        StringBuilder mirroredBuilder = new StringBuilder();

        // Riflessione orizzontale
        for (int row = 0; row < B.M; row++) {
            for (int col = B.N - 1; col >= 0; col--) {
                mirroredBuilder.append(board[row][col].toString());
            }
        }

        return mirroredBuilder.toString();
    }






    //a function that calculates the value of
    // the board depending on the placement of
    // pieces on the board.
    private int calculateEvaluation(CXBoard B) {

        if(getCountMosse() < B.X - 1) return  0;

        CXCellState[][] board = B.getBoard();


        //CHECK ROWS
        for (int row = 0; row < B.M; row++) {

            int playerScore = 0;
            int opponentScore = 0;

            for (int col = 0; col < B.N; col++) {

                if (board[row][col] == spycesar) {
                    playerScore++;
                    opponentScore = 0;
                } else if (board[row][col] == opponent) {
                    opponentScore++;
                    playerScore = 0;
                } else {
                    playerScore = 0;
                    opponentScore = 0;
                }

                if (playerScore >= B.X) return (B.numOfFreeCells() + 1);
                if (opponentScore >= B.X) return (((-1) * B.numOfFreeCells()) - 1);
            }
        }


        //CHECK COLUMNS
        for (int col = 0; col < B.N; col++) {

            int playerScore = 0;
            int opponentScore = 0;

            for (int row = 0; row < B.M; row++) {

                if (board[row][col] == spycesar) {
                    playerScore++;
                    opponentScore = 0;
                } else if (board[row][col] == opponent) {
                    opponentScore++;
                    playerScore = 0;
                } else {
                    playerScore = 0;
                    opponentScore = 0;
                }

                if (playerScore >= B.X) return (B.numOfFreeCells() + 1);
                if (opponentScore >= B.X) return (((-1) * B.numOfFreeCells()) - 1);
            }
        }


        //CHECK DIAGONAL
        for (int row = B.X - 1; row < B.M - 1; row++) {

            int count = 0;
            int playerScore = 0;
            int opponentScore = 0;

            while (count <= row) {

                if (board[row - count][count] == spycesar) {
                    playerScore++;
                    opponentScore = 0;

                } else if (board[row - count][count] == opponent) {
                    opponentScore++;
                    playerScore = 0;

                } else {
                    playerScore = 0;
                    opponentScore = 0;
                }

                if (playerScore >= B.X) return (B.numOfFreeCells() + 1);
                if (opponentScore >= B.X) return (((-1) * B.numOfFreeCells()) - 1);

                count++;
            }
        }
        // CHECK_DIAG FOR LAST ROW

        int countrighe = 0;

        for (int col = 0; col < B.N - B.X + 1; col++) {

            int playerScore = 0;
            int opponentScore = 0;
            int count = 0;


            for (int row = B.M - 1; row >= countrighe; row--) {

                int col2 = Math.min(col + count, 6);

                if (board[row][col2] == spycesar) {
                    playerScore++;
                    opponentScore = 0;

                } else if (board[row][col2] == opponent) {
                    opponentScore++;
                    playerScore = 0;

                } else {
                    playerScore = 0;
                    opponentScore = 0;
                }

                if (playerScore >= B.X) return (B.numOfFreeCells() + 1);
                if (opponentScore >= B.X) return (((-1) * B.numOfFreeCells()) - 1);

                count++;
            }
            countrighe++;
        }


        //CHECK ANTI-DIGONAL
        for (int row = B.X - 1; row < B.M - 1; row++) {

            int count = 0;
            int j = B.N - 1; // col
            int playserScore = 0;
            int opponentScore = 0;

            while (count <= row) {

                if (board[row - count][j - count] == spycesar) {

                    playserScore++;
                    opponentScore = 0;
                } else if (board[row - count][j - count] == opponent) {

                    opponentScore++;
                    playserScore = 0;
                } else {
                    playserScore = 0;
                    opponentScore = 0;
                }

                if (playserScore >= B.X) return (B.numOfFreeCells() + 1);
                if (opponentScore >= B.X) return (((-1) * B.numOfFreeCells()) - 1);

                count++;
            }
        }
        // CHECK_ANTI-DIAG FOR LAST ROW

        countrighe = 0;

        for (int col = B.N - 1; col >= B.X - 1; col--) {

            int count = 0;
            int playerScore = 0;
            int opponentScore = 0;

            for (int row = B.M - 1; row >= countrighe; row--) {

                if (board[row][col - count] == spycesar) {
                    playerScore++;
                    opponentScore = 0;
                } else if (board[row][col - count] == opponent) {
                    opponentScore++;
                    playerScore = 0;
                } else {
                    playerScore = 0;
                    opponentScore = 0;
                }

                if (playerScore >= B.X) return (B.numOfFreeCells() + 1);
                if (opponentScore >= B.X) return (((-1) * B.numOfFreeCells()) - 1);

                count++;
            }
            countrighe++;
        }

        return 0;
    }










    /**
     * Check if we can win in a single move
     * <p>
     * Returns the winning column if there is one, otherwise -1
     */

    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin) {
                System.err.println("colonna vincente : " + i);
                B.unmarkColumn();
                return i; // Winning column found: return immediately
            }
            B.unmarkColumn();
        }
        //System.err.println("colonna vincente non trovata. vado a SingleMoveBlock() ");
        return -1;
    }


    /**
     * Check if we can block adversary's victory
     * <p>
     * Returns a blocking column if there is one, otherwise a random one
     */
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

        for (int i : L) {
            checktime();
            B.markColumn(i);
            T.add(i); // We consider column i as a possible move

            int j;
            boolean stop;

            for (j = 0, stop = false; j < L.length && !stop; j++) {
                checktime();
                if (!B.fullColumn(L[j])) {
                    CXGameState state = B.markColumn(L[j]);
                    if (state == yourWin) {
                        T.remove(i); // We ignore the i-th column as a possible move
                        stop = true; // We don't need to check more
                        //System.err.println("SingleMoveBlock trovato, colonna : " + i + " e mossa adv in " + L[j]);
                    }
                    B.unmarkColumn();
                }
            }
            B.unmarkColumn();
            //System.err.println("SingleMoveBlock non trovato per colonna "+ i);

        }

        Integer[] X = T.toArray(new Integer[T.size()]);

        if (!T.isEmpty()){
            //System.err.println("T.size >= 1");
            return findBestMove(B,X);
        }
        /*
        if (T.size() > 1) {
            System.err.println("T.size > 1");
            //return X[rand.nextInt(X.length)];
            return findBestMove(B, X);
        }
        */
        else { // T.size == 0 --> qualsiasi mossa faccio perdo
            //System.out.println("Else random");
            //return L[rand.nextInt(L.length)];
            return findBestMove(B, L);
        }
    }


    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0)) {
            System.err.println("Timeout Exception");
            throw new TimeoutException();
        }
    }


    private boolean checktimeToCutDepth(CXBoard B,int colNumber) {

        int freeCell = getNumberOfFreeCell(B);

        if(freeCell <= 25) return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0)) / colNumber;

        else if(freeCell <= 36) return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0) * 0.90) / colNumber;

        else if(freeCell <= 49) return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0) * 0.80) / colNumber;

        else if(freeCell <= 64) return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0) * 0.70) / colNumber;

        else if(freeCell <= 100) return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0) * 0.45) / colNumber;

        else if(freeCell <= 225) return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0) * 0.30) / colNumber;

        else if(freeCell <= 500) return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0) * 0.15) / colNumber;

        else return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0) * 0.05) / colNumber;

        //return (System.currentTimeMillis() - START2) / 1000.0 >= (TIMEOUT * (96.0 / 100.0)) / colNumber;
    }



    private int getCountMosse(){

        return this.countMosse;
    }

    private void incrementCountMosse(){

        this.countMosse++;
    }

    private void decrementCountMosse(){

        this.countMosse--;
    }


    private int getNumberOfFreeCell(CXBoard B){

        return (B.M * B.N) - B.getMarkedCells().length;
    }








    @Override
    public String playerName() {

        return "Spycesar";
    }
}

