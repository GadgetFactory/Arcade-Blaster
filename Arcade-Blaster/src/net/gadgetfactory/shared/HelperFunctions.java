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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HelperFunctions
{
    /**
     *	This uses Java 1.6's Desktop class. Unfortunately, Desktop.browse(URI uri) takes 
     *	years to complete execution.  
     */
    public static boolean JavaBrowseURL(final String sURL)
    {
    	final Desktop desktop;
    	
    	if (!Desktop.isDesktopSupported())
    		return false;
    	
    	// TODO: Prior to .getDesktop(), do we need to check for GraphicsEnvironment.isHeadless() as well?
    	desktop = Desktop.getDesktop();

/*	------------------------------------------------------------------------------------
 * 	Desktop.isSupported(Desktop.Action)
 * 		Even when the platform supports an action, a file or URI may not have a registered 
 * 		application for the action. For example, most of the platforms support the 
 * 		Desktop.Action.OPEN action. But for a specific file, there may not be an application 
 * 		registered to open it. In this case, isSupported(java.awt.Desktop.Action) may return 
 * 		true, but the corresponding action method will throw an IOException. 
 *	------------------------------------------------------------------------------------ */

        if (!desktop.isSupported(Desktop.Action.BROWSE))
        	return false;

        Thread thread = new Thread(new Runnable() {
			@Override
			public void run()
			{
		        try {
		        	URI uri = new URI(sURL);
		        	desktop.browse(uri);
		        } catch(IOException ioe) {
		            System.err.println(ioe.getMessage());
		        } catch(URISyntaxException use) {
		            System.err.println(use.getMessage());
		        } catch(Exception e) {
		            System.err.println(e.getMessage());
		        }
			}
		});
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
        
        return true;
    }

    public static void BrowseURL(String sURL, boolean runningonWindows)
    {
	    Process process = null;
	    String[] commandLine = {"cmd.exe", "/C", "start", sURL};
	    String[] commandLineLinux = {"xdg-open", sURL};

	    try {
	    	if (runningonWindows) 
	    		process = Runtime.getRuntime().exec(commandLine, null, null);
	    	else
	    		process = Runtime.getRuntime().exec(commandLineLinux, null, null);
	    } catch (IOException e) {
	      System.err.println(e.getMessage());
	    }

/*	------------------------------------------------------------------------------------
 *  Class Process 
 *  	The created subprocess does not have its own terminal or console. All its standard io 
 *  	(i.e. stdin, stdout, stderr) operations will be redirected to the parent process through 
 *  	three streams (getOutputStream(), getInputStream(), getErrorStream()). The parent process 
 *  	uses these streams to feed input to and get output from the subprocess.
 *  	Because some native platforms only provide limited buffer size for standard input and 
 *  	output streams, FAILURE TO PROMPTLY WRITE THE INPUT STREAM OR READ THE OUTPUT STREAM 
 *  	of the subprocess may cause the subprocess to block, and even deadlock. 
 *	------------------------------------------------------------------------------------ */

	    /*	As per Java guidelines mentioned above, when spawning a console program, we should 
			consume the StdOut and StdErr of spawned console program - otherwise deadlock or
			blocking will happen. However, in case of using start "command" of %COMSPEC% to 
			launch the default Internet Browser, the URL passed is always valid. Thus, nothing 
			will be spewed in StdErr. Besides, executing - start http://papilio.cc - does not
			write anything to StdOut. Thus, there is NO need to consume StdOut and StdErr.
			As a matter of fact, by not consuming StdOut and StdErr, BrowseURL becomes very fast.
	    */
    }


    /**
     * <p>Chops <code>noOfChars</code> characters from right of the string <code>strAny</code>.</p> 
     * 
     * @param strAny		any String
     * @param noOfChars		number of characters to chop from right
     * @return
     * 		"", if <code>strAny</code> is empty or <code>noOfChars</code> is greater than
     * 		or equal to length of <code>strAny</code>.<br> 
     * 		chopped string, otherwise.
     */
    public static String StrChopRight(String strAny, int noOfChars) {
    	if ((strAny.isEmpty()) || noOfChars >= strAny.length())
    		return "";
    	return strAny.substring(0, strAny.length() - noOfChars);
    }

    /**
     * Parses <code>strNumber</code> as a signed long in specified <code>radix</code>.
     * 
     * @param strNumber the String containing the long representation to be parsed.
     * @param radix the radix to be used while parsing. 
     * @return
     * 		Long.MIN_VALUE, if NumberFormatException occurs (signaling that 
     * 						<code>strNumber</code> does not contain a valid long value.<br>
     * 		parsed long, otherwise.
     */
    public static long ParseLong(String strNumber, int radix) {
    	try {
			return Long.parseLong(strNumber, radix);
		}
		catch (NumberFormatException e) {
			return Long.MIN_VALUE;
		}
    }


    public static String SimpleElementText(Element elmGeneric)
    {
    	NodeList children; Node textNode;
		int j;

		children = elmGeneric.getChildNodes();
		for (j = 0; j < children.getLength(); j++) {
			textNode = children.item(j);
			if (textNode.getNodeType() == Node.TEXT_NODE) {
				return textNode.getNodeValue();
			}
		}
		return null;	// Start and end tags of elmGeneric juxtapose
    }

}
