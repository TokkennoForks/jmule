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
package org.jmule.core.jkad.indexer;

import org.jmule.core.configmanager.ConfigurationManager;
import org.jmule.core.configmanager.ConfigurationManagerException;
import org.jmule.core.configmanager.ConfigurationManagerSingleton;
import org.jmule.core.edonkey.FileHash;
import org.jmule.core.edonkey.packet.tag.IntTag;
import org.jmule.core.edonkey.packet.tag.ShortTag;
import org.jmule.core.edonkey.packet.tag.TagList;
import org.jmule.core.jkad.Int128;
import org.jmule.core.jkad.InternalJKadManager;
import org.jmule.core.jkad.JKadConstants;
import org.jmule.core.jkad.JKadManagerSingleton;
import org.jmule.core.jkad.logger.Logger;
import org.jmule.core.jkad.utils.timer.Task;
import org.jmule.core.jkad.utils.timer.Timer;
import org.jmule.core.sharingmanager.SharedFile;
import org.jmule.core.sharingmanager.SharingManagerSingleton;
import org.jmule.core.utils.Convert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jmule.core.jkad.JKadConstants.*;

/**
 * Created on Jan 5, 2009
 * @author binary256
 * @version $Revision: 1.8 $
 * Last changed by $Author: binary255 $ on $Date: 2009/09/17 18:06:41 $
 */
public class Indexer {
	
	private static Indexer singleton = null;
	
	public static Indexer getSingleton() {
		if (singleton == null)
			singleton = new Indexer();
		return singleton;
	}
	
	private Map<Int128, Index> notes = new ConcurrentHashMap<Int128,Index>();
	private Map<Int128, Index> keywords = new ConcurrentHashMap<Int128, Index>();
	private Map<Int128, Index> sources = new ConcurrentHashMap<Int128, Index>();
	
	private Task save_data_task;
	private Task cleaner_task;
	private boolean is_started = false;

	private Indexer() {
		
	}
	
	public void start() {
		is_started = true;
		try {
			notes.putAll(SrcIndexDat.loadFile(NOTE_INDEX_DAT));
			keywords.putAll(SrcIndexDat.loadFile(KEY_INDEX_DAT));
			sources.putAll(SrcIndexDat.loadFile(SRC_INDEX_DAT));
		} catch (Throwable e1) {
			Logger.getSingleton().logException(e1);
			e1.printStackTrace();
		}
		Logger.getSingleton().logMessage("Loaded notes : " + notes.size());
		Logger.getSingleton().logMessage("Loaded keywords : " + keywords.size());
		Logger.getSingleton().logMessage("Loaded sources : " + sources.size());
		
		save_data_task = new Task() {
			public void run() {
				synchronized (notes) {
					try {
						SrcIndexDat.writeFile(NOTE_INDEX_DAT, notes);
					} catch (Throwable e) {
						Logger.getSingleton().logException(e);
						e.printStackTrace();
					}
				}
				
				synchronized (keywords) {
					try {
						SrcIndexDat.writeFile(KEY_INDEX_DAT, keywords);
					} catch (Throwable e) {
						Logger.getSingleton().logException(e);
						e.printStackTrace();
					}
				}
				synchronized (sources) {
					try {
							SrcIndexDat.writeFile(SRC_INDEX_DAT, sources);
					} catch (Throwable e) {
						Logger.getSingleton().logException(e);
						e.printStackTrace();
					}
				}
			}
		};
		
		Timer.getSingleton().addTask(INDEXER_SAVE_DATA_INTERVAL, save_data_task, true);
		
		cleaner_task = new Task() {
			public void run() {
				synchronized (notes) {
					for(Int128 id : notes.keySet()) {
						Index index = notes.get(id);
						index.removeContactsWithTimeOut(JKadConstants.TIME_24_HOURS);
						if (index.isEmpty()) notes.remove(id);
					}
				}
				
				synchronized (keywords) {
					for(Int128 id : keywords.keySet()) {
						Index index = keywords.get(id);
						index.removeContactsWithTimeOut(JKadConstants.TIME_24_HOURS);
						if (index.isEmpty()) keywords.remove(id);
					}
				}
				
				synchronized (sources) {
					for(Int128 id : sources.keySet()) {
						Index index = sources.get(id);
						index.removeContactsWithTimeOut(JKadConstants.TIME_24_HOURS);
						if (index.isEmpty()) sources.remove(id);
					}
				}
				
				
				
			}
		};
		Timer.getSingleton().addTask(INDEXER_CLEAN_DATA_INTERVAL, cleaner_task, true);
		
	}

	public void stop() {
		is_started = false;
		Timer.getSingleton().removeTask(save_data_task);
		Timer.getSingleton().removeTask(cleaner_task);
		
		synchronized (notes) {
			notes.clear();
		}
		synchronized (keywords) {
			keywords.clear();
		}
		synchronized (sources) {
			sources.clear();
		}
		
	}
	
	public boolean isStarted() {
		return is_started;
	}
	
	public int getKeywordLoad() {
		return (keywords.size() / INDEX_MAX_KEYWORDS) * 100;
	}
	
	public int getNoteLoad() {
		return (notes.size() / INDEX_MAX_NOTES) * 100;
	}
	
	public int getFileSourcesLoad() {
		return (sources.size() / INDEX_MAX_SOURCES) * 100;
	}
	
	public void addFileSource(Int128 fileID, Source source) {
		Index index = sources.get(fileID);
		if (index == null) {
			index = new SourceIndex(fileID);
			sources.put(fileID,index);
		}
		index.addSource(source);
	}
	
	public void addKeywordSource(Int128 keywordID, Source source) {
		Index index = keywords.get(keywordID);
		if (index == null) {
			index = new KeywordIndex(keywordID);
			keywords.put(keywordID, index);
		}
		index.addSource(source);
		
	}
	
	public void addNoteSource(Int128 noteID, Source source) {
		Index index = notes.get(noteID);		
		if (index == null) {
			index = new NoteIndex(noteID);
			notes.put(noteID, index);
		}
		index.addSource(source);
	}

	public List<Source> getFileSources(Int128 targetID) {
		Index indexer =  sources.get(targetID);
		
		FileHash fileHash = new FileHash(targetID.toByteArray());
		if (SharingManagerSingleton.getInstance().hasFile(fileHash)) {
			if (indexer == null) indexer = new Index(targetID);
			SharedFile file = SharingManagerSingleton.getInstance().getSharedFile(fileHash);
			InternalJKadManager _jkad_manager = (InternalJKadManager) JKadManagerSingleton.getInstance();
			ConfigurationManager config_manager = ConfigurationManagerSingleton.getInstance();
			TagList tagList = new TagList();
			tagList.addTag(new IntTag(JKadConstants.TAG_SOURCEIP, Convert.byteToInt(_jkad_manager.getIPAddress().getAddress())));
			try {
				tagList.addTag(new ShortTag(JKadConstants.TAG_SOURCEPORT, Convert.intToShort(config_manager.getTCP())));
			} catch (ConfigurationManagerException e) {
				e.printStackTrace();
				_jkad_manager.disconnect();
			}
			try {
				tagList.addTag(new ShortTag(JKadConstants.TAG_SOURCEUPORT, Convert.intToShort(config_manager.getUDP())));
			} catch (ConfigurationManagerException e) {
				e.printStackTrace();
				_jkad_manager.disconnect();
			}
			tagList.addTag(new IntTag(JKadConstants.TAG_FILESIZE, Convert.longToInt(file.length())));
			Source my_source = new Source(_jkad_manager.getClientID(), tagList);
			
			indexer.addSource(my_source);
		}
		
		if (indexer == null) return null;
		return indexer.getSourceList();
	}
	
	public List<Source> getFileSources(Int128 targetID,short start_position, long fileSize) {
		return this.getFileSources(targetID);
	}

	public List<Source> getKeywordSources(Int128 targetID) {
		Index indexer = keywords.get(targetID);
		if (indexer == null) return null;
		return indexer.getSourceList();
	}

	public List<Source> getNoteSources(Int128 noteID) {
		Index indexer = notes.get(noteID);
		if (indexer == null) return null;
		return indexer.getSourceList();
	}
	public List<Source> getNoteSources(Int128 noteID, long fileSize) {
		return getNoteSources(noteID);
	}

	
}
