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
    private long START;
    private final int alpha = -1000;
    private final int beta = 1000;
    // Nuova HashMap per memorizzare le valutazioni già calcolate
    private HashMap<String, Integer> evaluatedConfigurations;
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
        }
        catch (TimeoutException e){

            System.err.println("Timeout Exception !!!");
            return save;
        }
    }



    //TODO: sistemare mosse su colonna 0 quando non ho risultati da evaluate e tutte le colonne hanno val 0.
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

                    double hVal = setHeuristicsValue(i);

                    //try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout

                    checktime();
                    B.markColumn(i);
                    incrementCountMosse();
                    int score = minimax(B, 1, false, alpha, beta, START, hVal);
                    //System.err.println("------------------------------------------------------- SCORE COLONNA " + i + " = " + score);
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

            System.err.println("-----------------------------------");
            for (int i = 0; i < L.length; i++) {
                System.err.println("COLONNA " + b[i] + " SCORE " + a[i]);
            }

            //System.out.printf("FINAL BOARDHASH = " + calculateBoardHash(B) + " ");
            //System.out.printf("MOSSA NUMERO: " + getCountMosse());

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
    private int minimax(CXBoard B, int depth, Boolean isMax, int alpha, int beta, long START, double hVal) throws TimeoutException {

        checktime();
        int score = evaluate(B);

        if (B.gameState() != CXGameState.OPEN || checktimeToCutDepth()) {
            //System.err.println("Score = " + score);
            return score;
        }


        Integer[] L = B.getAvailableColumns();

        // If this maximizer's move
        if (isMax) {
            //System.out.println("IsMax");
            int eval = -1000;
            for (int i : L) {
                //System.err.println("IsMax1");
                if (!B.fullColumn(i)) {
                    //System.err.println(B.markColumn(i));
                    B.markColumn(i);
                    //System.err.println("MarkColumn " + i);
                    incrementCountMosse();
                    eval = Math.max(eval,
                            minimax(B, depth - 1, !isMax, alpha, beta, START, hVal));

                    decrementCountMosse();
                    B.unmarkColumn();
                    // CHECK ALPHABETA PRUNING
                    alpha = Math.max(eval, alpha);
                    if (beta <= alpha) {
                        //System.out.println("prune");
                        break;
                    }

                }
            }
            return eval;
        }
        else {
            // If this minimizer's move
            int eval = 1000;
            //System.err.println("IsMin");
            for (int i : L) {
                //System.err.println("IsMin1");
                if (!B.fullColumn(i)) {
                    //System.err.println(B.markColumn(i));
                    B.markColumn(i);
                    //System.err.println("MarkColumn " + i);
                    eval = Math.min(eval,
                            minimax(B, depth - 1, !isMax, alpha, beta, START, hVal));

                    B.unmarkColumn();
                    // CHECK ALPHABETA PRUNING
                    beta = Math.min(eval, beta);
                    if (beta <= alpha) {
                        //System.out.println("prune");
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

        //System.out.println("BOARD HASH = " + boardHash);

        // Controlla se la valutazione è già presente nella cache
        if (evaluatedConfigurations.containsKey(boardHash)) {
            //System.out.println(" VALUTAZIONE GIA PRESENTE NELLA CACHE: " + evaluatedConfigurations.get(boardHash));
            return evaluatedConfigurations.get(boardHash);
        }

        // Valuta la configurazione trasposta orizzontalmente della matrice
        if (evaluatedConfigurations.containsKey(mirrorBoardHash)) {
            //System.out.println(" VALUTAZIONE GIA PRESENTE NELLA CACHE: " + evaluatedConfigurations.get(boardHash));
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



    //TODO: implements depth
    private int calculateDepth(CXBoard board, long elapsedTime) {
        // Calcolo del tempo rimanente
        long remainingTime = 10000 - elapsedTime;

        // Se il tempo rimanente è inferiore a una soglia, diminuisci la profondità
        if (remainingTime < 2000) { // Ad esempio, se rimangono meno di 2 secondi
            return 1; // Profondità minima per garantire una mossa entro il limite di tempo
        }

        // Calcolo della fase del gioco basata sul numero di celle marcate
        int markedCells = board.numOfMarkedCells();
        if (markedCells < (board.M * board.N) / 2) { // Ad esempio, se meno della metà delle celle è stata marcate
            return 3; // Profondità intermedia
        } else {
            return 5; // Profondità maggiore nella seconda metà del gioco
        }
    }




    //TODO: semplificare evaluate
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
                //if(playerScore >= B.X) return 10;
                //if(opponentScore >= B.X) return -10;
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
                //if(playerScore >= B.X) return 10;
                //if(opponentScore >= B.X) return -10;
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
                //if(playerScore >= B.X) return 10;
                //if(opponentScore >= B.X) return -10;

                count++;
            }
        }
        // CHECK_DIAG FOR LAST ROW
        //int count = 0;
        //int row = B.M - 1;

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
                //if (playerScore >= B.X) return 10;
                //if (opponentScore >= B.X) return -10;

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
                //if(playserScore >= B.X) return 10;
                //if(opponentScore >= B.X) return -10;

                count++;
                //j--;
            }
        }
        // CHECK_ANTI-DIAG FOR LAST ROW

        //int row = B.M - 1;
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
                //if (playerScore >= B.X) return 10;
                //if (opponentScore >= B.X) return -10;

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
                System.out.println("colonna vincente : " + i);
                return i; // Winning column found: return immediately
            }
            B.unmarkColumn();
        }
        //System.out.println("colonna vincente non trovata. vado a SingleMoveBlock() ");
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
            T.add(i); // We consider column i as a possible move
            B.markColumn(i);

            int j;
            boolean stop;

            for (j = 0, stop = false; j < L.length && !stop; j++) {
                //try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
                checktime();
                if (!B.fullColumn(L[j])) {
                    CXGameState state = B.markColumn(L[j]);
                    if (state == yourWin) {
                        T.remove(i); // We ignore the i-th column as a possible move
                        stop = true; // We don't need to check more
                        //System.out.println("SingleMoveBlock trovato, colonna : " + i + " e mossa adv in " + L[j]);
                    }
                    B.unmarkColumn();
                }
            }
            B.unmarkColumn();
            //System.out.println("SingleMoveBlock non trovato per colonna "+ i);

        }

        Integer[] X = T.toArray(new Integer[T.size()]);

        if (T.size() == 1){
            System.err.println("T.size == 1");
            return findBestMove(B,X);
        }
        if (T.size() > 1) {
            System.err.println("T.size > 1");
            //return X[rand.nextInt(X.length)];
            return findBestMove(B, X);
        }
        else { // T.size == 0 --> qualsiasi mossa faccio perdo
            System.out.println("Else random");
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


    private boolean checktimeToCutDepth() {
        if ((System.currentTimeMillis() - START) / 1000.0 >= (TIMEOUT * (97.0 / 100.0)) / freeCol.length) {
            //System.err.println("DEPTH cut");
            return true;
        }
        return false;
    }


    //TODO: pensare se serve settare un peso ad ogni mossa possibile
    private double setHeuristicsValue(int column) {



        return 1;
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








    @Override
    public String playerName() {

        return "Spycesar";
    }
}

