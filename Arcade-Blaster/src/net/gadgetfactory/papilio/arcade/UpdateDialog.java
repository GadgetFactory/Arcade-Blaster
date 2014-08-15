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
import static net.gadgetfactory.papilio.arcade.PapilioArcade.currTheme;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.programSettings;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.urlUpdateGamesBase;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import net.gadgetfactory.shared.UIHelpers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class UpdateDialog extends JDialog implements ItemListener,
		ActionListener, ListSelectionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 625354969945252531L;
	private final int CELL_X_PADDING = 4;
	private final int CELL_Y_PADDING = 2;

	private final PapilioArcade parentFrame;
	private ProgressGlassPane glpUpdate = new ProgressGlassPane(null, true);
	private JTable grdUpdates;
	private GameUpdateTableModel dataModel;
	private TableRowSorter<GameUpdateTableModel> sorter;
	private GameExcludeFilter gameFilter = new GameExcludeFilter();

	private AsyncWebImageBox aixMonogram, aixScreenshot;
	// private JCheckBox chkLastUpdate = new
	// JCheckBox("Show games since last update");
	private JCheckBox chkAvailable = new JCheckBox("Show games in DB");
	private JCheckBox chkInstalled = new JCheckBox("Show installed games", true);
	private JCheckBox chkAvailableHardware = new JCheckBox(
			"Show games for available hardware only", true);
	private JButton btnMarkAll = new JButton("Mark all");
	private JButton btnUpdateDB = new JButton("Update DB");

	private Map<String, Boolean> colGameInstallStatus = new HashMap<String, Boolean>(
			16);
	private Map<String, Boolean> colHardware = new HashMap<String, Boolean>(6);

	private static File latestArcadeUpdatesFile;
	private final File screenshotsPath;
	private final File monogramsPath;
	private final File imagesFolder;
	private final File helpFolder = new File("help");
	private final File helpImagesPath = new File(helpFolder, "images");

	public UpdateDialog(PapilioArcade fraMain, Document docGameXML,
			Document docHardwareXML, File romsFolder, File imagesFolder) {
		super(fraMain, "Update Games DB", true); // Constructor should be 1st
													// statement

		final int PANEL_X_MARGIN = 13;
		final int PANEL_Y_MARGIN = 10;

		parentFrame = fraMain;
		Container contentPane = this.getContentPane();
		Box boxFilters = Box.createVerticalBox();
		Box boxButtons = Box.createVerticalBox();
		Box boxPreview = Box.createVerticalBox();
		Insets emptyInsets = new Insets(0, 0, 0, 0);
		Insets gridInsets = new Insets(PANEL_Y_MARGIN, PANEL_X_MARGIN,
				PANEL_Y_MARGIN, PANEL_X_MARGIN);
		Dimension dimUpdateDB = btnUpdateDB.getPreferredSize();
		File thumbsupFile, tickmarkFile, crossFile;
		int statusImageHeight;

		this.imagesFolder = imagesFolder;
		screenshotsPath = new File(imagesFolder, "screenshots");
		monogramsPath = new File(imagesFolder, "monograms");

		/*
		 * 0) Collect Ids and installed status of games present in user's
		 * Games.xml file.
		 */

		CacheGameStatus(docGameXML, romsFolder);

		/* 1) Collect Ids of hardware present in user's Hardware.xml file. */

		CacheAvailableHardware(docHardwareXML);

		/* 2) Create grid displaying available updates */

		/*
		 * We don't want to instantiate GameUpdateTableModel here - defer it
		 * until PopulateForm() is called. So, create JTable to use dummy
		 * DefaultTableModel with very small memory footprint - just 1 row and 1
		 * column.
		 */
		grdUpdates = new JTable(1, 1);
		grdUpdates.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		grdUpdates.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		statusImageHeight = grdUpdates.getRowHeight(); // Position is important!
		grdUpdates.setRowMargin(CELL_Y_PADDING);
		grdUpdates.setRowHeight(grdUpdates.getRowHeight() + 2 * CELL_Y_PADDING);

		/*
		 * Check whether image files required for displaying "Sopported" and
		 * "Installed" status of games are available or not. Install our own
		 * GameStatusRenderer only if these images are present. Otherwise
		 * default Boolean renderer (checkbox) will do just fine.
		 */
		thumbsupFile = new File(imagesFolder, "game_installed.png");
		tickmarkFile = new File(imagesFolder, "game_supported.png");
		crossFile = new File(imagesFolder, "game_cantplay.png");
		if ((thumbsupFile.isFile()) && (tickmarkFile.isFile())
				&& (crossFile.isFile()))
			grdUpdates.setDefaultRenderer(Boolean.class,
					new GameStatusRenderer(thumbsupFile, tickmarkFile,
							crossFile, statusImageHeight));

		/*
		 * 3) Create ImageBoxes, for displaying monogram and screenshot, stacked
		 * vertically
		 */

		// Provide border and margin around ImageBoxes, otherwise their top side
		// of
		// will touch top side of dialog and so on.
		boxPreview.setBorder(UIHelpers.CreateTitledBorderwMargin("",
				PANEL_Y_MARGIN, 0, PANEL_Y_MARGIN, PANEL_X_MARGIN));

		// Add a fixed space (5 pixels vertically) before Monogram ImageBox
		// boxPreview.add(Box.createRigidArea(new Dimension(0, 5)));
		// Monogram ImageBox
		aixMonogram = new AsyncWebImageBox(currTheme.getMonogramWidth(),
				currTheme.getMonogramHeight());
		boxPreview.add(aixMonogram);
		// Screenshot ImageBox
		aixScreenshot = new AsyncWebImageBox(currTheme.getScreenshotWidth(),
				currTheme.getScreenshotHeight());
		boxPreview.add(aixScreenshot);

		/* ) Create filter CheckBoxes stacked vertically */

		// Provide empty space (margin) inside the Box, otherwise bottom sides
		// of
		// CheckBoxes will butt against bottom side of grid cell (dialog) and so
		// on.
		boxFilters.setBorder(BorderFactory.createEmptyBorder(PANEL_Y_MARGIN,
				PANEL_X_MARGIN, PANEL_Y_MARGIN, PANEL_X_MARGIN));

		// boxFilters.add(chkLastUpdate);
		// chkAvailable.setAlignmentX(Component.LEFT_ALIGNMENT);
		boxFilters.add(chkAvailable);
		// chkInstalled.setAlignmentX(0.1f);
		// As "Show available games" checkbox is unticked by default, disable
		// "Show installed games"
		chkInstalled.setEnabled(false);
		boxFilters.add(chkInstalled);
		// chkAvailableHardware.setAlignmentX(Component.LEFT_ALIGNMENT);
		boxFilters.add(chkAvailableHardware);
		// UIHelpers.PrintSizes("Available Games Checkbox", chkAvailable,
		// "Installed Games Checkbox", chkInstalled,
		// "Available Hardware Checkbox", chkAvailableHardware);

		/* ) Create buttons stacked vertically */

		// Provide empty space (margin) inside the Box, otherwise bottom sides
		// of
		// CheckBoxes will butt against bottom side of grid cell (dialog) and so
		// on.
		boxButtons.setBorder(BorderFactory.createEmptyBorder(PANEL_Y_MARGIN,
				PANEL_X_MARGIN, PANEL_Y_MARGIN, PANEL_X_MARGIN));

		/*
		 * ----------------------------------------------------------------------
		 * -------------- NOTE: A button's maximum size is generally the same as
		 * its preferred size and BoxLayout never makes a button wider than its
		 * maximum size. Thus, if you want the button to be drawn wider, then
		 * you need to change its maximum size.
		 * ----------------------------------
		 * --------------------------------------------------
		 */

		// Safety, just in case width of [Mark all] button comes more than
		// [Update DB].
		if (dimUpdateDB.width < btnMarkAll.getPreferredSize().width) {
			dimUpdateDB.width = btnMarkAll.getPreferredSize().width;
			btnUpdateDB.setMaximumSize(dimUpdateDB);
			btnUpdateDB.setPreferredSize(dimUpdateDB);
		}
		/*
		 * If we do not set size explicitly, then Swing makes width of [Mark
		 * all] button < that of [Update DB] button - owing to their captions
		 * being different. Override this by making make width of both buttons
		 * same. Note that we HAVE to call .setMaximumSize() for [Mark all]
		 * button - otherwise its width does not change.
		 */
		btnMarkAll.setMaximumSize(dimUpdateDB);
		btnMarkAll.setPreferredSize(dimUpdateDB); // safety

		// The default X alignment of a JButton is LEFT_ALIGNMENT.
		btnMarkAll.setAlignmentX(/* Component.CENTER_ALIGNMENT */0.7f);
		btnUpdateDB.setAlignmentX(/* Component.CENTER_ALIGNMENT */0.7f);

		boxButtons.add(btnMarkAll);
		// Add a fixed space (10 pixels vertically) between [Mark all] and
		// [Update DB] buttons.
		boxButtons.add(Box.createRigidArea(new Dimension(0, 10)));
		boxButtons.add(btnUpdateDB);

		/* ) Set GridBagLayout for dialog */

		// TODO: Write why GridBagLayout is suitable and notes from
		// "An Introduction to GUI...".
		this.setLayout(new GridBagLayout());

		/*
		 * ) Add constituent controls of GridBagLayout along with their
		 * GridBagConstraints
		 */

		// Row Index 0
		// Updates grid (Column 0)
		contentPane.add(new JScrollPane(grdUpdates), new GridBagConstraints(0,
				0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, gridInsets, 0, 0));
		// Box containing Monogram and Screenshot ImageBoxes (Column 1)
		contentPane.add(boxPreview, new GridBagConstraints(1, 0, 1, 1, 0.36,
				1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				emptyInsets, 0, 0));

		// Row Index 1
		// Filters Box containing CheckBoxes (Column 0)
		contentPane.add(boxFilters, new GridBagConstraints(0, 1, 1, 1, 1.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				emptyInsets, 0, 0));
		// Butons Box (Column 1)
		contentPane.add(boxButtons, new GridBagConstraints(1, 1, 1, 1, 0.36,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				emptyInsets, 0, 0));

		/* ) Set up dialog */

		this.setGlassPane(glpUpdate);
		this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		// this.setResizable(false); // No need for resizable dialog

		/*
		 * ----------------------------------------------------------------------
		 * -------------- Window.pack() Causes this Window to be sized to fit
		 * the preferred size and layouts of its subcomponents. If the window
		 * and/or its owner are not yet displayable, both are made displayable
		 * before calculating the preferred size. The Window will be validated
		 * after the preferredSize is calculated.
		 * --------------------------------
		 * ----------------------------------------------------
		 */
		/*
		 * ----------------------------------------------------------------------
		 * -------------- After a component is created it is in the invalid
		 * state by default. The Window.pack method validates the window and
		 * lays out the window's component hierarchy for the first time.
		 * --------
		 * --------------------------------------------------------------
		 * --------------
		 */
		this.pack();

		/*
		 * Call to this.setLocationRelativeTo(fraMain) must be AFTER
		 * this.pack(). Otherwise, top-left of dialog is placed at the center of
		 * screen, instead of placing center of dialog at the center of the
		 * screen.
		 */

		/*
		 * ----------------------------------------------------------------------
		 * -------------- Window.setLocationRelativeTo(Component c) Sets the
		 * location of the window relative to the specified component. If the
		 * component is not currently showing, or c is null, the window is
		 * placed at the center of the screen. The center point can be
		 * determined with GraphicsEnvironment.getCenterPoint If the bottom of
		 * the component is offscreen, the window is placed to the side of the
		 * Component that is closest to the center of the screen. So if the
		 * Component is on the right part of the screen, the Window is placed to
		 * its left, and visa versa.
		 * --------------------------------------------
		 * ----------------------------------------
		 */
		this.setLocationRelativeTo(fraMain); // Position is important!

		/* ) Add event listeners for dialog and controls */

		grdUpdates.getSelectionModel().addListSelectionListener(this);
		// chkLastUpdate.addItemListener(this);
		chkAvailable.addItemListener(this);
		chkInstalled.addItemListener(this);
		chkAvailableHardware.addItemListener(this);

		btnMarkAll.addActionListener(this);
		btnUpdateDB.addActionListener(this);
	}

	// Used by UpdateDialog constructor only
	private void CacheGameStatus(Document docGameXML, File romsFolder) {
		NodeList gameList = docGameXML.getElementsByTagName("game");
		Element elmGame;
		File gamePath;
		int i;
		String gameid;

		for (i = 0; i < gameList.getLength(); i++) {
			elmGame = (Element) gameList.item(i);
			gameid = elmGame.getAttribute("id");
			gamePath = new File(romsFolder, gameid);
			colGameInstallStatus.put(gameid,
					new Boolean(gamePath.isDirectory()));
		}
	}

	// Used by UpdateDialog constructor only
	private void CacheAvailableHardware(Document docHardwareXML) {
		NodeList hardwareList = docHardwareXML.getElementsByTagName("hardware");
		Element elmHardware;
		int i;

		for (i = 0; i < hardwareList.getLength(); i++) {
			elmHardware = (Element) hardwareList.item(i);
			colHardware.put(elmHardware.getAttribute("id"),
					Boolean.valueOf(true));
		}
	}

	public static boolean CheckForUpdates(List<Boolean> alPossibleErrors) {
		BufferedReader br = null;
		FileOutputStream fout = null;
		BufferedWriter bw = null;
		boolean bError = false;
		String sLine;

		if (programSettings.EnsureSettingsFolder())
			latestArcadeUpdatesFile = new File(
					programSettings.getSettingsPath(),
					"latest-arcade-updates.txt");
		else
			return false;

		alPossibleErrors.add(Boolean.FALSE); // IP error
		alPossibleErrors.add(Boolean.FALSE); // Timeout
		alPossibleErrors.add(Boolean.FALSE); // No updates found
		try {
			URLConnection ucArcadeGameUpdates = new URL(urlUpdateGamesBase,
					"arcade-updates.txt").openConnection();
			ucArcadeGameUpdates.setConnectTimeout(10000);

			br = new BufferedReader(new InputStreamReader(
					ucArcadeGameUpdates.getInputStream(), "UTF-8"));
			fout = new FileOutputStream(latestArcadeUpdatesFile);
			bw = new BufferedWriter(new OutputStreamWriter(fout, "UTF-8"));
			while ((sLine = br.readLine()) != null) {
				bw.write(sLine);
				bw.newLine();
			}
			bw.flush();
		} catch (UnknownHostException e) {
			// Thrown to indicate that the IP address of a host could not be
			// determined.
			// Most probably, user's internet connection is down.
			System.err.println("UpdateDialog.CheckForUpdates\t"
					+ e.getMessage());
			alPossibleErrors.set(0, Boolean.TRUE);
		} catch (SocketTimeoutException e) {
			System.err.println("UpdateDialog.CheckForUpdates\t"
					+ e.getMessage());
			alPossibleErrors.set(1, Boolean.TRUE);
		} catch (MalformedURLException e) {
			// Should never get here
			System.err.println("UpdateDialog.CheckForUpdates\t"
					+ e.getMessage());
		} catch (IOException e) {
			System.err.println("UpdateDialog.CheckForUpdates\t"
					+ e.getMessage());
			bError = true;
		} finally {
			if (br != null) {
				try {
					br.close(); // This also closes (!?)
								// ucArcadeGameUpdates.getInputStream()
								// and frees network resources.
				} catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
			if (bw != null) {
				try {
					bw.close();
					fout.close();
				} catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
		}

		return !bError;
	}

	public void PopulateForm() {
		// Don't initialize here.
		TableColumnModel defColModel;
		TableColumn genericColumn;
		TableCellRenderer renderer;
		Component comp;
		int colPreferredWidth, headerPreferredWidth, viewportWidth = 0;

		/*
		 * 1) Populate GameUpdateTableModel - the JTable data model as well as
		 * JTable
		 */

		dataModel = new GameUpdateTableModel();
		grdUpdates.setModel(dataModel);

		defColModel = grdUpdates.getColumnModel();
		defColModel.setColumnMargin(CELL_X_PADDING);

		/* 2) Set preferred width of each column just wide as its longest entry */

		// By default, width of each column is 75 pixels.
		for (int colIndex = 0; colIndex < defColModel.getColumnCount(); colIndex++) {
			genericColumn = defColModel.getColumn(colIndex);

			if (colIndex == GameUpdateTableModel.CI_GAME_ID) {
				// Do not remove gameid column, just hide it. Removing the
				// column would
				// break the column indexes which are used in
				// GameStatusRenderer.
				genericColumn.setMinWidth(0);
				genericColumn.setPreferredWidth(0);
				genericColumn.setMaxWidth(0);
				genericColumn.setResizable(false);
				continue;
			}

			renderer = grdUpdates.getTableHeader().getDefaultRenderer();
			headerPreferredWidth = renderer.getTableCellRendererComponent(
					grdUpdates, genericColumn.getHeaderValue(), false, false,
					-1, colIndex).getPreferredSize().width;

			/*
			 * By default, headerRenderer and cellRenderer are null for a
			 * column. We need to get TableCellRenderer in order to calculate
			 * preferred width of the widest entry of that column. We already
			 * have widest entry with us (in GameUpdateTableModel). The best way
			 * to get TableCellRenderer is to call JTable.getDefaultRenderer().
			 */
			renderer = grdUpdates.getDefaultRenderer(dataModel
					.getColumnClass(colIndex));

			/*
			 * Preferred width of widest entry should not be dependent on
			 * whether its cell is selected or not, or whether its cell has
			 * focus or not. So, it will be safe to pass false, false as
			 * arguments. Similarly, preferred width should not be dependent on
			 * row index and we can pass 1 as argument in following call.
			 * However, passing proper colIndex is must.
			 */
			colPreferredWidth = renderer.getTableCellRendererComponent(
					grdUpdates, dataModel.getWidestValue(colIndex), false,
					false, 1, colIndex).getPreferredSize().width;

			colPreferredWidth += 2 * CELL_X_PADDING; // Account for X padding
			if (headerPreferredWidth >= colPreferredWidth)
				colPreferredWidth = headerPreferredWidth + 12; // Account for
																// sorting
																// legend

			genericColumn.setPreferredWidth(colPreferredWidth); // Auto-size
																// column
			viewportWidth += colPreferredWidth;

			// Preventing column resizing does not help with layout "problems"
			// in any way.
			switch (colIndex) {
			case GameUpdateTableModel.CI_GAME_YEAR:
			case GameUpdateTableModel.CI_GAME_AVAILABLE:
			case GameUpdateTableModel.CI_GAME_INSTALLED:
				genericColumn.setResizable(false);
				break;
			}
		}
		grdUpdates.setPreferredScrollableViewportSize(new Dimension(
				viewportWidth, 300));

		/* 3) Set up and apply row filter to updates */

		sorter = new TableRowSorter<GameUpdateTableModel>(dataModel);
		grdUpdates.setRowSorter(sorter); // At this point, sorting is enabled by
											// Java.

		/*
		 * At startup, we don't want to show games present in Games.xml nor we
		 * want to show games for those hardware which are not present in
		 * Hardware.xml file. This is to avoid clutter.
		 */
		gameFilter.Init4GUI();
		sorter.setRowFilter(gameFilter);

		this.pack();
		this.setVisible(true);
	}

	/*
	 * --------------------------------------------------------------------------
	 * ---------- NOTE: For JCheckBox, as with JToggleButton, the better
	 * listener to subscribe to is an ItemListener. The ItemEvent passed to the
	 * itemStateChanged() method of ItemListener includes the CURRENT STATE of
	 * the check box. This allows you to respond appropriately, without need to
	 * find out the current button state.
	 * ----------------------------------------
	 * --------------------------------------------
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		/*
		 * 1) Enable/disable "Show installed games" checkbox w.r.t.
		 * "Show available games" checkbox
		 */

		if (((JCheckBox) e.getSource()) == chkAvailable)
			chkInstalled.setEnabled(e.getStateChange() == ItemEvent.SELECTED);

		/*
		 * At this point, we can query state of each of the JCheckboxes and it
		 * is perfectly current. Thus, no need for any special handling for the
		 * JCheckbox that has currently changed state.
		 */
		gameFilter.Init4GUI();
		sorter.setRowFilter(gameFilter);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String currAction = e.getActionCommand();
		List<String> alSelGames = new ArrayList<String>(12);

		if (currAction.equals("Mark all")) {
			for (int rowIndex = 0; rowIndex < dataModel.noOfRows; rowIndex++) {
				// Though .isCellEditable() call is redundant, it is kept for
				// safety.
				if (dataModel.isCellEditable(rowIndex,
						GameUpdateTableModel.CI_GAME_AVAILABLE)) {
					if (dataModel.getValueAt(rowIndex,
							GameUpdateTableModel.CI_GAME_AVAILABLE).equals(
							Boolean.FALSE))
						dataModel.setValueAt(Boolean.TRUE, rowIndex,
								GameUpdateTableModel.CI_GAME_AVAILABLE);
				}
			}
		}

		else if (currAction.equals("Update DB")) {
			if (FormValid(alSelGames)) {
				glpUpdate.resetProgress("Connecting");
				glpUpdate.setVisible(true);
				new UpdateWorker(alSelGames).execute();
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		String gameid;

		if (!e.getValueIsAdjusting()) {
			gameid = (String) dataModel.getValueAt(grdUpdates
					.convertRowIndexToModel(grdUpdates.getSelectedRow()),
					GameUpdateTableModel.CI_GAME_ID);
			try {
				if (colGameInstallStatus.containsKey(gameid)) {
					aixMonogram.setLocalImage(imagesFolder + "/monograms/"
							+ gameid + ".png");
					aixScreenshot.setLocalImage(imagesFolder + "/screenshots/"
							+ gameid + ".png");
				} else {
					aixMonogram.setImage(new URL(urlUpdateGamesBase, "logos/"
							+ gameid + ".png"));
					aixScreenshot.setImage(new URL(urlUpdateGamesBase,
							"screenshots/" + gameid + ".png"));
				}
			} catch (MalformedURLException e1) {
				System.err.println("UpdateDialog.valueChanged\t"
						+ e1.getMessage());
			}
		}
		// else
		// System.out.println("UpdateDialog : valueChanged\tselection in progress!");
	}

	private boolean FormValid(List<String> alSelGames) {
		for (int rowIndex = 0; rowIndex < dataModel.noOfRows; rowIndex++) {
			if (dataModel.isCellEditable(rowIndex,
					GameUpdateTableModel.CI_GAME_AVAILABLE)) {
				if (dataModel.getValueAt(rowIndex,
						GameUpdateTableModel.CI_GAME_AVAILABLE).equals(
						Boolean.TRUE)) {
					alSelGames.add(dataModel.getValueAt(rowIndex,
							GameUpdateTableModel.CI_GAME_ID).toString());
					alSelGames.add(dataModel.getValueAt(rowIndex,
							GameUpdateTableModel.CI_GAME_NAME).toString());
				}
			}
		}

		if (alSelGames.isEmpty()) {
			JOptionPane
					.showMessageDialog(
							this,
							"Please mark at least one game before proceeding to update Games DB",
							"No Game Marked", JOptionPane.WARNING_MESSAGE);
			return false;
		} else
			return true;
	}

	/*
	 * --------------------------------------------------------------------------
	 * ---------- DefaultTableCellRenderer IMPLEMENTATION NOTE: This class
	 * inherits from JLabel, a standard component class. However JTable employs
	 * a unique mechanism for rendering its cells and therefore requires some
	 * slightly modified behavior from its cell renderer. The table class
	 * defines a single cell renderer and uses it as a as a rubber-stamp for
	 * rendering all cells in the table; it renders the first cell, changes the
	 * contents of that cell renderer, shifts the origin to the new location,
	 * re-draws it, and so on.
	 * 
	 * The standard JLabel component was not designed to be used this way and we
	 * want to AVOID TRIGGERING A revalidate EACH TIME THE CELL IS DRAWN. This
	 * would greatly decrease performance because the revalidate message would
	 * be passed up the hierarchy of the container to determine whether any
	 * other components would be affected. As the renderer is only parented for
	 * the lifetime of a painting operation we similarly want to avoid the
	 * overhead associated with walking the hierarchy for painting operations.
	 * So this class overrides the validate, invalidate, revalidate, repaint,
	 * and firePropertyChange methods to be NO-OPS and override the isOpaque
	 * method solely to improve performance. If you write your own renderer,
	 * please keep this performance consideration in mind.
	 * ----------------------
	 * --------------------------------------------------------------
	 */

	private class GameStatusRenderer implements TableCellRenderer {
		// TODO: Mention why DefaultTableCellRenderer is not used for displaying
		// status images
		// TODO: Write experiments with DefaultTableCellRenderer
		// private DefaultTableCellRenderer imageRenderer = new
		// DefaultTableCellRenderer();

		private TableCellRenderer imageIconRenderer = grdUpdates
				.getDefaultRenderer(ImageIcon.class);
		private TableCellRenderer booleanRenderer = grdUpdates
				.getDefaultRenderer(Boolean.class);
		private Component compRendered;

		private ImageIcon imgThumbsup, imgTickmark, imgCross;

		public GameStatusRenderer(File thumbsupFile, File tickmarkFile,
				File crossFile, int statusImageHeight) {
			// game installes
			imgThumbsup = new ImageIcon(UIHelpers.LoadBufferedImage(
					thumbsupFile, statusImageHeight, statusImageHeight));
			// game supported
			imgTickmark = new ImageIcon(UIHelpers.LoadBufferedImage(
					tickmarkFile, statusImageHeight, statusImageHeight));
			imgCross = new ImageIcon(UIHelpers.LoadBufferedImage(crossFile,
					statusImageHeight, statusImageHeight));
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (value == null)
				return new JLabel();

			row = grdUpdates.convertRowIndexToModel(row);
			column = grdUpdates.convertColumnIndexToModel(column);

			switch (column) {
			case GameUpdateTableModel.CI_GAME_AVAILABLE:

				if (!colHardware.containsKey(dataModel.getValueAt(row,
						GameUpdateTableModel.CI_GAME_HARDWARE))) {
					// not supported
					compRendered = imageIconRenderer
							.getTableCellRendererComponent(table, imgCross,
									isSelected, hasFocus, row, column);
				} else {
					if (dataModel.isCellEditable(row, column))
						compRendered = booleanRenderer
								.getTableCellRendererComponent(table, value,
										isSelected, hasFocus, row, column);
					else {
						if (value.equals(Boolean.TRUE)) {
							
							compRendered = imageIconRenderer
									.getTableCellRendererComponent(table,
											value, isSelected, hasFocus, row,
											column);
							if (compRendered instanceof JLabel) {
								((JLabel) compRendered).setIcon(imgTickmark);
								((JLabel) compRendered).setText("");
							}
						} else
							// Will execution ever reach here?
							compRendered = imageIconRenderer
									.getTableCellRendererComponent(table, null,
											isSelected, hasFocus, row, column);
					}
				}
				break;

			case GameUpdateTableModel.CI_GAME_INSTALLED:

				if (value.equals(Boolean.TRUE)) {
					// game installed
					compRendered = imageIconRenderer
							.getTableCellRendererComponent(table, value,
									isSelected, hasFocus, row, column);
					if (compRendered instanceof JLabel) {
						((JLabel) compRendered).setIcon(imgThumbsup);
						((JLabel) compRendered).setText("");
					}

				} else
					compRendered = imageIconRenderer
							.getTableCellRendererComponent(table, null,
									isSelected, hasFocus, row, column);
				break;
			}

			return compRendered;
		}

	}

	private class GameExcludeFilter extends
			RowFilter<GameUpdateTableModel, Integer> {
		private boolean bAvailable, bInstalled, bAvailableHardware;

		public void Init4GUI() {
			bAvailable = chkAvailable.isSelected();
			bInstalled = chkInstalled.isSelected();
			bAvailableHardware = chkAvailableHardware.isSelected();
		}

		// The include() method of filter is ALWAYS called for EACH row in data
		// model.
		@Override
		public boolean include(
				Entry<? extends GameUpdateTableModel, ? extends Integer> entry) {
			boolean bExclude = false;

			if (bAvailable) {
				// => "Show available games" checkbox is ticked.
				if (!bInstalled)
					// => "Show installed games" is unticked.
					// => We need to exclude those games which have been
					// installed.
					bExclude |= entry.getValue(
							GameUpdateTableModel.CI_GAME_INSTALLED).equals(
							Boolean.TRUE);
			} else {
				// => "Show available games" checkbox is unchecked.
				// => We need to exclude those games which are available, i.e
				// those games which
				// have entry in Games.xml.
				// bExclude |=
				// entry.getValue(GameUpdateTableModel.CI_GAME_AVAILABLE)
				// .equals(Boolean.valueOf(true));
				bExclude |= dataModel.isGamesXmlEntry(entry.getIdentifier()
						.intValue());
			}

			if (bAvailableHardware)
				// => "Show games for available hardware only" checkbox is
				// ticked.
				// => We need to exclude those games whose hardware has no entry
				// in Hardware.xml.
				bExclude |= !colHardware.containsKey(entry
						.getStringValue(GameUpdateTableModel.CI_GAME_HARDWARE));

			return !bExclude;
		}
	}

	private class GameUpdateTableModel extends AbstractTableModel {
		public static final int CI_GAME_ID = 0;
		public static final int CI_GAME_NAME = 1;
		public static final int CI_GAME_GENRE = 2;
		public static final int CI_GAME_YEAR = 3;
		public static final int CI_GAME_HARDWARE = 4;
		public static final int CI_GAME_DATE_ADDED = 5;
		public static final int CI_GAME_AUTHOR = 6;
		public static final int CI_GAME_AVAILABLE = 7;
		public static final int CI_GAME_INSTALLED = 8;
		private final int NO_OF_COLUMNS = 9;
		private final String SEPARATOR = "~%";

		private Map<String, Object> colFullGameList = new HashMap<String, Object>(
				100);
		private Map<Integer, Object> colWidestValues = new HashMap<Integer, Object>(
				6);
		private Map<Integer, Object> colGamesXmlRows = new HashMap<Integer, Object>(
				10);
		// ArrayList is deliberately not used for colGamesXmlRows as we need it
		// for lookup
		// purposes only. Performance of ArrayList.contains() is O(n) while that
		// of
		// HashMap is HaspMap.containsKey() is O(1).

		String[] columnNames = { "Id", "Game", "Genre", "Year", "Hardware",
				"Date added", "Author", "Supp.", "Inst." };
		private int noOfRows = 0;

		public GameUpdateTableModel() {
			final int NO_OF_TOKENS = NO_OF_COLUMNS - 2; // to account for
														// "Avai." & "Inst."
														// columns
			Pattern PATT_COMMA = Pattern.compile(",", Pattern.LITERAL);
			FileInputStream fin = null;
			BufferedReader br = null;
			String[] asWidestValue = new String[7];
			String sLine;
			boolean bInsideGameList = false;
			String[] asTokens;
			String sFieldValue;
			int noOfTokens = 0;
			boolean bAvailable = false, bInstalled = false;

			// TODO: Write down philosophy behind finding widest value in a
			// column.
			colWidestValues.put(new Integer(CI_GAME_YEAR), new Integer(2020));
			colWidestValues.put(new Integer(CI_GAME_DATE_ADDED), "31 Mar 2020");
			asWidestValue[CI_GAME_NAME] = "";
			asWidestValue[CI_GAME_GENRE] = "";
			asWidestValue[CI_GAME_HARDWARE] = "";
			asWidestValue[CI_GAME_AUTHOR] = "";

			try {
				fin = new FileInputStream(latestArcadeUpdatesFile);
				br = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
				while ((sLine = br.readLine()) != null) {
					sLine = sLine.trim();
					if ((sLine.isEmpty()) || (sLine.startsWith("#")))
						continue; // Ignore blank lines and comments

					if (bInsideGameList) {
						if ((sLine.startsWith("@Hardware"))
								|| (sLine.startsWith("@Platforms")))
							break; // End of full game list is here.

						asTokens = PATT_COMMA.split(sLine);
						noOfTokens = asTokens.length;
						if (noOfTokens != NO_OF_TOKENS)
							continue;

						for (int colIndex = 0; colIndex < NO_OF_TOKENS; colIndex++) {
							sFieldValue = asTokens[colIndex].trim();
							switch (colIndex) {
							case CI_GAME_ID:
								if (colGameInstallStatus
										.containsKey(sFieldValue)) {
									// => Current game does indeed has entry in
									// Games.xml
									bAvailable = true;
									bInstalled = colGameInstallStatus.get(
											sFieldValue).booleanValue();
									// User can never mark this game entry for
									// update.
									colGamesXmlRows.put(new Integer(noOfRows),
											null);
								} else {
									// => Current game does not have entry in
									// Games.xml
									bAvailable = false;
									bInstalled = false;
									// User can, obviously, mark this game entry
									// for update.
								}
								break;

							case CI_GAME_NAME:
							case CI_GAME_GENRE:
							case CI_GAME_HARDWARE:
							case CI_GAME_AUTHOR:
								if (asWidestValue[colIndex].length() < sFieldValue
										.length())
									asWidestValue[colIndex] = sFieldValue;
								break;
							}

							colFullGameList.put(
									noOfRows + SEPARATOR + colIndex,
									sFieldValue);
						}

						// Available column
						colFullGameList.put(noOfRows + SEPARATOR
								+ CI_GAME_AVAILABLE, new Boolean(bAvailable));
						/*
						 * ------------------------------------------------------
						 * ------------------------------ NOTE:
						 * Boolean.valueOf(boolean b) method may return a newly
						 * constructed instance or a cached instance. For
						 * efficiency, you should always use valueOf method, in
						 * preference to direct construction - new
						 * Boolean(boolean b), unless you really need distinct
						 * instances that have the same value.
						 * ------------------
						 * ------------------------------------
						 * ------------------------------
						 */
						// Installed column
						colFullGameList.put(noOfRows + SEPARATOR
								+ CI_GAME_INSTALLED,
								Boolean.valueOf(bInstalled));

						noOfRows++;
					} else {
						if (sLine.startsWith("@Games"))
							bInsideGameList = true;
					}
				}

				colWidestValues.put(new Integer(CI_GAME_NAME),
						asWidestValue[CI_GAME_NAME]);
				colWidestValues.put(new Integer(CI_GAME_GENRE),
						asWidestValue[CI_GAME_GENRE]);
				colWidestValues.put(new Integer(CI_GAME_HARDWARE),
						asWidestValue[CI_GAME_HARDWARE]);
				colWidestValues.put(new Integer(CI_GAME_AUTHOR),
						asWidestValue[CI_GAME_AUTHOR]);
			} catch (IOException e) {
				System.err
						.println("UpdateDialog.GameUpdateTableModel.(Constr)\t"
								+ e.getMessage());
			} finally {
				if (br != null) {
					try {
						br.close(); // This also closes fin FileInputStream.
					} catch (IOException ioex) {
						System.err.println(ioex.getMessage());
					}
				}
			}

		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case CI_GAME_AVAILABLE:
			case CI_GAME_INSTALLED:
				return Boolean.class;
			case CI_GAME_YEAR:
				return Integer.class;
			default:
				return String.class;
			}
		}

		@Override
		public String getColumnName(int col) {
			return columnNames[col];
		}

		@Override
		public int getColumnCount() {
			return NO_OF_COLUMNS;
		}

		@Override
		public int getRowCount() {
			return noOfRows;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return colFullGameList.get(rowIndex + SEPARATOR + columnIndex);
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			/*
			 * ------------------------------------------------------------------
			 * ------------------ NOTE: Integer.valueOf(boolean b) method may
			 * return a newly constructed instance or a cached instance. For
			 * efficiency, you should always use valueOf method, in preference
			 * to direct construction - new Integer(int i), unless you really
			 * need distinct instances that have the same value.
			 * ----------------
			 * --------------------------------------------------
			 * ------------------
			 */
			return (columnIndex == CI_GAME_AVAILABLE)
					&& (!colGamesXmlRows.containsKey(Integer.valueOf(rowIndex)) && (colHardware
							.containsKey(getValueAt(rowIndex, CI_GAME_HARDWARE))));
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			colFullGameList.put(rowIndex + SEPARATOR + columnIndex, aValue);
			this.fireTableCellUpdated(rowIndex, columnIndex);
		}

		public Object getWidestValue(int colIndex) {
			return colWidestValues.get(new Integer(colIndex));
		}

		public boolean isGamesXmlEntry(int rowIndex) {
			return colGamesXmlRows.containsKey(Integer.valueOf(rowIndex));
		}
	}

	private class UpdateWorker extends
			SwingWorker<List<String>, IntermediateResult> {
		private List<String> alSelGames;

		public UpdateWorker(List<String> alSelGames) {
			this.alSelGames = alSelGames;
		}

		// Executed on a worker (background) thread
		// Should never touch any Swing component
		@Override
		protected List<String> doInBackground() {
			URLConnection ucGameSupportPack;
			List<String> alAddedGames = new ArrayList<String>(12);
			IntermediateResult progressObj = new IntermediateResult();
			InputStream in = null;
			FileOutputStream fout = null;
			File supportPackFile, tempFile, entryFile = null;
			Enumeration<? extends ZipEntry> eze;
			ZipFile zfSupportPack = null;
			ZipEntry ze;
			int offset, contentLength;
			String gameid, sSupportPackName, sParent, sName;
			boolean bError, bInternetDown;

			for (int i = 0; i < alSelGames.size() - 1; i += 2) {
				progressObj.gameName = alSelGames.get(i + 1);
				progressObj.gameProgress = 0;
				publish(progressObj);

				bError = false;
				bInternetDown = false;
				gameid = alSelGames.get(i);
				sSupportPackName = gameid + "-support.zip";
				try {
					ucGameSupportPack = new URL(urlUpdateGamesBase, "packs/"
							+ sSupportPackName).openConnection();
					ucGameSupportPack.setConnectTimeout(10000);
					// ucGameSupportPack.setReadTimeout(20000);

					/*
					 * HTTP servers don't always close the connection exactly
					 * where the data is finished; therefore, you don't know
					 * when to stop reading. To download a binary file, it is
					 * more reliable to use a URLConnection's getContentLength()
					 * method to find the file's length, then read exactly the
					 * number of bytes indicated.
					 */
					contentLength = ucGameSupportPack.getContentLength();
					if (contentLength != -1) {
						// TODO: Mention reason why ZipInputSteam is not used
						// TODO: Mention reason why BufferedInputStream is not
						// used
						in = ucGameSupportPack.getInputStream();
						offset = 0;
						supportPackFile = new File(AppPath, sSupportPackName);
						fout = new FileOutputStream(supportPackFile);
						for (int c = in.read(); c != -1; c = in.read()) {
							fout.write(c);
							offset++;
							if ((offset % 250 == 0)) {
								progressObj.gameProgress = (int) (offset * 100.0f / contentLength) - 3;
								publish(progressObj);
							}
						}
						fout.flush();
						fout.close();
						in.close();

						if (offset != contentLength) {
							// => Only read offset bytes; Expected contentLength
							// bytes
							supportPackFile.delete();
							bError = true;
							continue;
						}

						progressObj.gameProgress = 100;
						publish(progressObj);
						zfSupportPack = new ZipFile(supportPackFile,
								ZipFile.OPEN_DELETE | ZipFile.OPEN_READ);
						eze = zfSupportPack.entries();

						while (eze.hasMoreElements()) {
							ze = eze.nextElement();
							if (ze.isDirectory())
								continue;

							tempFile = new File(ze.getName());
							sParent = tempFile.getParent();

							if (sParent == null) {
								sName = tempFile.getName();
								if (sName.equals(gameid + ".html"))
									entryFile = new File(helpFolder, gameid
											+ ".html");
								else if (sName.equals(gameid + ".png"))
									entryFile = new File(monogramsPath, gameid
											+ ".png");
								else if (sName.equalsIgnoreCase("Game.xml"))
									entryFile = new File("roms", "Game.xml");
								else if (sName.equalsIgnoreCase("Control.xml"))
									entryFile = new File(helpFolder,
											"Control.xml");
							} else if (sParent.equalsIgnoreCase("images"))
								entryFile = new File(helpImagesPath,
										tempFile.getName());
							else if (sParent.equalsIgnoreCase("screenshots"))
								entryFile = new File(screenshotsPath,
										tempFile.getName());

							in = zfSupportPack.getInputStream(ze);
							fout = new FileOutputStream(entryFile);
							for (int c = in.read(); c != -1; c = in.read()) {
								fout.write(c);
							}
							fout.flush();
							fout.close();
							in.close();

							if (entryFile.getName().equals("Game.xml"))
								parentFrame.ImportGameXML(entryFile);
						}

						zfSupportPack.close();
					}
				} catch (UnknownHostException e) {
					// Thrown to indicate that the IP address of a host could
					// not be determined.
					// Most probably, user's internet connection is down.
					bInternetDown = true;
					System.err.println("UpdateWorker.doInBackground\t"
							+ e.getMessage());
				} catch (SocketTimeoutException e) {
					bError = true;
					System.err.println("UpdateWorker.doInBackground\t"
							+ e.getMessage());
				} catch (MalformedURLException e) {
					// Should never get here
					System.err.println("UpdateWorker.doInBackground\t"
							+ e.getMessage());
				}
				/*
				 * --------------------------------------------------------------
				 * ---------------------- ZipException is a subclass of
				 * IOException that indicates the data in the zip file doesn't
				 * fit the zip format. In this case, the zip exception's message
				 * will contain more details, like
				 * "invalid END header signature" or
				 * "cannot have more than one drive." While these may be useful
				 * to a zip expert, in general they indicate that the file is
				 * corrupted, and there's not much that can be done about it.
				 * ----
				 * ----------------------------------------------------------
				 * ----------------------
				 */
				catch (ZipException zex) {
					bError = true;
					System.err.println("UpdateWorker.doInBackground\t"
							+ zex.getMessage());
				} catch (IOException e) {
					bError = true;
					System.err.println("UpdateWorker.doInBackground\t"
							+ e.getMessage());
				} finally {
					if (in != null) {
						try {
							in.close(); // This also closes (!?)
										// ucGameSupportPack.getInputStream()
										// and frees network resources.
						} catch (IOException ioex) {
							System.err.println(ioex.getMessage());
						}
					}
					if (fout != null) {
						try {
							fout.close();
						} catch (IOException ioex) {
							System.err.println(ioex.getMessage());
						}
					}
				}

			}

			return alAddedGames;
		}

		// Executed on EDT
		@Override
		protected void process(List<IntermediateResult> chunks) {
			for (IntermediateResult chunk : chunks) {
				glpUpdate.setProgress(chunk.gameName, chunk.gameProgress);
			}
		}

		// Executed on EDT
		@Override
		protected void done() {
			glpUpdate.setVisible(false);
			super.done();
		}
	}

	private static class IntermediateResult {
		String gameName;
		int gameProgress;
	}

}
