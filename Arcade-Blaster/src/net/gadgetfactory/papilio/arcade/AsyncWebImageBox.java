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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import javax.swing.JComponent;

public class AsyncWebImageBox extends JComponent
{
	private final int DEFAULT_WIDTH = 30;
	private final int DEFAULT_HEIGHT = 30;
	
	private Toolkit defToolkit = Toolkit.getDefaultToolkit();
	private Image image = null;
	private int componentWidth = -1, componentHeight = -1;
	private int imageWidth, imageHeight;

	public AsyncWebImageBox() {
	}

	public AsyncWebImageBox(int width, int height) {
		this.componentWidth = width;
		this.componentHeight = height;
	}

	/**
	 * Set or change the current Image to display. setImage does a MediaTracker
	 * to ensure the Image is loaded. You don't have to. If you don't plan to
	 * use the old image again you should do a getImage().flush();
	 * 
	 * @param image
	 *            the new Image to be displayed. If the image jpg may have
	 *            recently changed, don't use getImage to create it, use
	 *            URL.openConnection() URLConnection.setUseCaches( false )
	 *            Connection.getContent Component.createImage
	 * 
	 */
	public void setImage(URL urlNewImage)
	{
		image = null;
		this.paintImmediately(getVisibleRect());
		image = defToolkit.createImage(urlNewImage);

		this.prepareImage(image, componentWidth, componentHeight, this);
	}

	public void setLocalImage(String pathNewImage)
	{
		image = null;
		this.paintImmediately(getVisibleRect());
		image = defToolkit.createImage(pathNewImage);

		this.prepareImage(image, componentWidth, componentHeight, this);
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		// get size of box we have to draw in
		Dimension dim = getSize();
		if (image != null) {
			/*
			 * center Image in box, normally should exactly fill the box. If we
			 * overflow, no problem, drawImage will clip.
			 */
			imageWidth = image.getWidth(this);
			imageHeight = image.getHeight(this);

			// this does not complete the job, just starts it.
			// We are notified of progress through our Component ImageObserver
			// interface.
			g.drawImage(image, 
						(dim.width - imageWidth) / 2, (dim.height - imageHeight) / 2,
						imageWidth, imageHeight, this);
//			System.out.println(imageWidth + ", " + imageHeight + ", " + g.getClip());
		}
		else {
			/* we have no Image, clear the box */
			g.setColor(getBackground());
			g.clearRect(0, 0, dim.width, dim.height);
//			System.out.println("No Image, " + g.getClip());
		}
	}

	@Override
	public Dimension getPreferredSize() {
		if ((componentWidth == -1) && (componentHeight == -1)) {
			if (image != null) {
				/* should just fit the Image */
				return (new Dimension(image.getWidth(this), image.getHeight(this)));
			}
			else {
				return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
			}
		}
		else
			return new Dimension(componentWidth, componentHeight);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

}