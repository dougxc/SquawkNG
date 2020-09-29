/*
 * @(#)PushPuzzleTimer.java	1.11 01/08/30
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

import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to be the timer used to make the moves in "run" mode.
 */
class PushPuzzleTimer extends TimerTask {
    int x, y;
    Board board;
    PushPuzzleCanvas canvas;

    /**
     * Create a new timer with the canvas, board and starting
     * location.
     */
    PushPuzzleTimer(PushPuzzleCanvas c, Board b, int x, int y) {
	canvas = c;
	board = b;
	this.x = x;
	this.y = y;
    }

    /**
     * Called each time the TimerTask should run.
     * The method tries to move toward the original goal
     * if it can.  If not, it cancels the animation.
     */
    public void run() {
	try {
	    int pos = board.getPusherLocation();
	    int dir = board.runTo(x, y, 1);
	    if (dir < 0) {
		cancel();	// If can't get there cancel the animation
	    } else {
		canvas.repaintNear(pos, dir);
	    }
	} catch (Exception ex) {
	    // Ignore and exit
	}
    }
}
