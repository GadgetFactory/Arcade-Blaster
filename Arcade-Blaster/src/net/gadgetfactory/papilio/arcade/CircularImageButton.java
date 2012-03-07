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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JButton;
import net.gadgetfactory.shared.UIHelpers;

public class CircularImageButton extends JButton
{
	private BufferedImage biFace;
	private Ellipse2D circularHitRegion;
	
	public CircularImageButton(File buttonFaceFile)
	{
		this.biFace = HelperFunctions.LoadBufferedImage(buttonFaceFile);
		this.setSize(biFace.getWidth(), biFace.getHeight());
		// TODO: Is there any "off by 1" error for Ellipse2D.Double? 
		circularHitRegion = new Ellipse2D.Double(0, 0, biFace.getWidth(), biFace.getHeight());
		
		this.setOpaque(false);		// must
		// Following does not have any effect on custom rendering
		this.setBorder(null);
		this.setBorderPainted(false);
		this.setMargin(new Insets(0,0,0,0));
		this.setFocusPainted(false);
	}

    @Override
	public void paintComponent(Graphics g)
    {
		Graphics2D g2d = (Graphics2D) g;
		
		System.out.println("paintComponent, " + g2d.getClip());
//		g2d.setClip(circularHitRegion);
//		System.out.println("New Clip for CircularImageButton = " + g2d.getClip());
//		super.paintComponent(g2d);
		g2d.drawImage(biFace, 0, 0, null);
    }

    @Override
    public boolean contains(int x, int y) {
        return circularHitRegion.contains(x, y);
    }

}
