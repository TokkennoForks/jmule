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
package org.jmule.core.jkad.utils;

import org.jmule.core.jkad.Int128;
import org.jmule.core.utils.Misc;

import java.nio.ByteBuffer;

/**
 * Created on Jan 8, 2009
 * @author binary256
 * @version $Revision: 1.1 $
 * Last changed by $Author: binary255 $ on $Date: 2009/07/06 14:13:25 $
 */
public class MD4 {
	
	public static Int128 MD4Digest(byte[] inputData) {
		org.jmule.core.utils.MD4 md4 = new org.jmule.core.utils.MD4();
		ByteBuffer input = Misc.getByteBuffer(inputData.length);
		input.put(inputData);
		input.position(0);
		md4.update(input);
		ByteBuffer output = Misc.getByteBuffer(16);
		md4.finalDigest(output);
		return new Int128(output.array());
	}

}
