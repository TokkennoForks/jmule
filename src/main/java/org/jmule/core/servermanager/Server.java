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
package org.jmule.core.servermanager;

import org.jmule.core.configmanager.ConfigurationManager;
import org.jmule.core.edonkey.ClientID;
import org.jmule.core.edonkey.E2DKConstants.*;
import org.jmule.core.edonkey.ED2KServerLink;
import org.jmule.core.edonkey.packet.tag.IntTag;
import org.jmule.core.edonkey.packet.tag.StringTag;
import org.jmule.core.edonkey.packet.tag.Tag;
import org.jmule.core.edonkey.packet.tag.TagList;
import org.jmule.core.utils.Convert;

import java.util.Set;

import static org.jmule.core.edonkey.E2DKConstants.*;

/**
 * Created on 2007-Nov-07
 * @author binary256
 * @author javajox
 * @version $$Revision: 1.4 $$
 * Last changed by $$Author: binary255 $$ on $$Date: 2009/10/23 18:10:39 $$
 */
public class Server {
	public static enum ServerStatus {
		CONNECTING, CONNECTED, DISCONNECTED
	}

	private String serverIP;
	private int serverPort;

	private ClientID clientID;
	private TagList tagList = new TagList();

	private ServerStatus status = ServerStatus.DISCONNECTED;

	private Set<ServerFeatures> serverFeatures;

	private boolean is_static = false;

	private long last_udp_response = System.currentTimeMillis();

	private int sended_challenge = 0;
	
	Server(String IPAddress, int port) {
		this.serverIP = IPAddress;
		this.serverPort = port;
	}

	Server(ED2KServerLink serverLink) {
		this(serverLink.getServerAddress(), serverLink.getServerPort());
	}

	public void setStatic(boolean status) {
		is_static = status;
	}

	public boolean isStatic() {
		return is_static;
	}

	public ServerStatus getStatus() {
		return status;
	}

	public boolean isConnected() {
		return status == ServerStatus.CONNECTED;
	}

	public String getAddress() {
		return serverIP;
	}

	public Set<ServerFeatures> getFeatures() {
		return serverFeatures;
	}

	void setFeatures(Set<ServerFeatures> features) {
		this.serverFeatures = features;
	}

	void setTagList(TagList tagList) {
		this.tagList = tagList;
	}

	public byte[] getAddressAsByte() {
		return Convert.stringIPToArray(getAddress());
	}

	public int getPort() {
		return serverPort;
	}

	public ED2KServerLink getServerLink() {
		return new ED2KServerLink(getAddress(), getPort());

	}

	void setChallenge(int challenge) {
		this.sended_challenge = challenge;
	}
	
	void setStatus(ServerStatus status) {
		this.status = status;
	}

	public ClientID getClientID() {
		return clientID;
	}

	void setClientID(ClientID clientID) {
		this.clientID = clientID;
	}

	public String toString() {
		return this.getAddress() + " : " + this.getPort();
	}

	public TagList getTagList() {
		return tagList;
	}

	void setName(String newName) {
		Tag tag = new StringTag(SL_SERVERNAME, newName);
		tagList.removeTag(SL_SERVERNAME);
		tagList.addTag(tag);
	}

	public String getName() {
		try {
			String result = (String) tagList.getTag(SL_SERVERNAME).getValue();
			result = result.trim();
			if (result.length() != 0)
				return result;
			return getAddress();
		} catch (Throwable t) {
			return getAddress();
		}
	}

	void setDesc(String serverDesc) {
		Tag tag = new StringTag(SL_DESCRIPTION, serverDesc);
		tagList.removeTag(SL_DESCRIPTION);
		tagList.addTag(tag);

	}

	public String getDesc() {
		try {
			return (String) tagList.getTag(SL_DESCRIPTION).getValue();
		} catch (Throwable e) {
			return "";
		}
	}

	void setSoftLimit(int softLimit) {
		Tag tag = new IntTag(SL_SOFTFILES, softLimit);
		tagList.removeTag(SL_SOFTFILES);
		tagList.addTag(tag);

	}

	public int getSoftLimit() {
		if (!tagList.hasTag(SL_SOFTFILES))
			return 0;
		try {
			return (Integer) tagList.getTag(SL_SOFTFILES).getValue();
		} catch (Throwable e) {
			return 0;
		}
	}

	void setHardLimit(int hardLimit) {
		Tag tag = new IntTag(SL_HARDFILES, hardLimit);
		tagList.removeTag(SL_HARDFILES);
		tagList.addTag(tag);
	}

	public int getHardLimit() {
		try {
			return (Integer) tagList.getTag(SL_HARDFILES).getValue();
		} catch (Throwable e) {
			return 0;
		}
	}
	
	void setPing(int receivedChallenge)  {
		Tag tag = new IntTag(SL_PING, Convert.longToInt(System.currentTimeMillis() - last_udp_response));
		tagList.removeTag(SL_PING);
		tagList.addTag(tag);
		last_udp_response = System.currentTimeMillis();
		sended_challenge = 0;
	}

	public long getPing() {
		try {
			return Convert.longToInt((Integer) tagList.getTag(SL_PING)
					.getValue());
		} catch (Throwable e) {
			return 0;
		}
	}

	public String getVersion() {
		try {
			long version = (Integer) tagList.getTag(SL_VERSION).getValue();
			long major = version >> 16;
			long minor = version & 0xFFFF;
			return major + "." + minor;
		} catch (Throwable e) {
			return "";
		}
	}

	public long getNumFiles() {
		try {
			return Convert.intToLong((Integer) tagList.getTag(SL_FILES)
					.getValue());
		} catch (Throwable e) {
			return 0;
		}
	}

	public long getMaxUsers() {
		try {
			return Convert.intToLong((Integer) tagList.getTag(SL_SRVMAXUSERS)
					.getValue());
		} catch (Throwable e) {
			return 0;
		}
	}

	public long getNumUsers() {
		try {
			return Convert.intToLong((Integer) tagList.getTag(SL_USERS)
					.getValue());
		} catch (Throwable e) {
			return 0;
		}
	}

	void setNumUsers(long numUsers) {
		last_udp_response = System.currentTimeMillis();
		Tag tag = new IntTag(SL_USERS, Convert.longToInt(numUsers));
		tagList.removeTag(SL_USERS);
		tagList.addTag(tag);
	}

	void setNumFiles(long numFiles) {
		last_udp_response = System.currentTimeMillis();
		Tag tag = new IntTag(SL_FILES, Convert.longToInt(numFiles));
		tagList.removeTag(SL_FILES);
		tagList.addTag(tag);

	}

	public boolean isDown() {
		if (isConnected())
			return false;
		if (System.currentTimeMillis() - last_udp_response > ConfigurationManager.SERVER_DOWN_TIMEOUT)
			return true;
		return false;
	}

	public int hashCode() {
		return (getAddress() + " : " + getPort()).hashCode();
	}

	public boolean equals(Object object) {
		if (!(object instanceof Server))
			return false;
		if (object.hashCode() != this.hashCode())
			return false;
		return true;
	}
}
