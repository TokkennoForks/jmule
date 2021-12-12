/*
 *  JMule - Java file sharing client
 *  Copyright (C) 2007-2009 JMule Team ( jmule@jmule.org / http://jmule.org )
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
package org.jmule.core.ipfilter;

import org.jmule.core.JMuleManager;


/**
 * Created on Nov 4, 2009
 * @author javajox
 * @version $Revision: 1.2 $
 * Last changed by $Author: javajox $ on $Date: 2010/01/10 14:17:22 $
 */
public interface InternalIPFilter extends IPFilter {

	// -------- ban the peer and tell who are you (which JMule manager?)
	public void addPeer(String address, BannedReason bannedReason, 
			int howLong, TimeUnit timeUnit, JMuleManager who);
	
	public void addPeer(String address, 
			BannedReason bannedReason, JMuleManager who);
	
	public void addPeer(String address, int howLong, 
			TimeUnit timeUnit, JMuleManager who);
	
	public void addPeer(String address, JMuleManager who);
	
	// -------- ban the server and tell who are you (which JMule manager?)
	
	public void addServer(String address, BannedReason bannedReason, 
			int howLong, TimeUnit timeUnit, JMuleManager who);
	
	public void addServer(String address, 
			BannedReason bannedReason, JMuleManager who);
	
	public void addServer(String address, int howLong, 
			TimeUnit timeUnit, JMuleManager who);
	
	public void addServer(String address, JMuleManager who);
	
}
