package mappaint;
import java.awt.Color;

public class LineElemStyle extends ElemStyle
{
	int width;
	int realWidth = 0; //the real width of this line in meter
	Color colour;

	public LineElemStyle (int width, int realWidth, Color colour, int minZoom)
	{
		this.width = width;
		this.realWidth = realWidth;
		this.colour = colour;
		this.minZoom = minZoom;
	}

	public int getWidth()
	{
		return width;
	}

	public int getRealWidth()
	{
		return realWidth;
	}

	public Color getColour()
	{
		return colour;
	}

	@Override public String toString()
	{
		return "LineElemStyle:  width= " + width +  " colour=" + colour;
	}
}
