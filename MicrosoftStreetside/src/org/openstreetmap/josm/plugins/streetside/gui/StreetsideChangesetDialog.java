// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.streetside.StreetsideLayer;
import org.openstreetmap.josm.plugins.streetside.StreetsideLocationChangeset;
import org.openstreetmap.josm.plugins.streetside.history.StreetsideRecord;
import org.openstreetmap.josm.plugins.streetside.history.commands.StreetsideCommand;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideChangesetListener;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

import org.openstreetmap.josm.plugins.streetside.actions.StreetsideSubmitCurrentChangesetAction;

/**
 * Toggle dialog that shows you the latest {@link StreetsideCommand} done and
 * allows the user to revert them.
 *
 * @see StreetsideRecord
 * @see StreetsideCommand
 */
public final class StreetsideChangesetDialog extends ToggleDialog implements StreetsideChangesetListener {
  private static final long serialVersionUID = -3019715241209349372L;
  private static StreetsideChangesetDialog instance;

  private final DefaultTreeModel changesetTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());

  private final Component spacer = Box.createRigidArea(new Dimension(0, 3));

  private final Container rootComponent = new JPanel(new BorderLayout());
  private final SideButton submitButton = new SideButton(new StreetsideSubmitCurrentChangesetAction(this));
  private final JProgressBar uploadPendingProgress = new JProgressBar();

  /**
   * Destroys the unique instance of the class.
   */
  public static void destroyInstance() {
    StreetsideChangesetDialog.instance = null;
  }

  private StreetsideChangesetDialog() {
    super(
      tr("Current Streetside changeset"),
      "streetside-upload",
      tr("Open Streetside changeset dialog"),
      Shortcut.registerShortcut(
        tr("Streetside changeset"), tr("Open Streetside changeset dialog"), KeyEvent.VK_9, Shortcut.NONE
      ),
      200
    );
    createLayout(rootComponent, false, Collections.singletonList(submitButton));

    final JTree changesetTree = new JTree(changesetTreeModel);
    changesetTree.expandRow(0);
    changesetTree.setShowsRootHandles(true);
    changesetTree.setRootVisible(false);
    changesetTree.setCellRenderer(new StreetsideImageTreeCellRenderer());
    changesetTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    final JPanel treesPanel = new JPanel(new GridBagLayout());
    treesPanel.add(spacer, GBC.eol());
    treesPanel.add(changesetTree, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
    treesPanel.add(new JSeparator(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
    treesPanel.add(Box.createRigidArea(new Dimension(0, 0)), GBC.std().weight(0, 1));
    rootComponent.add(new JScrollPane(treesPanel), BorderLayout.CENTER);

    uploadPendingProgress.setIndeterminate(true);
    uploadPendingProgress.setString(tr("Submitting changeset to server…"));
    uploadPendingProgress.setStringPainted(true);

    setUploadPending(false);
  }

  /**
   * Returns the unique instance of the class.
   *
   * @return The unique instance of the class.
   */
  public static synchronized StreetsideChangesetDialog getInstance() {
    if (instance == null) {
      instance = new StreetsideChangesetDialog();
    }
    return instance;
  }

  private void buildTree() {
    final StreetsideLocationChangeset changeset = StreetsideLayer.getInstance().getLocationChangeset();
    submitButton.setEnabled(!changeset.isEmpty());
    DefaultMutableTreeNode changesetRoot = new DefaultMutableTreeNode();

    changeset.parallelStream().filter(Objects::nonNull).forEach(img -> {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(img);
      changesetRoot.add(node);
    });

    spacer.setVisible(changeset.isEmpty());

    changesetTreeModel.setRoot(changesetRoot);
  }

  public void setUploadPending(final boolean isUploadPending) {
    if (isUploadPending) {
      rootComponent.add(uploadPendingProgress, BorderLayout.SOUTH);
    } else {
      rootComponent.remove(uploadPendingProgress);
    }
    submitButton.setEnabled(!isUploadPending && StreetsideLayer.hasInstance() && !StreetsideLayer.getInstance().getLocationChangeset().isEmpty());
    rootComponent.revalidate();
    rootComponent.repaint();
  }

  @Override
  public void changesetChanged() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(this::buildTree);
    } else {
      buildTree();
    }
  }
}
