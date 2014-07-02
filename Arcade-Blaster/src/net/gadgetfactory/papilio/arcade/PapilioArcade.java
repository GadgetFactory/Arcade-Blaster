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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import net.gadgetfactory.shared.AppSettings;
import net.gadgetfactory.shared.HelperFunctions;
import net.gadgetfactory.shared.MessageConsumer;
import net.gadgetfactory.shared.MessageSiphon;
import net.gadgetfactory.shared.UIHelpers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PapilioArcade extends JFrame
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 946037016052438496L;
	public static final boolean DEBUG = true;
	public static final int GAMEPAD_IMAGE_WIDTH = 900;
	public static final int GAMEPAD_IMAGE_HEIGHT = 700;

	private static final String APP_TITLE = "Papilio Arcade 1.0 Beta";
	private static final String SETTINGS_FOLDER = "papilio-arcade";
	private static final String PREFERENCES_FILE = "preferences.txt";
	private static final String LOG_FILE = "papilio-arcade.log";
	private static final String DEFAULT_PLATFORM_ID = "PapilioOne500k_ArcadeMegaWing";
	private static final String DEFAULT_JOYSTICK_ID = "atari2600";

	public static final ArcadeTheme currTheme = 
				new ArcadeTheme();
	public static final WrJoysticksArray supportedJoysticks = 
				new WrJoysticksArray();
	public static final boolean runningonWindows = 
				System.getProperty("os.name").toLowerCase().startsWith("windows");
	public static final boolean runningonLinux = 
				System.getProperty("os.name").toLowerCase().startsWith("linux");
				
	public static final File AppPath = new File(".");
	public static final AppSettings programSettings = 
				new AppSettings(SETTINGS_FOLDER, PREFERENCES_FILE, runningonWindows);
    public static final File romsFolder = 
    			new File("roms");

	public static URL urlUpdateGamesBase;

    private static boolean bShapingSupported;

    private GamePadPanel pnlGamepad;
    private ImageButton btnLoadGame;
    private ProgressGlassPane glpBurning;

    private final File imagesFolder = new File("images");
    private final File gamesXMLFile = new File(romsFolder, "Games.xml");
    private Document docGameXML;
    private final File hardwareFolder = new File("hardware");
    private final File hardwareXMLFile = new File(hardwareFolder, "Hardware.xml");
    private Document docHardwareXML;
    private final File platformsXMLFile = new File(hardwareFolder, "Platforms.xml");
    private Document docPlatformXML;

	private File rootProgrammerPath; 
	private File programmerPath;	// OS dependent folder where papilio-prog.exe, etc are located 
	private File papilioProgrammerFile, romgenFile, dataToMemFile;
	
	// Following variables are shared between FormValid() and AsyncProgrammer class
	private Element elmSelGame;
	// Following is the "hardware" for which .bit files are written. e.g. pacman, etc.
	private String selHardwareId;  
	private String selMemoryMap;
	private File gameBitFile, gameBmmFile;

	private String selPlatformId;
	private String sLastErrorMessage ="";
	private boolean bWrite2FPGA;
	private int exitCode;
	
	public void setSelectedPlatformId(String newPlatformId) {
		this.selPlatformId = newPlatformId;
	}

	public void setWrite2Target(String newTarget) {
		this.bWrite2FPGA = newTarget.equalsIgnoreCase("FPGA");
	}


	public static void main(String[] args)
	{
		try {
//			System.out.println(UIManager.getSystemLookAndFeelClassName());
		    // Set System L&F
	        UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (UnsupportedLookAndFeelException e) {
	    	System.err.println("()\t" + e.getMessage());
	    }
	    catch (ClassNotFoundException e) {
	    	System.err.println("()\t" + e.getMessage());
	    }
	    catch (InstantiationException e) {
	    	System.err.println("()\t" + e.getMessage());
	    }
	    catch (IllegalAccessException e) {
	    	System.err.println("()\t" + e.getMessage());
	    }
	    catch (Exception e) {
	    	System.err.println("()\t" + e.getMessage());
	    }
//		System.getProperties().list(System.out);
//	    System.out.println(Arrays.toString(args));
//	    System.out.println("Main thread: " + Thread.currentThread().getName());

        bShapingSupported = 
        	AWTUtilitiesWrapper.isTranslucencySupported(AWTUtilitiesWrapper.PERPIXEL_TRANSPARENT);
        if (!bShapingSupported) {
        	// NOTE: What needs to be done if shaped windows are not supported?
        	System.exit(0);
        }

		try {
			urlUpdateGamesBase = new URL("http://arcade.gadgetfactory.net/games/");
			/* Ending above URL with "/" is absolutely must in order to form relative URLs
			   such as http://arcade.gadgetfactory.net/games/arcade-updates.txt.
			   If trailing "/" is omitted, then constructing a relative URL such as 
			   new URL(urlUpdateGamesBase, "arcade-updates.txt") would yield URL
			   http://arcade.gadgetfactory.net/arcade-updates.txt, which is obviously 
			   incorrect. (This is because "games" is considered as "file/folder" and 
			   not as a "path".)
			 */
		}
		/* MalformedURLException is thrown only if string specifies an unknown protocol.
		   http being a very very standard protocol, following exception will never be thrown.
		   The catch block is there just because MalformedURLException being a checked 
		   exception.
		 */
		catch (MalformedURLException e) {
			System.err.println("UpdateDialog.UpdateDialog.(Constru)\t" + e.getMessage());
		}

	    EventQueue.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				new PapilioArcade().setVisible(true);
			}
		});
	}

	public PapilioArcade()
	{
/*	1) File, Folder, etc initialization and validation */

		if (AppCompromised())
			System.exit(0);
		
		

		rootProgrammerPath = new File(AppPath, "programmer");
		// Determine locations of Papilio Programmer, romgen and data2mem executables
		// depending on the current platform and architecture.
		if (runningonWindows)
		{
			programmerPath = new File(rootProgrammerPath, "win32");
			papilioProgrammerFile = new File(programmerPath, "papilio-prog.exe");
			romgenFile = new File(programmerPath, "romgen.exe");
			dataToMemFile = new File(programmerPath, "data2mem.exe");
		}
		else
		{
			
			programmerPath = new File(rootProgrammerPath, "linux32");
			dataToMemFile = new File(programmerPath, "data2mem");
			papilioProgrammerFile = new File(programmerPath, "papilio-prog");
			romgenFile = new File(programmerPath, "romgen");
			
		}

/*	2) Init UIHelpers.java */
		
		UIHelpers.initVariables(this);

/*	3) Load Games.xml and Hardware.xml files */
		
		LoadConfigXMLs();

/*	4) Add child controls to form */

		pnlGamepad = new GamePadPanel(this, docGameXML, docPlatformXML, docHardwareXML, 
									  imagesFolder);
		btnLoadGame = pnlGamepad.getLoadGameButton();
		this.getContentPane().add(pnlGamepad, BorderLayout.CENTER);
		
		this.getRootPane().setDefaultButton(btnLoadGame);
		glpBurning = new ProgressGlassPane(currTheme.getScreenshotPath(), false);
		this.setGlassPane(glpBurning);

/*	5) Initialize all application settings. */

		// This call must be after pnlGamepad has been instantiated as it is used the call.
		ReadSettings();

/*	6) Set up form */

		this.setTitle(APP_TITLE);
		this.setSize(GAMEPAD_IMAGE_WIDTH, GAMEPAD_IMAGE_HEIGHT);
		this.setLocationByPlatform(true);
		
/*	------------------------------------------------------------------------------------
 *		When setting the shape on your window, note that the effect supports only undecorated 
 * 		windows. If your window is decorated and you apply the effect, you will get the original 
 * 		shape of the window without the effect applied to it. If the window has been created by 
 * 		untrusted code (i.e. the window has a non-null warning string returned by getWarningString()),
 *		the method returns without affecting the shape of the window. Also note that the window 
 *		must not be in the full-screen mode when setting a non-null shape. Otherwise, an 
 *		IllegalArgumentException is thrown.
 *	------------------------------------------------------------------------------------ */
		try {
			this.dispose(); // If a dialog cames up
			this.setUndecorated(true);
			this.setVisible(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
// TODO: Mention reason why .setUndecorated() call must be PRIOR to .pack()  
		/*	Since the shape of application window is fixed and made of hard-coded points 
			resizing ability is out of question. 
		*/
		this.setResizable(false);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

/*	------------------------------------------------------------------------------------
 * 	Window.pack()
 * 		Causes this Window to be sized to fit the preferred size and layouts of its 
 * 		subcomponents. If the window and/or its owner are not yet displayable, both are 
 * 		made displayable before calculating the preferred size. The Window will be 
 * 		validated after the preferredSize is calculated.
 *	------------------------------------------------------------------------------------ */
/*	------------------------------------------------------------------------------------
 * 		After a component is created it is in the invalid state by default. 
 * 		The Window.pack method validates the window and lays out the window's 
 * 		component hierarchy for the first time. 		
 *	------------------------------------------------------------------------------------ */
		this.pack();

/*	7) Add event listeners for form */

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				CleanupAndExit();
			}
		});

		final PapilioArcade anonpa = this;
		// It is best practice to set the window's shape in the componentResized method.  
		// Then, if the window changes size, the shape will be correctly recalculated.
		this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                AWTUtilitiesWrapper.setWindowShape(anonpa, currTheme.getWindowPath());
            }
		});
	}

	public void CleanupAndExit()
	{
		SaveSettings();
		pnlGamepad.Cleanup();
		
		this.setVisible(false);
		this.dispose();
		System.exit(0);
	}

	private boolean AppCompromised()
	{
		boolean bValid = false;

		if (!imagesFolder.isDirectory())
			JOptionPane.showMessageDialog(this, 
										  "The images folder disappeared. Please reinstall the program.", 
										  "Folder Error", 
										  JOptionPane.ERROR_MESSAGE);
		else if (!new File(imagesFolder, "monograms").isDirectory())
			JOptionPane.showMessageDialog(this, 
										  "The monograms folder not found. Please reinstall the program.", 
										  "Folder Error", 
										  JOptionPane.ERROR_MESSAGE);
		else if (!romsFolder.isDirectory())
			JOptionPane.showMessageDialog(this, 
										  "The roms folder not found. Please reinstall the program.", 
										  "Folder Error", 
										  JOptionPane.ERROR_MESSAGE);
		else if (!gamesXMLFile.isFile())
			JOptionPane.showMessageDialog(this, 
										  "Games.xml not found. Please reinstall the program.", 
										  "File Error", 
										  JOptionPane.ERROR_MESSAGE);
		else if (!hardwareFolder.isDirectory())
			JOptionPane.showMessageDialog(this, 
										  "The hardware folder not found. Please reinstall the program.", 
										  "Folder Error", 
										  JOptionPane.ERROR_MESSAGE);
		else if (!hardwareXMLFile.isFile())
			JOptionPane.showMessageDialog(this, 
										  "Hardware.xml not found. Please reinstall the program.", 
										  "File Error", 
										  JOptionPane.ERROR_MESSAGE);
		else if (!platformsXMLFile.isFile())
			JOptionPane.showMessageDialog(this, 
										  "Platforms.xml not found. Please reinstall the program.", 
										  "File Error", 
										  JOptionPane.ERROR_MESSAGE);
		else if (runningonLinux)
		{
			JOptionPane.showMessageDialog(this, 
										  "Linux version is in alpha mode. Please build papilio-prog and romgen for your linux distro.", 
										  "Linux alpha mode", 
										  JOptionPane.WARNING_MESSAGE);
			bValid = true;
		}
		else if(runningonWindows)
			bValid = true;

		return !bValid;
	}


	private void LoadConfigXMLs()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			// TODO: Ensure validation using XSD

			DocumentBuilder builder = factory.newDocumentBuilder();
			docGameXML = builder.parse(gamesXMLFile);
			docHardwareXML = builder.parse(hardwareXMLFile);
			docPlatformXML = builder.parse(platformsXMLFile);
		}
		catch (SAXException e) {
	    	System.err.println("PapilioArcade.LoadConfigXMLs()\t" + e.getMessage());
		}
		catch (IOException e) {
	    	System.err.println("PapilioArcade.LoadConfigXMLs()\t" + e.getMessage());
		}
		catch (ParserConfigurationException e) {
	    	System.err.println("PapilioArcade.LoadConfigXMLs()\t" + e.getMessage());
		}
		catch (FactoryConfigurationError e) {
	    	System.err.println("PapilioArcade.LoadConfigXMLs()\t" + e.getMessage());
		}
	}

	public boolean ImportGameXML(File updateGameXMLFile)
	{
		Document docImportGameXML = null;
		boolean bError = true;
		
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			// TODO: Ensure validation using XSD

			DocumentBuilder builder = factory.newDocumentBuilder();
			docImportGameXML = builder.parse(updateGameXMLFile);
			bError = false;
		}
		catch (SAXException e) {
	    	System.err.println("PapilioArcade.ImportGameXML()\t" + e.getMessage());
		}
		catch (IOException e) {
	    	System.err.println("PapilioArcade.ImportGameXML()\t" + e.getMessage());
		}
		catch (ParserConfigurationException e) {
	    	System.err.println("PapilioArcade.ImportGameXML()\t" + e.getMessage());
		}
		catch (FactoryConfigurationError e) {
	    	System.err.println("PapilioArcade.ImportGameXML()\t" + e.getMessage());
		}
		if ((bError) && (DEBUG)) {
			JOptionPane.showMessageDialog(this, 
							"Validation failed for " + updateGameXMLFile.getName(), 
							"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		NodeList gameList = docImportGameXML.getElementsByTagName("game");
		Element elmImportGame; int i;

		for (i = 0; i < gameList.getLength(); i++) {
			elmImportGame = (Element) gameList.item(i);
			docGameXML.importNode(elmImportGame, true);
		}

		return true;
	}
	
	private Element PrimaryFilesetElement(Element elmAnyGame) {
		NodeList filesetList = elmAnyGame.getElementsByTagName("fileset");
		// If <fileset> element is not present, then filesetList does not become null.
		// Instead, filesetList.getLength() returns 0.
		Element elmFileSet;

		for (int i = 0; i < filesetList.getLength(); i++) {
			elmFileSet = (Element) filesetList.item(i);
			if (elmFileSet.hasAttribute("primary")) {
				if (Boolean.parseBoolean(elmFileSet.getAttribute("primary")))
					return elmFileSet;
			}
		}
		return null;
	}


	private void ReadSettings()
	{
		Properties defaultSettings = new Properties();
		
		defaultSettings.setProperty("PlatformId", DEFAULT_PLATFORM_ID);
		defaultSettings.setProperty("JoystickId", DEFAULT_JOYSTICK_ID);
		defaultSettings.setProperty("Writeto", "FPGA");
		defaultSettings.setProperty("WindowX", "150");
		defaultSettings.setProperty("WindowY", "80");
		
		programSettings.Cache(defaultSettings);
		
		// Propogate application settings to required settings variables. 
		selPlatformId = programSettings.getStringProperty("PlatformId");
		bWrite2FPGA = programSettings.getStringProperty("Writeto").equalsIgnoreCase("FPGA");
		pnlGamepad.setSelectedJoystickId(programSettings.getStringProperty("JoystickId"));
	}

	private void SaveSettings()
	{
		/* It is not required to save values of settings variables such as selPlatformId, 
		   bWrite2FPGA, etc. to programSettings because the two are always in sync with each
		   other. (PreferencesDialog takes care of that.) 
		 */
		
		if (!programSettings.isFilePresent()) {
/*	------------------------------------------------------------------------------------
 *	Properties.store(Writer writer, String comments)
 *		Writes this property list (key and element pairs) in this Properties table to the 
 *		output character stream in a format suitable for using the load(Reader) method. 
 *		Properties from the defaults table of this Properties table (if any) are **NOT** 
 *		written out by this method. 
 *	------------------------------------------------------------------------------------ */
			programSettings.setProperty("PlatformId", programSettings.getStringProperty("PlatformId"));
			programSettings.setProperty("JoystickId", programSettings.getStringProperty("JoystickId"));
			programSettings.setProperty("Writeto", programSettings.getStringProperty("Writeto"));
		}
		programSettings.setProperty("WindowX", this.getX() + "");
		programSettings.setProperty("WindowY", this.getX() + "");
		
		programSettings.Save();
	}


	public void PlaceGlassPane(String headerMsg) {
		glpBurning.resetProgress(headerMsg);
		glpBurning.setVisible(true);
	}
	
	public void RemoveGlassPane() {
		glpBurning.setVisible(false);
	}

	public void NotifyProcessComplete(int exitCode) {
		if (exitCode != 0)
		{
			if ((sLastErrorMessage != null) && (sLastErrorMessage.contains("Could not access USB")))
			{
				JOptionPane.showMessageDialog(
						this,
						"Cannot find Papilio Arcade board\nPlease make sure you have plugged in the USB cable properly.\n",
						"Error writing game",
						JOptionPane.ERROR_MESSAGE);				
			}
			else
			{
				JOptionPane.showMessageDialog(
						this,
						"An Error occured while writing the game:\n\n" + sLastErrorMessage,
						"Error writing game",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	public void BurnUsingProgrammer(String selGameId, String selGameName)
	{
		Thread thread;
		
		if (!FormValid(selGameId))
			return;

//		PlaceGlassPane("Downloading " + selGameName);
		
		thread = new Thread(new AsyncProgrammer(selGameId, selGameName));
		// It is better to set the priority of AsyncProgrammer thread same as that of
		// MessageSiphon thread (but greater than priority of EDT).
		// Anyway, this (AsyncProgrammer thread) is going to block (wait) on MessageSiphon 
		// threads, so everything will work out.
		thread.setPriority(Thread.MAX_PRIORITY-2);
		thread.start();
	}

	private boolean FormValid(String selGameId)
	{
		if (!papilioProgrammerFile.isFile()) {
		// => papilio-prog.exe is NOT present
			JOptionPane.showMessageDialog(
							this,
							"Papilio programmer ("
									+ papilioProgrammerFile.getName()
									+ ") does not exist on disk. Please reinstall the program.",
							"Papilio Programmer Not Found",
							JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if (!romgenFile.isFile()) {
		// => romgen.exe is NOT present
			JOptionPane.showMessageDialog(
							this,
							"romgen program ("
									+ romgenFile.getName()
									+ ") does not exist on disk. Please reinstall the program.",
							"romgen Program Not Found",
							JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if (!dataToMemFile.isFile()) {
		// => data2mem.exe is NOT present
			JOptionPane.showMessageDialog(
							this,
							"data2mem program ("
									+ dataToMemFile.getName()
									+ ") does not exist on disk. Please reinstall the program.",
							"data2mem Program Not Found",
							JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if (!GameValid(selGameId))
			return false;
		else
			return HardwarePlatformValid(selGameId);
	}

	private boolean GameValid(String selGameId)
	{
		NodeList gameList = docGameXML.getElementsByTagName("game");
		File gamePath = new File(romsFolder, selGameId);
		boolean bFound = false;
		
		for (int i = 0; i < gameList.getLength(); i++) {
			elmSelGame = (Element) gameList.item(i);
			if (elmSelGame.getAttribute("id").equals(selGameId)) {
				bFound = true;
				break;
			}
		}
		
		if (!bFound) {
			JOptionPane.showMessageDialog(this, "Please select a game to load", 
										 "Game Not Selected", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else if (!gamePath.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Please ensure that " + selGameId + " is present " + 
										 "in roms folder.", 
					 					 "Game Folder Invalid", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else
		{
			Element elmFileSet = PrimaryFilesetElement(elmSelGame);
			NodeList romList; Element elmROM; File romFile;

			if (elmFileSet != null) {
				romList = elmFileSet.getElementsByTagName("rom");
				for (int j = 0; j < romList.getLength(); j++) {
					elmROM = (Element) romList.item(j);
					romFile = new File(gamePath, elmROM.getAttribute("file"));
					if (!romFile.isFile()) {
						JOptionPane.showMessageDialog(this, "Please ensure that " + 
								 elmROM.getAttribute("file") + " is present in " + selGameId + 
								 " folder.", "Game Folder Invalid", 
								 JOptionPane.WARNING_MESSAGE);
						return false;
					}
				}
			}
			else {
				JOptionPane.showMessageDialog(this, "No primary <fileset> element found " + 
						 "in Games.xml for " + selGameId + "\nPlease reinstall application", 
						 "Game.xml File Invalid", 
						 JOptionPane.WARNING_MESSAGE);
				return false;
			}

			return true;
		}
	}

	private boolean HardwarePlatformValid(String selGameId)
	{
		NodeList hardwareList = elmSelGame.getElementsByTagName("hardware");
		// If <hardware> element is not present, then hardwareList does not become null.
		// Instead, hardwareList.getLength() returns 0.
		Element elmHardware, elmPlatform; NodeList children, platformList; Node textNode;
		int i, j; boolean bFound = false; 
		String sBitFileName = ""; String sBmmFileName;
		File gameHardwarePath;

/*	1)	Find and validate <hardware> element for selected game in Games.xml */
		
		for (i = 0; i < hardwareList.getLength(); i++) {
			selHardwareId = HelperFunctions.SimpleElementText((Element) hardwareList.item(i));
			if (selHardwareId != null) {
				bFound = true;
				break;
			}
		}

		if (!bFound) {
			JOptionPane.showMessageDialog(this, "Hardware element was not found in Games.xml file. " + 
										  "Please reinstall the program.", 
										  "Hardware Not Found", JOptionPane.WARNING_MESSAGE);
			return false;
		}

/*	2)	Find and validate <hardware> element for selected hardware in Hardware.xml */

		selMemoryMap = "";
		bFound = false;
		hardwareList = docHardwareXML.getElementsByTagName("hardware");
		// If <hardware> element is not present, then hardwareList does not become null.
		// Instead, hardwareList.getLength() returns 0.
hardware_search:
		for (i = 0; i < hardwareList.getLength(); i++) {
			elmHardware = (Element) hardwareList.item(i);
			if (elmHardware.getAttribute("id").equals(selHardwareId)) {
				platformList = elmHardware.getElementsByTagName("platform");
				for (j = 0; j < platformList.getLength(); j++) {
					elmPlatform = (Element) platformList.item(j);
					if (elmPlatform.getAttribute("name").equals(selPlatformId)) {
						selMemoryMap = elmPlatform.getAttribute("memmap");
						sBitFileName = HelperFunctions.SimpleElementText(elmPlatform);
						if (sBitFileName != null)
							bFound = true;
						break hardware_search;
					}
				}
			}
		}

		if (!bFound) {
			JOptionPane.showMessageDialog(this, "Hardware element was not found in Hardware.xml file. " + 
										  "Please reinstall the program.", 
										  "Hardware Not Found", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else if (sBitFileName.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Platform element was not found in Hardware.xml file. " + 
										  "Please reinstall the program.", 
										  "Platform Not Found", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else if (sBitFileName.lastIndexOf(".bit") == -1) {
			JOptionPane.showMessageDialog(this, "Game bit file (" + sBitFileName + ") specified " + 
										  "in Hardware.xml file is incorrect. " + 
										  "Please reinstall the program.", 
										  "Bit File Not Found", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else if (selMemoryMap.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Memory map is not specified in Hardware.xml file. " + 
										  "Please reinstall the program.", 
										  "Memory Map Not Found", JOptionPane.WARNING_MESSAGE);
			return false;
		}

/*	3)	Find and validate required folder and .bit, .bmm files for selected hardware */

		gameHardwarePath = new File(hardwareFolder, selHardwareId);
		gameBitFile = new File(gameHardwarePath, sBitFileName);
		sBmmFileName = sBitFileName.substring(0, sBitFileName.lastIndexOf(".bit")) + "_bd.bmm";
		gameBmmFile = new File(gameHardwarePath, sBmmFileName);
		
		if (!gameHardwarePath.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Please ensure that " + selHardwareId + " is present " + 
										 "in hardware folder.", 
					 					 "Hardware Folder Invalid", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else if (!gameBitFile.isFile()) {
			JOptionPane.showMessageDialog(this, "Please ensure that " + sBitFileName + " is present " + 
										 "in hardware/" + selGameId + " folder.", 
										 "Hardware Folder Invalid", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else if (!gameBmmFile.isFile()) {
			JOptionPane.showMessageDialog(this, "Please ensure that " + sBmmFileName + " is present " + 
										 "in hardware/" + selGameId + " folder.", 
										 "Hardware Folder Invalid", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		else
			return true;
	}


	public class AsyncProgrammer implements Runnable, MessageConsumer
	{
		private File finalBitFile, bscanSPIBitFile;
		private String q_papilio_prog_exe, q_rom_gen_exe, q_data_2_mem_exe;
		private File gamePath, buildPath;
		private Map<String, File> colGroupFiles, colMemFiles, colBitFiles;
		private FileOutputStream foutMem = null; private BufferedWriter bwMem = null;
		private FileOutputStream foutLog = null; private BufferedWriter bwLog = null;
	    private boolean bMemSuccess, bCollectOutput;
		private boolean bLookforDesc; private String deviceID;
	    
		public AsyncProgrammer(String selectedGameId, String selGameName)
		{
			PlaceGlassPane("Downloading " + selGameName);
			
			colGroupFiles = new HashMap<String, File>(20);
			colMemFiles = new HashMap<String, File>(20);
			colBitFiles = new HashMap<String, File>(20);

			gamePath = new File(romsFolder, selectedGameId);
			q_papilio_prog_exe = UIHelpers.CanonicalPath(papilioProgrammerFile);
			q_rom_gen_exe = UIHelpers.CanonicalPath(romgenFile);
			q_data_2_mem_exe = UIHelpers.CanonicalPath(dataToMemFile);
			
			exitCode = 0;
		}
		
		@Override
		public void run()
		{
//		    System.out.println("AsyncProgrammer thread: " + Thread.currentThread().getName());
			boolean bSuccess = false;
			Random rand = new Random();
		    int randomInt = 1 + rand.nextInt();
		    int err = 0;
		    
		    try
		    {
			    buildPath = new File(System.getProperty("java.io.tmpdir"), 
			    						"arcade" + Math.abs(randomInt));
			    if (!buildPath.isDirectory())
			    	bSuccess = buildPath.mkdir();

			    if (!bSuccess) {
			    // => There was an error creating build path
			    	return;		    	
			    }
	
				foutLog = new FileOutputStream(new File(AppPath, LOG_FILE));
				bwLog = new BufferedWriter(new OutputStreamWriter(foutLog, "UTF-8"));

				bCollectOutput = false;
			    bLookforDesc = false;
			    glpBurning.setProgress("Converting Files");
				if (ConcatenateConsecutiveROMs()) {
					if (GenerateIntermediateMems()) {
					    glpBurning.setProgress("Creating Game");
						if (CreateFinalBitFile()) {
						    glpBurning.setProgress("Writing Game");
							if (bWrite2FPGA)
								err = BurnToFPGA();
							else
								err = BurnToSPIFlash();
						}
					}
				}
				
				bwLog.newLine();
				bwLog.write("\tExit code : " + err);
				bwLog.newLine();
				bwLog.newLine();				

				DeleteTemporaryFiles();

				bwLog.close();
				foutLog.close();
		    }
			catch (IOException e) {
				System.err.println("run()\t" + e.getMessage());
			}
			finally
			{
				if (bwLog != null) {
					try {
						bwLog.close();
						foutLog.close();
					}
					catch (IOException ioex) {
						System.err.println(ioex.getMessage());
					}
				}
			}
			
			RemoveGlassPane();	// TODO: Do we need to move this to EDT?
			NotifyProcessComplete(exitCode);			
		}

		private boolean ConcatenateConsecutiveROMs()
		{
			FileInputStream fin = null; FileOutputStream fout = null;
			File concatenatedFile;
			SortedMap<Long, String> sorROMFileNames = new TreeMap<Long, String>();
			NodeList groupList = PrimaryFilesetElement(elmSelGame).getElementsByTagName("group");
			NodeList romList; Element elmIterGroup, elmROM;
			int c; boolean bSuccess = true; String sConcatFileName;

			try
			{
				for (int i = 0; i < groupList.getLength(); i++)
				{
					elmIterGroup = (Element) groupList.item(i);
					sConcatFileName = elmIterGroup.getAttribute("name");
					concatenatedFile = new File(buildPath, sConcatFileName);
					colGroupFiles.put(sConcatFileName, concatenatedFile);
					fout = new FileOutputStream(concatenatedFile);
					bwLog.write("Concatenating rom files : ");
					
					sorROMFileNames.clear();
					romList = elmIterGroup.getElementsByTagName("rom");
					for (int j = 0; j < romList.getLength(); j++) {
						elmROM = (Element) romList.item(j);
						sorROMFileNames.put(
							new Long(HelperFunctions.ParseLong(elmROM.getAttribute("offset"), 16)), 
							elmROM.getAttribute("file"));
					}

					for (String sROMFileName : sorROMFileNames.values()) {
						System.out.println(sROMFileName);
						bwLog.write(sROMFileName + " ");
						fin = new FileInputStream(new File(gamePath, sROMFileName));
						while ((c = fin.read()) != -1)
							fout.write(c);
						fin.close();
					}

					fout.close();
					bwLog.write("into : " + sConcatFileName);
					bwLog.newLine();
				}
				bwLog.newLine();
			}
			catch (IOException e) {
				System.err.println("ConcatenateConsecutiveROMs()\t" + e.getMessage());
				bSuccess = false;
			}
			finally
			{
				if (fin != null) {
					try {
						fin.close();  
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
			}
			return bSuccess;
		}
		
		private boolean GenerateIntermediateMems()
		{
			NodeList generateList = elmSelGame.getElementsByTagName("generate");
			Element elmIterator;
			Map<String, String> colArguments = new HashMap<String, String>(10);
			String[] commandLine = {q_rom_gen_exe, "", "PROM_DST", "9", "m"};
			int i; String source, destination, sMemFileName;
			File outputMemFile;

			try
			{
				bCollectOutput = true;
				bMemSuccess = true;
				for (i = 0; i < generateList.getLength(); i++)
				{
					elmIterator = (Element) generateList.item(i);
					source = elmIterator.getAttribute("src");
					destination = elmIterator.getAttribute("file");

					if (colGroupFiles.containsKey(source)) {
						commandLine[1] = colGroupFiles.get(source).getCanonicalPath();
						
						ParseGenerateParameters(elmIterator.getAttribute("parameters"), colArguments);
						commandLine[2] = colArguments.get("entity");
						commandLine[3] = colArguments.get("addrbits");
						
						if (destination.endsWith(".mem"))
							sMemFileName = destination;
						else
							sMemFileName = destination + ".mem";
						outputMemFile = new File(buildPath, sMemFileName);
						foutMem = new FileOutputStream(outputMemFile);
						// The intermediate .mem files generated should be ASCII files with 
						// Unix line ending.
						bwMem = new BufferedWriter(new OutputStreamWriter(foutMem, "US-ASCII"));
						colMemFiles.put(destination, outputMemFile);
						bwLog.write("Generating intermediate file from : " + source + " into : " + 
									 sMemFileName);
						bwLog.newLine();
						bwLog.write("\t" + Arrays.toString(commandLine));
						bwLog.newLine();
						
						execSynchronously(commandLine, programmerPath);
						if (exitCode != 0) {
							bMemSuccess = false;
							break;
						}
						bwMem.flush();
						bwMem.close();
						foutMem.close();

						bwLog.write("\tExit code : " + exitCode);
						bwLog.newLine();
					}
				}
				bwLog.newLine();
			}
			catch (IOException e) {
				System.err.println("GenerateIntermediateMems()\t" + e.getMessage());
				bMemSuccess = false;
			}
			finally
			{
				if (bwMem != null) {
					try {
						bwMem.close();
						foutMem.close();
					}
					catch (IOException ioex) {
						System.err.println(ioex.getMessage());
					}
				}
			}
			bCollectOutput = false;
			return bMemSuccess;
		}

		private void ParseGenerateParameters(String argumentsAttr, Map<String, String> colArguments)
		{
			StringTokenizer stSemicolon = new StringTokenizer(argumentsAttr, ";");
			String sArgumentItem, sArgument, sKey;
			int pos;

			colArguments.clear();
			while (stSemicolon.hasMoreTokens()) {
				sArgumentItem = stSemicolon.nextToken().trim();
		        if (!sArgumentItem.equals("")) {
		        	pos = sArgumentItem.indexOf("=");
		        	if (pos != -1)
		        	{
		        		sKey = sArgumentItem.substring(0, pos);
		        		sArgument = sArgumentItem.substring(pos + 1);
		        		colArguments.put(sKey, sArgument);
		        	}
		        }
			}
			
		}

		private boolean CreateFinalBitFile()
		{
			final String TEMP_BIT_FILE_SUFFIX = "temp";
			NodeList assemblyList = elmSelGame.getElementsByTagName("assembly");
			NodeList pieceList;
			Element elmAssembly, elmPiece;
			int i, j; boolean bSuccess = true;
			String[] data2memCommand = 
					{q_data_2_mem_exe, "-bm", "", "-bt", "", "-bd", "", "tag", "", "-o", "b", ""};
			String fileKey, tagValue, sQIntermediateBitFile; String sQPrevBitFile = "";

			try
			{
				data2memCommand[2] = gameBmmFile.getCanonicalPath();
				finalBitFile = new File(buildPath, "out.bit");
				bwLog.write("Creating final .bit file...");
				bwLog.newLine();

process_assembly:
				for (i = 0; i < assemblyList.getLength(); i++)
				{
					elmAssembly = (Element) assemblyList.item(i);
					pieceList = elmAssembly.getElementsByTagName("piece");
					for (j = 0; j < pieceList.getLength(); j++)
					{
						elmPiece = (Element) pieceList.item(j);
						fileKey = elmPiece.getAttribute("file");
						tagValue = selMemoryMap + "." + elmPiece.getAttribute("tag");
						
						if (colMemFiles.containsKey(fileKey))
						{
							if (sQPrevBitFile.isEmpty())
								sQPrevBitFile = gameBitFile.getCanonicalPath();
							sQIntermediateBitFile = new File(buildPath, 
										TEMP_BIT_FILE_SUFFIX + j + ".bit").getCanonicalPath();
							
							data2memCommand[4] = sQPrevBitFile;
							data2memCommand[6] = colMemFiles.get(fileKey).getCanonicalPath();
							data2memCommand[8] = tagValue;
							if (j == (pieceList.getLength() - 1)) {
								data2memCommand[11] = finalBitFile.getCanonicalPath();
								colBitFiles.put(fileKey, finalBitFile);
							}
							else {
								data2memCommand[11] = sQIntermediateBitFile;
								colBitFiles.put(fileKey, new File(sQIntermediateBitFile));
							}
							bwLog.write("\t" + Arrays.toString(data2memCommand));
							bwLog.newLine();
							
							execSynchronously(data2memCommand, programmerPath);

							bwLog.write("\tExit code : " + exitCode);
							bwLog.newLine();
							bwLog.newLine();
							
							if (exitCode != 0) {
								bSuccess = false;
								break process_assembly;
							}

							sQPrevBitFile = sQIntermediateBitFile;
						}
					}
				}
				bwLog.newLine();
				if (bSuccess)
				{
					bSuccess = finalBitFile.isFile();
					if (!bSuccess)
					{
						bwLog.write("Output file could not be created");
						bwLog.newLine();
						bwLog.write("Game generation file may have an error.");
						bwLog.newLine();
					}
				}
			}
			catch (IOException e) {
				System.err.println("CreateFinalBitFile()\t" + e.getMessage());
				bSuccess = false;
			}
			return bSuccess;
		}

		
		private void DeleteTemporaryFiles()
		{
			for (File iteratorFile : colGroupFiles.values())
				iteratorFile.delete();		// No Exception is thrown.
			colGroupFiles.clear();
			
			for (File iteratorFile : colMemFiles.values())
				iteratorFile.delete();		// No Exception is thrown.
			colMemFiles.clear();

			for (File iteratorFile : colBitFiles.values())
				iteratorFile.delete();		// No Exception is thrown.
			colBitFiles.clear();
			
			buildPath.delete();		// No Exception is thrown.
		}


		private File DetectJTAGchain() throws IOException
		{
			File bscanBitFile = null;
			String[] scanJTAG = {q_papilio_prog_exe, "-j"};

			bwLog.write("Detecting JTAG chain\t" + Arrays.toString(scanJTAG));
			bwLog.newLine();
			bwLog.newLine();
			
			bLookforDesc = true;
	    	deviceID = "";
			execSynchronously(scanJTAG, programmerPath);
			bLookforDesc = false;
			
			if (!deviceID.isEmpty()) {
				if (deviceID.equals("XC3S250E"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc3s250e.bit");
				else if (deviceID.equals("XC3S500E"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc3s500e.bit");
				else if (deviceID.equals("XC3S100E"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc3s100e.bit");
				else if (deviceID.equals("XC6SLX9"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_lx9.bit");
			}
			
			bwLog.newLine();
			bwLog.write("\tExit code : " + exitCode);
			bwLog.newLine();
			bwLog.newLine();
			
			return bscanBitFile;
		}
		
		private int BurnToFPGA() throws IOException
		{
			String[] commandLine = {q_papilio_prog_exe, "-v", 
									"-f", UIHelpers.CanonicalPath(finalBitFile)};
			bwLog.write("Writing to FPGA...");
			bwLog.newLine();
			bwLog.write("\t" + Arrays.toString(commandLine));
			bwLog.newLine();
			bwLog.newLine();

			execSynchronously(commandLine, programmerPath);

			return exitCode;
		}

		private int BurnToSPIFlash() throws IOException
		{
	    	bscanSPIBitFile = DetectJTAGchain();
			
			if (bscanSPIBitFile != null)
			{
				String[] commandLine = {q_papilio_prog_exe, "-v", 
										"-f", UIHelpers.CanonicalPath(finalBitFile), 
										"-b", UIHelpers.CanonicalPath(bscanSPIBitFile), 
										"-sa", "-r"};
				bwLog.write("Writing to SPI Flash and triggering reconfiguration...");
				bwLog.newLine();
				bwLog.write("\t" + Arrays.toString(commandLine));
				bwLog.newLine();
				bwLog.newLine();
				
				execSynchronously(commandLine, programmerPath);
				
				bwLog.newLine();
				bwLog.write("\tExit code : " + exitCode);
				bwLog.newLine();
				bwLog.newLine();
				bwLog.write("Displaying current status of FPGA...");
				bwLog.newLine();
				bwLog.write("\t" + Arrays.toString(commandLine));
				bwLog.newLine();
				bwLog.newLine();

				execSynchronously(new String[] {q_papilio_prog_exe, "-c"}, programmerPath);
			}
			return exitCode;
		}

		
		private void execSynchronously(String[] command, File workingDir)
		{
		    Process process = null;
		    
		    try {
		      process = Runtime.getRuntime().exec(command, null, workingDir);
		    } catch (IOException e) {
		      System.err.println("()\t" + e.getMessage());
		    }
		    if(process == null)
		    {
		    	return;
		    }
		    // any output?
		    MessageSiphon in = 
		    	new MessageSiphon("Message-Siphon-StdOut", process.getInputStream(), this);
		    // any error message?
		    MessageSiphon err = 
		    	new MessageSiphon("Message-Siphon-StdErr", process.getErrorStream(), this);

		    // Kick both of them off.
		    err.KickOff();
		    in.KickOff();

		    // wait for the exec'd process to finish.  if interrupted
		    // before waitFor returns, continue waiting
		    boolean burning = true;
		    while (burning) {
		      try {
	/*	------------------------------------------------------------------------------------
	 * 		One thread can wait for another thread to terminate by using one of the other
	 * 		thread's join methods.
	 * 		When in.getThread().join() returns, in.run is guaranteed to have finished.
	 * 		If in.run is already finished by the time in.getThread().join() is called,
	 * 		in.getThread().join() returns immediately. 
	 *	------------------------------------------------------------------------------------ */
		    	if (in.getThread() != null)
		    		in.getThread().join();

	/*	------------------------------------------------------------------------------------
	 * 		One thread can wait for another thread to terminate by using one of the other
	 * 		thread's join methods.
	 * 		When err.getThread().join() returns, err.run is guaranteed to have finished.
	 * 		If err.run is already finished by the time err.getThread().join() is called,
	 * 		err.getThread().join() returns immediately. 
	 *	------------------------------------------------------------------------------------ */
				if (err.getThread() != null)
					err.getThread().join();

	/*	------------------------------------------------------------------------------------
	 * 		waitFor causes the current thread to wait, if necessary, until the process 
	 * 		represented by the Process object on which .waitFor() is called, has terminated. 
	 * 		waitFor method returns immediately if the subprocess has already terminated. 
	 * 		If the subprocess has not yet terminated, the calling thread will be blocked 
	 * 		until the subprocess exits. 
	 *	------------------------------------------------------------------------------------ */
				exitCode = process.waitFor();
				if (exitCode != 0) {
					System.out.println(Arrays.toString(command));
					System.out.println("Exit code is " + exitCode);
				}

				burning = false;
		      } catch (InterruptedException ignored) { }
		    }
		}

		/**
		 * Part of the MessageConsumer interface.
		 * This is called whenever a piece (usually a line) of error message is
		 * spewed out from the compiler. The errors are parsed for their contents
		 * and line number, which is then reported back to Editor.
		 */
		@Override
		public void DeliverMessage(final String stdline)
		{
			try {
				if (bCollectOutput) {
					if ((stdline.contains("MikeJ")) || (stdline.contains("INFO :")) || 
						(stdline.isEmpty()))
						;	// Ignore
					else
					//  data2mem expects "out.mem" to have Unix line ending
						bwMem.write(stdline);	
				}
				else if (bLookforDesc) {
					if (deviceID.isEmpty()) {
						int pos = stdline.lastIndexOf("Desc: ");
						if (pos != -1) {
							deviceID = stdline.substring(pos + "Desc: ".length(), stdline.length());
						}
					}
					bwLog.write("\t" + stdline);
					bwLog.newLine();
				}
				else {
					sLastErrorMessage = stdline;
					System.out.println(stdline);
					bwLog.write("\t" + stdline);
					bwLog.newLine();
				}
			}
			catch (IOException e) {
				System.err.println("DeliverMessage()\t" + e.getMessage());
				bMemSuccess = false;
			}
		}
	
	}

}
