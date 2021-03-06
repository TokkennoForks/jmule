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
import org.jmule.core.edonkey.packet.tag.TagList;
import org.jmule.core.jkad.*;
import org.jmule.core.jkad.JKadConstants.RequestType;
import org.jmule.core.jkad.lookup.Lookup;
import org.jmule.core.jkad.lookup.LookupTask;
import org.jmule.core.jkad.packet.KadPacket;
import org.jmule.core.jkad.packet.PacketFactory;
import org.jmule.core.jkad.routingtable.KadContact;

import java.util.LinkedList;
import java.util.List;

import static org.jmule.core.jkad.JKadConstants.KADEMLIA2_HELLO_RES;


/**
 * Created on Jan 8, 2009
 * @author binary256
 * @version $Revision: 1.13 $
 * Last changed by $Author: binary255 $ on $Date: 2010/01/13 18:42:15 $
 */
public class KeywordSearchTask extends SearchTask {
	
	private LookupTask lookup_task = null;
	
	private String searchKeyword = "";
	
	private List<KadContact> used_contacts = new LinkedList<KadContact>();
	public KeywordSearchTask(Int128 searchID) {
		super(searchID);
	}
	
	public String getKeyword() {
		return searchKeyword;
	}
	
	public void setSearchKeyword(String searchKeyword) {
		this.searchKeyword = searchKeyword;
	}
	
	public void startSearch() {
		isStarted = true;
				
		lookup_task = new LookupTask(RequestType.FIND_VALUE, searchID, JKadConstants.toleranceZone) {
			public void lookupTimeout() {}

			public void processToleranceContacts(ContactAddress sender,
					List<KadContact> results) {
				
				for(KadContact contact : results) {
					used_contacts.add(contact);
					KadPacket hello;
					try {
						hello = PacketFactory.getHello2ReqPacket(TagList.EMPTY_TAG_LIST);
						_network_manager.sendKadPacket(hello, contact.getIPAddress(), contact.getUDPPort());
					} catch (JMException e) {						
						e.printStackTrace();
					}
					
					
					PacketListener listener = new PacketListener(KADEMLIA2_HELLO_RES, contact.getContactAddress().getAsInetSocketAddress()) {
						public void packetReceived(KadPacket packet) {
							KadPacket responsePacket = PacketFactory.getSearchReqPacket(searchID,false);
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
		lookup_task.setTimeOut(JKadConstants.SEARCH_KEYWORD_TIMEOUT);
		Lookup.getSingleton().addLookupTask(lookup_task);
		if (listener!=null)
			listener.searchStarted();
			
	}

	public void stopSearch() {
		if (!isStarted) return;
		isStarted = false;
		Lookup.getSingleton().removeLookupTask(searchID);
		
		if (listener!=null)
			listener.searchFinished();
		
		Search.getSingleton().removeSearchID(searchID);
		
	}
	
	
}
