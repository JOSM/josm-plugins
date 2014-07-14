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
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.plugins.tracer2.TracerPlugin;

public class ServerParamSelectDialog extends JPanel {
	
	private JComboBox<String> m_oComboBox;
    List<ServerParam> m_listServerParam;
    private boolean m_bShow = true;
    
    public boolean getShow() {
    	return m_bShow;
    }
    
	public ServerParamSelectDialog(List<ServerParam> listParam) {
		Init(m_listServerParam, null);
	}
	
	public ServerParamSelectDialog(List<ServerParam> listServerParam, ServerParam activParam) {
		Init(listServerParam, activParam);
	}
	
	private void Init(List<ServerParam> listParam, ServerParam activParam) {
        GridBagConstraints c = new GridBagConstraints();
        
        String[] astr = new String[listParam.size()];
        
        m_listServerParam = listParam;
        
        if ( activParam == null ) {
        	activParam = m_listServerParam.get(0);
        }
        int i = 0;
        int pos = 0;
        for ( ServerParam param: m_listServerParam ) {
        	astr[i] = param.getName();
        	if (param.equals(activParam)) {
        		pos = i;
        	}
        	i++;
        }
        m_oComboBox = new JComboBox<>(astr);
        m_oComboBox.setSelectedIndex(pos);
        
        setLayout(new GridBagLayout());
        
        c.insets = new Insets(4,4,4,4);
        c.gridwidth = 1;
        c.weightx = 0.8;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        add(new JLabel(tr("Parameter:")), c);
        
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.5;
        add(m_oComboBox, c);
	}
	
	public ServerParam getSelectedParam() {
     	int nSel = m_oComboBox.getSelectedIndex();
        return m_listServerParam.get(nSel);
    }
	public void checkComboBox() {
     	int nSel = m_oComboBox.getSelectedIndex();
        TracerPlugin.s_oPlugin.m_oParamList.setActivParam(m_listServerParam.get(nSel));
    }
	
}
