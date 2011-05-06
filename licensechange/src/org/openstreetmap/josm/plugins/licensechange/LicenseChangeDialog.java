// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.plugins.licensechange;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * A small tool dialog for displaying the current problems. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 */
public class LicenseChangeDialog extends ToggleDialog implements ActionListener, SelectionChangedListener {
    private LicenseChangePlugin plugin;

    /** The display tree */
    protected ProblemTreePanel tree;

    private SideButton selectButton;
    /** The select button */

    private JPopupMenu popupMenu;
    private LicenseProblem popupMenuError = null;

    /** Last selected element */
    private DefaultMutableTreeNode lastSelectedNode = null;

    /**
     * Constructor
     */
    public LicenseChangeDialog(LicenseChangePlugin plugin) 
    {
        super(tr("Relicensing problems"), "licensechange", tr("Open the relicensing window."),
                Shortcut.registerShortcut("subwindow:licensechange", tr("Toggle: {0}", tr("Relicensing problems")),
                        KeyEvent.VK_V, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150);

        this.plugin = plugin;
        popupMenu = new JPopupMenu();

        JMenuItem zoomTo = new JMenuItem(tr("Zoom to problem"));
        zoomTo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                zoomToProblem();
            }
        });
        popupMenu.add(zoomTo);

        tree = new ProblemTreePanel();
        tree.addMouseListener(new ClickWatch());
        tree.addTreeSelectionListener(new SelectionWatch());

        add(new JScrollPane(tree), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));

        selectButton = new SideButton(marktr("Select"), "select", "LicenseChange",
                tr("Set the selected elements on the map to the selected items in the list above."), this);
        selectButton.setEnabled(false);
        buttonPanel.add(selectButton);
        buttonPanel.add(new SideButton(plugin.validateAction), "refresh");
        add(buttonPanel, BorderLayout.SOUTH);

    }

    @Override
    public void showNotify() 
    {
        DataSet.addSelectionListener(this);
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds != null) {
            updateSelection(ds.getSelected());
        }
    }

    @Override
    public void hideNotify() 
    {
        DataSet.removeSelectionListener(this);
    }

    @Override
    public void setVisible(boolean v) 
    {
        if (tree != null)
            tree.setVisible(v);
        super.setVisible(v);
        Main.map.repaint();
    }

    @SuppressWarnings("unchecked")
    private void showPopupMenu(MouseEvent e) 
    {
        if (!e.isPopupTrigger())
            return;
        popupMenuError = null;
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null)
            return;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getPathComponent(selPath.getPathCount() - 1);
        if (!(node.getUserObject() instanceof LicenseProblem))
            return;
        popupMenuError = (LicenseProblem) node.getUserObject();
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void zoomToProblem() 
    {
        if (popupMenuError == null)
            return;
        LicenseChangeBoundingXYVisitor bbox = new LicenseChangeBoundingXYVisitor();
        popupMenuError.visitHighlighted(bbox);
        if (bbox.getBounds() == null)
            return;
        bbox.enlargeBoundingBox();
        Main.map.mapView.recalculateCenterScale(bbox);
    }

    /**
     * Sets the selection of the map to the current selected items.
     */
    @SuppressWarnings("unchecked")
    private void setSelectedItems() 
    {
        if (tree == null)
            return;

        Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>(40);

        TreePath[] selectedPaths = tree.getSelectionPaths();
        if (selectedPaths == null)
            return;

        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Enumeration<DefaultMutableTreeNode> children = node.breadthFirstEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = children.nextElement();
                Object nodeInfo = childNode.getUserObject();
                if (nodeInfo instanceof LicenseProblem) {
                    LicenseProblem error = (LicenseProblem) nodeInfo;
                    sel.addAll(error.getPrimitives());
                }
            }
        }

        Main.main.getCurrentDataSet().setSelected(sel);
    }

    public void actionPerformed(ActionEvent e) 
    {
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("Select"))
            setSelectedItems();
    }

    /**
     * @param sel
     *            The collection where to add all selected elements
     * @param addSelected
     *            if true, add all selected elements to collection
     */
    @SuppressWarnings("unchecked")
    private void setSelection(Collection<OsmPrimitive> sel, boolean addSelected) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (lastSelectedNode != null && !lastSelectedNode.equals(node)) {
            Enumeration<DefaultMutableTreeNode> children = lastSelectedNode.breadthFirstEnumeration();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = children.nextElement();
                Object nodeInfo = childNode.getUserObject();
                if (nodeInfo instanceof LicenseProblem) {
                    LicenseProblem error = (LicenseProblem) nodeInfo;
                    error.setSelected(false);
                }
            }
        }

        lastSelectedNode = node;
        if (node == null) return;

        Enumeration<DefaultMutableTreeNode> children = node.breadthFirstEnumeration();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childNode = children.nextElement();
            Object nodeInfo = childNode.getUserObject();
            if (nodeInfo instanceof LicenseProblem) {
                LicenseProblem error = (LicenseProblem) nodeInfo;
                error.setSelected(true);

                if (addSelected) {
                    sel.addAll(error.getPrimitives());
                }
            }
        }
        selectButton.setEnabled(true);
    }

    /**
     * Watches for clicks.
     */
    public class ClickWatch extends MouseAdapter 
    {
        @Override
        public void mouseClicked(MouseEvent e) {
            selectButton.setEnabled(false);

            boolean isDblClick = e.getClickCount() > 1;

            Collection<OsmPrimitive> sel = isDblClick ? new HashSet<OsmPrimitive>(40) : null;

            setSelection(sel, isDblClick);

            if (isDblClick) {
                Main.main.getCurrentDataSet().setSelected(sel);
                if(Main.pref.getBoolean("licensechange.autozoom", false))
                    AutoScaleAction.zoomTo(sel);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            showPopupMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopupMenu(e);
        }

    }

    /**
     * Watches for tree selection.
     */
    public class SelectionWatch implements TreeSelectionListener 
    {
        public void valueChanged(TreeSelectionEvent e) {
            selectButton.setEnabled(false);

            if (e.getSource() instanceof JScrollPane) {
                System.out.println(e.getSource());
                return;
            }

            setSelection(null, false);
            Main.map.repaint();
        }
    }

    public static class LicenseChangeBoundingXYVisitor extends BoundingXYVisitor implements LicenseChangeVisitor 
    {
        public void visit(OsmPrimitive p) {
            if (p.isUsable()) {
                p.visit(this);
            }
        }

        public void visit(WaySegment ws) {
            if (ws.lowerIndex < 0 || ws.lowerIndex + 1 >= ws.way.getNodesCount())
                return;
            visit(ws.way.getNodes().get(ws.lowerIndex));
            visit(ws.way.getNodes().get(ws.lowerIndex + 1));
        }

        public void visit(List<Node> nodes) {
            for (Node n: nodes) {
                visit(n);
            }
        }
    }

    public void updateSelection(Collection<? extends OsmPrimitive> newSelection) 
    {
        if (newSelection.isEmpty())
            tree.setFilter(null);
        HashSet<OsmPrimitive> filter = new HashSet<OsmPrimitive>(newSelection);
        tree.setFilter(filter);
    }

    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) 
    {
        updateSelection(newSelection);
    }
}
