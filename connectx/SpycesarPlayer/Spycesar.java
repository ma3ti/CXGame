package connectx.SpycesarPlayer;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;

import java.util.Random;

public class Spycesar implements CXPlayer {


    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int  TIMEOUT;
    private long START;


    public Spycesar(){}

    @Override
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

        rand    = new Random(System.currentTimeMillis());
        myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;

    }

    @Override
    public int selectColumn(CXBoard B) {
        return 0;
    }

    @Override
    public String playerName() {

        return "Spycesar";
    }
}
