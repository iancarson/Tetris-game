package view;
import controller.*;
import model.Block;
import model.Board;
import model.MovableTetrisPiece;
import model.TetrisPiece;
import view.GamePanel;
import sounds.Sound;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

import static model.Block.I;
import static model.Block.J;
import static model.TetrisPiece.*;
import static model.TetrisPiece.T;
import static model.TetrisPiece.Z;

public class Game implements Runnable, KeyListener {
    public static final Dimension DIM = new Dimension(500, 700); //the dimension of the game.
    public static final int THRESHOLD = 2400; // threshold to increase speed as score goes up
    public static int nAutoDelay = 300; // how fast the tetrominoes come down
    public static final int TETROMINO_NUMBER = 100; // for tetromino probability of which comes next
    private GamePanel gmpPanel;
    public static Random R = new Random();
    public final static int ANIM_DELAY = 45; // milliseconds between screen updates (animation)
    //	threads for game play
    private Thread thrAnim;
    private Thread thrAutoDown;
    private Thread thrLoaded;
    private long lTime; // time stamp
    private long lTimeStep;
    final static int PRESS_DELAY = 40; // avoid double pressing
    private boolean bMuted = true;
    Board board=new Board();

    private final int PAUSE = 80, // p key
            QUIT = 81, // q key
            LEFT = 37, // move piece left; left arrow or letter A
            RIGHT = 39, // move piece right; right arrow OR LETTER D
            START = 83, // s key
            MUTE = 77, // m-key
            DOWN = 40, // move piece faster down OR LETTER S
            SPACE = 32; // rotate piece


    private Clip clpMusicBackground; // background music
    private Clip clpBomb; // noise for when bomb (black square piece) hits




    public Game() {

        board.newGame();//acessing the Board via an instance

        gmpPanel = new GamePanel(DIM);
        gmpPanel.addKeyListener(this);
        clpBomb = Sound.clipForLoopFactory("/sounds/explosion-02.wav");
        clpMusicBackground = Sound.clipForLoopFactory("/sounds/tetris_tone_loop_1_.wav");


    }

    // ===============================================
    // ==METHODS
    // ===============================================

    public static void main(String args[]) {
        EventQueue.invokeLater(() -> {
            try {
                Game game = new Game(); // construct itself
                game.fireUpThreads();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private void fireUpThreads() { // called initially
        if (thrAnim == null) {
            thrAnim = new Thread(this); // pass the thread a runnable object (this)
            thrAnim.start();
        }
        if (thrAutoDown == null) {
            thrAutoDown = new Thread(this);
            thrAutoDown.start();
        }

        if (!Commandcontrol.getInstance().isLoaded() && thrLoaded == null) {
            thrLoaded = new Thread(this);
            thrLoaded.start();
        }
    }

    // implements runnable - must have run method
    public void run() {

        // lower this thread's priority; let the "main" aka 'Event Dispatch'
        // giving a priority to this thread
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        // and get the current time
        long lStartTime = System.currentTimeMillis();
        if (!Commandcontrol.getInstance().isLoaded() && Thread.currentThread() == thrLoaded) {
            Commandcontrol.getInstance().setLoaded(true);
        }

        // thread animates the scene
        while (Thread.currentThread() == thrAutoDown) {
            if (!Commandcontrol.getInstance().isPaused() && Commandcontrol.getInstance().isPlaying()) {
                tryMovingDown();
            }
            gmpPanel.repaint();
            try {
                lStartTime += nAutoDelay;
                Thread.sleep(Math.max(0, lStartTime - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                break;
            }
        }
        while (Thread.currentThread() == thrAnim) {
            if (!Commandcontrol.getInstance().isPaused() && Commandcontrol.getInstance().isPlaying()) {
                updateGrid();
            }
            gmpPanel.repaint();


            try {
                // The total amount of time is guaranteed to be at least ANIM_DELAY long.  If processing (update)
                // between frames takes longer than ANIM_DELAY, then the difference between lStartTime -
                // System.currentTimeMillis() will be negative, then zero will be the sleep time
                lStartTime += ANIM_DELAY;
                Thread.sleep(Math.max(0, lStartTime - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                // just skip this frame -- continue;
                break;
            }
        } // end while
    } // end run

    private void updateGrid() {
        gmpPanel.grid.setBlocks(gmpPanel.tetrCurrent);

    }


    private void tryMovingDown() {
//		uses a test tetromino to see if can move down in board
        Tetromino tetrTest = gmpPanel.tetrCurrent.cloneTetromino();
        board.drop();
        tetrTest.moveDown();
        if (gmpPanel.grid.requestDown(tetrTest)) {
            gmpPanel.tetrCurrent.moveDown();
            tetrTest = null;
        }
//		once bomb hits the bottom, plays bomb noise, clears the board and adds to score
        else if (Commandcontrol.getInstance().isPlaying() && gmpPanel.tetrCurrent instanceof Bomb) {
            clpBomb.stop();
            clpBomb.flush();
            clpBomb.setFramePosition(0);
            clpBomb.start();

            gmpPanel.tetrCurrent = gmpPanel.tetrOnDeck;
            gmpPanel.tetrOnDeck = createNewTetromino();
            tetrTest = null;
        }
//		once a tetromino hits the bottom, check if game is over (top row)
//  check if any full rows completed, generate new tetromino for on deck, switch on deck to current
        else if (Commandcontrol.getInstance().isPlaying()) {
            gmpPanel.grid.addToOccupied(gmpPanel.tetrCurrent);
            gmpPanel.grid.checkTopRow();
            gmpPanel.grid.checkCompletedRow();
            gmpPanel.tetrCurrent = gmpPanel.tetrOnDeck;
            gmpPanel.tetrOnDeck = createNewTetromino();
            tetrTest = null;
        } else {
            tetrTest = null;
        }

    }


    // Called when user presses 's'
    private void startGame() {
        board.newGame();
        gmpPanel.tetrCurrent = createNewTetromino();
        gmpPanel.tetrOnDeck = createNewTetromino();

        Commandcontrol.getInstance().clearAll();
        Commandcontrol.getInstance().initGame();
        Commandcontrol.getInstance().setPlaying(true);
        Commandcontrol.getInstance().setPaused(false);
        Commandcontrol.getInstance().setGameOver(false);
        if (!bMuted)
            clpMusicBackground.loop(Clip.LOOP_CONTINUOUSLY);
    }


    // creates the next tetromino from the different options available
    private Tetromino createNewTetromino() {
        Block block = null;
        int nKey = R.nextInt(TETROMINO_NUMBER);
       /** switch(block)
        {
            case EMPTY:
                break;
            case I:
                if(nKey > 84 && nKey <= 98)
                    return new PlusPiece();

            case J:
                if(nKey > 71 && nKey <= 84)
                    return new JPiece();
                break;
            case L:
                if(nKey > 58 && nKey <= 71)
                    return  new LPiece();
                break;
            case O:
                if(nKey > 12 && nKey <= 23)
                    return new SquarePiece();
                break;
            case S:
                if(nKey > 23 && nKey <= 35)
                    return new SPiece();
                break;
            case T:
                if(nKey > 46 && nKey <= 58)
                    return  new TPiece();
                break;
            case Z:
                if(nKey > 46 && nKey <= 58)
                    return new ZPiece();
                break;
        }
**/

        if (nKey >= 0 && nKey <= 12) {

            return new LongPiece();
        } else if (nKey > 12 && nKey <= 23) {
            return new OPiece();
        } else if (nKey > 23 && nKey <= 35) {
            return new SPiece();
        } else if (nKey > 35 && nKey <= 46) {
            return new TPiece();
        } else if (nKey > 46 && nKey <= 58) {
            return new ZPiece();
        } else if (nKey > 58 && nKey <= 71) {
            return new LPiece();
        } else if (nKey > 71 && nKey <= 84) {
            return new JPiece();
        } else if (nKey > 84 && nKey <= 98) {
            return new PlusPiece();
        } else {
            return new Bomb();
        }
    }


    // Varargs for stopping looping-music-clips
    private static void stopLoopingSounds(Clip... clpClips) {
        for (Clip clp : clpClips) {
            clp.stop();
        }
    }

  /***************************************************************************
     KEYLISTENER METHODS WHEN THE SHORTCUT KEYS ARE PRESSED.
    **/

    @Override
    public void keyPressed(KeyEvent e) {
        lTime = System.currentTimeMillis();//To generate the keyboard shortcuts
        int nKeyPressed = e.getKeyCode();
        if (nKeyPressed == START && Commandcontrol.getInstance().isLoaded() && !Commandcontrol.getInstance().isPlaying())
            startGame();

        if (nKeyPressed == QUIT && lTime > lTimeStep + PRESS_DELAY) {
            System.exit(0);
        }
        //To move down using S
        if (nKeyPressed == DOWN ||nKeyPressed== KeyEvent.VK_S && (lTime > lTimeStep + PRESS_DELAY - 35) && Commandcontrol.getInstance().isPlaying()) {
            tryMovingDown();
            board.down();
            lTimeStep = System.currentTimeMillis();
        }
        //To move right using D
        if (nKeyPressed == RIGHT || nKeyPressed== KeyEvent.VK_D && lTime > lTimeStep + PRESS_DELAY) {
            Tetromino tetrTest = gmpPanel.tetrCurrent.cloneTetromino();
            tetrTest.moveRight();
            board.right();
            if (gmpPanel.grid.requestLateral(tetrTest)) {
                gmpPanel.tetrCurrent.moveRight();
                tetrTest = null;
                lTimeStep = System.currentTimeMillis();
            } else {
                tetrTest = null;
            }
        }
        //To move left  using A
        if (nKeyPressed == LEFT || nKeyPressed==KeyEvent.VK_A && lTime > lTimeStep + PRESS_DELAY) {
            Tetromino tetrTest = gmpPanel.tetrCurrent.cloneTetromino();
            tetrTest.moveLeft();
            board.left();
            if (gmpPanel.grid.requestLateral(tetrTest)) {
                gmpPanel.tetrCurrent.moveLeft();
                tetrTest = null;
                lTimeStep = System.currentTimeMillis();
            } else {
                tetrTest = null;
            }
        }
        // space = rotate
        if (nKeyPressed == SPACE|| nKeyPressed==KeyEvent.VK_UP || nKeyPressed==KeyEvent.VK_W) {
            //board.rotate();
           // MovableTetrisPiece mov=new MovableTetrisPiece();
            Tetromino tetrTest = gmpPanel.tetrCurrent.cloneTetromino();
            tetrTest.rotate();
            if (gmpPanel.grid.requestLateral(tetrTest)) {
                gmpPanel.tetrCurrent.rotate();
                tetrTest = null;
                lTimeStep = System.currentTimeMillis();
            } else {
                tetrTest = null;
            }
        }
        if (nKeyPressed == MUTE) {
            if (!bMuted) {
                stopLoopingSounds(clpMusicBackground);
                stopLoopingSounds(clpBomb);
                bMuted = !bMuted;
            } else {
                clpMusicBackground.loop(Clip.LOOP_CONTINUOUSLY);
                bMuted = !bMuted;
            }
        }

    }


    @Override
    // Needed because of KeyListener implementation
    public void keyReleased(KeyEvent e) {

    }

    @Override
    // Needed because of KeyListener implementation
    public void keyTyped(KeyEvent e) {
    }

}


