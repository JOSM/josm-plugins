// License: WTFPL
package geochat;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.*;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

/**
 * Chat Panel. Contains of one public chat pane and multiple private ones.
 *
 * @author zverik
 */
public class GeoChatPanel extends ToggleDialog implements ChatServerConnectionListener, MapViewPaintable {
    private JTextField input;
    private JTabbedPane tabs;
    private JComponent noData;
    private JPanel loginPanel;
    private JPanel gcPanel;
    private ChatServerConnection connection;
    // those fields should be visible to popup menu actions
    Map<String, LatLon> users;
    ChatPaneManager chatPanes;
    boolean userLayerActive;
    
    public GeoChatPanel() {
        super(tr("GeoChat"), "geochat", tr("Open GeoChat panel"), null, 200, true);

        noData = new JLabel(tr("Zoom in to see messages"), SwingConstants.CENTER);

        tabs = new JTabbedPane();
        tabs.addMouseListener(new GeoChatPopupAdapter(this));
        chatPanes = new ChatPaneManager(this, tabs);

        input = new JPanelTextField() {
            @Override
            protected void processEnter( String text ) {
                connection.postMessage(text, chatPanes.getRecipient());
            }

            @Override
            protected String autoComplete( String word, boolean atStart ) {
                return autoCompleteUser(word, atStart);
            }
        };
        
        String defaultUserName = constructUserName();
        loginPanel = createLoginPanel(defaultUserName);

        gcPanel = new JPanel(new BorderLayout());
        gcPanel.add(loginPanel, BorderLayout.CENTER);
        createLayout(gcPanel, false, null);

        users = new TreeMap<>();
        // Start threads
        connection = ChatServerConnection.getInstance();
        connection.addListener(this);
        boolean autoLogin = Main.pref.get("geochat.username", null) == null ? false : Main.pref.getBoolean("geochat.autologin", true);
        connection.autoLoginWithDelay(autoLogin ? defaultUserName : null);
        updateTitleAlarm();
    }

    private String constructUserName() {
        String userName = Main.pref.get("geochat.username", null); // so the default is null
        if( userName == null )
            userName = JosmUserIdentityManager.getInstance().getUserName();
        if( userName == null )
            userName = "";
        if( userName.contains("@") )
            userName = userName.substring(0, userName.indexOf('@'));
        userName = userName.replace(' ', '_');
        return userName;
    }

    private JPanel createLoginPanel( String defaultUserName ) {
        final JTextField nameField = new JPanelTextField() {
            @Override
            protected void processEnter( String text ) {
                connection.login(text);
            }
        };
        nameField.setText(defaultUserName);

        JButton loginButton = new JButton(tr("Login"));
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                connection.login(nameField.getText());
            }
        });
        nameField.setPreferredSize(new Dimension(nameField.getPreferredSize().width, loginButton.getPreferredSize().height));

        final JCheckBox autoLoginBox = new JCheckBox(tr("Enable autologin"), Main.pref.getBoolean("geochat.autologin", true));
        autoLoginBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                Main.pref.put("geochat.autologin", autoLoginBox.isSelected());
            }
        });

        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(nameField, GBC.std().fill(GridBagConstraints.HORIZONTAL).insets(15, 0, 5, 0));
        panel.add(loginButton, GBC.eol().fill(GridBagConstraints.NONE).insets(0, 0, 15, 0));
        panel.add(autoLoginBox, GBC.std().insets(15, 0, 15, 0));
        return panel;
    }

    protected void logout() {
        connection.logout();
    }

    @Override
    public void destroy() {
        try {
            if( Main.pref.getBoolean("geochat.logout.on.close", true) ) {
                connection.removeListener(this);
                connection.bruteLogout();
            }
        } catch( IOException e ) {
            Main.warn("Failed to logout from geochat server: " + e.getMessage());
        }
        super.destroy();
    }

    private String autoCompleteUser( String word, boolean atStart ) {
        String result = null;
        boolean singleUser = true;
        for( String user : users.keySet() ) {
            if( user.startsWith(word) ) {
                if( result == null )
                    result = user;
                else {
                    singleUser = false;
                    int i = word.length();
                    while( i < result.length() && i < user.length() && result.charAt(i) == user.charAt(i) )
                        i++;
                    if( i < result.length() )
                        result = result.substring(0, i);
                }
            }
        }
        return result == null ? null : !singleUser ? result : atStart ? result + ": " : result + " ";
    }

    /**
     * This is implementation of a "temporary layer". It paints circles
     * for all users nearby.
     */
    @Override
    public void paint( Graphics2D g, MapView mv, Bounds bbox ) {
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Composite ac04 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
        Composite ac10 = g2d.getComposite();

        Font font = g2d.getFont().deriveFont(Font.BOLD, g2d.getFont().getSize2D() + 2.0f);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        for( String user : users.keySet() ) {
            int stringWidth = fm.stringWidth(user);
            int radius = stringWidth / 2 + 10;
            Point p = mv.getPoint(users.get(user));

            g2d.setComposite(ac04);
            g2d.setColor(Color.white);
            g2d.fillOval(p.x - radius, p.y - radius, radius * 2 + 1, radius * 2 + 1);

            g2d.setComposite(ac10);
            g2d.setColor(Color.black);
            g2d.drawString(user, p.x - stringWidth / 2, p.y + fm.getDescent());
        }
    }

    /* ================== Notifications in the title ======================= */

    /**
     * Display number of users and notifications in the panel title.
     */
    protected void updateTitleAlarm() {
        int alarmLevel = connection.isLoggedIn() ? chatPanes.getNotifyLevel() : 0;
        if( !isDialogInCollapsedView() && alarmLevel > 1 )
            alarmLevel = 1;

        String comment;
        if( connection.isLoggedIn() ) {
            comment = trn("{0} user", "{0} users", users.size() + 1, users.size() + 1);
        } else {
            comment = tr("not logged in");
        }

        String title = tr("GeoChat");
        if( comment != null )
            title = title + " (" + comment + ")";
        final String alarm = (alarmLevel <= 0 ? "" : alarmLevel == 1 ? "* " : "!!! ") + title;
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                setTitle(alarm);
            }
        });
    }

    /**
     * Track panel collapse events.
     */
    @Override
    protected void setIsCollapsed( boolean val ) {
        super.setIsCollapsed(val);
        chatPanes.setCollapsed(val);
        updateTitleAlarm();
    }

    /* ============ ChatServerConnectionListener methods ============= */

    @Override
    public void loggedIn( String userName ) {
        Main.pref.put("geochat.username", userName);
        if( gcPanel.getComponentCount() == 1 ) {
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
                    gcPanel.remove(0);
                    gcPanel.add(tabs, BorderLayout.CENTER);
                    gcPanel.add(input, BorderLayout.SOUTH);
                }
            });
        }
        updateTitleAlarm();
    }

    @Override
    public void notLoggedIn( final String reason ) {
        if( reason != null ) {
            GuiHelper.runInEDT(new Runnable() {
                @Override
                public void run() {
                    new Notification(tr("Failed to log in to GeoChat:") + "\n" + reason).show();
                }
            });
        } else {
            // regular logout
            if( gcPanel.getComponentCount() > 1 ) {
                gcPanel.removeAll();
                gcPanel.add(loginPanel, BorderLayout.CENTER);
            }
        }
        updateTitleAlarm();
    }

    @Override
    public void messageSendFailed( final String reason ) {
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                new Notification(tr("Failed to send message:") + "\n" + reason).show();
            }
        });
    }

    @Override
    public void statusChanged( boolean active ) {
        // only the public tab, because private chats don't rely on coordinates
        tabs.setComponentAt(0, active ? chatPanes.getPublicChatComponent() : noData);
        repaint();
    }

    @Override
    public void updateUsers( Map<String, LatLon> newUsers ) {
        for( String uname : this.users.keySet() ) {
            if( !newUsers.containsKey(uname) )
                chatPanes.addLineToPublic(tr("User {0} has left", uname), ChatPaneManager.MESSAGE_TYPE_INFORMATION);
        }
        for( String uname : newUsers.keySet() ) {
            if( !this.users.containsKey(uname) )
                chatPanes.addLineToPublic(tr("User {0} is mapping nearby", uname), ChatPaneManager.MESSAGE_TYPE_INFORMATION);
        }
        this.users = newUsers;
        updateTitleAlarm();
        if( userLayerActive && Main.map.mapView != null )
            Main.map.mapView.repaint();
    }

    private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    private void formatMessage( StringBuilder sb, ChatMessage msg ) {
        sb.append("\n");
        sb.append('[').append(TIME_FORMAT.format(msg.getTime())).append("] ");
        sb.append(msg.getAuthor()).append(": ").append(msg.getMessage());
    }

    @Override
    public void receivedMessages( boolean replace, List<ChatMessage> messages ) {
        if( replace )
            chatPanes.clearPublicChatPane();
        if( !messages.isEmpty() ) {
            int alarm = 0;
            StringBuilder sb = new StringBuilder();
            for( ChatMessage msg : messages ) {
                boolean important = msg.isIncoming() && containsName(msg.getMessage());
                if( msg.isIncoming() && alarm < 2 ) {
                    alarm = important ? 2 : 1;
                }
                if( important ) {
                    // add buffer, then add current line with italic, then clear buffer
                    chatPanes.addLineToPublic(sb.toString());
                    sb.setLength(0);
                    formatMessage(sb, msg);
                    chatPanes.addLineToPublic(sb.toString(), ChatPaneManager.MESSAGE_TYPE_ATTENTION);
                    sb.setLength(0);
                } else
                    formatMessage(sb, msg);
            }
            chatPanes.addLineToPublic(sb.toString());
            if( alarm > 0 )
                chatPanes.notify(null, alarm);
        }
        if( replace )
            showNearbyUsers();
    }

    private void showNearbyUsers() {
        if( !users.isEmpty() ) {
            StringBuilder sb = new StringBuilder(tr("Users mapping nearby:"));
            boolean first = true;
            for( String user : users.keySet() ) {
                sb.append(first ? " " : ", ");
                sb.append(user);
            }
            chatPanes.addLineToPublic(sb.toString(), ChatPaneManager.MESSAGE_TYPE_INFORMATION);
        }
    }

    private boolean containsName( String message ) {
        String userName = connection.getUserName();
        int length = userName.length();
        int i = message.indexOf(userName);
        while( i >= 0 ) {
            if( (i == 0 || !Character.isJavaIdentifierPart(message.charAt(i - 1)))
                    && (i + length >= message.length() || !Character.isJavaIdentifierPart(message.charAt(i + length))) )
                return true;
            i = message.indexOf(userName, i + 1);
        }
        return false;
    }

    @Override
    public void receivedPrivateMessages( boolean replace, List<ChatMessage> messages ) {
        if( replace )
            chatPanes.closePrivateChatPanes();
        for( ChatMessage msg : messages ) {
            StringBuilder sb = new StringBuilder();
            formatMessage(sb, msg);
            chatPanes.addLineToChatPane(msg.isIncoming() ? msg.getAuthor() : msg.getRecipient(), sb.toString());
            if( msg.isIncoming() )
                chatPanes.notify(msg.getAuthor(), 2);
        }
    }
}
