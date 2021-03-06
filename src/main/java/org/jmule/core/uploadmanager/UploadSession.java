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
package org.jmule.core.uploadmanager;

import org.jmule.core.downloadmanager.FileChunk;
import org.jmule.core.edonkey.ED2KFileLink;
import org.jmule.core.edonkey.FileHash;
import org.jmule.core.networkmanager.InternalNetworkManager;
import org.jmule.core.networkmanager.NetworkManagerSingleton;
import org.jmule.core.peermanager.Peer;
import org.jmule.core.peermanager.PeerManagerException;
import org.jmule.core.peermanager.PeerManagerSingleton;
import org.jmule.core.session.JMTransferSession;
import org.jmule.core.sharingmanager.*;
import org.jmule.core.utils.Misc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 
 * @author binary256
 * @version $$Revision: 1.23 $$
 * Last changed by $$Author: binary255 $$ on $$Date: 2010/01/12 14:39:12 $$
 */
public class UploadSession implements JMTransferSession {
	private SharedFile sharedFile;	
	
	private Collection<Peer> session_peers = new ConcurrentLinkedQueue<Peer>();
	private Map<Peer, Set<FileChunkRequest>> requested_chunks = new ConcurrentHashMap<Peer, Set<FileChunkRequest>>();
	//private Map<Peer, Set<FileChunkRequest>> sended_chunks;
	
	private InternalNetworkManager network_manager = (InternalNetworkManager) NetworkManagerSingleton.getInstance();
	private long totalUploaded = 0;
	
	UploadSession(SharedFile sFile) {
		sharedFile = sFile;
	}
	
	void stopSession() {
		for(Peer peer : session_peers) {
			if (peer.isConnected())
				network_manager.sendSlotRelease(peer.getIP(), peer.getPort());
		}
	}
	
	public boolean sharingCompleteFile() {
		boolean result = sharedFile instanceof CompletedFile;
		if (!result) {
			PartialFile pFile = (PartialFile) sharedFile;
			if (pFile.getPercentCompleted() == 100d)
				result = true;
		}

		return result;
	}
		
	public int getPeerCount() {
		return session_peers.size();
	}
	
	public List<Peer> getPeers() {
		return Arrays.asList(session_peers.toArray(new Peer[0]));
	}
	
	public boolean hasPeer(Peer peer) {
		return session_peers.contains(peer);
	}
	
	public String getSharingName() {
		return sharedFile.getSharingName();
	}
	
	public float getSpeed() {
		float upload_speed = 0.0f;
		for(Peer peer : session_peers)
			upload_speed += peer.getUploadSpeed();
		return upload_speed;
	}
		
	public long getETA() {
		float upload_speed = getSpeed();
		if (upload_speed != 0)
			return (long) (getFileSize() / upload_speed);
		else
			return Misc.INFINITY_AS_INT;
	}
	
	void removePeer(Peer sender) {
		session_peers.remove(sender);
		if (requested_chunks.containsKey(sender))
			requested_chunks.remove(sender);
	}
	
	void receivedFileChunkRequestFromPeer(Peer sender,
			FileHash fileHash, List<FileChunkRequest> chunkList) {
		
		if (sharedFile instanceof PartialFile) {
			PartialFile partial_file = (PartialFile) sharedFile;
			GapList gapList = partial_file.getGapList();
//			System.out.println("Request from partial file : \n" + partial_file);
			for(FileChunkRequest request : chunkList) {
//				System.out.println("Gap request : " + request+"\nIntersected gaps :");
//				for(Gap gap : gapList.getIntersectedGaps(request.getChunkBegin(), request.getChunkEnd()))
//					System.out.println(gap);
				if (gapList.getIntersectedGaps(request.getChunkBegin(), request.getChunkEnd()).size()!=0) {
					try {
						PeerManagerSingleton.getInstance().disconnect(sender);
					} catch (PeerManagerException e) {
						e.printStackTrace();
					}
					return;
				}
			}
		}
		
		if (!requested_chunks.containsKey(sender)) {
			requested_chunks.put(sender, new ConcurrentSkipListSet<FileChunkRequest>());
		}
		
		Set<FileChunkRequest> chunks = requested_chunks.get(sender);
		
		List<FileChunkRequest> duplicate_chunks = new LinkedList<FileChunkRequest>();
		
		for(FileChunkRequest chunk_request : chunkList) {
			if (chunks.contains(chunk_request)) {
				duplicate_chunks.add(chunk_request);
			}
			else {
				chunks.add(chunk_request);
			}
		}
		chunkList.removeAll(duplicate_chunks);
		
		for(FileChunkRequest chunk_request : chunkList) {
			FileChunk file_chunk;
			try {
				file_chunk = sharedFile.getData(chunk_request);
			} catch (SharedFileException e) {
				e.printStackTrace();
				continue;
			}
			network_manager.sendFileChunk(sender.getIP(), sender.getPort(), getFileHash(), file_chunk);
		}
	}
	
	void receivedSlotRequestFromPeer(Peer sender,FileHash fileHash) {
		session_peers.add(sender);
	}
	
	void peerConnected(Peer peer) {
/*		List<Peer> peer_list = uploadQueue.getSlotPeers(PeerQueueStatus.SLOTTAKEN);
		for (Peer p : peer_list) {
			if (p.equals(peer)) {
				network_manager.sendSlotGiven(peer.getIP(), peer.getPort(), getFileHash());
				break;
			}
		}*/
			
	}
	
	void peerConnectingFailed(Peer peer, Throwable cause) {
		
	}
	
		
	public String toString() {
		String str = "[\n ";
		str += this.sharedFile + "\n";
		for(Peer peer : session_peers)
			str += " " +peer + "\n";
		str += "\n]";
		return str;
	}

	public int hashCode() {
		return sharedFile.hashCode();
	}

	public boolean equals(Object object) {
		if (object == null)
			return false;
		if (!(object instanceof UploadSession))
			return false;
		return this.hashCode() == object.hashCode();
	}

	public FileHash getFileHash() {
		return this.sharedFile.getFileHash();
	}
	
	void addTransferredBytes(long addBytes) {
		this.totalUploaded += addBytes;
	}

	public long getTransferredBytes() {
		return totalUploaded;
	}

	public long getFileSize() {
		return sharedFile.length();
	}

	public ED2KFileLink getED2KLink() {
		return sharedFile.getED2KLink();
	}

	public SharedFile getSharedFile() {
		return sharedFile;
	}

	void setSharedFile(SharedFile newFile) {
		sharedFile = newFile;
	}
}
