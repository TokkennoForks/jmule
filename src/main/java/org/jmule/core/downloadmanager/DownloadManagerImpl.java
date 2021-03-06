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
package org.jmule.core.downloadmanager;

import org.jmule.core.JMThread;
import org.jmule.core.JMuleAbstractManager;
import org.jmule.core.JMuleManagerException;
import org.jmule.core.configmanager.ConfigurationManager;
import org.jmule.core.edonkey.ED2KFileLink;
import org.jmule.core.edonkey.FileHash;
import org.jmule.core.edonkey.PartHashSet;
import org.jmule.core.networkmanager.InternalNetworkManager;
import org.jmule.core.networkmanager.NetworkManagerSingleton;
import org.jmule.core.peermanager.Peer;
import org.jmule.core.searchmanager.SearchResultItem;
import org.jmule.core.servermanager.Server;
import org.jmule.core.sharingmanager.JMuleBitSet;
import org.jmule.core.sharingmanager.PartialFile;
import org.jmule.core.statistics.JMuleCoreStats;
import org.jmule.core.statistics.JMuleCoreStatsProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created on 2008-Jul-08
 * @author javajox
 * @author binary256
 * @version $$Revision: 1.26 $$
 * Last changed by $$Author: javajox $$ on $$Date: 2010/01/12 13:33:36 $$
 */
public class DownloadManagerImpl extends JMuleAbstractManager implements InternalDownloadManager {

	private Map<FileHash, DownloadSession> session_list = new ConcurrentHashMap<FileHash, DownloadSession>();

	private List<DownloadManagerListener> download_manager_listeners = new LinkedList<DownloadManagerListener>();
	private List<NeedMorePeersListener> need_more_peers_listeners = new LinkedList<NeedMorePeersListener>();
	
	// this data structure is the helper for addAndStartSilentDownloads and addAndStartSilentDownloads methods 
    private List<ED2KFileLink> ed2k_links_add_helper = new CopyOnWriteArrayList<ED2KFileLink>();
	
	private InternalNetworkManager _network_manager;
	
	private Timer need_more_peers_timer;
	
	private static final long   NEED_MORE_PEERS_INTERVAL           =    60*1000;
	private static final float  MEDIUM_SPEED_CONSIDERED_AS_SMALL   =    10*1000;
	
	public DownloadManagerImpl() {
	}
	
	public void addDownload(SearchResultItem searchResult) throws DownloadManagerException {
		if (hasDownload(searchResult.getFileHash()))
			throw new DownloadManagerException("Download "
					+ searchResult.getFileHash() + " already exists");
		DownloadSession download_session = new DownloadSession(searchResult);
		session_list.put(searchResult.getFileHash(), download_session);
		notifyDownloadAdded(searchResult.getFileHash());
	}

	public void addDownload(ED2KFileLink fileLink) throws DownloadManagerException {
		if (hasDownload(fileLink.getFileHash()))
			throw new DownloadManagerException("Download "
					+ fileLink.getFileHash() + " already exists");
		DownloadSession download_session = new DownloadSession(fileLink);
		session_list.put(fileLink.getFileHash(), download_session);
		notifyDownloadAdded(fileLink.getFileHash());
	}
	
	public void addDownload(String ed2kLinkAsString) throws DownloadManagerException {
		try {
		  ED2KFileLink ed2k_file_link = new ED2KFileLink( ed2kLinkAsString ); 
		  this.addDownload( ed2k_file_link );
		}catch( Throwable cause ) {
			throw new DownloadManagerException( cause );
		}
	}

	public void addAndStartSilentDownload(String ed2kLinkAsString) {
		try {
			this.addAndStartSilentDownload(new ED2KFileLink(ed2kLinkAsString));
		}catch( Throwable cause ) {
			cause.printStackTrace();
		}
	}
	
	public void addAndStartSilentDownload(ED2KFileLink ed2kLink) {
		try {
		  this.addDownload( ed2kLink );
		  this.startDownload( ed2kLink.getFileHash() );
		}catch( Throwable cause ) {
			cause.printStackTrace();
		}
	}
	
	public DownloadManager addAndStartSilentDownloads(String edk2LinkAsString) {
		try {
			ed2k_links_add_helper.add(new ED2KFileLink( edk2LinkAsString ) );
		}catch(Throwable cause) {
			cause.printStackTrace();
		}
		return this;
	}
	
	public void finishAddAndStartSilentDownloads() {
		for(ED2KFileLink file_link : ed2k_links_add_helper) 
	      try {
	    	  this.addAndStartSilentDownload( file_link );
	    	  ed2k_links_add_helper.remove(file_link);
	      }catch(Throwable cause) {
	    	  cause.printStackTrace();
	      }
	}
	
	public void addDownload(PartialFile partialFile) throws DownloadManagerException {
		if (hasDownload(partialFile.getFileHash()))
			throw new DownloadManagerException("Download "
					+ partialFile.getFileHash() + " already exists");
		DownloadSession download_session = new DownloadSession(partialFile);
		session_list.put(partialFile.getFileHash(), download_session);
		notifyDownloadAdded(partialFile.getFileHash());
	}

	public void cancelDownload(FileHash fileHash) throws DownloadManagerException {
		if (!hasDownload(fileHash))
			throw new DownloadManagerException("Download " + fileHash
					+ " not found ");
		DownloadSession download_session = getDownload(fileHash);
		
		if (download_session.getPercentCompleted() != 100d)
			download_session.cancelDownload(); 
		
		session_list.remove(fileHash);
		notifyDownloadRemoved(fileHash);
	}

	public void cancelSilentDownloads() {
		try {
			Collection<DownloadSession> ds_collection = session_list.values();
			for(DownloadSession ds : ds_collection)
				this.cancelDownload( ds.getFileHash() );
		}catch( Throwable cause ) {
			cause.printStackTrace();
		}
	}
	
	public void startDownload(FileHash fileHash) throws DownloadManagerException {
		if (!hasDownload(fileHash))
			throw new DownloadManagerException("Download " + fileHash
					+ " not found ");
		DownloadSession download_session = session_list.get(fileHash);
		if (download_session.isStarted())
			throw new DownloadManagerException("Download " + fileHash+" is already started");
		download_session.startDownload();
		notifyDownloadStarted(fileHash);
	}

	public void stopDownload(FileHash fileHash)  throws DownloadManagerException {
		if (!hasDownload(fileHash))
			throw new DownloadManagerException("Download " + fileHash
					+ " not found ");
		DownloadSession download_session = session_list.get(fileHash);
		if (!download_session.isStarted())
			throw new DownloadManagerException("Download " + fileHash + " is already stopped");
		download_session.stopDownload();
		notifyDownloadStopped(fileHash);
	}

	public int getDownloadCount() {
		return session_list.size();
	}

	public boolean hasDownload(FileHash fileHash) {
		return session_list.containsKey(fileHash);
	}

	public List<DownloadSession> getDownloads() {
		List<DownloadSession> result = new ArrayList<DownloadSession>();
		result.addAll(session_list.values());
		return result;
	}
	
	public void startDownload() {
		for(DownloadSession session : session_list.values()) {
			if (!session.isStarted())
				session.startDownload();
		}
	}
	
	public void stopDownload() {
		for(DownloadSession session : session_list.values()) {
			if (session.isStarted())
				session.stopDownload(true);
		}
	}

	public void initialize() {
		try {
			super.initialize();
		} catch (JMuleManagerException e) {
			e.printStackTrace();
			return;
		}

		Set<String> types = new HashSet<String>();
		types.add(JMuleCoreStats.ST_NET_SESSION_DOWNLOAD_BYTES);
		types.add(JMuleCoreStats.ST_NET_SESSION_DOWNLOAD_COUNT);
		types.add(JMuleCoreStats.ST_NET_PEERS_DOWNLOAD_COUNT);
		JMuleCoreStats.registerProvider(types, new JMuleCoreStatsProvider() {
			public void updateStats(Set<String> types,
					Map<String, Object> values) {
				if (types
						.contains(JMuleCoreStats.ST_NET_SESSION_DOWNLOAD_BYTES)) {
					long total_downloaded_bytes = 0;
					for (DownloadSession session : session_list.values()) {
						total_downloaded_bytes += session.getTransferredBytes();
					}
					values.put(JMuleCoreStats.ST_NET_SESSION_DOWNLOAD_BYTES,
							total_downloaded_bytes);
				}
				if (types
						.contains(JMuleCoreStats.ST_NET_SESSION_DOWNLOAD_COUNT)) {
					values.put(JMuleCoreStats.ST_NET_SESSION_DOWNLOAD_COUNT,
							session_list.size());
				}
				if (types.contains(JMuleCoreStats.ST_NET_PEERS_DOWNLOAD_COUNT)) {
					int download_peers_count = 0;
					for (DownloadSession session : session_list.values()) {
						download_peers_count += session.getPeerCount();
					}
					values.put(JMuleCoreStats.ST_NET_PEERS_DOWNLOAD_COUNT,
							download_peers_count);
				}
			}
		});
	}

	public void shutdown() {

		try {
			super.shutdown();
		} catch (JMuleManagerException e) {
			e.printStackTrace();
			return;
		}

		need_more_peers_timer.cancel();
		
		for (DownloadSession download_session : session_list.values())
			if (download_session.isStarted())
				download_session.stopDownload(false);

	}

	public void start() {
		try {
			super.start();	
		} catch (JMuleManagerException e) {
			e.printStackTrace();
			return;
		}
		_network_manager = (InternalNetworkManager) NetworkManagerSingleton.getInstance();
		need_more_peers_timer = new Timer( "Need more peers timer", true );
		need_more_peers_timer.scheduleAtFixedRate( new TimerTask() {
			@Override
			public void run() {
				Set<FileHash> file_hashes = session_list.keySet();
				List<FileHash> file_hashes_needed_help = new ArrayList<FileHash>();
				for(FileHash file_hash : file_hashes) {
					DownloadSession download_session = session_list.get( file_hash );
					if( download_session.isStarted() && 
					    ( download_session.getSpeed() <= MEDIUM_SPEED_CONSIDERED_AS_SMALL ) ) 
						   file_hashes_needed_help.add( file_hash );
				}
				notifyNeedMorePeersForFiles( file_hashes_needed_help );
			}
		}, (long)1, NEED_MORE_PEERS_INTERVAL);
		
	}

	public void addDownloadPeers(FileHash fileHash, List<Peer> peerList) {
		DownloadSession downloadSession = session_list.get(fileHash);
		if (downloadSession != null)
			downloadSession.addDownloadPeers(peerList);
	}

	public DownloadSession getDownload(FileHash fileHash) throws DownloadManagerException {
		if (!hasDownload(fileHash))
			throw new DownloadManagerException("Download session " + fileHash + " not found");
		return session_list.get(fileHash);
	}

	public void addDownloadManagerListener(DownloadManagerListener listener) {
		download_manager_listeners.add(listener);
	}

	public void removeDownloadMangerListener(DownloadManagerListener listener) {
		download_manager_listeners.add(listener);
	}

	protected boolean iAmStoppable() {
		return false;
	}
	
	public void peerConnected(Peer peer) {
		for(DownloadSession session : session_list.values())
			if (session.hasPeer(peer)) {
				try {
					session.peerConnected(peer);
				}catch(Throwable cause) { cause.printStackTrace(); }
				return ;
			}
	}

	public void peerDisconnected(Peer peer) {
		for(DownloadSession session : session_list.values())
			if (session.hasPeer(peer)) {
				try {
					session.peerDisconnected(peer);
				}catch(Throwable cause) { cause.printStackTrace(); }
				return ;
			}
	}
	
	public void peerConnectingFailed(Peer peer, Throwable cause) {
		for(DownloadSession session : session_list.values())
			if (session.hasPeer(peer)) {
				try {
					session.peerConnectingFailed(peer, cause);
				}catch(Throwable fail_cause) { fail_cause.printStackTrace(); }
				return ;
			}
	}

	public void receivedCompressedFileChunk(Peer sender,
			FileHash fileHash, FileChunk compressedFileChunk) {
		DownloadSession session;
		try {
			session = getDownload(fileHash);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return;
		}
		session.receivedCompressedFileChunk(sender,
				compressedFileChunk);
	}

	public void receivedFileNotFoundFromPeer(Peer sender,
			FileHash fileHash) {
		DownloadSession session;
		if (hasDownload(fileHash)) {
			try {
				session = getDownload(fileHash);
			} catch (DownloadManagerException e) {
				e.printStackTrace();
				return;
			}
			session.receivedFileNotFoundFromPeer(sender);
		}
	}

	public void receivedFileRequestAnswerFromPeer(Peer sender,
			FileHash fileHash, String fileName) {
		DownloadSession session;
		try {
			session = getDownload(fileHash);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return;
		}
		session.receivedFileRequestAnswerFromPeer(sender, fileName);
	}

	public void receivedFileStatusResponseFromPeer(Peer sender,
			FileHash fileHash, JMuleBitSet partStatus) {
		DownloadSession session;
		try {
			session = getDownload(fileHash);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return;
		}
		session.receivedFileStatusResponseFromPeer(sender, fileHash,
				partStatus);
	}

	public void receivedHashSetResponseFromPeer(Peer sender,
			FileHash fileHash, PartHashSet partHashSet) {
		DownloadSession session;
		try {
			session = getDownload(fileHash);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return;
		}
		session.receivedHashSetResponseFromPeer(sender, partHashSet);
	}

	public void receivedQueueRankFromPeer(Peer sender,int queueRank) {
		DownloadSession session;
		try {
			session = getDownloadSession(sender);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return ;
		}	
		session.receivedQueueRankFromPeer(sender, queueRank);
	}

	public void receivedRequestedFileChunkFromPeer(Peer sender, FileHash fileHash, FileChunk chunk) {
		DownloadSession session;
		try {
			session = getDownload(fileHash);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return;
		}
		session.receivedRequestedFileChunkFromPeer(sender, fileHash,
				chunk);
	}

	public void receivedSlotGivenFromPeer(Peer sender) {
		DownloadSession session;
		try {
			session = getDownloadSession(sender);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return;
		}
		session.receivedSlotGivenFromPeer(sender);
	}

	public void receivedSlotTakenFromPeer(Peer sender) {
		DownloadSession session;
		try {
			session = getDownloadSession(sender);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return ;
		}
		session.receivedSlotTakenFromPeer(sender);
	}

	public void receivedSourcesFromServer(FileHash fileHash, List<Peer> peerList) {
		DownloadSession session;
		try {
			session = getDownload(fileHash);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
			return;
		}
		session.addDownloadPeers(peerList);
	}

	private DownloadSession getDownloadSession(Peer peer) throws DownloadManagerException {
		for(DownloadSession session : session_list.values())
			if (session.hasPeer(peer)) 
				return session;
		throw new DownloadManagerException("Download session with " + peer + " not found");
	}

	public boolean hasPeer(Peer peer) {
		for(DownloadSession session : session_list.values())
			if (session.hasPeer(peer)) 
				return true;
		return false;
	}
	
	private void notifyDownloadStarted(FileHash fileHash) {
		for(DownloadManagerListener listener : download_manager_listeners)
			try {
				listener.downloadStarted(fileHash);
			}catch(Throwable t) {
				t.printStackTrace();
			}
	}
	
	private void notifyDownloadStopped(FileHash fileHash) {
		for(DownloadManagerListener listener : download_manager_listeners)
			try {
				listener.downloadStopped(fileHash);
			}catch(Throwable t) {
				t.printStackTrace();
			}
	}
	
	private void notifyDownloadAdded(FileHash fileHash) {
		for(DownloadManagerListener listener : download_manager_listeners)
			try {
				listener.downloadAdded(fileHash);
			}catch(Throwable t) {
				t.printStackTrace();
			}
	}
	
	private void notifyDownloadRemoved(FileHash fileHash) {
		for(DownloadManagerListener listener : download_manager_listeners)
			try {
				listener.downloadRemoved(fileHash);
			}catch(Throwable t) {
				t.printStackTrace();
			}
	}

	public void receivedSourcesRequestFromPeer(Peer peer, FileHash fileHash) {
		if (!hasDownload(fileHash))
			return;
		try {
			DownloadSession session = getDownload(fileHash);
			List<Peer> session_peers = session.getPeers();
			FilePartStatus part_status = session.getPartStatus();
			List<Peer> response_peers = new ArrayList<Peer>();
			for(Peer p : session_peers) {
				if (response_peers.size() > ConfigurationManager.MAX_PEX_RESPONSE ) break;
				if (!part_status.hasStatus(p)) continue;
				JMuleBitSet bit_set = part_status.get(p);
				if (bit_set.hasAtLeastOne(true))
					response_peers.add(p);
			}
			_network_manager.sendSourcesResponse(peer.getIP(), peer.getPort(), fileHash, response_peers);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
		}
	}

	public void receivedSourcesAnswerFromPeer(Peer peer, FileHash fileHash,
			List<Peer> peerList) {
		try {
			DownloadSession session = getDownload(fileHash);
			session.receivedSourcesAnswerFromPeer(peer, peerList);
		} catch (DownloadManagerException e) {
			e.printStackTrace();
		}
	}
	
	private JMThread notify_thread = null;
	
	public void connectedToServer(Server server) {
		notify_thread = new JMThread() {
			boolean stop_thread = false;
			public void run() {
				List<DownloadSession> downloads = getDownloads();
				for(DownloadSession session : downloads) {
					synchronized(this) {
					try {
						this.wait(10000);
					} catch (InterruptedException e) {
					} }
					if (stop_thread) return ;
					session.queueSourcesFromServer();
				}
				notify_thread = null;
			}
			
			public void JMStop() {
				stop_thread = true;
				synchronized (this) {
					this.notify();
				}
			}
		};
		notify_thread.start();
		
	}
	
	public void disconnectedFromServer(Server server) {
		if (notify_thread != null)
			notify_thread.JMStop();
	}
	
	public void addNeedMorePeersListener(NeedMorePeersListener listener) {
		
		need_more_peers_listeners.add( listener );
	}

	public void removeNeedMorePeersListener(NeedMorePeersListener listener) {
		
		need_more_peers_listeners.remove( listener );
	}
	
	private void notifyNeedMorePeersForFiles(List<FileHash> fileHashes) {
		for( NeedMorePeersListener listener : need_more_peers_listeners ) 
		   try {
			   listener.needMorePeersForFiles( fileHashes );
		   }catch( Throwable cause ) {
			   cause.printStackTrace();
		   }
	}

}
