// License: GPL. Copyright 2007 by Immanuel Scholz and others
package utilsplugin2;

import java.awt.GridBagLayout;
import java.io.UnsupportedEncodingException;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URLEncoder;
import java.util.Collection;

import java.util.List;
import java.util.regex.Matcher;

import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Mirror the selected ways nodes or ways along line given by two first selected points
 *
 * Note: If a ways are selected, their nodes are mirrored
 *
 * @author Alexei Kasatkin, based on much copy&Paste from other MirrorAction :)
 */ 
public final class OpenPageAction extends JosmAction {
    private final String defaultURL = "http://ru.wikipedia.org/w/index.php?search={name}&fulltext=Search";

    public OpenPageAction() {
        super(tr("Open custom URL"), "openurl",
                tr("Opens specified URL browser"),
                Shortcut.registerShortcut("tools:openurl", tr("Tool: {0}", tr("Open custom URL")),
                KeyEvent.VK_BACK_SLASH, Shortcut.GROUP_EDIT, Shortcut.SHIFT_DEFAULT), true);
        putValue("help", ht("/Action/OpenPage"));
    }

    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        OsmPrimitive p=null;
        if (sel.size()==1) {
            p=sel.iterator().next();
        } else {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select one element to open custom URL for it. You can choose the URL in Preferences, Utils tab."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        if (Main.pref.getBoolean("utilsplugin2.askurl",false)==true) 
            ChooseURLAction.showConfigDialog();
        
        String addr = Main.pref.get("utilsplugin2.customurl", defaultURL);
        Pattern pat = Pattern.compile("\\{([^\\}]*)\\}");
        Matcher m = pat.matcher(addr);
        String val,key;
        String keys[]=new String[100],vals[]=new String[100];
        int i=0;
        try {
        while (m.find()) {
                key=m.group(1);
                if (key.equals("#id")) {
                    val=Long.toString(p.getId());
                } else if (key.equals("#type")) {
                    val = OsmPrimitiveType.from(p).getAPIName();
                } else if (key.equals("#lat")) {
                    val = Double.toString(p.getBBox().getTopLeft().lat());
                } else if (key.equals("#lon")) {
                    val = Double.toString(p.getBBox().getTopLeft().lon());
                }
                else {
                    val =p.get(key);
                    if (val!=null) val =URLEncoder.encode(p.get(key), "UTF-8"); else return;
                }
                keys[i]=m.group();
                if  (val!=null) vals[i]=val;
                else vals[i]="";
                i++;
        }
        } catch (UnsupportedEncodingException ex) {
            System.err.println("Encoding error");
            return;
        }
        for (int j=0;j<i;j++){
            addr = addr.replace(keys[j],vals[j]);
        }
        try {
            OpenBrowser.displayUrl(addr);
        } catch (Exception ex) {
            System.err.println("Can not open URL"+addr);
        }
        //Collection<Command> cmds = new LinkedList<Command>();

          //  cmds.add(new MoveCommand(n, -2*ne*pr, -2*nn*pr ));
        //Main.main.undoRedo.add(new SequenceCommand(tr("Symmetry"), cmds));
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null );
    }

}
