// License: GPL. For details, see LICENSE file.
package mergeoverlap.hack;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.ImageProvider;

public class MyRelationMemberConflictResolver extends JPanel {

    private AutoCompletingTextField tfRole;
    private AutoCompletingTextField tfKey;
    private AutoCompletingTextField tfValue;
    private JCheckBox cbTagRelations;
    private MyRelationMemberConflictResolverModel model;
    private MyRelationMemberConflictResolverTable tblResolver;
    private JMultilineLabel lblHeader;

    protected void build() {
        setLayout(new GridBagLayout());
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(lblHeader = new JMultilineLabel(""));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weighty = 0.0;
        gc.weightx = 1.0;
        gc.insets = new Insets(5,5,5,5);
        add(pnl, gc);
        model = new MyRelationMemberConflictResolverModel();

        gc.gridy = 1;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0,0,0,0);
        add(new JScrollPane(tblResolver = new MyRelationMemberConflictResolverTable(model)), gc);
        pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.add(buildRoleEditingPanel());
        pnl.add(buildTagRelationsPanel());
        gc.gridy = 2;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        add(pnl,gc);
    }

    protected JPanel buildRoleEditingPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));
        pnl.add(new JLabel(tr("Role:")));
        pnl.add(tfRole = new AutoCompletingTextField(10));
        tfRole.setToolTipText(tr("Enter a role for all relation memberships"));
        pnl.add(new JButton(new ApplyRoleAction()));
        tfRole.addActionListener(new ApplyRoleAction());
        tfRole.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        tfRole.selectAll();
                    }
                }
        );
        return pnl;
    }

    protected JPanel buildTagRelationsPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT));
        cbTagRelations = new JCheckBox(tr("Tag modified relations with "));
        cbTagRelations.addChangeListener(new ToggleTagRelationsAction());
        cbTagRelations.setToolTipText(
                tr("<html>Select to enable entering a tag which will be applied<br>"
                        + "to all modified relations.</html>"));
        pnl.add(cbTagRelations);
        pnl.add(new JLabel(trc("tag", "Key:")));
        pnl.add(tfKey = new AutoCompletingTextField(10));
        tfKey.setToolTipText(tr("<html>Enter a tag key, i.e. <strong><tt>fixme</tt></strong></html>"));
        pnl.add(new JLabel(tr("Value:")));
        pnl.add(tfValue = new AutoCompletingTextField(10));
        tfValue.setToolTipText(tr("<html>Enter a tag value, i.e. <strong><tt>check members</tt></strong></html>"));
        cbTagRelations.setSelected(false);
        tfKey.setEnabled(false);
        tfValue.setEnabled(false);
        return pnl;
    }

    public MyRelationMemberConflictResolver() {
        build();
    }

    public void initForWayCombining() {
        lblHeader.setText(tr("<html>The combined ways are members in one ore more relations. "
                + "Please decide whether you want to <strong>keep</strong> these memberships "
                + "for the combined way or whether you want to <strong>remove</strong> them.<br>"
                + "The default is to <strong>keep</strong> the first way and <strong>remove</strong> "
                + "the other ways that are members of the same relation: the combined way will "
                + "take the place of the original way in the relation."
                + "</html>"));
        invalidate();
    }

    public void initForNodeMerging() {
        lblHeader.setText(tr("<html>The merged nodes are members in one ore more relations. "
                + "Please decide whether you want to <strong>keep</strong> these memberships "
                + "for the target node or whether you want to <strong>remove</strong> them.<br>"
                + "The default is to <strong>keep</strong> the first node and <strong>remove</strong> "
                + "the other nodes that are members of the same relation: the target node will "
                + "take the place of the original node in the relation."
                + "</html>"));
        invalidate();
    }

    class ApplyRoleAction extends AbstractAction {
        public ApplyRoleAction() {
            putValue(NAME, tr("Apply"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(SHORT_DESCRIPTION, tr("Apply this role to all members"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.applyRole(tfRole.getText());
        }
    }

    class ToggleTagRelationsAction implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            ButtonModel buttonModel = ((AbstractButton) e.getSource()).getModel();
            tfKey.setEnabled(buttonModel.isSelected());
            tfValue.setEnabled(buttonModel.isSelected());
            tfKey.setBackground(buttonModel.isSelected() ? UIManager.getColor("TextField.background") : UIManager
                    .getColor("Panel.background"));
            tfValue.setBackground(buttonModel.isSelected() ? UIManager.getColor("TextField.background") : UIManager
                    .getColor("Panel.background"));
        }
    }

    public MyRelationMemberConflictResolverModel getModel() {
        return model;
    }

    public Command buildTagApplyCommands(Collection<? extends OsmPrimitive> primitives) {
        if (!cbTagRelations.isSelected())
            return null;
        if (tfKey.getText().trim().equals(""))
            return null;
        if (tfValue.getText().trim().equals(""))
            return null;
        if (primitives == null || primitives.isEmpty())
            return null;
        return new ChangePropertyCommand(primitives, tfKey.getText(), tfValue.getText());
    }

    public void prepareForEditing() {
        AutoCompletionList acList = new AutoCompletionList();
        Main.main.getEditLayer().data.getAutoCompletionManager().populateWithMemberRoles(acList);
        tfRole.setAutoCompletionList(acList);
        AutoCompletingTextField editor = (AutoCompletingTextField) tblResolver.getColumnModel().getColumn(2).getCellEditor();
        if (editor != null) {
            editor.setAutoCompletionList(acList);
        }
        AutoCompletionList acList2 = new AutoCompletionList();
        Main.main.getEditLayer().data.getAutoCompletionManager().populateWithKeys(acList2);
        tfKey.setAutoCompletionList(acList2);
    }
}