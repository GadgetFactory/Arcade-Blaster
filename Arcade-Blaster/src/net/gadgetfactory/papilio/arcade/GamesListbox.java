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

import static net.gadgetfactory.papilio.arcade.PapilioArcade.currTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.List;
import javax.swing.JComponent;
import net.gadgetfactory.shared.UIHelpers;

public class GamesListbox extends JComponent
{
	private final int IMAGE_PADDING = 3;
	private final double RATIO_TOP_IMAGE = 0.32;
	private final double RATIO_MAIN_IMAGE = 0.36;
//	private final double RATIO_BOTTOM_IMAGE = 0.32;
	// RATIO_TOP_IMAGE + RATIO_CENTER_IMAGE + RATIO_BOTTOM_IMAGE should be 1

	private Path2D.Double shapedListbox = new Path2D.Double();
	private List<GamesListbox.GameStruct> alGamesInfo;

	// Box blur
	private float[] blurKernel = {0.1111111F, 0.1111111F, 0.1111111F, 
								  0.1111111F, 0.1111111F, 0.1111111F, 
								  0.1111111F, 0.1111111F, 0.1111111F};
	// Gaussian blur
//	private float[] blurKernel = {0.000F, 0.013F, 0.000F, 
//								  0.013F, 1.197F, 0.013F, 
//								  0.000F, 0.013F, 0.000F};

	private BufferedImageOp blurOp = 
			new ConvolveOp(new Kernel(3, 3, blurKernel), 
						   ConvolveOp.EDGE_NO_OP, 
						   null);
	private BufferedImage biTopBlurred, biBottomBlurred;

	private boolean bNoGameInstalled = false;
	private int selGameIndex;	// must be between [1, alGamesInfo.size() - 2] inclusive
	private int noOfGames;		// Number of elements in alGamesInfo
	private int listboxWidth, listboxHeight;

	public boolean hasNoGames() {
		return bNoGameInstalled;
	}

	public String getSelectedGameId() {
		return alGamesInfo.get(selGameIndex).getId();
	}
	
	public String getSelectedGameName() {
		return alGamesInfo.get(selGameIndex).getName();
	}

	// Assumption: alGamesInfo is set to a valid object and noOfGames = alGamesInfo.size().
	public void setSelectedGameIndex(int newGameIndex) {
		if ((newGameIndex < 1) || (newGameIndex > noOfGames - 2))
		// => Invalid game index passed.
			selGameIndex = 1;	// reset
		else
			selGameIndex = newGameIndex;
		
// FIXME: Handle case when alGamesInfo.get(%index%).getLogo() = null (when %id%.png is absent)
		// Create top blurred image.
		BufferedImage biTemp = alGamesInfo.get(selGameIndex - 1).getLogo();
		biTopBlurred = 
			UIHelpers.EmptyCompatibleImage(biTemp.getWidth(), 
												 biTemp.getHeight(), 
												 biTemp.getColorModel().getTransparency());
		blurOp.filter(biTemp, biTopBlurred);
		biTopBlurred = blurOp.filter(biTopBlurred, null);
		biTopBlurred = blurOp.filter(biTopBlurred, null);
		
		// Create bottom blurred image.
		// TODO: NULL pointer exception occurs here is there are no
		// logo and screenshot images available for a game 
		biTemp = alGamesInfo.get(selGameIndex + 1).getLogo();
		biBottomBlurred = 
			UIHelpers.EmptyCompatibleImage(biTemp.getWidth(), 
												 biTemp.getHeight(), 
												 biTemp.getColorModel().getTransparency());
		blurOp.filter(biTemp, biBottomBlurred);
		biBottomBlurred = blurOp.filter(biBottomBlurred, null);
		biBottomBlurred = blurOp.filter(biBottomBlurred, null);
	}

	public GamesListbox(List<GamesListbox.GameStruct> gamesArrayList)
	{
		listboxWidth = currTheme.getListboxWidth();
		listboxHeight = currTheme.getListboxHeight();
		
		shapedListbox = currTheme.getListboxPath();
		shapedListbox.transform(
				AffineTransform.getTranslateInstance(-currTheme.getListboxLeft(), 
													 -currTheme.getListboxTop()));

		this.setLocation(currTheme.getListboxLeft(), currTheme.getListboxTop());
		this.setSize(listboxWidth, listboxHeight);
		this.setOpaque(false);
		// No question of removing borders of around JList because it does not have any(!?)
		// unless present in a scroll pane.

		alGamesInfo = gamesArrayList;
		if (alGamesInfo.size() == 1) {
			bNoGameInstalled = true;
			alGamesInfo.add(new GameStruct("",	"Install Hint", InstallHintMonogram()));
		}
		alGamesInfo.add(new GameStruct("",	"Bottom Filler", alGamesInfo.get(0).getLogo()));
		noOfGames = alGamesInfo.size();
		setSelectedGameIndex(1);		// Position is important!
	}
	
	// Used only by GamesListbox constructor
	private BufferedImage InstallHintMonogram() {
		Font ft = new Font(Font.DIALOG, Font.PLAIN, 16);
		BufferedImage biHintMonogram = 
				UIHelpers.EmptyCompatibleImage(currTheme.getMonogramWidth(), 
											   currTheme.getMonogramHeight());
		Graphics2D g2d = biHintMonogram.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, currTheme.getMonogramWidth(), currTheme.getMonogramHeight());
		g2d.setFont(ft);
		g2d.setColor(Color.BLACK);
		g2d.drawString("Install Games", 15, 25);

		g2d.dispose();
		return biHintMonogram;
	}

	/**
	 * Since games JList does not have any children (list items are not considered as its
	 * children) and we are not interested in letting Java2D to draw its borders, the 
	 * paintBorder() and paintChildren() would be superfluous. So, instead of overriding
	 * paintComponent() - the "Swing way", we override paint() which results in wee bit
	 * improvement.
	 */
    @Override
	public void paint(Graphics g)
    {
    	GameStruct udtGameInfo;	BufferedImage biLogo;
    	int topImageHeight = (int) Math.ceil(RATIO_TOP_IMAGE * listboxHeight);		// Round up
    	int mainImageHeight = (int) Math.ceil(RATIO_MAIN_IMAGE * listboxHeight);	// Round up
//    	double bottomImageHeight = RATIO_BOTTOM_IMAGE * listboxHeight;
    	int yImage, ySelectionWindowTop;
//    	super.paint(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		System.out.println("GamesListbox : paint()\t" + g2d.getClip().getBounds().toString());

		g2d.setClip(shapedListbox);
		
		// Paint image of game prior to selected game.
		udtGameInfo = alGamesInfo.get(selGameIndex - 1);
		biLogo = udtGameInfo.getLogo();
		yImage = topImageHeight - biLogo.getHeight();// - IMAGE_PADDING;
//		System.out.println("Top Image Y : " + yImage);
		g2d.drawImage(biTopBlurred, 4, yImage, null);
		
		// Paint selected (main) game image
		yImage = topImageHeight + IMAGE_PADDING;
//		System.out.println("Main Image Y : " + yImage);
		udtGameInfo = alGamesInfo.get(selGameIndex);
		biLogo = udtGameInfo.getLogo();
		g2d.drawImage(biLogo, 4, yImage, null);

		// Paint image of game next to selected game.
		yImage += topImageHeight + IMAGE_PADDING;
//		System.out.println("Bottom Image Y : " + yImage);
		udtGameInfo = alGamesInfo.get(selGameIndex + 1);
		biLogo = udtGameInfo.getLogo();
		g2d.drawImage(biBottomBlurred, 4, yImage, null);

		// Draw selection window
		g2d.setColor(new Color(179, 9, 4)); // 705872
		g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
		ySelectionWindowTop = topImageHeight - IMAGE_PADDING/2;
		g2d.drawRect(0, ySelectionWindowTop, listboxWidth, topImageHeight + IMAGE_PADDING/2);
		
//		g2d.drawLine(0, ySelectionWindowTop, listboxWidth, ySelectionWindowTop);
//		g2d.drawLine(listboxWidth - 1, ySelectionWindowTop, listboxWidth - 1, ySelectionWindowBottom);
//		g2d.drawLine(0, ySelectionWindowBottom, listboxWidth, ySelectionWindowBottom);
    }

    @Override
    public boolean contains(int x, int y) {
        return shapedListbox.contains(x, y);
    }
    
	
	public boolean ScrollUp()
	{
		// The case when there are no games installed has no effect in scrolling operation.
		
		// If currently selected game is penultimate (which => immediately next game 
		// is bottom filler), then there is no question of scrolling up.
		if (selGameIndex == (noOfGames - 2))
			return false;

		setSelectedGameIndex(selGameIndex + 1);
		this.repaint();	// Will it be beneficial to call paintImmediately()?
		return true;
	}

	public boolean ScrollDown()
	{
		// The case when there are no games installed has no effect in scrolling operation.

		// If currently selected game is first (which => immediately previuos game 
		// is top filler), then there is no question of scrolling down.
		if (selGameIndex == 1)
			return false;
		
		setSelectedGameIndex(selGameIndex - 1);
		this.repaint();	// Will it be beneficial to call paintImmediately()?
		return true;
	}

	
	public void AddInstalledGame(int installedGameIndex, GameStruct recInstalledGame)
	{
		int actualGameIndex;

		if (bNoGameInstalled) {
		// => User has installed his very first game.
			// Replace the "Install Hint" entry (at index = 1) in alGamesInfo 
			// with recInstalledGame.
			alGamesInfo.set(1, recInstalledGame);
			// No question of setting value of noOfGames as no entries were added/removed

			actualGameIndex = 1;
			bNoGameInstalled = false;
		}
		else {
			if (installedGameIndex <= (noOfGames - 2)) {
				actualGameIndex = installedGameIndex;
				alGamesInfo.add(actualGameIndex, recInstalledGame);
			}
			else {
			// => The ordinal index, of installed game, in Games.xml as related to entries
			//	  in alGamesInfo is greater than that of penultimate entry in alGamesInfo.
			// => User has installed the games in random order (as for Games.xml).

				// We need to ignore installedGameIndex and add the entry for installed 
				// game as penultimate entry in alGamesInfo - because last entry is filler.
				actualGameIndex = alGamesInfo.size() - 2;	// Do not use noOfGames here
				alGamesInfo.add(actualGameIndex, recInstalledGame);
			}
			noOfGames = alGamesInfo.size();
		}

		setSelectedGameIndex(actualGameIndex);
		this.repaint();
	}
	
	
    public static class GameStruct
    {
    	private String m_id, m_name;
    	private BufferedImage m_biLogo;
    	
    	public GameStruct(String id, String name, BufferedImage biLogo) {
    		m_id = id;
    		m_name = name;
    		m_biLogo = biLogo;
    	}

		public String getId() {
			return this.m_id;
		}

		public String getName() {
			return this.m_name;
		}
		
		public BufferedImage getLogo() {
			return this.m_biLogo;
		}
    }
    
}
