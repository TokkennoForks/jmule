/*
 *  JMule - Java file sharing client
 *  Copyright (C) 2007-2009 JMule team ( jmule@jmule.org / http://jmule.org )
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
package org.jmule.core.networkmanager;

import org.jmule.core.utils.Convert;

import javax.management.JMException;

/**
 * Created on Aug 21, 2009
 * @author binary256
 * @version $Revision: 1.1 $
 * Last changed by $Author: binary255 $ on $Date: 2009/08/31 17:24:11 $
 */
public class UnknownPacketException extends JMException {
	
	public UnknownPacketException(byte header,byte opCode, byte[] packetContent) {
		super("Unknown packet exception " + "\nHeader : " + Convert.byteToHex(header) + "\nOpCode : " + Convert.byteToHex(opCode) +"\n" +Convert.byteToHexString(packetContent," "));
	}
	
	public UnknownPacketException(String cause) {
		super(cause);
	}

}
