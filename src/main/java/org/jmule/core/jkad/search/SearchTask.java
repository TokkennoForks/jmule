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

import org.jmule.core.jkad.Int128;
import org.jmule.core.jkad.indexer.Source;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created on Jan 8, 2009
 * @author binary256
 * @version $Revision: 1.3 $
 * Last changed by $Author: binary255 $ on $Date: 2009/09/17 18:10:21 $
 */
public abstract class SearchTask {

	protected Int128 searchID;

	protected List<Source> searchResults = new CopyOnWriteArrayList<Source>();
	protected boolean isStarted = false;
	
	protected SearchResultListener listener = null;
	
	public SearchTask(Int128 searchID) {
		super();
		this.searchID = searchID;
	}

	public abstract void startSearch();
	public abstract void stopSearch();
	
	public void setSearchResultListener(SearchResultListener listener) {
		this.listener = listener;
	}
	
	public Int128 getSearchID() {
		return searchID;
	}

	public List<Source> getSearchResults() {
		return searchResults;
	}

	public boolean isStarted() {
		return isStarted;
	}
	
	void addSearchResult(Source result) {
		searchResults.add(result);
	}
	
	void addSearchResult(List<Source> result) {
		List<Source> unicalList = new LinkedList<Source>();
		for(Source s : result)
			if (!searchResults.contains(s))
				unicalList.add(s);
		searchResults.addAll(unicalList);
		if (listener != null)
			listener.processNewResults(unicalList);
	}
}
