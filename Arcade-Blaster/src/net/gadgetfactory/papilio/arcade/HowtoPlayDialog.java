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

import static net.gadgetfactory.papilio.arcade.PapilioArcade.supportedJoysticks;
import static net.gadgetfactory.papilio.arcade.PapilioArcade.runningonWindows;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import net.gadgetfactory.shared.HelperFunctions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HowtoPlayDialog extends JDialog
{
	// Mime type must be set for JEditorPane, otherwise wbrHelp.setText call display 
	// HTML as plain text.
	private JEditorPane wbrHelp = new JEditorPane("text/html", "");
	
	private File helpFolder = new File("help");
	private File controlsXMLFile = new File("help", "Controls.xml");
    private Document docControlXML;
	private Element elmSelGame, elmJoystickCtrls;

	public HowtoPlayDialog(JFrame owner)
	{
		super(owner, "How to play ", true);		// Constructor should be 1st statement

		final int DIALOG_WIDTH = 550;
		final int DIALOG_HEIGHT = 600;

/*	1) Add child controls to dialog */

		// We want JEditorPane to act as viewer, not as editor. Besides, hyperlink events won't
		// be generated unless JEditorPane is NOT editable.
		wbrHelp.setEditable(false);
		wbrHelp.setMargin(new Insets(10, 10, 10, 10));
		wbrHelp.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					HelperFunctions.BrowseURL(e.getURL().toString(), runningonWindows);
				}
			}
		});

/*	2) Set up dialog */

		// The default contentPane for JDialog has a BorderLayout manager set on it.
		this.getContentPane().add(new JScrollPane(wbrHelp), BorderLayout.CENTER);

		// The defaultCloseOperation property is set to HIDE_ON_CLOSE, by default, which is
		// the desirable behaviour for this dialogbox.

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
		this.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
		
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

/*	4) Add event listeners for dialog */

		/* When this dialog is shown, JEditorPane scrolls to the very bottom of HTML
		   content. To override this behavior, we need to have a named anchor  
		   (<A NAME="Top"> at the top to which we can scroll programmatically. If we do
		   this in PopulateNShow() function just after the statement 
		   			this.setVisible(true);
		   it has no effect. The only remedy to this problem, we need to place the 
		   call to .scrollToReference immediately after the dialog is shown, i.e. in 
		   componentShown event.
		 */
		this.addComponentListener(new ComponentAdapter() {
            @Override
			public void componentShown(ComponentEvent evt) {
    			wbrHelp.scrollToReference("Top");	// No Exception is thrown
            }
		});
	}
	
	
	/**
	 * Validates the <code>Controls.xml</code> file present in help folder w.r.t. selected game. 
	 * <code>Controls.xml</code> is invalid under following circumstances:<ol>
	 * 		<li>It is not present in help folder</li>
	 * 		<li>It is invalid, i.e. fatal error occurs while parsing it</li>
	 * 		<li>It does not contain a {@literal <game>} element whose id = {@code selGameId}</li>
	 * 		<li>It does not contain a {@literal <joystick> element within the <game>} 
	 * 		   element found above.</li></ol>
	 * If neither of above condition is true, then <code>Controls.xml</code> is valid.
	 * 
	 * @param selGameId Id of selected game in GamesListbox 
	 * @return false if <code>Controls.xml</code> is invalid, true otherwise
	 */
	private boolean ControlsXMLValid(String selGameId)
	{
		DocumentBuilderFactory factory;
		NodeList gameList, joystickList;
		boolean bSuccess = true, bFound = false;
		int i;

		if (!controlsXMLFile.isFile())
			return false;

		try
		{
			factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			docControlXML = factory.newDocumentBuilder().parse(controlsXMLFile);
			
			gameList = docControlXML.getElementsByTagName("game");
			for (i = 0; i < gameList.getLength(); i++) {
				elmSelGame = (Element) gameList.item(i);
				if (elmSelGame.getAttribute("id").equals(selGameId)) {
					bFound = true;
					break;
				}
			}
			
			if (bFound) {
				joystickList = elmSelGame.getElementsByTagName("joystick");
				// If <joystick> element is not present, then joystickList does not become null.
				// Instead, joystickList.getLength() returns 0.
				if (joystickList.getLength() == 0)
					bSuccess = false;
				else
					elmJoystickCtrls = (Element) joystickList.item(0);
			}
			else
				bSuccess = false;
		}
		catch (SAXException e) {
			System.err.println("HowtoPlayDialog.ControlsValid\t" + e.getMessage());
			bSuccess = false;
		}
		catch (IOException e) {
			System.err.println("HowtoPlayDialog.ControlsValid\t" + e.getMessage());
			bSuccess = false;
		}
		catch (ParserConfigurationException e) {
			System.err.println("HowtoPlayDialog.ControlsValid\t" + e.getMessage());
			bSuccess = false;
		}
		catch (FactoryConfigurationError e) {
			System.err.println("HowtoPlayDialog.ControlsValid\t" + e.getMessage());
			bSuccess = false;
		}
		return bSuccess;
	}


	/** 
	 * Combines "How to Play" (Help) HTML, user controls information of selected game and
	 * image for selected joystick on-the-fly and displays resulting HTML in this dialogbox. 
	 * Controls information is "overlaid" on joystick image at appropriate places using 
	 * {@code colJoystickTables} (See {@link #ConstructJoystickTable}). The files used are:<ol>
	 * 		<li>{@code %selGameId%.html} in help folder</li>
	 * 		<li>{@code %selJoystickId%.png} in help/images folder</li>
	 * 		<li>{@code Controls.xml} in help folder.<br>Information is retrieved from 
	 * 			{@literal <game>} element whose id = {@code selGameId}</li></ol>
	 * 
	 * <p><b>Validations:</b></p>
	 * Above action is not carried out if any of following is true<ul>
	 * 		<li>{@code %selGameId%.html} file is absent</li>
	 * 		<li>{@code %selJoystickId%.png} file is absent</li>
	 * 		<li>{@code Controls.xml} has invalid state. (See {@link #ControlsXMLValid})</li></ul>
	 * 
	 * @param selGameId		Id of selected game in GamesListbox
	 * @param selGameName	Name of selected game in GamesListbox
	 * @param selJoystickId Id of joystick that is selected in Preferences dialogbox
	 */
	public void PopulateNShow(String selGameId, String selGameName, String selJoystickId)
	{
		File howToPlayHTML = new File(helpFolder, selGameId + ".html");
		File joystickImageFile = new File("help/images", selJoystickId + ".png");
		FileInputStream fin = null; BufferedReader br = null;
		String sLine; StringBuilder sbHelpHTML = new StringBuilder();
		String lineBreak = System.getProperty("line.separator");

/*	1) Carry out necessary validations first. */
		
		if (!howToPlayHTML.isFile())
		// => help/%selGameId%.html file is absent
			return;
		else if (!joystickImageFile.isFile())
		// => help/images/%selJoystickId%.png file is absent
			return;
		else if (!ControlsXMLValid(selGameId))
		// => help/Controls.xml file has invalid state
			return;
		
		try
		{
			this.setTitle("How to play " + selGameName);

/*	2) Generate required Help HTML in memory and assign it to JEditorPane */

			fin = new FileInputStream(howToPlayHTML);
			br = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
			while ((sLine = br.readLine()) != null) {
				/* Fully-qualified path of folder containing Help HTML file MUST be set 
				   in <BASE HREF="~%html_path%~">. Otherwise JEditorPane cannot resolve 
				   relative paths such as "images/atari2600.png", "images/screen1.png", etc. 
				   Further, the full path must be in the form of file:/ protocol. */
				if (sLine.contains("~%html_path%~"))
					sbHelpHTML.append(sLine.replace("~%html_path%~", 
											howToPlayHTML.getParentFile().toURI().toString()) + 
							  lineBreak);
				else if (sLine.contains("~%joystick_table%~"))
					ConstructJoystickTable(sbHelpHTML, selJoystickId);
				else
					sbHelpHTML.append(sLine + lineBreak);
			}

//			System.out.println(sbHelpHTML.toString());
			wbrHelp.setText(sbHelpHTML.toString());

/*	3) Finally, show "How to Play" dialogbox. */

			this.setVisible(true);
		}
		catch (IOException e) {
			System.err.println("HowtoPlayDialog.PopulateNShow\t" + e.getMessage());
		}
		finally
		{
			if (br != null) {
				try {
					br.close();		// This also closes fin FileInputStream.  
				}
				catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
		}

	}
	
	// TODO: Write javadoc comment
	private void ConstructJoystickTable(StringBuilder sbHelpHTML, String selJoystickId)
	{
		NodeList children = elmJoystickCtrls.getChildNodes();
		Node elementNode; 
		int pos, i; String templateVar;

		sbHelpHTML.append(supportedJoysticks.getHTMLTable(selJoystickId));
		
		pos = sbHelpHTML.indexOf("~%joystick_image%~");
		if (pos != -1) {
			sbHelpHTML.replace(pos, pos + "~%joystick_image%~".length(), 
									"images/" + selJoystickId + ".png");
		}
		
		for (i = 0; i < children.getLength(); i++) {
			elementNode = children.item(i);
			if (elementNode.getNodeType() == Node.ELEMENT_NODE) {
				templateVar = "~%" + elementNode.getNodeName() + "%~";
				pos = sbHelpHTML.indexOf(templateVar);
				if (pos != -1) {
					sbHelpHTML.replace(pos, pos + templateVar.length(), 
							   HelperFunctions.SimpleElementText((Element) elementNode) + "");
				}
			}
		}
	}
	
}
