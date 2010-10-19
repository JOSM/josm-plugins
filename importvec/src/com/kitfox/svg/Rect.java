/*
 * Rect.java
 *
 *
 *  The Salamander Project - 2D and 3D graphics libraries in Java
 *  Copyright (C) 2004 Mark McKay
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *  Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 *  projects can be found at http://www.kitfox.com
 *
 * Created on January 26, 2004, 5:25 PM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;

import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class Rect extends ShapeElement {

    float x = 0f;
    float y = 0f;
    float width = 0f;
    float height = 0f;
    float rx = 0f;
    float ry = 0f;

    RectangularShape rect;

    /** Creates a new instance of Rect */
    public Rect() {
    }

    private void writeObject(ObjectOutputStream out) throws IOException    
    {
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(width);
        out.writeFloat(height);
        out.writeFloat(rx);
        out.writeFloat(ry);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
        x = in.readFloat();
        y = in.readFloat();
        width = in.readFloat();
        height = in.readFloat();
        rx = in.readFloat();
        ry = in.readFloat();
        
        if (rx == 0f && ry == 0f)
        {
            rect = new Rectangle2D.Float(x, y, width, height);
        }
        else
        {
            rect = new RoundRectangle2D.Float(x, y, width, height, rx * 2, ry * 2);
        }
    }
    
    /*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        String x = attrs.getValue("x");
        String y = attrs.getValue("y");
        String width = attrs.getValue("width");
        String height = attrs.getValue("height");
        String rx = attrs.getValue("rx");
        String ry = attrs.getValue("ry");

        if (rx == null) rx = ry;
        if (ry == null) ry = rx;

        this.x = XMLParseUtil.parseFloat(x);
        this.y = XMLParseUtil.parseFloat(y);
        this.width = XMLParseUtil.parseFloat(width);
        this.height = XMLParseUtil.parseFloat(height);
        if (rx != null)
        {
            this.rx = XMLParseUtil.parseFloat(rx);
            this.ry = XMLParseUtil.parseFloat(ry);
        }

        build();
//        setBounds(this.x, this.y, this.width, this.height);
    }
*/
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
//        SVGElement parent = this.getParent();
//        if (parent instanceof RenderableElement)
//        {
//            RenderableElement re = (RenderableElement)parent;
//            Rectangle2D bounds = re.getBoundingBox();
//            bounds = null;
//        }
        
        
        if (getPres(sty.setName("x"))) x = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("y"))) y = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("width"))) width = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("height"))) height = sty.getFloatValueWithUnits();

        boolean rxSet = false;
        if (getPres(sty.setName("rx"))) { rx = sty.getFloatValueWithUnits(); rxSet = true; }
        
        boolean rySet = false;
        if (getPres(sty.setName("ry"))) { ry = sty.getFloatValueWithUnits(); rySet = true; }
        
        if (!rxSet) rx = ry;
        if (!rySet) ry = rx;

        
        if (rx == 0f && ry == 0f)
        {
            rect = new Rectangle2D.Float(x, y, width, height);
        }
        else
        {
            rect = new RoundRectangle2D.Float(x, y, width, height, rx * 2, ry * 2);
        }
    }

    public void render(Graphics2D g) throws SVGException
    {
        beginLayer(g);
        renderShape(g, rect);
        finishLayer(g);
    }

    public Shape getShape()
    {
        return shapeToParent(rect);
    }

    public Rectangle2D getBoundingBox() throws SVGException
    {
        return boundsToParent(includeStrokeInBounds(rect.getBounds2D()));
    }

    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
//        if (trackManager.getNumTracks() == 0) return false;
        boolean changeState = super.updateTime(curTime);

        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        boolean shapeChange = false;
        
        if (getPres(sty.setName("x")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != x)
            {
                x = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("y")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != y)
            {
                y = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("width")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != width)
            {
                width = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("height")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != height)
            {
                height = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("rx")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != rx)
            {
                rx = newVal;
                shapeChange = true;
            }
        }

        if (getPres(sty.setName("ry")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != ry)
            {
                ry = newVal;
                shapeChange = true;
            }
        }

        if (shapeChange)
        {
            build();
//            if (rx == 0f && ry == 0f)
//            {
//                rect = new Rectangle2D.Float(x, y, width, height);
//            }
//            else
//            {
//                rect = new RoundRectangle2D.Float(x, y, width, height, rx * 2, ry * 2);
//            }
//            return true;
        }
        
        return changeState || shapeChange;
    }
}
