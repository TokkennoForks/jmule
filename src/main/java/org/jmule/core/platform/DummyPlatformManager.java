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
package org.jmule.core.platform;

import org.jmule.core.JMuleManager;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;

/**
 * Created on Aug 30, 2009
 * @author javajox
 * @version $Revision: 1.2 $
 * Last changed by $Author: javajox $ on $Date: 2009/10/25 08:36:11 $
 */
public class DummyPlatformManager extends AbstractPlatformManager {

	private static final String UC = "Unsupported capability";

	DummyPlatformManager() {
		
	}

	public String getOSName() throws PlatformManagerException {
		throw new PlatformManagerException( UC );
	}
	
	public String getOSVersion() throws PlatformManagerException {
		throw new PlatformManagerException( UC );
	}
	
	public List<CPUCapabilities> getCPUCapabilities() throws PlatformManagerException {
		throw new PlatformManagerException( UC );
	}
	
	public void copyFile(File source, File destination) throws PlatformManagerException {
	    throw new PlatformManagerException( UC );	
	}
	
	public void moveFile(File source, File destination) throws PlatformManagerException {
	    throw new PlatformManagerException( UC );	
	}
	
	public PingResult ping(InetAddress source, 
                           NetworkInterface networkInterface,
                           int count) throws PlatformManagerException {
		throw new PlatformManagerException( UC );
	}
	
	public void addToIPFilter(Object ip) throws PlatformManagerException {
         throw new PlatformManagerException( UC );
	}

	public boolean isNativeAvailable(JMuleManager manager, String methodName)
			throws PlatformManagerException {
		
		return false;
	}

	public void removeFromIPFilter(Object ip) throws PlatformManagerException {
		throw new PlatformManagerException( UC );
	}

}
