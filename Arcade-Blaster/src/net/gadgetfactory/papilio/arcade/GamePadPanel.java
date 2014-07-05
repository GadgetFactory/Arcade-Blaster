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

import static net.gadgetfactory.papilio.arcade.PapilioArcade.AppPath;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.GAMEPAD_IMAGE_HEIGHT;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.GAMEPAD_IMAGE_WIDTH;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.currTheme;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.programSettings;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.romsFolder;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.runningonWindows;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.gadgetfactory.shared.HelperFunctions;
import net.gadgetfactory.shared.UIHelpers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GamePadPanel extends JPanel implements ActionListener
{
	private final PapilioArcade parentFrame;
	private FileNameExtensionFilter zipFileFilter = new FileNameExtensionFilter("Zip files", "zip");
    private GamesListbox lstGames;
    private HowtoPlayDialog dlgHowtoPlay;
    private PreferencesDialog dlgPreferences;
    private UpdateDialog dlgUpdate;

    private ImageButton btnUp = new ImageButton("images/up-button.gif", 
				  								"images/up-button-down.gif", 
				  								"images/up-button-highlighted.gif");
    private ImageButton btnDown = new ImageButton("images/down-button.gif", 
			   									  "images/down-button-down.gif", 
	   											  "images/down-button-highlighted.gif");
    private ImageButton btnInstall = new ImageButton("images/install-button.gif", 
				  								  "images/install-button-down.gif", 
				  								  "images/install-button-highlighted.gif");
    private ImageButton btnHowToPlay = new ImageButton("images/how-to-play-button.gif", 
			  										   "images/how-to-play-button-down.gif", 
			  										   "images/how-to-play-button-highlighted.gif");
    private ImageButton btnLoadGame = new ImageButton("images/load-game-button.gif", 
			  										  "images/load-game-button-down.gif", 
			  										  "images/load-game-button-highlighted.gif");
    private ImageButton btnPreferences = new ImageButton("images/prefs-button.gif", 
														 "images/prefs-button-down.gif", 
														 "images/prefs-button-highlighted.gif");
    private ImageButton btnUpdate = new ImageButton("images/update-button.gif", 
											  		"images/update-button-down.gif", 
											  		"images/update-button-highlighted.gif");
    private ImageButton btnHelp = new ImageButton("images/help-button.gif", 
												  "images/help-button-down.gif", 
												  "images/help-button-highlighted.gif");    
    private ImageButton btnExit = new ImageButton("images/exit-button.gif", 
	  											  "images/exit-button-down.gif", 
	  											  "images/exit-button-highlighted.gif");

	private Path2D.Double shapedScreenshot;
    private BufferedImage biGamePad, biScreenshot;
    private final File screenshotsPath;
    private final File monogramsPath;
    private boolean bLoadScreenshot;
    private int scaledScrLeft, scaledScrTop, scaledScrWidth, scaledScrHeight;
    private int screenshotWidth, screenshotHeight;

    private final File imagesFolder;
    private Document docGameXML;
    private Document docPlatformXML;
    private Document docHardwareXML;

    private boolean bReinstalledGame;
    private String installedGameId, installedGameName;
    private int installedGameIndex;

	private String selJoystickId;

    public ImageButton getLoadGameButton() {
		return this.btnLoadGame;
	}

	public void setSelectedJoystickId(String newJoystickId) {
		this.selJoystickId = newJoystickId;
	}

	public GamePadPanel(PapilioArcade fraMain, 
						Document docGameXML, Document docPlatformXML, Document docHardwareXML, 
						File imagesFolder)
    {
		List<GamesListbox.GameStruct> alGamesInfo = new ArrayList<GamesListbox.GameStruct>(25);

		this.shapedScreenshot = currTheme.getScreenshotPath();
		this.docGameXML = docGameXML;
		this.docPlatformXML = docPlatformXML;
		this.docHardwareXML = docHardwareXML;
		this.imagesFolder = imagesFolder;

		bLoadScreenshot = true;
    	parentFrame = fraMain;
    	screenshotsPath = new File(imagesFolder, "screenshots");
    	monogramsPath = new File(imagesFolder, "monograms");
		biGamePad = UIHelpers.LoadBufferedImage(new File(imagesFolder, "gamepad.png"));
		screenshotWidth = currTheme.getScreenshotWidth();
		screenshotHeight = currTheme.getScreenshotHeight();

		this.setPreferredSize(new Dimension(GAMEPAD_IMAGE_WIDTH, GAMEPAD_IMAGE_HEIGHT));
		this.setLayout(null);
    	GamePadMouseAdapter ma = new GamePadMouseAdapter();
		this.addMouseListener(ma);
		this.addMouseMotionListener(ma);

		PopulateGamesList(alGamesInfo);
		lstGames = new GamesListbox(alGamesInfo);
		this.add(lstGames);
		
		btnUp.setLocation(currTheme.getButton(ArcadeTheme.UP_BUTTON));
		btnUp.setActionCommand(ArcadeTheme.UP_BUTTON_ACTION);
        btnUp.addActionListener(this);
		this.add(btnUp);
		
        btnDown.setLocation(currTheme.getButton(ArcadeTheme.DOWN_BUTTON));
        btnDown.setActionCommand(ArcadeTheme.DOWN_BUTTON_ACTION);
        btnDown.addActionListener(this);
        this.add(btnDown);

        btnLoadGame.setLocation(currTheme.getButton(ArcadeTheme.LOAD_GAME_BUTTON));
        btnLoadGame.setActionCommand(ArcadeTheme.LOAD_GAME_BUTTON_ACTION);
        btnLoadGame.addActionListener(this);
        this.add(btnLoadGame);

        btnHowToPlay.setLocation(currTheme.getButton(ArcadeTheme.HOW_TO_PLAY_BUTTON));
        btnHowToPlay.setActionCommand(ArcadeTheme.HOW_TO_PLAY_BUTTON_ACTION);
        btnHowToPlay.addActionListener(this);
        this.add(btnHowToPlay);

        btnInstall.setLocation(currTheme.getButton(ArcadeTheme.INSTALL_GAME_BUTTON));
        btnInstall.setActionCommand(ArcadeTheme.INSTALL_GAME_BUTTON_ACTION);
        btnInstall.addActionListener(this);
        this.add(btnInstall);

        btnPreferences.setLocation(currTheme.getButton(ArcadeTheme.PREFERENCES_BUTTON));
        btnPreferences.setActionCommand(ArcadeTheme.PREFERENCES_BUTTON_ACTION);
        btnPreferences.addActionListener(this);
        this.add(btnPreferences);

        btnUpdate.setLocation(currTheme.getButton(ArcadeTheme.UPDATE_BUTTON));
        btnUpdate.setActionCommand(ArcadeTheme.UPDATE_BUTTON_ACTION);
        btnUpdate.addActionListener(this);
        this.add(btnUpdate);

        btnHelp.setLocation(currTheme.getButton(ArcadeTheme.HELP_BUTTON));
        btnHelp.setActionCommand(ArcadeTheme.HELP_BUTTON_ACTION);
        btnHelp.addActionListener(this);
        this.add(btnHelp);
        
        btnExit.setLocation(currTheme.getButton(ArcadeTheme.EXIT_BUTTON));
        btnExit.setActionCommand(ArcadeTheme.EXIT_BUTTON_ACTION);
        btnExit.addActionListener(this);
        this.add(btnExit);
        
        // Keyboard actionlistener
        ActionListener kbdActionListener = new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent actionEvent) {
            	DoCommand(actionEvent);
            }
        };
        this.registerKeyboardAction(kbdActionListener, ArcadeTheme.HELP_BUTTON_ACTION,
        		KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_FOCUSED);
        this.registerKeyboardAction(kbdActionListener, ArcadeTheme.UP_BUTTON_ACTION,
        		KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_FOCUSED);        
        this.registerKeyboardAction(kbdActionListener, ArcadeTheme.DOWN_BUTTON_ACTION,
        		KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_FOCUSED);
        this.registerKeyboardAction(kbdActionListener, ArcadeTheme.HOW_TO_PLAY_BUTTON_ACTION,
        		KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0), JComponent.WHEN_FOCUSED);        
        this.registerKeyboardAction(kbdActionListener, ArcadeTheme.HOW_TO_PLAY_BUTTON_ACTION,
        		KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.SHIFT_DOWN_MASK), JComponent.WHEN_FOCUSED);        
        this.registerKeyboardAction(kbdActionListener, ArcadeTheme.LOAD_GAME_BUTTON_ACTION,
        		KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), JComponent.WHEN_FOCUSED); 
        }
    
	// Used by GamePadPanel constructor only
	private void PopulateGamesList(List<GamesListbox.GameStruct> alGamesInfo)
	{
		NodeList gameList = docGameXML.getElementsByTagName("game");
		Element elmGame; File logoImageFile; BufferedImage biLogo;
		int i; String id;
		int nGameFiles = 0;
		
		alGamesInfo.add(new GamesListbox.GameStruct("", "Top Filler", 
								UIHelpers.EmptyCompatibleImage(currTheme.getMonogramWidth(), 
										   					   currTheme.getMonogramHeight())));
		for (i = 0; i < gameList.getLength(); i++) {
			elmGame = (Element) gameList.item(i);
			id = elmGame.getAttribute("id");
			if (!id.isEmpty()) {
				File gamesDirectory = new File(romsFolder, id);
				if (gamesDirectory.isDirectory()) {
					// TODO: Need some more validation to determine if the game is installed
					// Need to check at least one complete set of files is installed in these folders
					// Checking every file will be time consuming as number of installed games increase.
					// Maybe just the number of files (as per number of <rom> nodes in the primary
					// fileset element), actual game load will validate the file.
					// Number of files equal to or more than the number of <rom> nodes is ok as it may have
					// additional copyright/readme files
					
					// TODO: Shouldn't we move this code to game Hashmap creation?
					NodeList elmGameFiles = elmGame.getElementsByTagName("fileset");
					int j;
					int nGameFilesets = elmGameFiles.getLength();
					for (j = 0; j < nGameFilesets; j++)
					{
						Element fileset;
						fileset = (Element) elmGameFiles.item(j);
						if (fileset.getAttribute("primary").equalsIgnoreCase("true"))
						{
							NodeList elmRomfiles = fileset.getElementsByTagName("rom");
							nGameFiles = elmRomfiles.getLength();
						}
					}
					
					int nFolderFiles = gamesDirectory.listFiles().length;
					if (nFolderFiles >= nGameFiles)
					{
						biLogo = null;
						logoImageFile = new File(monogramsPath, id + ".png");
						if (logoImageFile.isFile())
							biLogo = UIHelpers.LoadBufferedImage(logoImageFile);
						else
							biLogo = UIHelpers.LoadBufferedImage(new File(monogramsPath, "nologo.png"));
						
						alGamesInfo.add( 
								new GamesListbox.GameStruct(id, 
										elmGame.getAttribute("name"), 
										biLogo));
					}
				}
			}
		}
	}


	public void Cleanup() {
		if (dlgHowtoPlay != null)
			dlgHowtoPlay.dispose();
		// dlgPreferences is disposed automatically. 
	}
	
	
    @Override
	public void paintComponent(Graphics g)
    {
		Graphics2D g2d = (Graphics2D) g;
//		System.out.println("GamePadPanel : paintComponent\t" + g2d.getClip().getBounds());

		// Use anti-aliasing
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Paint gamepad.png canvas.
		g2d.drawImage(biGamePad, 0, 0, null);
		
		// Next, paint screenshot image for currently selected game in shaped window.

		if (bLoadScreenshot) {
		// => Either program has just been loaded
		//	  Or user has changed selected game in Games listbox
		//	  Or user has installed a new game
			bLoadScreenshot = false;
			LoadScreenshotImage();
		}

		if (biScreenshot != null) {
			Shape correctClip = g2d.getClip();
			g2d.setClip(shapedScreenshot);
			g2d.drawImage(biScreenshot, 
						  scaledScrLeft, scaledScrTop, scaledScrWidth, scaledScrHeight, 
						  null);
			g2d.setClip(correctClip);
		}
		
		// No need to call super.paintComponent() as entire client area Gamepad panel will
		// be overwritten with gamepad.png. In other words, we are completely responsible 
		// for drawing the client area.
    }

	// Used by paintComponent() only
	private void LoadScreenshotImage()
	{
		int imageWidth, imageHeight;
		double aspectRatio;

		if (lstGames.hasNoGames()) {
			biScreenshot = InstallHintScreenshot();

			scaledScrLeft = currTheme.getScreenshotLeft();
			scaledScrTop = currTheme.getScreenshotTop();
			scaledScrHeight = screenshotHeight;
			scaledScrWidth = screenshotWidth;
			return;
		}

		File screenshotFile = new File(screenshotsPath, lstGames.getSelectedGameId() + ".png");
		if (screenshotFile.isFile()) {
			biScreenshot = UIHelpers.LoadBufferedImage(screenshotFile);
		}
		else {
			// TODO: Create "Screenshot Not Available" image in memory or load it from .jar file
			biScreenshot = UIHelpers.LoadBufferedImage(new File(screenshotsPath, "noscreenshot.png"));
		}
			
		imageWidth = biScreenshot.getWidth();
		imageHeight = biScreenshot.getHeight();
		aspectRatio = imageWidth / (double) imageHeight;
		scaledScrLeft = currTheme.getScreenshotLeft();
		scaledScrTop = currTheme.getScreenshotTop();
		
		if (aspectRatio < 1) {
		// => screenshot PNG image is in portrait mode.
			// Scale the PNG image such that its height becomes = Screenshot window height
			scaledScrHeight = screenshotHeight;
			scaledScrWidth = (int) Math.ceil(aspectRatio * screenshotHeight);
			scaledScrLeft += (screenshotWidth - scaledScrWidth) / 2;
		}
		else {
		// => screenshot PNG image is in landscape mode.
			// Scale the PNG image such that its width becomes = Screenshot window width
			scaledScrWidth = screenshotWidth;
			scaledScrHeight = (int) Math.ceil(screenshotWidth / (double) aspectRatio);
			scaledScrTop += (screenshotHeight - scaledScrHeight) / 2;
		}
	}

	// Used only by LoadScreenshotImage() only
	private BufferedImage InstallHintScreenshot() {
		Font ft = new Font(Font.DIALOG, Font.PLAIN, 16);
		BufferedImage biHintScreenshot = 
				UIHelpers.EmptyCompatibleImage(screenshotWidth, screenshotHeight);
		Graphics2D g2d = biHintScreenshot.createGraphics();
		
		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, screenshotWidth, screenshotHeight);
		g2d.setFont(ft);
		g2d.setColor(Color.BLACK);
		g2d.drawString("Install Games", 15, 25);

		g2d.dispose();
		return biHintScreenshot;
	}

	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		DoCommand(e);
	}
	
	public void DoCommand(ActionEvent e)
	{
		String currAction = e.getActionCommand();

		if (currAction.equals(ArcadeTheme.UP_BUTTON_ACTION)) {
			if (lstGames.ScrollDown()) {
				bLoadScreenshot = true;		// Position is important!
				this.repaint();
			}
		}

		else if (currAction.equals(ArcadeTheme.DOWN_BUTTON_ACTION)) {
			if (lstGames.ScrollUp()) {
				bLoadScreenshot = true;		// Position is important!
				this.repaint();
			}
		}
		
		else if (currAction.equals(ArcadeTheme.LOAD_GAME_BUTTON_ACTION)) {
			if (lstGames.hasNoGames())
				JOptionPane.showMessageDialog(this, "You need to install at least " + 
						  "one game before trying to load.", 
						  "No Game Installed", JOptionPane.WARNING_MESSAGE);
			else
	        	parentFrame.BurnUsingProgrammer(lstGames.getSelectedGameId(), 
	        									lstGames.getSelectedGameName());
		}
		
		else if (currAction.equals(ArcadeTheme.HOW_TO_PLAY_BUTTON_ACTION)) {
			if (lstGames.hasNoGames())
				JOptionPane.showMessageDialog(this, "No games are installed", 
						  "No Game Installed", JOptionPane.WARNING_MESSAGE);
			else {
				if (dlgHowtoPlay == null)
					dlgHowtoPlay = new HowtoPlayDialog(parentFrame);
				
	        	dlgHowtoPlay.PopulateNShow(lstGames.getSelectedGameId(), 
	        							   lstGames.getSelectedGameName(), 
	        							   selJoystickId);
			}
		}

		else if (currAction.equals(ArcadeTheme.INSTALL_GAME_BUTTON_ACTION)) {
			File romZipFile = 
				UIHelpers.ShowFileOpen("Open Zip file containing game rom files", 
											 zipFileFilter, ".zip", 
											 null, null);
			if (romZipFile != null) {
				if (InstallGame(romZipFile)) {
					if (!bReinstalledGame) {
						File logoImageFile; BufferedImage biLogo = null;
						logoImageFile = new File(monogramsPath, installedGameId + ".png");
						if (logoImageFile.isFile())
							biLogo = UIHelpers.LoadBufferedImage(logoImageFile);
						else
							biLogo = UIHelpers.LoadBufferedImage(new File(monogramsPath, "nologo.png"));
						lstGames.AddInstalledGame(installedGameIndex, 
									new GamesListbox.GameStruct(installedGameId, 
												installedGameName, biLogo));

						bLoadScreenshot = true;		// Position is important!
						this.repaint();
					}
				}
			}
			
		}

		else if (currAction.equals(ArcadeTheme.PREFERENCES_BUTTON_ACTION)) {
			// Always instantiate dlgPreferences
			dlgPreferences = new PreferencesDialog(parentFrame, docPlatformXML);	
			dlgPreferences.PopulateForm();
			if (dlgPreferences.isOKClicked()) {
				// Propogate changed application settings to required settings variables.
				selJoystickId = programSettings.getStringProperty("JoystickId");
				parentFrame.setSelectedPlatformId(programSettings.getStringProperty("PlatformId"));
				parentFrame.setWrite2Target(programSettings.getStringProperty("Writeto"));
			}
			/* User is not likely to invoke Preferences dialogbox many times, so it makes no 
			   sense to cache it in memory. Further, the dialogbox does not have to free any 
			   external resources such as files, etc. Hence, it is best to dispose it when user 
			   dismisses it.
			 */
			dlgPreferences.dispose();
		}

		else if (currAction.equals(ArcadeTheme.UPDATE_BUTTON_ACTION)) {
			parentFrame.PlaceGlassPane("Checking for Updates");

			new SwingWorker<List<Boolean>, Void>() {
				/* It is tempting to declare List<Boolean> alPossibleErrors here - as class
				   level variable. This way, it will be available to doInBackground() as well
				   as done(). But bear in mind that said procedures are being executed on 2
				   different threads. Thus, accessing alPossibleErrors becomes a cross-thread
				   issue. Even though it is possible that no issue will arise, it is better
				   to let Java handle it. So, return alPossibleErrors in doInBackground()
				   and retrieve it using get() in done().
				 */

				// Executed on a worker (background) thread
				// Should never touch any Swing component
				@Override
				protected List<Boolean> doInBackground()
				{
					List<Boolean> alPossibleErrors = new ArrayList<Boolean>(3);
					if (UpdateDialog.CheckForUpdates(alPossibleErrors)) {
						return alPossibleErrors;
					}
					else
						return null;
				}

				// Executed on EDT
				@Override
				protected void done()
				{
					try
					{
						parentFrame.RemoveGlassPane();

						List<Boolean> alPossibleErrors = get();
						if (alPossibleErrors == null)
							JOptionPane.showMessageDialog(parentFrame, 
									"There was an error connecting Gadgetfactory website", 
									"Update Error", 
									JOptionPane.ERROR_MESSAGE);
						else if (alPossibleErrors.get(0).equals(Boolean.TRUE))
							JOptionPane.showMessageDialog(parentFrame, 
									"You are not connected to Internet. " + 
									"Please connect before updating", 
									"Update Error", 
									JOptionPane.ERROR_MESSAGE);
						else if (alPossibleErrors.get(1).equals(Boolean.TRUE))
							JOptionPane.showMessageDialog(parentFrame, 
									"It is taking too long to connect to Gadgetfactory " + 
									"website, aborting update", 
									"Update Error", 
									JOptionPane.ERROR_MESSAGE);
						else
						{
						/* User is not likely to invoke Update dialogbox many times, so it makes  
						   sense to use it on "on-demand" basis - so that unnecessary memory will   
						   not be allocated. Hence, it is best to instantiate it and dispose it  
						   when user dismisses it.
						 */
							dlgUpdate = new UpdateDialog(parentFrame, docGameXML, docHardwareXML, 
														 romsFolder, imagesFolder);	
							dlgUpdate.PopulateForm();
//							if (dlgPreferences.isOKClicked()) {
//							}
							dlgUpdate.dispose();
						}
					}
					catch (InterruptedException e) {
						System.err.println("(Anonymous).done\t" + e.getMessage());
					}
					catch (ExecutionException e) {
						System.err.println("(Anonymous).done\t" + e.getMessage());
					}
				}

			}.execute();
		}
		
		else if (currAction.equals(ArcadeTheme.HELP_BUTTON_ACTION)) {
			File helpFile = new File(AppPath, "help/index.html");
			HelperFunctions.BrowseURL(helpFile.toURI().toString(), runningonWindows);
		}

		else if (currAction.equals(ArcadeTheme.EXIT_BUTTON_ACTION)) {
			parentFrame.CleanupAndExit();
		}
		
	}


	private boolean InstallGame(File romZipFile)
	{
		NodeList gameList = docGameXML.getElementsByTagName("game");
		Element elmIterGame = null;
		Element elmRomFile, elmFileset;
		Map<Long, Long> colZipEntries = new HashMap<Long, Long>(15);
		Map<Object, Object> FilesetEntries = new HashMap<Object, Object>(15);
		Enumeration<? extends ZipEntry> eze;
		ZipFile zfGame = null; ZipEntry ze;
		NodeList filesetList, romList; Element elmROM;
		File romFile, gamePath;
		String sROMFileName; long lROMFileSize, lROMCRC;
		InputStream in = null; FileOutputStream fout = null;
		boolean bFound = false, bSuccess = true;
		boolean bFileFound;

		bReinstalledGame = false;
		try
		{
			zfGame = new ZipFile(romZipFile);
/*			if ((zfGame.size() > 40) || (zfGame.size() <= 0)) {
				JOptionPane.showMessageDialog(parentFrame, "The game (" + romZipFile.getName() + 
									") file is invalid. A game zip file can contain at most 40 " + 
									"rom files.", "Invalid Game File", JOptionPane.WARNING_MESSAGE);
				return false;
				// finally block is guaranteedly executed even if we do return.
			}
*/
			eze = zfGame.entries();
			while (eze.hasMoreElements()) {
				ze = eze.nextElement();
				if (!ze.isDirectory()) {
					if (!ze.getName().contains("/")) {
						lROMFileSize = ze.getSize();
						lROMCRC = ze.getCrc();
						
						if (lROMFileSize == -1) {
							JOptionPane.showMessageDialog(parentFrame, "The game (" + 
									romZipFile.getName() + ") file is invalid.",  
									"Invalid Game File", JOptionPane.WARNING_MESSAGE);
							return false;
							// finally block is guaranteedly executed even if we do return.
						}
						else if (lROMCRC == -1) {
							JOptionPane.showMessageDialog(parentFrame, "(" + 
									romZipFile.getName() + ") contains invalid files (no CRC)",
									"Invalid Game File", 
									JOptionPane.WARNING_MESSAGE);
							return false;
							// finally block is guaranteedly executed even if we do return.
						}
						
						colZipEntries.put(Long.valueOf(lROMCRC), Long.valueOf(lROMFileSize));
					}
				}
			}
			//return false;

			for (int gameIndex = 0; gameIndex < gameList.getLength(); gameIndex++) {
				elmIterGame = (Element) gameList.item(gameIndex);
				filesetList = elmIterGame.getElementsByTagName("fileset");
				// TODO: Make a map of matching file names, if any mismatch occurs
				// reset this map and start over
				FilesetEntries.clear();
				for (int filesetIndex = 0; filesetIndex < filesetList.getLength(); filesetIndex++) {
					elmFileset = (Element) filesetList.item(filesetIndex);
					romList = elmFileset.getElementsByTagName("rom");
					for (int romIndex = 0; romIndex < romList.getLength(); romIndex++) {
						// TODO: Check if each file in the roms list has a match in the zip file
						elmRomFile = (Element)romList.item(romIndex);
						eze = zfGame.entries();
						bFileFound = false;
						while (eze.hasMoreElements()) {
							// according to rom tag order
							ze = eze.nextElement();
							if (!ze.isDirectory()) {
								// Check if this file matches the current rom file
								// on size and CRC
								if (Long.valueOf(elmRomFile.getAttribute("crc").toString(), 16).longValue() == ze.getCrc())
								{
									FilesetEntries.put(elmRomFile, ze);
									bFileFound = true;
									break;
								}
							}
						}
						if (!bFileFound)
							break;
					}
					// TODO: Check if all files match, if yes create the folder if
					// does not exist and extract the files according to
					// the map to the game folder by corresponding names
					if (FilesetEntries.size() == romList.getLength())
					{
						byte[] buf = new byte[1024];
						// Found a complete match of all files in the fileset
						gamePath = new File(romsFolder, elmIterGame.getAttribute("id"));
						gamePath.mkdir();
						// Extract all the files in the zip file to the game folder
						for (Entry<Object, Object> entry : FilesetEntries.entrySet()) 
						{
							// Extract the file
							ze = zfGame.getEntry(entry.getValue().toString());
							elmRomFile = (Element) entry.getKey();
							File newFile = new File(gamePath, elmRomFile.getAttribute("file"));
							InputStream fin = zfGame.getInputStream(ze);
							int n;
							
							fout = new FileOutputStream(newFile);
							
							while ((n = fin.read(buf, 0, 1024)) > -1)
			                    fout.write(buf, 0, n);

			                fout.close(); 
						}
						
						// TODO: Check if all appropriate number of files are present
						// in the game folder, if yes add this to the installed games list
						installedGameName = elmIterGame.getAttribute("name");
						installedGameId = elmIterGame.getAttribute("id");
						if (gamePath.listFiles().length != romList.getLength())
						{
							bSuccess = false;
							if (elmFileset.getAttribute("primary").equalsIgnoreCase("true"))
							{
								// If no, and current fileset is primary, issue a message saying
								// more files are needed
								JOptionPane.showMessageDialog(this, "Not all files required by " + installedGameName +
										  " were installed. Please install rest of the files to play " + installedGameName, 
										  "Game Installation Unsuccessful", JOptionPane.WARNING_MESSAGE);								
							}
							else
							{
								// else if current fileset is not primary
								// issue a message saying parent game files required
								JOptionPane.showMessageDialog(this, "Not all files required by " + installedGameName +
										  " were installed. Please install rest of the files to play " + installedGameName, 
										  "Game Installation Unsuccessful", JOptionPane.WARNING_MESSAGE);									
							}
						}
						else
						{
							// Game is successfully installed
							bSuccess = true;
							installedGameIndex = gameIndex + 1;		// Taking into account "Top Filler"
						}
						
						bFound = true;
						break;
					}
					if (bFound)
						break;					
				}
				if (bFound)
					break;
			}
		}
		catch (ZipException zex) {
/*	------------------------------------------------------------------------------------
 * 		ZipException is a subclass of IOException that indicates the data in the zip file 
 * 		doesn't fit the zip format. In this case, the zip exception's message will contain 
 * 		more details, like "invalid END header signature" or "cannot have more than one drive." 
 * 		While these may be useful to a zip expert, in general they indicate that the file is 
 * 		corrupted, and there's not much that can be done about it.
 *	------------------------------------------------------------------------------------ */
			System.err.println("InstallGame()\t" + zex.getMessage());
			bSuccess = false;
			JOptionPane.showMessageDialog(this, "The game zip file (" + romZipFile.getName() + ")" + 
							", you are attempting to install, is corrupt. The game cannot be installed.", 
							"Corrupt Zip File", JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException e) {
			System.err.println("InstallGame()\t" + e.getMessage());
			bSuccess = false;
		}
		finally
		{
			if (in != null) {
				try {
					in.close();  
				}
				catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
			if (fout != null) {
				try {
					fout.close();
				}
				catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
			if (zfGame != null) {
				try {
					zfGame.close();  
				}
				catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
		}

		if (!bFound) {
			JOptionPane.showMessageDialog(this, 
										  "The game (" + romZipFile.getName() + ") you are " + 
										  "trying to install does not match any known game " + 
										  "names.\nThis game cannot be installed.", 
										  "Invalid Game File", JOptionPane.ERROR_MESSAGE);
			return false;
		}
/*		else if (gamePath.isDirectory()) {
			bReinstalledGame = true;
			if (JOptionPane.showConfirmDialog(this, 
					 "The game " + installedGameId + " is already installed.\n" + 
					 "Do you want to reinstall " + installedGameId + "?", 
					 "Game Already Installed", 
					  JOptionPane.YES_NO_OPTION, 
					  JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
				return false;
		}
*/
		if (bSuccess) {
			installedGameName = elmIterGame.getAttribute("name");
			JOptionPane.showMessageDialog(this, installedGameName + " has been " + 
					  "successfully installed.\nYou can now load and play " + installedGameName + ".", 
					  "Game Installation Successful", JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		else
			return false;
	}


    private class GamePadMouseAdapter extends MouseAdapter
    {
        Point pivotPoint, formTopLeft;
        Point offset = new Point();

        @Override
        public void mousePressed(MouseEvent mev) {
            pivotPoint = mev.getPoint();
            formTopLeft = parentFrame.getLocationOnScreen();
            
            // Following illustrates why pivot point cannot be in absolute screen co-ordinates
            // as it "differs" from that given by MouseEvent. The difference is not due to
            // absolutely and relative co-ordinates but functional.
//            System.out.println(mev.getX() + ", " + mev.getY() + ", " + mev.getLocationOnScreen() + "\n");
        }

        @Override
        public void mouseDragged(MouseEvent mev) {
            offset.setLocation(mev.getX() - pivotPoint.x, mev.getY() - pivotPoint.y);
            formTopLeft.translate(offset.x, offset.y);
            parentFrame.setLocation(formTopLeft);

            // Following illustrates why pivot point cannot be in absolute screen co-ordinates
            // as it "differs" from that given by MouseEvent. The difference is not due to
            // absolutely and relative co-ordinates but functional.
//            System.out.println(mev.getX() + ", " + mev.getY() + ", " + mev.getLocationOnScreen());
        }
    }

}
