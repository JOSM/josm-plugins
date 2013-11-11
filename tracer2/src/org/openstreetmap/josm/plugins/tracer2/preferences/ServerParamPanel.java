/**
 *  Tracer2 - plug-in for JOSM to capture contours
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.openstreetmap.josm.plugins.tracer2.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;

public class ServerParamPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6174275926314685531L;
	
	ServerParamList m_listParam;
	
    public ServerParamPanel(ServerParamList listParam) {
        super(new GridBagLayout());
        m_listParam = listParam;
    }
    
    public void refresh() {
        removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 5, 2, 5);
        
        for (final ServerParam param : m_listParam.getParamList()) {
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            
            final JCheckBox cbParam = new JCheckBox(param.getName());
            cbParam.setSelected(param.isEnabled());
            cbParam.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    param.setEnabled(cbParam.isSelected());
                }
            });
            add(cbParam, gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.EAST;
            
            final JButton bEdit = new JButton(tr("Edit"));
            bEdit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    JDialog dlg = new ServerParamDialog(param);
                    dlg.setVisible(true);
                    dlg.dispose();
                    refresh();
                }
            });
            add(bEdit, gbc);
            
            gbc.gridx = 2;
            final JButton bDel = new JButton(tr("Delete"));
            bDel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (JOptionPane.showConfirmDialog(Main.parent,
                            tr("Delete parameter \"{0}\"?", param.getName()),
                            tr("Are you sure?"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
                    {
                    	m_listParam.removeParam(param);
                        refresh();
                    }
                }
            });
            add(bDel, gbc);
            
            gbc.gridy++;
        }
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        
        JPanel p = new JPanel(new GridBagLayout());
        
        final JButton bNew = new JButton(tr("Add new"));
        bNew.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                JDialog dlg = new ServerParamDialog(null);
                dlg.setVisible(true);
                dlg.dispose();
                ServerParam param = ((ServerParamDialog)dlg).getServerParam();
                if (param != null && param.getName() != null && (!"".equals(param.getName()))) {
                    m_listParam.addParam(param);
                    param.setEnabled(true);
                }
                refresh();
            }
        });
        p.add(bNew);
        
        final JButton bPredefined = new JButton(tr("Add predefined"));
        bPredefined.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            	ServerParamList myParamList;

            	myParamList = new ServerParamList(null);
            	
            	ServerParamSelectDialog dialog = new ServerParamSelectDialog(myParamList.getParamList(), null);
            	
                if (dialog.getShow()) {
                    JOptionPane pane = new JOptionPane(dialog, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                    JDialog dlg = pane.createDialog(Main.parent, tr("Tracer2") + " - " + tr("Select predefined parameter"));
                	dlg.setVisible(true);
                	Object obj = pane.getValue();
                    dlg.dispose();
                	if(obj != null && ((Integer)obj) == JOptionPane.OK_OPTION) {
                		ServerParam param = dialog.getSelectedParam();

                		dlg = new ServerParamDialog(param);
                		dlg.setVisible(true);
                		dlg.dispose();
                        param = ((ServerParamDialog)dlg).getServerParam();
                        if (param != null && param.getName() != null && (!"".equals(param.getName()))) {
                            m_listParam.addParam(param);
                            param.setEnabled(true);
                        }
                	}
                }
                refresh();
            }
        });
        p.add(bPredefined);
        
        add(p, gbc);
        gbc.gridy++;
        
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gbc);
        revalidate();
        repaint();
    }

}
