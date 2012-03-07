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

import static net.gadgetfactory.papilio.arcade.PapilioArcade.DEBUG;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.programSettings;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.supportedJoysticks;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import net.gadgetfactory.shared.UIHelpers;
import net.gadgetfactory.shared.HelperFunctions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PreferencesDialog extends JDialog implements ActionListener
{
	private JComboBox cboPlatforms;
	private JComboBox cboJoysticks;
	private JRadioButton optFPGA = new JRadioButton("FPGA");
	private JRadioButton optFlash = new JRadioButton("Flash");

	// Elements at same index corresponding to an item at the same ordinal position 
	// in Platforms Combobox
	private List<String> alPlatformIds = new ArrayList<String>(5);
	private List<String> alPlatformNames = new ArrayList<String>(5);

	private boolean m_OKClicked = false;
	
	public boolean isOKClicked() {
		return this.m_OKClicked;
	}

	public PreferencesDialog(JFrame owner, Document docPlatformXML)
	{
		super(owner, "Preferences", true);		// Constructor should be 1st statement
		
		final int PANEL_LEFT_MARGIN = 13;
		final int PANEL_RIGHT_MARGIN = 13;
		final int PANEL_X_PADDING = 4;
		final int PANEL_Y_PADDING = 6;
		final int CELL_X_DIFFERENCE = 12;
		final int CELL_Y_DIFFERENCE = 8;
		final int BUTTON_WIDTH = 70;
		final int BUTTON_HEIGHT = 28;
		final Dimension BUTTON_SIZE = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT); 

		// Use Spring layout as we are designing a Form.
		SpringLayout layout = new SpringLayout();

/*	------------------------------------------------------------------------------------
 * 	NOTE:	Every child of a SpringLayout-controlled container, as well as the container 
 * 			itself, has EXACTLY ONE set of constraints associated with it. These constraints 
 * 			are represented by a SINGLE SpringLayout.Constraints object, which contains 
 * 			SIX springs - for the component's width and height and each of its four edges. 
 * 			The SpringLayout class AUTOMATICALLY generates a SpringLayout.Constraints object 
 * 			for every component that's added to its container, so in order to change any of 
 * 			the springs you can query the Constraints object and set new values.
 *  
 * 			By default, SpringLayout creates constraints that make their associated component 
 * 			have the minimum, preferred, and maximum sizes returned by the component's 
 * 			Component.getMinimumSize(), Component.getPreferredSize(), and 
 * 			Component.getMaximumSize() methods. The x and y positions are initially NOT 
 * 			constrained, so that until you constrain them the Component will be positioned 
 * 			at 0,0 relative to the Insets of the parent Container.  
 *	------------------------------------------------------------------------------------ */

		JPanel pnlPreferences = new JPanel(layout);
		Spring maxWidthSpring = Spring.constant(0);
		final JLabel[] lblCaption = new JLabel[3];
		Border bdrMargin, bdrTitled;
		ButtonGroup group = new ButtonGroup();
		Box boxButtons = Box.createHorizontalBox();
		JButton btnDismiss;

		CachePlatformsList(docPlatformXML);

/*	1) Create border and margin around the user input controls. */

		// Provide empty space (margin) around controls, otherwise top side of border
		// will butt against top side of dialog and so on.
		
		pnlPreferences.setBorder(
				UIHelpers.CreateTitledBorderwMargin("", 
													11, PANEL_LEFT_MARGIN, 13, PANEL_RIGHT_MARGIN));
		/* Out of all border types, Titled border correctly assumes the L&F of platform
		   border. (I have verified this on Windows at least.) Lowered etched border looks 
		   tempting but is not required.
		 */

/*	2) Create and add "Board" Label and platforms Combobox */

		lblCaption[0] = new JLabel("Board", JLabel.TRAILING);	// right-alignment
		pnlPreferences.add(lblCaption[0]);
		cboPlatforms = new JComboBox(alPlatformNames.toArray());
		lblCaption[0].setLabelFor(cboPlatforms);
		pnlPreferences.add(cboPlatforms);

/*	3) Create and add "Joystick" Label and corresponding Combobox */

		lblCaption[1] = new JLabel("Joystick", JLabel.TRAILING);	// right-alignment
		pnlPreferences.add(lblCaption[1]);
		cboJoysticks = new JComboBox(supportedJoysticks.getUnderlyingArray());
		lblCaption[1].setLabelFor(cboJoysticks);
		pnlPreferences.add(cboJoysticks);

/*	5) Create and add "Write to" Label and associated Option buttons */

		lblCaption[2] = new JLabel("Write to", JLabel.TRAILING);	// right-alignment
		pnlPreferences.add(lblCaption[2]);
		pnlPreferences.add(optFPGA);
		group.add(optFPGA);
		pnlPreferences.add(optFlash);
		group.add(optFlash);

/*	------------------------------------------------------------------------------------
 * 	NOTE:	Because of the cyclic nature of springs, i.e., they are often calculated based 
 * 			on existing springs that are combined to create new compound springs, the order 
 * 			in which the constraints are set is important. For example, if you query the 
 * 			west or width spring of a component, should you then set its east spring, 
 * 			afterward this could affect either of the previously queried springs, making 
 * 			their values stale. 
 * 			Hence, generally, it's a good idea to specify any explicit width or height springs 
 * 			BEFORE defining the positional springs between components to ensure that no 
 * 			queried width or height springs are later replaced. 
 *	------------------------------------------------------------------------------------ */

/*	7) Make width springs of each Label in first column SAME */

		/* We need to make width of each Label in first column SAME so that controls in 
		   second column will align up nicely. By default, Labels {"Board", "Joystick", 
		   "Write to"} will not have same widths, owing to their contents being different.
		 */

		for (JLabel lblIterator : lblCaption) {
			maxWidthSpring = Spring.max(maxWidthSpring, 
										layout.getConstraints(lblIterator).getWidth());
		}
		for (JLabel lblIterator : lblCaption) {
			layout.getConstraints(lblIterator).setWidth(maxWidthSpring);
			layout.getConstraints(lblIterator).setX(Spring.constant(PANEL_X_PADDING));
		}

// TODO: Write down experiments with SpringLayout

/*	------------------------------------------------------------------------------------
 * 	NOTE:	The SpringLayout.putConstraint() method is a convenience method that lets you 
 * 			modify a component's constraints without needing to use the full spring layout API. 
 *	------------------------------------------------------------------------------------ */
  
/*	8) Set springs (directional constraints) for "Board" Label and platforms Combobox */

		// Set "Board" Label's left edge to a FIXED distance from parent Panel (anchor). 
//		layout.putConstraint(SpringLayout.WEST, lblCaption[0],
//							 PANEL_X_PADDING,
//							 SpringLayout.WEST, pnlPreferences);
		// Make "Board" Label's baseline SAME as platform Combobox (anchor). 
		layout.putConstraint(SpringLayout.BASELINE, lblCaption[0],
				 			 0,
				 			 SpringLayout.BASELINE, cboPlatforms);

		// Set platform Combobox's left edge to a FIXED distance from "Board" Label (anchor). 
		layout.putConstraint(SpringLayout.WEST, cboPlatforms,
							 CELL_X_DIFFERENCE,
							 SpringLayout.EAST, lblCaption[0]);
		// Set platform Combobox's top edge to a FIXED distance from parent Panel (anchor). 
		layout.putConstraint(SpringLayout.NORTH, cboPlatforms,
							 PANEL_Y_PADDING,
							 SpringLayout.NORTH, pnlPreferences);

/*	9) Set springs (directional constraints) for "Joystick" Label and corresponding Combobox */

		// Set "Joystick" Label's left edge to a FIXED distance from parent Panel (anchor). 
//		layout.putConstraint(SpringLayout.WEST, lblCaption[1],
//							 PANEL_X_PADDING,
//							 SpringLayout.WEST, pnlPreferences);
		// Make "Joystick" Label's baseline SAME as joystick Combobox (anchor). 
		layout.putConstraint(SpringLayout.BASELINE, lblCaption[1],
				 			 0,
				 			 SpringLayout.BASELINE, cboJoysticks);

		// Set joystick Combobox's left edge to a FIXED distance from "Joystick" Label (anchor). 
//		layout.putConstraint(SpringLayout.WEST, cboJoysticks,
//							 CELL_X_PADDING,
//							 SpringLayout.EAST, lblCaption[1]);
		layout.putConstraint(SpringLayout.WEST, cboJoysticks,
				 			 0,
				 			 SpringLayout.WEST, cboPlatforms);
		// Set joystick Combobox's top edge to a FIXED distance from platform Combobox (anchor). 
		layout.putConstraint(SpringLayout.NORTH, cboJoysticks,
							 CELL_Y_DIFFERENCE,
							 SpringLayout.SOUTH, cboPlatforms);

/*	11) Set springs (directional constraints) for "Write to" Label and associated Option buttons */

		// Set "Write to" Label's left edge to a FIXED distance from parent Panel (anchor). 
//		layout.putConstraint(SpringLayout.WEST, lblCaption[2],
//							 PANEL_X_PADDING,
//							 SpringLayout.WEST, pnlPreferences);
		// Make "Write to" Label's baseline SAME as FPGA OptionButton (anchor). 
		layout.putConstraint(SpringLayout.BASELINE, lblCaption[2],
				 			 0,
				 			 SpringLayout.BASELINE, optFPGA);

		// Set FPGA OptionButton's left edge to a FIXED distance from "Write to" Label (anchor). 
		layout.putConstraint(SpringLayout.WEST, optFPGA,
							 CELL_X_DIFFERENCE,
							 SpringLayout.EAST, lblCaption[2]);
		// Set FPGA OptionButton's top edge to a FIXED distance from "Use Keyboard" (anchor). 
		layout.putConstraint(SpringLayout.NORTH, optFPGA,
							 CELL_Y_DIFFERENCE,
							 SpringLayout.SOUTH, cboJoysticks);

		// Set Flash OptionButton's left edge to a FIXED distance from FPGA OptionButton (anchor). 
		layout.putConstraint(SpringLayout.WEST, optFlash,
							 CELL_X_DIFFERENCE,
							 SpringLayout.EAST, optFPGA);
		// Set Flash OptionButton's top edge to a FIXED distance from "Use Keyboard" (anchor). 
		layout.putConstraint(SpringLayout.NORTH, optFlash,
							 CELL_Y_DIFFERENCE,
							 SpringLayout.SOUTH, cboJoysticks);

/*	------------------------------------------------------------------------------------
 * 	NOTE:	To make the container initially appear at the right size, we need to set the 
 * 			springs that define the right (east) and bottom (south) edges of the container 
 * 			itself. No constraints for the right and bottom container edges are set by 
 * 			default. The size of the container is defined by setting these constraints. 
 *	------------------------------------------------------------------------------------ */

/*	13) Set springs (directional constraints) for Panel containing user input controls */

		// Set parent Panel's right edge to a FIXED distance from platform Combobox (anchor). 
		layout.putConstraint(SpringLayout.EAST, pnlPreferences,
							 PANEL_X_PADDING,
							 SpringLayout.EAST, cboPlatforms);
		// Set parent Panel's bottom edge to a FIXED distance from "Diagnostics" Checkbox (anchor). 
		layout.putConstraint(SpringLayout.SOUTH, pnlPreferences,
							 PANEL_Y_PADDING,
							 SpringLayout.SOUTH, optFPGA);


/*	14) Create [OK] and [Cancel] buttons and lay them from left to right. */

		// Provide empty space (margin) inside the Box, otherwise bottom sides of [OK]
		// [Cancel] buttons will butt against bottom side of dialog and so on.
		boxButtons.setBorder(BorderFactory.createEmptyBorder(0, PANEL_LEFT_MARGIN, 
															 10, PANEL_RIGHT_MARGIN));

		// Add horizontal glue first so that [OK], [Cancel] buttons will be right-aligned.
        boxButtons.add(Box.createHorizontalGlue());

        // [OK] button
		btnDismiss = new JButton("OK");
		/* If we do not set size explicitly, then Swing makes width of [OK] button < that
		   of [Cancel] button - owing to their captions being different. To override this,
		   at least make widths of both buttons same, we set size explicitly here.
		 */
		btnDismiss.setMinimumSize(BUTTON_SIZE);
		btnDismiss.setPreferredSize(BUTTON_SIZE);
		btnDismiss.addActionListener(this);
        boxButtons.add(btnDismiss);

		this.getRootPane().setDefaultButton(btnDismiss);	// Set [OK] button as default

/*	------------------------------------------------------------------------------------
 * 	NOTE:	We cannot use a horizontal strut in place of rigid area. Because, struts have 
 * 			unlimited maximum heights or widths (for horizontal and vertical struts, respectively). 
 * 			This means that if you use a horizontal box within a vertical box (as in this
 * 			JDialog), for example, the horizontal box can sometimes become too tall. For 
 * 			this reason, rigid area is used instead of struts. 
 *	------------------------------------------------------------------------------------ */

		// Add a fixed space (10 pixels horizontally) between [OK] and [Cancel] buttons.  
        boxButtons.add(Box.createRigidArea(new Dimension(10, 0)));

        // [Cancel] button
		btnDismiss = new JButton("Cancel");
		/* If we do not set size explicitly, then Swing makes width of [OK] button < that
		   of [Cancel] button - owing to their captions being different. To override this,
		   at least make widths of both buttons same, we set size explicitly here.
		   */
		btnDismiss.setMinimumSize(BUTTON_SIZE);
		btnDismiss.setPreferredSize(BUTTON_SIZE);
		btnDismiss.addActionListener(this);
        boxButtons.add(btnDismiss);

/*	15) Set up dialog */

		// The default contentPane for JDialog has a BorderLayout manager set on it.
		this.getContentPane().add(pnlPreferences, BorderLayout.CENTER);
        this.getContentPane().add(boxButtons, BorderLayout.PAGE_END);

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

    /* Call to this.setLocationRelativeTo(owner) must be AFTER this.pack(). Otherwise,
 	   top-left of dialog is placed at the center of screen, instead of placing 
 	   center of dialog at the center of the screen.
     */

/*	------------------------------------------------------------------------------------
 * 	Window.setLocationRelativeTo(Component c)
 * 		Sets the location of the window relative to the specified component.
 * 		If the component is not currently showing, or c is null, the window is placed at 
 * 		the center of the screen. The center point can be determined with 
 * 		GraphicsEnvironment.getCenterPoint
 * 		If the bottom of the component is offscreen, the window is placed to the side of 
 * 		the Component that is closest to the center of the screen. So if the Component is 
 * 		on the right part of the screen, the Window is placed to its left, and visa versa.  		
 *	------------------------------------------------------------------------------------ */
		this.setLocationRelativeTo(owner);		// Position is important!
		
		this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		this.setResizable(false);	// No need for resizable dialog

/*	16) Add event listeners for dialog and controls */

		if (DEBUG)
			this.addComponentListener(new ComponentAdapter() {
	            @Override
				public void componentShown(ComponentEvent evt) {
	        		UIHelpers.PrintLocations("Board Label", lblCaption[0], 
    						"Joystick Label", lblCaption[1], "Write to Label", lblCaption[2]);
	            	UIHelpers.PrintSizes("Board Label", lblCaption[0], 
    						"Joystick Label", lblCaption[1], "Write to Label", lblCaption[2]);
	        		UIHelpers.PrintLocations("cboPlatforms", cboPlatforms, 
	        				"cboJoysticks", cboJoysticks, "optFPGA", optFPGA);
	        		UIHelpers.PrintSizes("cboPlatforms", cboPlatforms, 
	        				"cboJoysticks", cboJoysticks, "optFPGA", optFPGA);
	            }
			});
	}

	// Used by PreferencesDialog constructor only
	private void CachePlatformsList(Document docPlatformXML)
	{
		NodeList platformList = docPlatformXML.getElementsByTagName("platform");
		NodeList displayNameList; Element elmPlatform;
		int i; String id, sDisplayName;

		for (i = 0; i < platformList.getLength(); i++) {
			elmPlatform = (Element) platformList.item(i);
			id = elmPlatform.getAttribute("id");
			alPlatformIds.add(id);
			
			displayNameList = elmPlatform.getElementsByTagName("displayname");
			// If <displayname> element is not present, then displayNameList does not 
			// become null. Instead, displayNameList.getLength() returns 0.
			if (displayNameList.getLength() != 0) {
				sDisplayName = 
					HelperFunctions.SimpleElementText((Element) displayNameList.item(0));
				alPlatformNames.add(sDisplayName);
			}
		}
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("OK")) {
			SaveForm();
			m_OKClicked = true;
		}
		
		this.setVisible(false);		// To parallel HIDE_ON_CLOSE behaviour
	}


	public void PopulateForm()
	{
		/* It is not required to query values of settings variables such as selPlatformId, 
		   bWrite2FPGA, etc. (in PapilioArcade class) here. programSettings suffices (and is
		   the policy) as the two are always in sync with each other.
		 */

		String selPlatformId = programSettings.getStringProperty("PlatformId");
		String selJoystickId = programSettings.getStringProperty("JoystickId");
		String selWriteTarget = programSettings.getStringProperty("Writeto");
		int i;
		
/*	1) Select platform corresponding to selPlatformId */

		i = 0;
		for (String platformId : alPlatformIds) {
			// We should NOT use equalsIgnoreCase since program logic dictates exact 
			// Platform Id such as "PapilioOne500k_ArcadeMegaWing", etc. be used.  
			if (platformId.equals(selPlatformId)) {
				cboPlatforms.setSelectedIndex(i);
				break;
			}
			i++;
		}
		// In case selPlatformId is invalid, first item is selected in choPlatforms.

/*	2) Select joystick corresponding to selJoystickId */

		// TODO: Confirm the sequence in supportedJoysticks.getUnderlyingArray() with cboJoysticks 
		for (WrJoysticksArray.JoystickStruct iteratorJoystick : 
								supportedJoysticks.getUnderlyingArray()) {
			// We should NOT use equalsIgnoreCase since program logic dictates exact 
			// Joystick Id such as "atari2600", etc. be used.  
			if (iteratorJoystick.id.equals(selJoystickId)) {
				cboJoysticks.setSelectedItem(iteratorJoystick);
			}
		}
		// In case selJoystickId is invalid, first item is selected in choJoysticks.

/*	3) Select "Write to" target corresponding to selWriteTarget */
		
		if (selWriteTarget.equalsIgnoreCase("FPGA"))
			optFPGA.setSelected(true);
		else if (selWriteTarget.equalsIgnoreCase("Flash"))
			optFlash.setSelected(true);
		else
		// => selWriteTarget is invalid.
			optFPGA.setSelected(true);		// default target is FPGA

/*	4) Finally, show Preferences dialogbox. */
		this.setVisible(true);
	}

	private void SaveForm()
	{
		programSettings.setProperty("PlatformId", 
									alPlatformIds.get(cboPlatforms.getSelectedIndex()));
		programSettings.setProperty("JoystickId", 
				((WrJoysticksArray.JoystickStruct) cboJoysticks.getSelectedItem()).id);
		
		if (optFPGA.isSelected())
			programSettings.setProperty("Writeto", "FPGA");
		else if (optFlash.isSelected())
			programSettings.setProperty("Writeto", "Flash");
	}

}
