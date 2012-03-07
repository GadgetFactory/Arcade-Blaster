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


public class WrJoysticksArray
{
	private final int NO_OF_JOYSTICKS = 1;
	private final JoystickStruct[] joysticksInfo = new JoystickStruct[NO_OF_JOYSTICKS];

	public JoystickStruct[] getUnderlyingArray() {
		return this.joysticksInfo;
	}

	public WrJoysticksArray()
	{
		joysticksInfo[0] = 
			new JoystickStruct("atari2600", "Atari 2600", 
				"<TABLE BACKGROUND=\"~%joystick_image%~\" BORDER=0 WIDTH=350>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD COLSPAN=\"4\" ALIGN=\"CENTER\"><B>~%up%~</B></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD COLSPAN=\"3\" ALIGN=\"LEFT\"><B>~%right%~</B></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD COLSPAN=\"3\" ALIGN=\"CENTER\"><B>~%fire1%~</B></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD COLSPAN=4 ALIGN=\"CENTER\"><B>~%left%~</B></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD COLSPAN=\"3\" ALIGN=\"CENTER\"><B>~%down%~</B></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"<TR><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD>\r\n" + 
				"    <TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD><TD WIDTH=25></TD></TR>\r\n" + 
				"</TABLE>");
	}
	
	public String getHTMLTable(String joystickId)
	{
		for (JoystickStruct iteratorJoystick : joysticksInfo) {
			if (iteratorJoystick.id.equals(joystickId))
				return iteratorJoystick.HTMLTable;
		}
		return "";
	}
	
    public static class JoystickStruct
    {
    	public String id, name, HTMLTable;

		public JoystickStruct(String id, String name, String tableHTML) {
    		this.id = id;
    		this.name = name;
    		this.HTMLTable = tableHTML;
    	}

    	@Override
		public String toString() {
    		return this.name;
    	}
    }

}
