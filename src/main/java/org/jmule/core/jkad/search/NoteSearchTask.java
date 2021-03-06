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
package org.jmule.core.jkad.search;

import org.jmule.core.JMException;
import org.jmule.core.jkad.*;
import org.jmule.core.jkad.JKadConstants.RequestType;
import org.jmule.core.jkad.lookup.Lookup;
import org.jmule.core.jkad.lookup.LookupTask;
import org.jmule.core.jkad.packet.KadPacket;
import org.jmule.core.jkad.packet.PacketFactory;
import org.jmule.core.jkad.routingtable.KadContact;

import java.util.List;

import static org.jmule.core.jkad.JKadConstants.KADEMLIA_HELLO_RES;


/**
 * Created on Jan 16, 2009
 * @author binary256
 * @version $Revision: 1.9 $
 * Last changed by $Author: binary255 $ on $Date: 2010/01/13 18:42:15 $
 */
public class NoteSearchTask extends SearchTask {

	private LookupTask lookup_task = null;
	
	public NoteSearchTask(Int128 searchID) {
		super(searchID);
		
	}

	public void startSearch() {
		isStarted = true;
			
		lookup_task = new LookupTask(RequestType.FIND_VALUE, searchID, JKadConstants.toleranceZone) {
			public void lookupTimeout() {
				isStarted = false;
			}

			public void processToleranceContacts(ContactAddress sender,
					List<KadContact> results) {
				
				for(KadContact contact : results) {
					KadPacket packet;
					try {
						packet = PacketFactory.getHello1ReqPacket();
						_network_manager.sendKadPacket(packet, contact.getIPAddress(), contact.getUDPPort());
					} catch (JMException e) {
						e.printStackTrace();
					}
					
					
					PacketListener listener = new PacketListener(KADEMLIA_HELLO_RES, contact.getContactAddress().getAsInetSocketAddress()) {
						public void packetReceived(KadPacket packet) {
							KadPacket responsePacket = PacketFactory.getNotesReq(searchID);
							_network_manager.sendKadPacket(responsePacket, new IPAddress(packet.getAddress()), packet.getAddress().getPort());
							_jkad_manager.removePacketListener(this);
						}
					};
					_jkad_manager.addPacketListener(listener);
				}
			}
			
			public void stopLookupEvent() {
				stopSearch();
			}
			
		};
		lookup_task.setTimeOut(JKadConstants.SEARCH_NOTES_TIMEOUT);
		Lookup.getSingleton().addLookupTask(lookup_task);
		if (listener!=null)
			listener.searchStarted();
	}

	public void stopSearch() {
		if (!isStarted) return;
		isStarted = false;
		if (listener!=null)
			listener.searchFinished();
		Lookup.getSingleton().removeLookupTask(searchID);
		Search.getSingleton().removeSearchID(searchID);
	}

}
