/*
  Part of the Papilio Arcade Blaster

  Copyright (c) 2010-12 GadgetFactory LLC

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package net.gadgetfactory.papilio.arcade;

import java.awt.Point;
import java.awt.geom.Path2D;

public class ArcadeTheme
{
	public static final int UP_BUTTON = 0;
	public static final int DOWN_BUTTON = 1;
	public static final int LOAD_GAME_BUTTON = 2;
	public static final int HOW_TO_PLAY_BUTTON = 3;
	public static final int INSTALL_GAME_BUTTON = 4;
	public static final int PREFERENCES_BUTTON = 5;
	public static final int UPDATE_BUTTON = 6;
	public static final int EXIT_BUTTON = 7;
	public static final int HELP_BUTTON = 8;

	public static final String UP_BUTTON_ACTION = "Up";
	public static final String DOWN_BUTTON_ACTION = "Down";
	public static final String LOAD_GAME_BUTTON_ACTION = "Load";
	public static final String HOW_TO_PLAY_BUTTON_ACTION = "HowTo";
	public static final String INSTALL_GAME_BUTTON_ACTION = "Install";
	public static final String PREFERENCES_BUTTON_ACTION = "Preferences";
	public static final String UPDATE_BUTTON_ACTION = "Update";
	public static final String EXIT_BUTTON_ACTION = "Exit";
	public static final String HELP_BUTTON_ACTION = "Help";

	private final int SCREENSHOT_LEFT = 352;
	private final int SCREENSHOT_TOP = 180;
	// TODO: Calculate SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT from bounding rectangle of shapedScreenshot
	private final int SCREENSHOT_WIDTH = 205;
	private final int SCREENSHOT_HEIGHT = 205;

	private final int LISTBOX_LEFT = 180;		// Left of bounding rectangle
	private final int LISTBOX_TOP = 191;		// Top of bounding rectangle
	private final int LISTBOX_WIDTH = 129;		// Width of bounding rectangle
	private final int LISTBOX_HEIGHT = 195;		// Height of bounding rectangle

	private final int MONOGRAM_WIDTH = 125;
	private final int MONOGRAM_HEIGHT = 57;

	private final int NO_OF_BUTTONS = 9;
	private final Point[] buttonLocations = new Point[NO_OF_BUTTONS];

	private Path2D.Double shapedScreenshot = new Path2D.Double();	// Screenshot window
	private Path2D.Double shapedListbox = new Path2D.Double();		// Games Listbox
	private Path2D.Double shapedOutline = new Path2D.Double();		// Window Outline

	public Path2D.Double getScreenshotPath() {
		return this.shapedScreenshot;
	}

	public Path2D.Double getListboxPath() {
		return this.shapedListbox;
	}

	public Path2D.Double getWindowPath() {
		return this.shapedOutline;
	}

	public int getScreenshotLeft() {
		return SCREENSHOT_LEFT;
	}

	public int getScreenshotTop() {
		return SCREENSHOT_TOP;
	}

	public int getScreenshotWidth() {
		return SCREENSHOT_WIDTH;
	}

	public int getScreenshotHeight() {
		return SCREENSHOT_HEIGHT;
	}

	public int getListboxLeft() {
		return LISTBOX_LEFT;
	}

	public int getListboxTop() {
		return LISTBOX_TOP;
	}

	public int getListboxWidth() {
		return LISTBOX_WIDTH;
	}

	public int getListboxHeight() {
		return LISTBOX_HEIGHT;
	}

	public int getMonogramWidth() {
		return MONOGRAM_WIDTH;
	}

	public int getMonogramHeight() {
		return MONOGRAM_HEIGHT;
	}

	/**
	 * buttonIndex must be one of XXX_BUTTON constants
	 */
	public Point getButton(int buttonIndex) {
		return this.buttonLocations[buttonIndex];
	}
	
	public ArcadeTheme()
	{
		// Construct path for Screenshot window
		shapedScreenshot.moveTo(352.0, 180.0);
		shapedScreenshot.lineTo(352.0, 385.0);
		shapedScreenshot.lineTo(557.0, 385.0);
		shapedScreenshot.lineTo(557.0, 180.0);

		// Construct path for GamesListbox
		shapedListbox.moveTo(181.0, 191.0);
		shapedListbox.lineTo(181.0, 398.0);
		shapedListbox.lineTo(311.0, 398.0);
		shapedListbox.lineTo(311.0, 191.0);

		// Construct path for application main window
		shapedOutline.moveTo(154.0, 170.0);
		shapedOutline.quadTo(178.0, 136.0, 201.0, 124.0);
		shapedOutline.quadTo(276.0, 80.0, 334.0, 91.0);
		shapedOutline.quadTo(351.0, 95.0, 362.0, 119.0);
		shapedOutline.quadTo(375.0, 147.0, 420.0, 127.0);
		shapedOutline.quadTo(427.0, 106.0, 447.0, 106.0);
		shapedOutline.quadTo(466.0, 106.0, 475.0, 124.0);
		shapedOutline.quadTo(532.0, 147.0, 558.0, 117.0);
		shapedOutline.quadTo(568.0, 105.0, 577.0, 93.0);
		shapedOutline.quadTo(594.0, 79.0, 629.0, 88.0);
		shapedOutline.quadTo(647.0, 91.0, 670.0, 101.0);
		shapedOutline.quadTo(740.0, 127.0, 782.0, 232.0);
		shapedOutline.quadTo(853.0, 430.0, 839.0, 558.0);
		shapedOutline.quadTo(827.0, 631.0, 741.0, 606.0);
		shapedOutline.quadTo(686.0, 567.0, 645.0, 519.0);
		shapedOutline.quadTo(597.0, 446.0, 546.0, 431.0);
		shapedOutline.quadTo(463.0, 419.0, 364.0, 430.0);
		shapedOutline.quadTo(329.0, 441.0, 304.0, 464.0);
		shapedOutline.quadTo(283.0, 493.0, 245.0, 531.0);
		shapedOutline.quadTo(196.0, 595.0, 152.0, 610.0);
		shapedOutline.quadTo(79.0, 629.0, 66.0, 554.0);
		shapedOutline.quadTo(57.0, 498.0, 70.0, 426.0);
		shapedOutline.quadTo(88.0, 338.0, 109.0, 267.0);
		shapedOutline.quadTo(134.0, 205.0, 154.0, 170.0);

		// Intitialize locations of buttons.
    	buttonLocations[UP_BUTTON] = new Point(202, 135);
		buttonLocations[DOWN_BUTTON] = new Point(195, 397);
		buttonLocations[LOAD_GAME_BUTTON] = new Point(578, 232);
		buttonLocations[HOW_TO_PLAY_BUTTON] = new Point(644, 154);
		buttonLocations[INSTALL_GAME_BUTTON] = new Point(680, 313);
		buttonLocations[PREFERENCES_BUTTON] = new Point(584, 351);
		buttonLocations[UPDATE_BUTTON] = new Point(701, 223);
		buttonLocations[EXIT_BUTTON] = new Point(421, 108);
		buttonLocations[HELP_BUTTON] = new Point(584, 128);
	}

}
