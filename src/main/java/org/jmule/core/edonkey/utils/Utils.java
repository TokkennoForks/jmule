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
package org.jmule.core.edonkey.utils;

import org.jmule.core.edonkey.E2DKConstants.*;
import org.jmule.core.edonkey.packet.tag.Tag;
import org.jmule.core.edonkey.packet.tag.TagList;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import static org.jmule.core.edonkey.E2DKConstants.*;
import static org.jmule.core.edonkey.E2DKConstants.PeerFeatures.*;

/**
 * Created on Dec 24, 2008
 * @author binary256
 * @version $Revision: 1.4 $
 * Last changed by $Author: binary255 $ on $Date: 2009/12/25 20:11:49 $
 */
public class Utils {

	public static int peerFeatures1ToInt( Map<PeerFeatures, Integer> clientFeatures) {
		int misc_optins1 = 0;
		
		misc_optins1 |= (byte)(clientFeatures.get(AICHVer) << 29);
		misc_optins1 |= (int)(clientFeatures.get(UnicodeSupport) << 28);
		misc_optins1 |= (int)(clientFeatures.get(UDPVer) << 24);
		misc_optins1 |= (int)(clientFeatures.get(DataCompressionVer) << 20);
		misc_optins1 |= (int)(clientFeatures.get(SupportSecIdent) << 16);
		misc_optins1 |= (int)(clientFeatures.get(SourceExchange1Ver) << 12);
		misc_optins1 |= (int)(clientFeatures.get(ExtendedRequestsVer) << 8);
		misc_optins1 |= (int)(clientFeatures.get(AcceptCommentVer) << 4);
		misc_optins1 |= (byte)(clientFeatures.get(PeerCache) << 3);
		misc_optins1 |= (byte)(clientFeatures.get(NoViewSharedFiles) << 2);
		misc_optins1 |= (byte)(clientFeatures.get(MultiPacket) << 1);
		misc_optins1 |= (byte)(clientFeatures.get(SupportPreview) << 0);
		
		return misc_optins1;
	}
	
	public static int peerFeatures2ToInt(Map<PeerFeatures, Integer> clientFeatures) {
		int misc_optins2 = 0;
		misc_optins2 |= (clientFeatures.get(DirectUDPCallback) << 12);
		misc_optins2 |= (clientFeatures.get(SupportsCaptcha) << 11);
		misc_optins2 |= (clientFeatures.get(SupportsSourceEx2) << 10);
		misc_optins2 |= (clientFeatures.get(RequiresCryptLayer) << 9);
		misc_optins2 |= (clientFeatures.get(RequestsCryptLayer) << 8);
		misc_optins2 |= (clientFeatures.get(SupportsCryptLayer) << 7);
		misc_optins2 |= (clientFeatures.get(Reserved) << 6);
		misc_optins2 |= (clientFeatures.get(MultiPacket) << 5);
		misc_optins2 |= (clientFeatures.get(SupportLargeFiles) << 4);
		misc_optins2 |= (clientFeatures.get(KadVersion) << 0);
		
		return misc_optins2;
	}
	
	public static Map<PeerFeatures,Integer> scanTCPPeerFeatures1(int rawData) {
		Map<PeerFeatures,Integer> result = new Hashtable<PeerFeatures,Integer>();
		
		result.put(AICHVer, (rawData >> 29) & 0x07);
		result.put(UnicodeSupport, (rawData >> 28) & 0x01);
		result.put(UDPVer, (rawData >> 24) & 0x0f);
		result.put(DataCompressionVer, (rawData >> 20) & 0x0f);
		result.put(SupportSecIdent, (rawData >> 16) & 0x0f);
		result.put(SourceExchange1Ver, (rawData >> 12) & 0x0f);
		result.put(ExtendedRequestsVer,(rawData >>  8) & 0x0f);
		result.put(AcceptCommentVer, (rawData >>  4) & 0x0f);
		result.put(PeerCache, (rawData >>  3) & 0x01);
		result.put(NoViewSharedFiles, (rawData >>  2) & 0x01);
		result.put(MultiPacket, (rawData >>  1) & 0x01);
		result.put(SupportPreview, (rawData >>  0) & 0x01);
		
		return result;
	}
	
	public static Map<PeerFeatures,Integer> scanTCPPeerFeatures2(int rawData) {
		Map<PeerFeatures,Integer> result = new Hashtable<PeerFeatures,Integer>();
		
		result.put(DirectUDPCallback, (rawData >> 12) & 0x01);
		result.put(SupportsCaptcha, (rawData >> 11) & 0x01);
		result.put(SupportsSourceEx2, (rawData >> 10) & 0x01);
		result.put(RequiresCryptLayer, (rawData >> 9) & 0x01);
		result.put(RequestsCryptLayer, (rawData >> 8) & 0x01);
		result.put(SupportsCryptLayer, (rawData >> 7) & 0x01);
		result.put(Reserved, (rawData >> 6) & 0x01);
		result.put(MultiPacket, (rawData >> 5) & 0x01);
		result.put(SupportLargeFiles, (rawData >> 4) & 0x01);
		result.put(KadVersion, (rawData >> 0) & 0x01);
		
		return result;
	}
	
	public static Map<PeerFeatures, Integer> scanTagListPeerFeatures(TagList tagList) {
		Map<PeerFeatures,Integer> result = new Hashtable<PeerFeatures,Integer>();
		Tag tag;
		
		tag = tagList.getTag(ET_COMPRESSION);
		if (tag != null)
			result.put(DataCompressionVer, (Integer)tag.getValue());
		
		tag = tagList.getTag(ET_UDPVER);
		if (tag != null)
			result.put(UDPVer, (Integer)tag.getValue());
		
		tag = tagList.getTag(ET_SOURCEEXCHANGE);
		if (tag != null)
			result.put(SourceExchange1Ver, (Integer)tag.getValue());
		
		tag = tagList.getTag(ET_COMMENTS);
		if (tag != null)
			result.put(AcceptCommentVer, (Integer)tag.getValue());
		
		tag = tagList.getTag(ET_EXTENDEDREQUEST);
		if (tag != null)
			result.put(ExtendedRequestsVer, (Integer)tag.getValue());
		
		tag = tagList.getTag(ET_FEATURES);
		if (tag != null)
			result.put(SupportPreview, (Integer)tag.getValue());
		
		return result;
	}
	
	public static Set<ServerFeatures> scanTCPServerFeatures(int serverFeatures) {
		Set<ServerFeatures> result = new HashSet<ServerFeatures>();
		
		if ((serverFeatures & SRV_TCPFLG_COMPRESSION) != 0) 
			result.add(ServerFeatures.Compression);

		if ((serverFeatures & SRV_TCPFLG_NEWTAGS) != 0) 
			result.add(ServerFeatures.NewTags);
		
		if ((serverFeatures & SRV_TCPFLG_UNICODE) != 0) 
			result.add(ServerFeatures.Unicode);
		
		if ((serverFeatures & SRV_TCPFLG_RELATEDSEARCH) != 0) 
			result.add(ServerFeatures.RelatedSearch);
		
		if ((serverFeatures & SRV_TCPFLG_TYPETAGINTEGER) != 0) 
			result.add(ServerFeatures.TypeTagInteger);
		
		if ((serverFeatures & SRV_TCPFLG_LARGEFILES) != 0) 
			result.add(ServerFeatures.LargeFiles);
		
		if ((serverFeatures & SRV_TCPFLG_TCPOBFUSCATION) != 0) 
			result.add(ServerFeatures.TCPObfusication);
		
		return result;
	}
	
	public static Set<ServerFeatures> scanUDPFeatures(int serverUDPFeatures) {
		Set<ServerFeatures> result = new HashSet<ServerFeatures>();
		
		if ((serverUDPFeatures & SRV_UDPFLG_EXT_GETSOURCES) != 0) 
			result.add(ServerFeatures.GetSources);
		
		if ((serverUDPFeatures & SRV_UDPFLG_EXT_GETFILES) != 0) 
			result.add(ServerFeatures.GetFiles);
		
		if ((serverUDPFeatures & SRV_UDPFLG_NEWTAGS) != 0) 
			result.add(ServerFeatures.NewTags);
		
		if ((serverUDPFeatures & SRV_UDPFLG_UNICODE) != 0) 
			result.add(ServerFeatures.Unicode);
		
		if ((serverUDPFeatures & SRV_UDPFLG_EXT_GETSOURCES2) != 0) 
			result.add(ServerFeatures.GetSources2);
		
		if ((serverUDPFeatures & SRV_UDPFLG_LARGEFILES) != 0) 
			result.add(ServerFeatures.LargeFiles);
		
		if ((serverUDPFeatures & SRV_UDPFLG_UDPOBFUSCATION) != 0) 
			result.add(ServerFeatures.UDPObfusication);
		
		if ((serverUDPFeatures & SRV_UDPFLG_TCPOBFUSCATION) != 0) 
			result.add(ServerFeatures.TCPObfusication);
		
		return result;
	}
	
}
