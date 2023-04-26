package connectx.Spycesar;

import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;

import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

public class Spycesar implements CXPlayer {

    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
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

    }
// TODO: gestire out of bound
    @Override
    public int selectColumn(CXBoard B) {

        START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
        int save    = L[rand.nextInt(L.length)]; // Save a random column



        try {

            // first move in the center column if is first
            if (B.numOfMarkedCells() == 0){ return B.N / 2; }

            // first move above the player or in the center column if is second
            if(B.numOfMarkedCells() == 1){

                if(B.getLastMove().j == B.N / 2) return B.getLastMove().j;
                else return B.N / 2;
            }

            int col = singleMoveWin(B,L);
            if(col != -1)
                return col;
            else
                return singleMoveBlock(B,L);
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
    //TODO: Cercare solo sulle colonne adiacenti e non su tutte le libere
    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for(int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin)
                return i; // Winning column found: return immediately
            B.unmarkColumn();
        }
        return -1;
    }



    /**
     * Check if we can block adversary's victory
     *
     * Returns a blocking column if there is one, otherwise a random one
     */
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

        for(int i : L) {
            checktime();
            T.add(i); // We consider column i as a possible move
            B.markColumn(i);

            int j;
            boolean stop;

            for(j = 0, stop=false; j < L.length && !stop; j++) {
                // TODO: capire riga sotto
                //try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
                checktime();
                if(!B.fullColumn(L[j])) {
                    CXGameState state = B.markColumn(L[j]);
                    if (state == yourWin) {
                        T.remove(i); // We ignore the i-th column as a possible move
                        stop = true; // We don't need to check more
                    }
                    B.unmarkColumn(); //
                }
            }
            B.unmarkColumn();
        }

        if (T.size() > 0) {
            Integer[] X = T.toArray(new Integer[T.size()]);
            return X[rand.nextInt(X.length)];
        } else {
            return L[rand.nextInt(L.length)];
        }
    }


    @Override
    public String playerName() {

        return "Spycesar";
    }
}
