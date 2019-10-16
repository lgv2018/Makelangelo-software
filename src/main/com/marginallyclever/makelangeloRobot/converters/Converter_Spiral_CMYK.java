package com.marginallyclever.makelangeloRobot.converters;


import java.awt.Color;
import java.io.IOException;
import java.io.Writer;

import com.marginallyclever.makelangelo.Log;
import com.marginallyclever.makelangeloRobot.TransformedImage;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangeloRobot.imageFilters.Filter_CMYK;

/**
 * Generate a Gcode file from the BufferedImage supplied.<br>
 * Use the filename given in the constructor as a basis for the gcode filename, but change the extension to .ngc
 *  
 * Inspired by reddit user bosny
 * 
 * @author Dan
 */
public class Converter_Spiral_CMYK extends ImageConverter {
	private static boolean convertToCorners = false;  // draw the spiral right out to the edges of the square bounds.

	@Override
	public String getName() {
		return Translator.get("SpiralCMYKName");
	}

	@Override
	public ImageConverterPanel getPanel() {
		return new Converter_Spiral_CMYK_Panel(this);
	}

	public boolean getToCorners() {
		return convertToCorners;
	}
	
	public void setToCorners(boolean arg0) {
		convertToCorners=arg0;
	}
	
	/**
	 * create a spiral across the image.  raise and lower the pen to darken the appropriate areas
	 *
	 * @param img the image to convert.
	 */
	@Override
	public void finish(Writer out) throws IOException {
		Filter_CMYK cmyk = new Filter_CMYK();
		cmyk.filter(sourceImage);

		// avoids changing to black and then back to yellow.
		Color defaultColor = machine.getPenDownColorDefault();
		Color yellow = new Color(255,255,  0);
		machine.setPenDownColorDefault(yellow);
		
		imageStart(out);

		double separation; 
		float h2 = (float)machine.getPaperHeight();
		float w2 = (float)machine.getPaperWidth();
		separation = (w2<h2) ? w2/4 : h2/4;
		
		Log.info("Yellow...");		outputChannel(out,cmyk.getY(),new Color(255,255,  0),255.0*1.0,Math.cos(Math.toRadians(45    ))*separation,Math.sin(Math.toRadians(45    ))*separation);
		Log.info("Cyan...");		outputChannel(out,cmyk.getC(),new Color(  0,255,255),255.0*1.0,Math.cos(Math.toRadians(45+ 90))*separation,Math.sin(Math.toRadians(45+ 90))*separation);
		Log.info("Magenta...");		outputChannel(out,cmyk.getM(),new Color(255,  0,255),255.0*1.0,Math.cos(Math.toRadians(45+180))*separation,Math.sin(Math.toRadians(45+180))*separation);
		Log.info("Black...");		outputChannel(out,cmyk.getK(),new Color(  0,  0,  0),255.0*1.0,Math.cos(Math.toRadians(45+270))*separation,Math.sin(Math.toRadians(45+270))*separation);
		Log.info("Finishing...");

		machine.setPenDownColorDefault(defaultColor);
		imageEnd(out);
	}

	protected void outputChannel(Writer out,TransformedImage img,Color newColor,double cutoff,double cx,double cy) throws IOException {
		liftPen(out);
		machine.writeChangeTo(out,newColor);
		
		double toolDiameter = machine.getPenDiameter();

		int i, j;
		int steps = 4;
		double leveladd = cutoff / (double)(steps+1);
		double level;
		int z = 0;

		float maxr;
		if (convertToCorners) {
			// go right to the corners
			float h2 = (float)machine.getPaperHeight();
			float w2 = (float)machine.getPaperWidth();
			maxr = (float) (Math.sqrt(h2 * h2 + w2 * w2) + 1.0f);
		} else {
			// do the largest circle that still fits in the image.
			float w = (float)machine.getPaperWidth()/2.0f;
			float h = (float)machine.getPaperHeight()/2.0f;
			maxr = (float)( h < w ? h : w );
			maxr *= machine.getPaperMargin() ;
		}

		
		double r = maxr, f;
		double fx, fy;
		int numRings = 0;
		j = 0;
		while (r > toolDiameter) {
			++j;
			level = leveladd * (1+(j % steps));
			// find circumference of current circle
			double circumference = Math.floor((2.0f * r - toolDiameter) * Math.PI);
			if (circumference > 360.0f) circumference = 360.0f;
			
			for (i = 1; i <= circumference; ++i) {
				f = Math.PI * 2.0f * (double)i / circumference;
				fx = cx+Math.cos(f) * r;
				fy = cy+Math.sin(f) * r;
				
				boolean isInside = isInsidePaperMargins(fx, fy);
				if(isInside) {
					try {
						z = img.sample3x3(fx, fy);
					} catch(Exception e) {
						e.printStackTrace();
					}
					
					if(z<level) {
						lowerPen(out);
					} else liftPen(out);
				} else liftPen(out);
				machine.writeMoveTo(out, fx, fy, isPenUp());
			}
			r -= toolDiameter;
			++numRings;
		}

		Log.info("yellow", numRings + " rings.");
	}
}


/**
 * This file is part of Makelangelo.
 * <p>
 * Makelangelo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Makelangelo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Makelangelo.  If not, see <http://www.gnu.org/licenses/>.
 */