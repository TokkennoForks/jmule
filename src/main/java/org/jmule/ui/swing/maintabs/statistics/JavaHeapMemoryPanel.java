/*
 *  JMule - Java file sharing client
 *  Copyright (C) 2007-2008 JMule team ( jmule@jmule.org / http://jmule.org )
 *
 *  Any parts of this program derived from other projects, or contributed
 *  by third-party developers are copyrighted by their respective authors.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package org.jmule.ui.swing.maintabs.statistics;

import javax.swing.*;
import java.awt.*;

/**
 *
 * Created on Oct 12, 2008
 * @author javajox
 * @version $Revision: 1.1 $
 * Last changed by $Author: javajox $ on $Date: 2008/10/16 17:35:11 $
 */
public class JavaHeapMemoryPanel extends JPanel {

	private JLabel initial_heap_momory_label = new JLabel("Initial heap memory");
	public JLabel used_heap_memory_value = new JLabel();
	public JLabel maximum_heap_memory_value = new JLabel();
	public JLabel initial_heap_memory_value = new JLabel();
	private JLabel maximum_heap_memory_label = new JLabel("Maximum heap memory");
	private JLabel used_heap_label = new JLabel("Used heap memory");
	
	public JavaHeapMemoryPanel() {
		
		init();
	}
	
	private void init() {
		GridBagLayout thisLayout = new GridBagLayout();
		this.setPreferredSize(new java.awt.Dimension(314, 81));
		thisLayout.rowWeights = new double[] {0.0, 0.0, 0.0, 0.1};
		thisLayout.rowHeights = new int[] {20, 20, 20, 7};
		thisLayout.columnWeights = new double[] {0.0, 0.0, 0.0, 0.1};
		thisLayout.columnWidths = new int[] {7, 164, 19, 7};
		this.setLayout(thisLayout);
		this.setBorder(BorderFactory.createTitledBorder("Heap memory"));
		this.add(initial_heap_momory_label, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(used_heap_label, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(maximum_heap_memory_label, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(initial_heap_memory_value, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(used_heap_memory_value, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(maximum_heap_memory_value, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}
	
}
