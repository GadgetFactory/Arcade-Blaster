/*
  Part of the Papilio Java Utility Library

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

package net.gadgetfactory.shared;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

public class UIHelpers
{
	private static JFileChooser m_CommonDialog = new JFileChooser();
	private static Component m_Parent;
	private static GraphicsConfiguration defGraphicsConfig;

	public static GraphicsConfiguration getDefaultGraphicsConfig() {
		return defGraphicsConfig;
	}

	
    public static void initVariables(Component parent) {
		UIHelpers.m_Parent = parent;
// TODO: What if user has multiple monitors or has configuration other than default? 
		defGraphicsConfig = 
				GraphicsEnvironment.getLocalGraphicsEnvironment().
					getDefaultScreenDevice().getDefaultConfiguration();
	}


    public static Rectangle getMaximizedBounds() {
    	// Get current screen resolution. rc.x and rc.y are always 0.
    	Rectangle rc = defGraphicsConfig.getBounds();
    	
    	// Account for Taskbar, etc which a maximized window cannot overlap.
    	Insets si = Toolkit.getDefaultToolkit().getScreenInsets(defGraphicsConfig);
    	rc.setSize(rc.width - si.left - si.right, rc.height - si.top - si.bottom);
    	
    	return rc;
    }

    
	/**
     * Debugging utility that prints location of component(s). 
     * 2nd/3rd component(s) (not argument(s)) may be null.
     */
    public static void PrintLocations(String c1Name, Component c1, String c2Name, Component c2, 
    								  String c3Name, Component c3)
    {
    	System.out.print(c1Name + " Location = " + c1.getLocation());
    	if (c2 != null)
    		System.out.print(", " + c2Name + " Location = " + c2.getLocation());
    	if (c3 != null)
    		System.out.print(", " + c3Name + " Location = " + c3.getLocation());
    	System.out.println();
    }

	/**
     * Debugging utility that prints component's minimum, preferred, and maximum sizes.
     * 2nd/3rd component(s) (not argument(s)) may be null.
     */
    public static void PrintSizes(String c1Name, Component c1, String c2Name, Component c2, 
			  					  String c3Name, Component c3)
    {
        System.out.println(c1Name + " : minimumSize = " + c1.getMinimumSize() + ", " + 
        					"preferredSize = " + c1.getPreferredSize() + ", " + 
        					"maximumSize = " + c1.getMaximumSize());
        if (c2 != null)
            System.out.println(c2Name + " : minimumSize = " + c2.getMinimumSize() + ", " + 
					"preferredSize = " + c2.getPreferredSize() + ", " + 
					"maximumSize = " + c2.getMaximumSize());
        if (c3 != null)
            System.out.println(c3Name + " : minimumSize = " + c3.getMinimumSize() + ", " + 
					"preferredSize = " + c3.getPreferredSize() + ", " + 
					"maximumSize = " + c3.getMaximumSize());
    }

    /**
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component in a column is as wide as the maximum
     * preferred width of the components in that column;
     * height is similarly determined for each row.
     * The parent is made just big enough to fit them all.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad x padding between cells
     * @param yPad y padding between cells
     */
    public static void makeCompactGrid(Container parent,
                                       int rows, int cols,
                                       int initialX, int initialY,
                                       int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
            return;
        }

        //Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width,
                                   getConstraintsForCell(r, c, parent, cols).
                                       getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        //Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height,
                                    getConstraintsForCell(r, c, parent, cols).
                                        getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

    /* Used by makeCompactGrid. */
    private static SpringLayout.Constraints getConstraintsForCell(
                                                int row, int col,
                                                Container parent,
                                                int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }

    /**
     * Gets the canonical path of File object. Canonical path is somehow more
     * "real" than absolute path for the File object.  
     * @param anyFile File object to be operated on
     * @return 
     * 		"" in case of IOException<br>
     * 		canonical path otherwise
     */
    public static String CanonicalPath(File anyFile)
	{
    	String sQFile = "";
		try {
/*	------------------------------------------------------------------------------------
 * 		Exactly what a canonical path is, and how it differs from an absolute path, 
 * 		is system-dependent, but it tends to mean that the path is somehow more real than 
 * 		the absolute path. Typically, if the full path contains aliases, shortcuts, shadows, 
 * 		or symbolic links of some kind, the canonical path resolves those aliases to the 
 * 		actual directories they refer to.
 *	------------------------------------------------------------------------------------ */
			sQFile = anyFile.getCanonicalPath();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return sQFile;
	}

    /** Exactly like CanonicalPath except returns File rather than String. */
    public static File CanonicalFile(File anyFile)
	{
    	File canonFile = null;
		try {
			canonFile = anyFile.getCanonicalFile();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return canonFile;
	}

    
    public static void CreateOptionButtonGroup(Box boxGeneric, Border bdrButtonGroup, 
    										   String[] elements, String[] commandActions, 
    										   Dimension elementDimension) {
        ButtonGroup group = new ButtonGroup();
        JRadioButton optElement;
        int i = 0;
    	
    	boxGeneric.setBorder(bdrButtonGroup);
    	
        for (String iterElement : elements) {
	        optElement = new JRadioButton(iterElement);
	        optElement.setActionCommand(commandActions[i]);
	        if (elementDimension != null) {
		        optElement.setPreferredSize(elementDimension);
		        optElement.setMaximumSize(elementDimension);
	        }

	        boxGeneric.add(optElement);
	        group.add(optElement);
	        i++;
        }
    }
    
    public static String SelectedOptionButton(Container container)
    {
    	for (Component optIterator : container.getComponents()) {
			if (optIterator instanceof JRadioButton) {
				JRadioButton optSelected = (JRadioButton) optIterator;
				if (optSelected.isSelected()) {
					return optSelected.getActionCommand();
				}
			}
		}
    	return "";
    }
    
    public static void SelectOption4Group(Container container, String selActionCommand)
    {
    	for (Component optIterator : container.getComponents()) {
			if (optIterator instanceof JRadioButton) {
				JRadioButton optSelected = (JRadioButton) optIterator;
				if (optSelected.getActionCommand().equals(selActionCommand)) {
					optSelected.setSelected(true);
				}
			}
		}
    }
 
    
    /**
     * Show the About box.
     */
    public static void DisplayAboutBox(Frame owner, File imageFile) 
	{
        Toolkit tk = Toolkit.getDefaultToolkit();
        
	    Image image = tk.getImage(imageFile.getAbsolutePath());
	    MediaTracker tracker = new MediaTracker(owner);
	    tracker.addImage(image, 0);
	    try {
	      tracker.waitForID(0);
	    } catch (InterruptedException e) { }
	    final Image aboutImage = image;

	    final Window window = new Window(owner) {
	    	@Override
			public void paint(Graphics g) {
	    		g.drawImage(aboutImage, 0, 0, null);
	    	}
	    };
	    
		window.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				window.dispose();
			}
		});

		int w = image.getWidth(owner);
		int h = image.getHeight(owner);
		Dimension screen = tk.getScreenSize();
		window.setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
		window.setVisible(true);
	}
    
    public static BufferedImage LoadBufferedImage(File imageFile)
    {
    	Graphics2D g2d;
    	BufferedImage bi, biOptimized;
    	int transparency;
    	
    	try {
			bi = ImageIO.read(imageFile);
			
			transparency = bi.getColorModel().getTransparency();
/*	------------------------------------------------------------------------------------
 * 	GraphicsConfiguration.createCompatibleImage()
 * 		Returns a BufferedImage that supports the specified transparency and has a data layout 
 * 		and color model compatible with this GraphicsConfiguration. This method has nothing to 
 * 		do with memory-mapping a device. The returned BufferedImage has a layout and color model 
 * 		that can be optimally blitted to a device with this GraphicsConfiguration. 
 *	------------------------------------------------------------------------------------ */
			biOptimized = defGraphicsConfig.createCompatibleImage(bi.getWidth(), 
																  bi.getHeight(), 
																  transparency);
			
			g2d = biOptimized.createGraphics();
/*	------------------------------------------------------------------------------------
 * 	Graphics.drawImage()
 * 		Draws as much of the specified image as is currently available. The image is drawn with 
 * 		its top-left corner at (x, y) in this graphics context's coordinate space. Transparent 
 * 		pixels in the image do not affect whatever pixels are already there.
 * 		This method returns immediately in all cases, even if the complete image has not yet been 
 * 		loaded, and it has not been dithered and converted for the current output device.
 * 
 * 		If the image has completely loaded and its pixels are no longer being changed, then 
 * 		drawImage returns true. Otherwise, drawImage returns false and as more of the image 
 * 		becomes available or it is time to draw another frame of animation, the process that 
 * 		loads the image notifies the specified image observer. 
 *	------------------------------------------------------------------------------------ */
			g2d.drawImage(bi, 0, 0, null);
			// bi has already been completely loaded from disk, so further "download" is 
			// unnecessary. Thus, drawImage() can be supplied with a null ImageObserver.
			g2d.dispose();
			return biOptimized;
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
	    	return null;
		}
    }

    public static BufferedImage LoadBufferedImage(File imageFile, int width, int height)
    {
    	Graphics2D g2d;
    	BufferedImage bi, biOptimized;
    	int transparency;
    	
    	try {
			bi = ImageIO.read(imageFile);
			
			transparency = bi.getColorModel().getTransparency();
/*	------------------------------------------------------------------------------------
 * 	GraphicsConfiguration.createCompatibleImage()
 * 		Returns a BufferedImage that supports the specified transparency and has a data layout 
 * 		and color model compatible with this GraphicsConfiguration. This method has nothing to 
 * 		do with memory-mapping a device. The returned BufferedImage has a layout and color model 
 * 		that can be optimally blitted to a device with this GraphicsConfiguration. 
 *	------------------------------------------------------------------------------------ */
			biOptimized = defGraphicsConfig.createCompatibleImage(width, height, 
																  transparency);
			
			g2d = biOptimized.createGraphics();
			// bi has already been completely loaded from disk, so further "download" is 
			// unnecessary. Thus, drawImage() can be supplied with a null ImageObserver.
			g2d.drawImage(bi, 0, 0, width, height, null);
			g2d.dispose();
			return biOptimized;
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
	    	return null;
		}
    }

    public static BufferedImage EmptyCompatibleImage(int width, int height) {
/*	------------------------------------------------------------------------------------
 * 	GraphicsConfiguration.createCompatibleImage()
 * 		Returns a BufferedImage that supports the specified transparency and has a data layout 
 * 		and color model compatible with this GraphicsConfiguration. This method has nothing to 
 * 		do with memory-mapping a device. The returned BufferedImage has a layout and color model 
 * 		that can be optimally blitted to a device with this GraphicsConfiguration. 
 *	------------------------------------------------------------------------------------ */
    	return defGraphicsConfig.createCompatibleImage(width, height, Transparency.OPAQUE);
    }

    public static BufferedImage EmptyCompatibleImage(int width, int height, int transparency) {
/*	------------------------------------------------------------------------------------
 * 	GraphicsConfiguration.createCompatibleImage()
 * 		Returns a BufferedImage that supports the specified transparency and has a data layout 
 * 		and color model compatible with this GraphicsConfiguration. This method has nothing to 
 * 		do with memory-mapping a device. The returned BufferedImage has a layout and color model 
 * 		that can be optimally blitted to a device with this GraphicsConfiguration. 
 *	------------------------------------------------------------------------------------ */
    	return defGraphicsConfig.createCompatibleImage(width, height, transparency);
    }

    
    public static File ShowFileOpen(String dialogTitle, 
									FileNameExtensionFilter extFileFilter,
									String defaultExtension, 
									File selectedDirectory, 
									File selFile)
    {
		File extFile;

		m_CommonDialog.resetChoosableFileFilters(); // Initialization
		m_CommonDialog.setAcceptAllFileFilterUsed(false);
		m_CommonDialog.setFileFilter(extFileFilter);
		m_CommonDialog.setDialogTitle(dialogTitle);

		if (selectedDirectory != null)
			m_CommonDialog.setCurrentDirectory(selectedDirectory);
		if (selFile == null)
			// Clear file selected on previous invocation, if any, of file chooser.
			// Calling .setSelectedFile(null) will NOT clear previously selected file.
			m_CommonDialog.setSelectedFile(new File(""));
		else
			m_CommonDialog.setSelectedFile(selFile);

		int retval = m_CommonDialog.showOpenDialog(m_Parent);

		if (retval == JFileChooser.APPROVE_OPTION) {
			extFile = m_CommonDialog.getSelectedFile();
			// If user has selected a file with extension other than default extension,
			// prompt the user as such.
			if (!extFile.getName().endsWith(defaultExtension)) {
				JOptionPane.showMessageDialog(m_Parent, "The file specified " + extFile.getName() +
						" does not end with " + defaultExtension + " extension.\nPlease specify " + 
						"a valid file with " + defaultExtension + " extension.",
						"Invalid File Extension", JOptionPane.WARNING_MESSAGE);
					return null;
			}

			// Invalid file prompt if selected file does not exist.
			if (!extFile.isFile()) {
				JOptionPane.showMessageDialog(m_Parent, "The file specified "
						+ CanonicalPath(extFile)
						+ " is invalid and does not exist on disk.\nPlease specify a valid file.",
						"File Not Found", JOptionPane.WARNING_MESSAGE);
				return null;
			}

			return extFile;
		}
		else
			return null;
}

    public static File ShowFileSave(String dialogTitle, 
    								FileNameExtensionFilter extFileFilter,
    								String defaultExtension, 
    								File selectedDirectory, 
    								File selFile)
    {
    	File extFile;
    	
		m_CommonDialog.resetChoosableFileFilters();	// Initialization
		m_CommonDialog.setAcceptAllFileFilterUsed(false);
		m_CommonDialog.setFileFilter(extFileFilter);
		m_CommonDialog.setDialogTitle(dialogTitle);

//		m_CommonDialog.setCurrentDirectory(selectedDirectory);
		if (selFile == null)
			// Clear file selected on previous invocation, if any, of file chooser.
			// Calling .setSelectedFile(null) will NOT clear previously selected file.
			m_CommonDialog.setSelectedFile(new File(""));
		else
			m_CommonDialog.setSelectedFile(selFile);

		int retval = m_CommonDialog.showSaveDialog(m_Parent);
		
		if (retval == JFileChooser.APPROVE_OPTION) {
			extFile = m_CommonDialog.getSelectedFile();
			// If user has not specified default file extension, append it manually
			if (!extFile.getName().endsWith(defaultExtension))
				extFile = new File(extFile.getAbsolutePath() + defaultExtension);

			// Overwrite prompt if selected file already exists.
			if (extFile.isFile()) {
				if (JOptionPane.showConfirmDialog(m_Parent, 
										 "The file " + CanonicalPath(extFile) + " already exists.\nDo you want to replace it?", 
										 "Confirm Overwrite", 
										  JOptionPane.YES_NO_OPTION, 
										  JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
				return null;
			}

			// TODO: Validate for invalid characters in file name, depending on OS.
			return extFile;
		}
		else
			return null;
    }

    
    /**
     * Creates a Titled border having {@code title} that is on top and specified margins 
     * around it. Margins are achieved with the help of Empty border, which just takes 
     * space on screen and does no drawing.
     * 
     * <p>This helper function was created because margins cannot be provided while
     * constructing a Titled border.</p> 
     * 
     * @param title Required title
     * @param topMargin (in pixel)
     * @param leftMargin (in pixel)
     * @param bottomMargin (in pixel)
     * @param rightMargin (in pixel)
     * @return the Border object
     */
    public static Border CreateTitledBorderwMargin(String title, 
    											   int topMargin, int leftMargin, 
    											   int bottomMargin, int rightMargin) {
    	Border bdrMargin = 
    		BorderFactory.createEmptyBorder(topMargin, leftMargin, bottomMargin, rightMargin);
    	
    	return BorderFactory.createCompoundBorder(bdrMargin, 
    											  BorderFactory.createTitledBorder(title));
    }

}