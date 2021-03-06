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
package org.jmule.ui.swing.wizards;

import org.jmule.ui.swing.UISwingImageRepository;

import javax.swing.*;
import java.awt.*;

/**
 * 
 * @author javajox
 * @version $$Revision: 1.2 $$
 * Last changed by $$Author: javajox $$ on $$Date: 2008/10/16 16:10:38 $$
 */
public class LeftLogo extends JPanel {
	
	public LeftLogo() {
		this.setLayout(new BorderLayout());
		this.setBackground(new java.awt.Color(255, 140, 5));
		left_logo_image = new javax.swing.JLabel();
		left_logo_image.setIcon(UISwingImageRepository.getIcon("jmule_wizard_left_logo.png"));
		this.add(left_logo_image,BorderLayout.SOUTH);
	}

    private javax.swing.JLabel left_logo_image;

}
