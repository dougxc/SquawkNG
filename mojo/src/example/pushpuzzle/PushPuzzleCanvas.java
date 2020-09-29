/*
 * @(#)PushPuzzleCanvas.java	1.38 01/08/30
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */

package example.pushpuzzle;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * PushPuzzleCanvas displays the game board and handles key events.
 * The PushPuzzle game logic and algorithms are separated into Board.java.
 * PushPuzzleCanvas does not setup or use any Commands.  Commands for each
 * screen and listeners should be setup outside this class.
 * PushPuzzleCanvas generates a SELECT_COMMAND when the current level
 * is solved. Sequencing through screens is done in the PushPuzzle MIDlet.
 * <p>
 * PushPuzzleCanvas handles the reading, initialization, and sequencing
 * of individual puzzle screens.
 * <p>
 * PushPuzzleCanvas uses the Score class to restore and save game levels
 * and scores for each level. To display the scores use getScoreScreen.
 * It will be initialized with the current scores.
 * To select a new level use the getLevelScreen and gotoLevel
 * methods.
 * <p>
 * PushPuzzleCanvas handled key events for LEFT, RIGHT, UP, and DOWN to
 * move the pusher in the game board.  Pointer pressed events
 * are used to move the pusher to the target location (if possible).
 * The PushPuzzleTimer class handles moving toward the target one step
 * at a time when the timer triggers.
 * <p>
 * PushPuzzleCanvas selects color and grayscale values for the different
 * parts of the board.  The colors can be overridden with attributes
 * in the descriptor file. The attributes are PushPuzzleWallColor,
 * PushPuzzleGroundColor, PushPuzzlePacketColor, PushPuzzleStoreColor,
 * PushPuzzlePusherColor.  They should have values of the form
 * rrggbb, where each digit is a hexadecimal digit for red, green, or blue.
 */
class PushPuzzleCanvas extends Canvas {
    private int level = 1;
    private boolean solved;
    // number of pixels per cell (updated by readscreen)
    private int cell = 1;
    private int w, h;		// dimensions of this canvas
    private int bwidth, bheight; // dimensions of current screen

    private Board board;
    private Score score;
    private PushPuzzle pushpuzzle;	// The pushpuzzle MIDlet
    private Display display;
    private Timer timer;
    private TimerTask timertask;
    private CommandListener listener;

    private Form scoreForm;	// form for scores
    private TextBox levelText;	// for input of new level

    // 2 Bit color/grayscale defaults
    private static int wallColor =   0x7f7f7f;
    private static int groundColor = 0xffffff;
    private static int packetColor = 0x000000;
    private static int storeColor =  0x000000;
    private static int pusherColor = 0x000000;
    private static boolean useDottedLine = false;

    /**
     * Construct a new canvas
     */
    public PushPuzzleCanvas(PushPuzzle pushpuzzle, Score s) {
	this.pushpuzzle = pushpuzzle;
	display = Display.getDisplay(pushpuzzle);
	score = s;
	board = new Board();
	initColors();
    }

    /**
     * Read the previous level number from the score file.
     * Read in the level data.
     */
    public void init() {
	h = getHeight();
	w = getWidth();

	// Read the last level; if it can't be found, revert to level 0
	level = score.getLevel();
	if (!readScreen(level)) {
	    level = 0;
	    readScreen(level);
	}
	repaint();
    }

    /**
     * Cleanup and destroy.
     */
    public void destroy() {
	cancelTo();
	if (timer != null) {
	    timer.cancel();
	    timer = null;
	}
    }

    /*
     * Read the colors to use from the descriptor.
     */
    private void initColors() {
	boolean isColor = display.isColor();
	int numColors = display.numColors();

	if (isColor) {
            setColors(0x006D55, 0xffffff, 0xff6d00, 0xb60055, 0x6d6dff);
	} else {
	    if (numColors > 2) {
		setColors(0x999999, 0xffffff, 0x666666, 0xbbbbbb, 0x000000);
	    } else {
		setColors(0x6a6a6a, 0xffffff, 0x6a6a6a, 0x000000, 0x000000);
                useDottedLine = true;
	    }
	}
    }

    /**
     * Set the colors.
     */
    private void setColors(int w, int g, int pa, int s, int pu) {
	if (w != -1)
	    wallColor =   w;
	if (g != -1)
	    groundColor = g;
	if (pa != -1)
	    packetColor = pa;
	if (s != -1)
	    storeColor =  s;
	if (pu != -1)
	    pusherColor = pu;
    }

    /**
     * Parse colors
     */
    private int parseColor(String s) {
	if (s == null)
	    return -1;
	return Integer.parseInt(s, 16);
    }

    /**
     * Undo the last move if possible. Redraw the cell
     * the pusher occupies after the undone move and the cells
     * in the direction of the original move.
     * Here so undo can be triggered by a command.
     */
    public void undoMove() {
	int dir = board.undoMove();
	if (dir >= 0) {
	    repaintNear(board.getPusherLocation(), dir);
	}
	solved = board.solved();
    }

    /**
     * Restart the current level.
     */
    public void restartLevel() {
	readScreen(level);
	repaint();
	solved = false;
    }

    /**
     * Start the next level.
     * @return true if the new level was loaded.
     */
    public boolean nextLevel(int offset) {
	updateScores();	// save best scores
	if (level + offset >= 0 && readScreen(level+offset)) {
	    level += offset;
	    score.setLevel(level);
	    solved = false;
	    return true;
	}
	return false;
    }

    /**
     * Get the current level.
     * @return the current level.
     */
    public int getLevel() {
	return level;
    }

    /**
     * Get a screen to let the user change the level.
     * A simple numeric TextBox will do.
     */
    public Screen getLevelScreen() {
	if (levelText == null) {
	    levelText = new TextBox("Enter Level",
				      Integer.toString(level), // default
				      4, TextField.NUMERIC);
	} else {
	    levelText.setString(Integer.toString(level));
	}
	return levelText;
    }

    /**
     * Go to the chosen Level.
     * @return true if the new level was loaded.
     */
    public boolean gotoLevel() {
	if (levelText != null) {
	    String s = levelText.getString();
	    int l = Integer.parseInt(s);

	    updateScores();
	    if (l >= 0 && readScreen(l)) {
		level = l;
		score.setLevel(level);
		solved = false;
		repaint();
		return true;
	    }
	}
	return false;
    }

    /**
     * Read and setup the next level.
     * Opens the resource file with the name "/Screen.<lev>"
     * and tells the board to read from the stream.
     * <STRONG>Must be called only with the board locked.</STRONG>
     * @param lev the level number to read.
     * @return true if the reading of the level worked, false otherwise.
     */
    private boolean readScreen(int lev) {
	if (lev <= 0) {
	    board.screen0();	// Initialize the default zero screen.
	} else {
	    InputStream is = null;
	    try {
		is = getClass().getResourceAsStream(
					"/example/pushpuzzle/data/screen."
					+ lev);
		if (is != null) {
		    board.read(is, lev);
		    is.close();
		} else {
		    System.out.println(
				   "Could not find the game board for level "
				   + lev);
		    return false;
		}
	    } catch (java.io.IOException ex) {
		return false;
	    }
	}
	bwidth = board.getWidth();
	bheight = board.getHeight();

	cell = ((h-14) / bheight < w / bwidth) ? (h-14) / bheight : w / bwidth;
	return true;
    }


    /**
     * Return the Screen to display scores.
     * It returns a screen with the current scores.
     * @return a screen initialized with the current score information.
     */
    public Screen getScoreScreen() {
	Form scoreForm = null; // Temp until form can do setItem
	int currPushes = board.getPushes();
	int bestPushes = score.getPushes();
	int currMoves = board.getMoves();
	int bestMoves = score.getMoves();
	boolean newbest = solved &&
	    (bestPushes == 0 || currPushes < bestPushes);

	scoreForm = new Form(null);

	scoreForm.append(new StringItem(
            newbest ? "New Best:\n" : "Current:\n",
            currPushes + " pushes\n" +
            currMoves  + " moves"));

	scoreForm.append(new StringItem(
            newbest ? "Old Best:\n" : "Best:\n",
            bestPushes + " pushes\n" +
            bestMoves  + " moves"));

	String title = "Scores";
	if (newbest) {
	    title = "Congratulations";
	}
	scoreForm.setTitle(title);
	return scoreForm;
    }

    /**
     * Handle a repeated arrow keys as though it were another press.
     * @param keyCode the key pressed.
     */
    protected void keyRepeated(int keyCode) {
        int action = getGameAction(keyCode);
        switch (action) {
        case Canvas.LEFT:
        case Canvas.RIGHT:
        case Canvas.UP:
        case Canvas.DOWN:
            keyPressed(keyCode);
	    break;
        default:
            break;
        }
    }

    /**
     * Handle a single key event.
     * The LEFT, RIGHT, UP, and DOWN keys are used to
     * move the pusher within the Board.
     * Other keys are ignored and have no effect.
     * Repaint the screen on every action key.
     */
    protected void keyPressed(int keyCode) {
        boolean newlySolved = false;

	// Protect the data from changing during painting.
	synchronized (board) {

	    cancelTo();
	    int action = getGameAction(keyCode);
	    int move = 0;
	    switch (action) {
	    case Canvas.LEFT:
		move = Board.LEFT;
		break;
	    case Canvas.RIGHT:
		move = Board.RIGHT;
		break;
	    case Canvas.DOWN:
		move = Board.DOWN;
		break;
	    case Canvas.UP:
		move = Board.UP;
		break;

		// case 0: // Ignore keycode that don't map to actions.
	    default:
		return;
	    }

	    // Tell the board to move the piece and queue a repaint
	    int pos = board.getPusherLocation();
	    int dir = board.move(move);
	    repaintNear(pos, dir);

	    if (!solved && board.solved()) {
		newlySolved = solved = true;
	    }
	} // End of synchronization on the Board.

        if (newlySolved && listener != null) {
            listener.commandAction(List.SELECT_COMMAND, this);
        }
    }

    /**
     * Update the scores for the current level if it has
     * been solved and the scores are better than before.
     */
    private void updateScores() {
	if (!solved)
	    return;
	int sp = score.getPushes();
	int bp = board.getPushes();
	int bm = board.getMoves();

	/*
	 * Update the scores.  If the score for this level is lower
	 * than the last recorded score save the lower scores.
	 */
	if (sp == 0 || bp < sp) {
	    score.setLevelScore(bp, bm);
	}
    }


    /**
     * Send the pusher to the specified location.
     * Use a timer to move one position each time.
     * If the timer is already running.
     * @param x location to go to
     * @param y location to go to
     */
    private void animateTo(int x, int y) {
	if (timer == null)
	    timer = new Timer();

	if (timertask != null) {
	    timertask.cancel();
	    timertask = null;
	}
	timertask = new PushPuzzleTimer(this, board, x, y);
	timer.schedule(timertask, (long)100, (long)100);
    }

    /**
     * Cancel the animation.
     */
    private void cancelTo() {
	if (timertask != null)
	    timertask.cancel();
    }

    /**
     * Called when the pointer is pressed.
     * @param x location in the Canvas
     * @param y location in the Canvas
     */
    protected void pointerPressed(int x, int y) {
        animateTo(x/cell, y/cell);
    }

    /**
     * Add a listener to notify when the level is solved.
     * The listener is send a List.SELECT_COMMAND when the
     * level is solved.
     * @param l the object implementing interface CommandListener
     */
    public void setCommandListener(CommandListener l) {
	super.setCommandListener(l);
        listener = l;
    }

    /**
     * Queue a repaint for a area around the specified location.
     * @param loc an encoded location from Board.getPusherLocation
     * @param dir that the pusher moved and flag if it pushed a packet
     */
    void repaintNear(int loc, int dir) {
	int x = loc & 0x7fff;
	int y = (loc >> 16) & 0x7fff;
	// Compute number of cells that need to be drawn
	int size = 1;
	if (dir >= 0) {
	    size += 1;
	    if ((dir & Board.MOVEPACKET) != 0) {
		size += 1;
	    }
	}
	int dx = 1;
	int dy = 1;
	switch (dir & 3) {
	case Board.UP:
	    y -= size-1;
	    dy = size;
	    break;
	case Board.DOWN:
	    dy = size;
	    break;
	case Board.RIGHT:
	    dx = size;
	    break;
	case Board.LEFT:
	    x -= size-1;
	    dx = size;
	    break;
	}
	repaint(x * cell, y * cell, dx * cell, dy * cell);
    }

    /**
     * Paint the contents of the Canvas.
     * The clip rectangle of the canvas is retrieved and used
     * to determine which cells of the board should be repainted.
     * @param g Graphics context to paint to.
     */
    protected void paint(Graphics g) {
	// Lock the board to keep it from changing during paint
	synchronized (board) {

	    int x = 0, y =  0, x2 = bwidth, y2 = bheight;

	    // Figure what part needs to be repainted.
	    int clipx = g.getClipX();
	    int clipy = g.getClipY();
	    int clipw = g.getClipWidth();
	    int cliph = g.getClipHeight();
	    x = clipx / cell;
	    y = clipy / cell;
	    x2 = (clipx + clipw + cell-1) / cell;
	    y2 = (clipy + cliph + cell-1) / cell;
	    if (x2 > bwidth)
		x2 = bwidth;
	    if (y2 > bheight)
		y2 = bheight;

	    // Fill entire area with ground color
	    g.setColor(groundColor);
	    g.fillRect(0, 0, w, h);

	    for (y = 0; y < y2; y++) {
		for (x = 0; x < x2; x++) {
		    byte v = board.get(x, y);
		    switch (v & ~Board.PUSHER) {
		    case Board.WALL:
			g.setColor(wallColor);
			g.fillRect(x*cell, y*cell, cell, cell);
			break;

		    case Board.PACKET:
		    case Board.PACKET | Board.STORE:
			g.setColor(packetColor);
			g.fillRect(x*cell+1, y*cell+1, cell-2, cell-2);
			break;
		    case Board.STORE:
			g.setColor(storeColor);
                        if (useDottedLine) {
                            g.setStrokeStyle(Graphics.DOTTED);
                        }
			g.drawRect(x*cell+1, y*cell+1, cell-2, cell-2);
			break;

		    case Board.GROUND:
		    default:
			// Noop since already filled.
			break;
		    }
		    if ((v & Board.PUSHER) != 0) {
			g.setColor(pusherColor);
			g.fillArc(x*cell, y*cell, cell, cell, 0, 360);
		    }
		}
	    }
	    g.drawString("PushPuzzle Level " + level, 0, h-14,
			 Graphics.TOP|Graphics.LEFT);
	}
    }
}
