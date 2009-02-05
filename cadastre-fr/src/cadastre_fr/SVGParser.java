package cadastre_fr;

import java.util.ArrayList;

/**
 * This class is not intended to be a real SVG parser. It's also not using existing
 * xml parsers. It's just extracting the required strings from an SVG file coming
 * from the French land registry cadastre.gouv.fr 
 *
 */
public class SVGParser {

    private String cViewBoxStart = "viewBox=\"";
    private String cViewBoxEnd = "\"";
    private String cPathStart = "<path d=\"";
    private String cPathEnd = "\"/>";

    /**
     * The SVG viewBox looks like this:
     *   viewBox="969780.0 320377.11 5466.130000000005 2846.429999999993"
     * @param svg the SVG XML data
     * @return double [x,y,dx,dy] of viewBox; null if parsing failed
     */
    public double[] getViewBox(String svg) {
        int s = svg.indexOf(cViewBoxStart)+cViewBoxStart.length();
        int e = svg.indexOf(cViewBoxEnd, s);
        if (s != -1 && e != -1) {
            try {
                String str = svg.substring(s, e);
                String [] viewBox = str.split(" ");
                double[] dbox = new double[4];
                for (int i = 0; i<4; i++)
                    dbox[i] = Double.parseDouble(viewBox[i]); 
                return dbox;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }
    
    public String [] getPaths(String svg) {
        ArrayList<String> path = new ArrayList<String>();
        int i = 0;
        while (svg.indexOf(cPathStart, i) != -1) {
            int s = svg.indexOf(cPathStart, i) + cViewBoxStart.length();
            int e = svg.indexOf(cPathEnd, s);
            if (s != -1 && e != -1) {
                path.add(svg.substring(s, e));
            } else
                break;
            i = e;
        }
        return path.toArray(new String[ path.size() ]);
    }

}
