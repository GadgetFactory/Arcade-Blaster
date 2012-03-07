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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

public class ProgressGlassPane extends JComponent
{
	private final Font MSG_HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
	private final Color TRANSLUCENT_COLOR = Color.WHITE;
	private final Color MSG_HEADER_FONT_COLOR = new Color(0xA01806);
	private final Color MSG_FONT_COLOR = new Color(0x251507);
	private final Color MSG_BACKGROUND_COLOR = new Color(0x001122);

    private static final int BAR_WIDTH = 350;
    private static final int BAR_HEIGHT = 13;
	private static final int CORRECTION = 7;
    
    private static final Font TEXT_FONT = new Font("Default", Font.BOLD, 16);
    private static final Color TEXT_COLOR = new Color(0x333333);
    private static final float[] GRADIENT_FRACTIONS = new float[] {
        0.0f, 0.499f, 0.5f, 1.0f
    };
    private static final Color[] GRADIENT_COLORS = new Color[] {
        Color.GRAY, Color.DARK_GRAY, Color.BLACK, Color.GRAY
    };
    private static final Color GRADIENT_COLOR2 = Color.WHITE;
    private static final Color GRADIENT_COLOR1 = Color.GRAY;

	private boolean bUpdateDialog;

	private Path2D.Double shapedScreenshot;

	private String[] msgLines = new String[4];
	private int stepIndex;
    private int progress;
	private String prevGameName;

	public ProgressGlassPane(Path2D.Double shapedScreenshot, boolean bUpdateDlg)
	{
		if (!bUpdateDlg) 
			this.shapedScreenshot = shapedScreenshot;
		bUpdateDialog = bUpdateDlg;

/*	Blocks mouse and keyboard input completely */
		this.addMouseListener(new MouseAdapter() { });
        this.addMouseMotionListener(new MouseMotionAdapter() { });
        this.addKeyListener(new KeyAdapter() { });

        this.setFocusTraversalKeysEnabled(false);
        this.addComponentListener(new ComponentAdapter() {
            @Override
			public void componentShown(ComponentEvent evt) {
                requestFocusInWindow();
            }
        });
	}

	@Override
	protected void paintComponent(Graphics g)
	{
        AlphaComposite alpha;
        Graphics2D g2d = (Graphics2D) g.create();
		Rectangle clip = g2d.getClipBounds();
        Composite prevComposite = g2d.getComposite();
		int x = 355;
		int y = 200;

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                			 RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (bUpdateDialog) {
        	g2d.setFont(TEXT_FONT);
            alpha = AlphaComposite.SrcOver.derive(0.65f);
            g2d.setComposite(alpha);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(clip.x, clip.y, clip.width, clip.height);

            // centers the progress bar on screen
            FontMetrics metrics = g.getFontMetrics();        
            x = (this.getWidth() - BAR_WIDTH) / 2;
            y = (this.getHeight() - BAR_HEIGHT - metrics.getDescent() - CORRECTION) / 2;
            // draws the text
            g2d.setColor(TEXT_COLOR);
            g2d.drawString(msgLines[0], x, y);
//            System.out.println("paintComponent, " + metrics.getDescent() + ", y = " + y + 
//            					", clip = " + clip);

            // goes to the position of the progress bar
            y += metrics.getDescent() + CORRECTION;
            // computes the size of the progress indicator
            int w = (int) (BAR_WIDTH * ((float) progress / 100.0f));
            int h = BAR_HEIGHT;
            Paint paint = g2d.getPaint();
            
            // bar's background
            Paint gradient = new GradientPaint(x, y, GRADIENT_COLOR1,
                    x, y + h, GRADIENT_COLOR2);
            g2d.setPaint(gradient);
            g2d.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);
            
            // actual progress
            gradient = new LinearGradientPaint(x, y, x, y + h,
                    GRADIENT_FRACTIONS, GRADIENT_COLORS);
            g2d.setPaint(gradient);
            g2d.fillRect(x, y, w, h);
            
            g2d.setPaint(paint);
            // draws the progress bar border
            g2d.drawRect(x, y, BAR_WIDTH, BAR_HEIGHT);
	        g2d.setComposite(prevComposite);
        }
        else {
        	alpha = AlphaComposite.SrcOver.derive(0.2f);
	        g2d.setComposite(alpha);
			g2d.setColor(TRANSLUCENT_COLOR);
			g2d.fillRect(clip.x, clip.y, clip.width, clip.height);
	
	        alpha = AlphaComposite.SrcOver.derive(0.65f);
	        g2d.setComposite(alpha);
	        g2d.setBackground(MSG_BACKGROUND_COLOR);
	        g2d.fill(shapedScreenshot);
	        g2d.setComposite(prevComposite);
			Font ftTemp = g2d.getFont();

	        // Write header
			g2d.setColor(MSG_HEADER_FONT_COLOR);
			g2d.setFont(MSG_HEADER_FONT);
			g2d.drawString(msgLines[0], x, y);
			y += 27;
			
			// Write progress
			for (int i = 1; i <= stepIndex; i++) {
				g2d.setFont(ftTemp);
				g2d.setColor(MSG_FONT_COLOR);
				g2d.drawString(msgLines[i], x, y);
				y += 17;
			}
        }

		g2d.dispose();
	}


	public void resetProgress(String headerMsg) {
		msgLines[0] = headerMsg + "...";
		stepIndex = 0;
		progress = 0;
		prevGameName = "";
	}
	
	public void setProgress(String msg) {
		stepIndex++;
		msgLines[stepIndex] = msg;
		
		this.repaint();		// TODO: Optimize
	}

    public void setProgress(String currGameName, int newProgress) {
        int oldProgress = progress;
        progress = newProgress;

        // computes the damaged area
        FontMetrics metrics = this.getGraphics().getFontMetrics(TEXT_FONT);
        int w = (int) (BAR_WIDTH * ((float) oldProgress / 100.0f));
        int x = w + (this.getWidth() - BAR_WIDTH) / 2;
        int y = (this.getHeight() - BAR_HEIGHT) / 2;
        int h = BAR_HEIGHT;

        if (!currGameName.equals(prevGameName)) {
        	msgLines[0] = "Downloading " + currGameName + " ...";
        	prevGameName = currGameName; 
            x = (this.getWidth() - BAR_WIDTH) / 2;
        	y -= (metrics.getDescent() + CORRECTION) / 2;
        	h += 150;
        	w = BAR_WIDTH;
//            System.out.println("setProgress, " + metrics.getDescent() + ", y = " + y + ", h = " + h);
        }
        else {
        	y += (metrics.getDescent() + CORRECTION) / 2;
        	w = (int) (BAR_WIDTH * ((float) newProgress / 100.0f)) - w;
        }
        
//        repaint(x, y, w, h);
        repaint();					// TODO: Optimize
    }

}
