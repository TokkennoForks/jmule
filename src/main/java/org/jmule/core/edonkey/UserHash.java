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
package org.jmule.core.edonkey;

import org.jmule.core.utils.Convert;

import java.util.Arrays;
import java.util.Random;

/**
 * 
 * @author binary256
 * @version $$Revision: 1.2 $$
 * Last changed by $$Author: binary255 $$ on $$Date: 2009/11/19 17:24:10 $$
 */
public class UserHash implements Comparable<UserHash> {
	
	private byte[] userHash = new byte[16];
	
	public UserHash(byte[] hash) {
		this.userHash = hash;
	}
	
	public static UserHash genNewUserHash() {
		byte[] hash = new byte[16];
		new Random().nextBytes( hash );
		hash[5] = 14;
		hash[14] = 111;
		return new UserHash(hash);
	}
	
	public byte[] getUserHash() {
		return this.userHash;
	}
	
	public String getAsString() {
		String value = "";
		for (int i = 0; i < userHash.length; i++)
			value = value + Convert.byteToHex(userHash[i]);
		return value;
	}
	
	public String toString() {
		String sValue = "";
		for (int i = 0; i < userHash.length; i++)
			sValue = sValue + Convert.byteToHex(userHash[i]);
		return sValue;
	}
	
	public void loadFromString(String inputString) {
		for (int i = 0; i < userHash.length; i++)
			userHash[i] = Convert.hexToByte(inputString.charAt(i * 2) + ""
					+ inputString.charAt(i * 2 + 1));
	}
	
	public void setUserHash(byte data[]) {
		userHash = data;
	}
	
	public int hashCode() {
		return getAsString().hashCode();
	}
	
	public boolean equals(Object object) {
		if (object==null) return false;
		if (!(object instanceof UserHash )) return false;
		UserHash hash = (UserHash)object;
		return Arrays.equals(userHash, hash.getUserHash());
	}

	public int compareTo(UserHash o) {
		int hash_code1 = hashCode();
		int hash_code2 = o.hashCode();
		if (hash_code1 < hash_code2)
			return -1;
		if (hash_code1 == hash_code2)
			return 0;
		return 1;
	}
	
}
