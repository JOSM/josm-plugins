package usertools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;

import javax.swing.*;

import org.openstreetmap.josm.actions.JosmAction;
//import org.openstreetmap.josm.actions.search.*;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.gui.dialogs.UserListDialog;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * plugin for JOSM Java OpenStreetMap Editor 
 * for use with authors and users
 * has basic functionality at the moment, and messy code!
 * opens browser for selected user page
 * Selects data based on user on map
 *
 * @author Tim Waters (chippy)
 */



public class UserToolsPlugin extends Plugin {
    static JMenu userJMenu;

    //private static User anonymousUser = User.get("(anonymous users)");

    public UserToolsPlugin() {
        refreshMenu();
    }
    public static enum SearchMode {replace, add, remove}

    private static String urlEncodeWithNoPluses(String input){
        if(input==null)
            return null;
        String inputEnc = "";
        try {
            inputEnc = java.net.URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        StringBuffer retu = new StringBuffer();
        for(int i=0;i<inputEnc.length();i++){
            if(inputEnc.charAt(i)=='+')
                retu.append("%20");
            else
                retu.append(inputEnc.charAt(i));
        }
        return retu.toString();
    }

//   TODO refactor these actions
    public static void refreshMenu() {
        MainMenu menu = Main.main.menu;

        if (userJMenu == null) {
            userJMenu = new JMenu(tr("User"));
            menu.add(userJMenu, KeyEvent.VK_U, "user");
            menu.add(userJMenu, 5);
        } else {
            userJMenu.removeAll();
        }
        JosmAction a = new JosmAction(tr("Show Author Panel"),
        "dialogs/userlist", tr("Show Author Panel"), null, false) {
            public void actionPerformed(ActionEvent ev) {
                int dialogIndex = 100;
                for (int i = 0; i < Main.map.toggleDialogs.getComponentCount(); i++) {

                    if (Main.map.toggleDialogs.getComponent(i).getClass()
                            .getSimpleName().equals("UserListDialog")) {

                        dialogIndex = i;
                    }
                }
                UserListDialog uld = (UserListDialog) Main.map.toggleDialogs.getComponent(dialogIndex);

                uld.setVisible(true);
            }
        };
        a.putValue("toolbar", "usertools_show");
        userJMenu.add(new JMenuItem(a));

        userJMenu.addSeparator();
        a = new JosmAction(tr("Open User Page"),
                "presets/letter", tr("Open User Page in browser"), null, false) {
            public void actionPerformed(ActionEvent ev) {
                String name =  getSelectedUser();
                if (!name.equals("0")){
                String userUrl = "http://www.openstreetmap.org/user/" + name;
                //System.out.println("User Tools plugin: Opening browser "+userUrl);
                OpenBrowser.displayUrl(userUrl);
                }
            }
        };
        a.putValue("toolbar", "usertools_open");
        userJMenu.add(new JMenuItem(a));

        a = new JosmAction(tr("Select User's Data"),
        "dialogs/search", tr("Replaces Selection with Users data"), null, false) {
            public void actionPerformed(ActionEvent ev) {
                String name =  getSelectedUser();
                if (!name.equals("0")){
                    SearchAction.SearchMode mode = SearchAction.SearchMode.replace;
                    SearchAction.search(name, mode, false,false);
                }
            }
        };
        a.putValue("toolbar", "usertools_search");
        userJMenu.add(new JMenuItem(a));

        setEnabledAll(false);
    }

    public static String getSelectedUser(){
        int dialogIndex = 100;
        for (int i = 0; i < Main.map.toggleDialogs.getComponentCount(); i++) {

            //  System.out.println(Main.map.toggleDialogs.getComponent(i)
            //      .getClass().getSimpleName());

            if (Main.map.toggleDialogs.getComponent(i).getClass()
                    .getSimpleName().equals("UserListDialog")) {
                dialogIndex = i;
            }
        }
        UserListDialog uld = (UserListDialog) Main.map.toggleDialogs.getComponent(dialogIndex);

        //these are hard coded, probably better to search, like above
        JScrollPane jsp = (JScrollPane) uld.getComponent(1);
        JViewport view = (JViewport)jsp.getComponent(0);
        JTable tab = (JTable)view.getComponent(0);

        if (tab.getRowCount() == 0) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select some data"));
            return "0";
        }
        if (tab.getSelectedRow() == -1 && tab.getRowCount() > 0){
            JOptionPane.showMessageDialog(Main.parent, tr("Please choose a user using the author panel"));
            return "0";
        }

        String name = "(anonymous users)";
        //(anonymous users)
        if (tab.getSelectedRow() > -1){
        //  System.out.println(tab.getValueAt(tab.getSelectedRow(), tab.getSelectedColumn()));
            name = (String)tab.getValueAt(tab.getSelectedRow(), 0);
        }

        if (name.equals("(anonymous users)") ) {
            JOptionPane.showMessageDialog(Main.parent,
            tr("Sorry, doesn't work with anonymous users"));
            return "0";
        }
        name = urlEncodeWithNoPluses(name);

        return name;
    }

    private static void setEnabledAll(boolean isEnabled) {
        for(int i=0; i < userJMenu.getItemCount(); i++) {
            JMenuItem item = userJMenu.getItem(i);

            if(item != null) item.setEnabled(isEnabled);
        }
    }

    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame==null && newFrame!=null) { 
            setEnabledAll(true);
        } else if (oldFrame!=null && newFrame==null ) {
            setEnabledAll(false);
        }
    }
}
