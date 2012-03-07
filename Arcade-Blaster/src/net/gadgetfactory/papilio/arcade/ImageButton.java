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

import java.awt.*;
import java.awt.geom.Ellipse2D;
import javax.swing.*;

public class ImageButton extends JButton {

	private Ellipse2D circularHitRegion;

    public ImageButton(String img, String pressedImg, String rolloverImg) {
        this(new ImageIcon(img), new ImageIcon(pressedImg), new ImageIcon(rolloverImg));
    }

    public ImageButton(ImageIcon icon, ImageIcon pressedIcon, ImageIcon rolloverIcon) {
        this.setIcon(icon);
        this.setText(null);
        this.setIconTextGap(0);
        this.setPressedIcon(pressedIcon);
        this.setRolloverIcon(rolloverIcon);

        this.setMargin(new Insets(0,0,0,0));
        this.setBorderPainted(false);
        this.setBorder(null);
        this.setContentAreaFilled(false);
		// Never call this.setOpaque(false);
		
        this.setSize(icon.getImage().getWidth(null),
                	 icon.getImage().getHeight(null));

		circularHitRegion = new Ellipse2D.Double(4, 4, icon.getImage().getWidth(null) - 8, icon.getImage().getHeight(null) - 8);
    }

    @Override
    public boolean contains(int x, int y) {
        return circularHitRegion.contains(x, y);
    }

}
