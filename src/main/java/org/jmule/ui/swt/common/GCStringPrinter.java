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
package org.jmule.ui.swt.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on Aug 22, 2008
 * @author binary256
 * @author Olivier Chalouhi
 * @author TuxPaper (rewrite)
 * @version $Revision: 1.2 $
 * Last changed by $Author: binary256_ $ on $Date: 2008/10/16 18:20:01 $
 */
public class GCStringPrinter {

	private static final boolean DEBUG = false;

	private static final String GOOD_STRING = "(/|,jI~`gy";

	private static final int FLAG_SKIPCLIP = 1;

	private static final int FLAG_FULLLINESONLY = 2;

	private static final int FLAG_NODRAW = 4;

	private static final int FLAG_KEEP_URL_INFO = 8;

	private static final Pattern patHREF = Pattern.compile(
			"<\\s*?a\\s.*?href\\s*?=\\s*?\"(.+?)\".*?>(.*?)<\\s*?/a\\s*?>",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern patAHREF_TITLE = Pattern.compile(
			"title=\\\"([^\\\"]+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern patAHREF_TARGET = Pattern.compile(
			"target=\\\"([^\\\"]+)", Pattern.CASE_INSENSITIVE);

	//private static final Pattern patOver1000 = Pattern.compile("[^\n]{1010,}");

	// Limit word/line length as OSX crashes on stringExtent on very very long words
	private static final int MAX_LINE_LEN = 4000;

	// max Word length can be same as line length since words are auto-split
	// across lines
	private static final int MAX_WORD_LEN = 4000;

	private boolean cutoff;

	private GC gc;

	private String string;

	private Rectangle printArea;

	private int swtFlags;

	private int printFlags;

	private Point size;

	private Color urlColor;

	private List listUrlInfo;

	private Image[] images;
	
	private float[] imageScales;

	public static class URLInfo
	{
		public String url;

		public String text;

		public Color urlColor;

		int relStartPos;

		// We could use a region, but that uses a resource that requires disposal
		List hitAreas = null;

		int titleLength;

		public String fullString;

		public String title;

		public String target;

		// @see java.lang.Object#toString()
		public String toString() {
			return super.toString() + ": relStart=" + relStartPos + ";url=" + url
					+ ";title=" + text + ";hit="
					+ (hitAreas == null ? 0 : hitAreas.size());
		}
	}

	private class LineInfo
	{
		public int width;

		String originalLine;

		String lineOutputed;

		int excessPos;

		public int relStartPos;
		
		public int height;
		
		public int imageIndexes[];

		public LineInfo(String originalLine, int relStartPos) {
			this.originalLine = originalLine;
			this.relStartPos = relStartPos;
		}

		// @see java.lang.Object#toString()
		public String toString() {
			return super.toString() + ": relStart=" + relStartPos + ";xcess="
					+ excessPos + ";orig=" + originalLine + ";output=" + lineOutputed;
		}
	}
	
	public static boolean printString(GC gc, String string, Rectangle printArea) {
		return printString(gc, string, printArea, false, false);
	}

	public static boolean printString(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly) {
		return printString(gc, string, printArea, skipClip, fullLinesOnly, SWT.WRAP
				| SWT.TOP);
	}

	/**
	 * 
	 * @param gc GC to print on
	 * @param string Text to print
	 * @param printArea Area of GC to print text to
	 * @param skipClip Don't set any clipping on the GC.  Text may overhang 
	 *                 printArea when this is true
	 * @param fullLinesOnly If bottom of a line will be chopped off, do not display it
	 * @param swtFlags SWT flags.  SWT.CENTER, SWT.BOTTOM, SWT.TOP, SWT.WRAP
	 * @return whether it fit
	 */
	public static boolean printString(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly, int swtFlags) {
		try {
			GCStringPrinter sp = new GCStringPrinter(gc, string, printArea, skipClip,
					fullLinesOnly, swtFlags);
			return sp.printString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * @param gc
	 * @param string
	 * @param printArea
	 * @param printFlags
	 * @param swtFlags
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	private boolean _printString() {
		size = new Point(0, 0);

		if (string == null) {
			return false;
		}

		if (printArea == null || printArea.isEmpty()) {
			return false;
		}

		ArrayList lines = new ArrayList();
		
		while (string.indexOf('\t') >= 0) {
			string = string.replace('\t', ' ');
		}

		boolean fullLinesOnly = (printFlags & FLAG_FULLLINESONLY) > 0;
		boolean skipClip = (printFlags & FLAG_SKIPCLIP) > 0;
		boolean noDraw = (printFlags & FLAG_NODRAW) > 0;
		boolean wrap = (swtFlags & SWT.WRAP) > 0;

		if ((printFlags & FLAG_KEEP_URL_INFO) == 0) {
			Matcher htmlMatcher = patHREF.matcher(string);
			boolean hasURL = htmlMatcher.find();
			if (hasURL) {
				listUrlInfo = new ArrayList(1);

				while (hasURL) {
					URLInfo urlInfo = new URLInfo();

					// Store the full ahref string once, then use substring which doesn't
					// create real strings :)
					urlInfo.fullString = htmlMatcher.group();
					urlInfo.relStartPos = htmlMatcher.start(0);

					urlInfo.url = string.substring(htmlMatcher.start(1),
							htmlMatcher.end(1));
					urlInfo.text = string.substring(htmlMatcher.start(2),
							htmlMatcher.end(2));
					urlInfo.titleLength = urlInfo.text.length();

					Matcher matcherTitle = patAHREF_TITLE.matcher(urlInfo.fullString);
					if (matcherTitle.find()) {
						urlInfo.title = string.substring(urlInfo.relStartPos
								+ matcherTitle.start(1), urlInfo.relStartPos
								+ matcherTitle.end(1));
					}

					Matcher matcherTarget = patAHREF_TARGET.matcher(urlInfo.fullString);
					if (matcherTarget.find()) {
						urlInfo.target = string.substring(urlInfo.relStartPos
								+ matcherTarget.start(1), urlInfo.relStartPos
								+ matcherTarget.end(1));
					}

					//System.out.println("URLINFO! " + urlInfo.fullString 
					//		+ "\ntarget="
					//		+ urlInfo.target + "\ntt=" + urlInfo.title + "\nurl="
					//		+ urlInfo.url + "\ntext=" + urlInfo.text + "\n\n");

					string = htmlMatcher.replaceFirst(urlInfo.text.replaceAll("\\$",
							"\\\\\\$"));
					
					listUrlInfo.add(urlInfo);
					htmlMatcher = patHREF.matcher(string);
					hasURL = htmlMatcher.find(urlInfo.relStartPos);
				}
			}
		} else {
			Matcher htmlMatcher = patHREF.matcher(string);
			string = htmlMatcher.replaceAll("$2");
		}

		Rectangle rectDraw = new Rectangle(printArea.x, printArea.y,
				printArea.width, printArea.height);

		Rectangle oldClipping = null;
		try {
			if (!skipClip && !noDraw) {
				oldClipping = gc.getClipping();

				// Protect the GC from drawing outside the drawing area
				gc.setClipping(printArea);
			}

			// Process string line by line
			int iCurrentHeight = 0;
			int currentCharPos = 0;
			
			int pos1 = string.indexOf('\n');
			int pos2 = string.indexOf('\r');
			if (pos2 == -1) {
				pos2 = pos1;
			}
			int posNewLine = Math.min(pos1, pos2);
			if (posNewLine < 0) {
				posNewLine = string.length();
			}
			int posLastNewLine = 0;
			while (posNewLine >= 0 && posLastNewLine < string.length()) {
				String sLine = string.substring(posLastNewLine, posNewLine);

				do {
					LineInfo lineInfo = new LineInfo(sLine, currentCharPos);
					lineInfo = processLine(gc, lineInfo, printArea, wrap, fullLinesOnly,
							false);
					String sProcessedLine = (String) lineInfo.lineOutputed;

					if (sProcessedLine != null && sProcessedLine.length() > 0) {
						if (lineInfo.width == 0 || lineInfo.height == 0) {
							Point gcExtent = gc.stringExtent(sProcessedLine);
							if (lineInfo.width == 0) {
								lineInfo.width = gcExtent.x;
							}
							if (lineInfo.height == 0) {
								lineInfo.height = gcExtent.y;
							}
						}
						Point extent = new Point(lineInfo.width, lineInfo.height);
						iCurrentHeight += extent.y;
						boolean isOverY = iCurrentHeight > printArea.height;

						if (DEBUG) {
							System.out.println("Adding Line: [" + sProcessedLine + "]"
									+ sProcessedLine.length() + "; h=" + iCurrentHeight + "("
									+ printArea.height + "). fullOnly?" + fullLinesOnly
									+ ". Excess: " + lineInfo.excessPos);
						}

						if (isOverY && !fullLinesOnly) {
							//fullLinesOnly = true; // <-- don't know why we needed this
							lines.add(lineInfo);
						} else if (isOverY && fullLinesOnly) {
							String excess = lineInfo.excessPos >= 0
									? sLine.substring(lineInfo.excessPos) : null;
							if (excess != null) {
								if (fullLinesOnly) {
									if (lines.size() > 0) {
										lineInfo = (LineInfo) lines.remove(lines.size() - 1);
										sProcessedLine = lineInfo.originalLine.length() > MAX_LINE_LEN
												? lineInfo.originalLine.substring(0, MAX_LINE_LEN)
												: lineInfo.originalLine;
										//sProcessedLine = ((LineInfo) lines.remove(lines.size() - 1)).originalLine;
										extent = gc.stringExtent(sProcessedLine);
									} else {
										if (DEBUG) {
											System.out.println("No PREV!?");
										}
										return false;
									}
								} else {
									sProcessedLine = sProcessedLine.length() > MAX_LINE_LEN
											? sProcessedLine.substring(0, MAX_LINE_LEN)
											: sProcessedLine;
								}

								if (excess.length() > MAX_LINE_LEN) {
									excess = excess.substring(0, MAX_LINE_LEN);
								}

								StringBuffer outputLine = new StringBuffer(sProcessedLine);
								lineInfo.width = extent.x;
								int newExcessPos = processWord(gc, sProcessedLine,
										" " + excess, printArea, false, lineInfo, outputLine,
										new StringBuffer());
								if (DEBUG) {
									System.out.println("  with word [" + excess + "] len is "
											+ lineInfo.width + "(" + printArea.width + ") w/excess "
											+ newExcessPos);
								}

								lineInfo.lineOutputed = outputLine.toString();
								lines.add(lineInfo);
								if (DEBUG) {
									System.out.println("replace prev line with: "
											+ outputLine.toString());
								}
							} else {
								if (DEBUG) {
									System.out.println("No Excess");
								}
							}
							cutoff = true;
							return false;
						} else {
							lines.add(lineInfo);
						}
						sLine = lineInfo.excessPos >= 0 && wrap
								? sLine.substring(lineInfo.excessPos) : null;
					} else {
						if (DEBUG) {
							System.out.println("Line process resulted in no text: " + sLine);
						}
						lines.add(lineInfo);
						currentCharPos++;
						break;
						//return false;
					}

					currentCharPos += lineInfo.excessPos >= 0 ? lineInfo.excessPos
							: lineInfo.lineOutputed.length();
					//System.out.println("output: " + lineInfo.lineOutputed.length() + ";" 
					//		+ lineInfo.lineOutputed + ";xc=" + lineInfo.excessPos + ";ccp=" + currentCharPos);
					//System.out.println("lineo=" + lineInfo.lineOutputed.length() + ";" + sLine.length() );
				} while (sLine != null);

				if (string.length() > posNewLine && string.charAt(posNewLine) == '\r'
						&& string.charAt(posNewLine + 1) == '\n') {
					posNewLine++;
				}
				posLastNewLine = posNewLine + 1;
				currentCharPos = posLastNewLine;

				pos1 = string.indexOf('\n', posLastNewLine);
				pos2 = string.indexOf('\r', posLastNewLine);
				if (pos2 == -1) {
					pos2 = pos1;
				}
				posNewLine = Math.min(pos1, pos2);
				if (posNewLine < 0) {
					posNewLine = string.length();
				}
			}
		} finally {
			if (!skipClip && !noDraw) {
				gc.setClipping(oldClipping);
			}

			if (lines.size() > 0) {
				// rebuild full text to get the exact y-extent of the output
				// this may be different (but shouldn't be!) than the height of each
				// line
				StringBuffer fullText = new StringBuffer(string.length() + 10);
				for (Iterator iter = lines.iterator(); iter.hasNext();) {
					LineInfo lineInfo = (LineInfo) iter.next();
					if (fullText.length() > 0) {
						fullText.append('\n');
					}
					fullText.append(lineInfo.lineOutputed);
				}

				//size = gc.textExtent(fullText.toString());

				for (Iterator iter = lines.iterator(); iter.hasNext();) {
					LineInfo lineInfo = (LineInfo) iter.next();
					size.x += Math.max(lineInfo.width, size.x);
					size.y += lineInfo.height;
				}
				
				if ((swtFlags & (SWT.BOTTOM)) != 0) {
					rectDraw.y = rectDraw.y + rectDraw.height - size.y;
				} else if ((swtFlags & SWT.TOP) == 0) {
					// center vert
					rectDraw.y = rectDraw.y + (rectDraw.height - size.y) / 2;
				}

				if (!noDraw || listUrlInfo != null) {
					for (Iterator iter = lines.iterator(); iter.hasNext();) {
						LineInfo lineInfo = (LineInfo) iter.next();
						try {
							drawLine(gc, lineInfo, swtFlags, rectDraw, noDraw);
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}
			}
		}
		

		return size.y <= printArea.height;
	}

	/**
	 * @param hasMoreElements 
	 * @param line
	 *
	 * @since 3.0.0.7
	 */
	private LineInfo processLine(final GC gc, final LineInfo lineInfo,
			final Rectangle printArea, final boolean wrap,
			final boolean fullLinesOnly, boolean hasMoreElements) {
		
		if (lineInfo.originalLine.length() == 0) {
			lineInfo.lineOutputed = "";
			lineInfo.height = gc.stringExtent(GOOD_STRING).y;
			return lineInfo;
		}
		
		StringBuffer outputLine = new StringBuffer();
		int excessPos = -1;

		if (images != null || lineInfo.originalLine.length() > MAX_LINE_LEN
				|| gc.stringExtent(lineInfo.originalLine).x > printArea.width) {
			if (DEBUG) {
				System.out.println("Line to process: " + lineInfo.originalLine);
			}
			StringBuffer space = new StringBuffer(1);

			if (!wrap && images == null) {
				if (DEBUG) {
					System.out.println("No Wrap.. doing all in one line");
				}

				String sProcessedLine = lineInfo.originalLine.length() > MAX_LINE_LEN
						? lineInfo.originalLine.substring(0, MAX_LINE_LEN)
						: lineInfo.originalLine;

				// if it weren't for the elipses, we could do:
				// outputLine.append(sProcessedLine);

				excessPos = processWord(gc, lineInfo.originalLine, sProcessedLine,
						printArea, wrap, lineInfo, outputLine, space);
			} else {
				int posLastWordStart = 0;
				int posWordStart = lineInfo.originalLine.indexOf(' ');
				if (posWordStart < 0) {
					posWordStart = lineInfo.originalLine.length();
				}
				// Process line word by word
				int curPos = 0;
				while (posWordStart >= 0 && posLastWordStart < lineInfo.originalLine.length()) {
					String word = lineInfo.originalLine.substring(posLastWordStart, posWordStart);
					if (word.length() == 0) {
						excessPos = -1;
						outputLine.append(' ');
					}

					for (int i = 0; i < word.length(); i += MAX_WORD_LEN) {
						String subWord;
						int endPos = i + MAX_WORD_LEN;
						if (endPos > word.length()) {
							subWord = word.substring(i);
						} else {
							subWord = word.substring(i, endPos);
						}

						excessPos = processWord(gc, lineInfo.originalLine, subWord,
								printArea, wrap, lineInfo, outputLine, space);
						if (DEBUG) {
							System.out.println("  with word [" + subWord + "] len is "
									+ lineInfo.width + "(" + printArea.width + ") w/excess "
									+ excessPos);
						}
						if (excessPos >= 0) {
							excessPos += curPos;
							break;
						}
						if (endPos <= word.length()) {
							space.setLength(0);
						}
						curPos += subWord.length() + 1;
					}
					if (excessPos >= 0) {
						break;
					}
					
					posLastWordStart = posWordStart + 1;
					posWordStart = lineInfo.originalLine.indexOf(' ', posLastWordStart);
					if (posWordStart < 0) {
						posWordStart = lineInfo.originalLine.length();
					}
				}
			}
		} else {
			outputLine.append(lineInfo.originalLine);
		}

		if (!wrap && hasMoreElements && excessPos >= 0) {
			outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
			cutoff = true;
		}
		//drawLine(gc, outputLine, swtFlags, rectDraw);
		//		if (!wrap) {
		//			return hasMoreElements;
		//		}
		lineInfo.excessPos = excessPos;
		lineInfo.lineOutputed = outputLine.toString();
		return lineInfo;
	}

	/**
	 * @param int Position of part of word that didn't fit
	 *
	 * @since 3.0.0.7
	 */
	private int processWord(final GC gc, final String sLine, String word,
			final Rectangle printArea, final boolean wrap, final LineInfo lineInfo,
			StringBuffer outputLine, final StringBuffer space) {

		if (word.length() == 0) {
			space.append(' ');
			return -1;
		}
		
		//System.out.println("PW: " + word);
		if (images != null && word.length() >= 2 && word.charAt(0) == '%') {
			int imgIdx = word.charAt(1) - '0';
			if (images.length > imgIdx && imgIdx >= 0 && images[imgIdx] != null) {
				Image img = images[imgIdx];
				Rectangle bounds = img.getBounds();
				if (imageScales != null && imageScales.length > imgIdx) {
					bounds.width = (int) (bounds.width * imageScales[imgIdx]);
					bounds.height = (int) (bounds.height * imageScales[imgIdx]);
				}
				
				Point spaceExtent = gc.stringExtent(space.toString());
				int newWidth = lineInfo.width + bounds.width + spaceExtent.x;


				if (newWidth > printArea.width) {
					if (bounds.width + spaceExtent.x < printArea.width || lineInfo.width > 0) {
						return 0;
					}
				}
				
				if (lineInfo.imageIndexes == null) {
					lineInfo.imageIndexes = new int[] { imgIdx };
				}
				
				lineInfo.width = newWidth;
				lineInfo.height = Math.max(bounds.height, lineInfo.height);

				outputLine.append(space);
				outputLine.append(word);
				if (space.length() > 0) {
					space.delete(0, space.length());
				}
				space.append(' ');
				
				return -1;
			}
		}

		Point ptWordSize = gc.stringExtent(word + " ");
		boolean bWordLargerThanWidth = ptWordSize.x > printArea.width;
		int targetWidth = lineInfo.width + ptWordSize.x;
		if (targetWidth > printArea.width) {
			// word is longer than space avail, split
			int endIndex = word.length();
			long diff = endIndex;

			while (targetWidth != printArea.width) {
				diff = (diff >> 1) + (diff % 2);

				if (diff <= 0) {
					diff = 1;
				}

				//System.out.println("diff=" + diff + ";e=" + endIndex + ";tw=" + targetWidth + ";paw= " + printArea.width);
				if (targetWidth > printArea.width) {
					endIndex -= diff;
					if (endIndex < 1) {
						endIndex = 1;
					}
				} else {
					endIndex += diff;
					if (endIndex > word.length()) {
						endIndex = word.length();
					}
				}

				ptWordSize = gc.stringExtent(word.substring(0, endIndex) + " ");
				targetWidth = lineInfo.width + ptWordSize.x;

				if (diff <= 1) {
					break;
				}
			}
			;
			if (endIndex == 0) {
				endIndex = 1;
			}
			if (targetWidth > printArea.width && endIndex > 1) {
				endIndex--;
				ptWordSize = gc.stringExtent(word.substring(0, endIndex) + " ");
			}

			if (DEBUG) {
				System.out.println("excess starts at " + endIndex + "(" + ptWordSize.x
						+ "px) of " + word.length() + ". "
						+ (ptWordSize.x + lineInfo.width) + "/" + printArea.width
						+ "; wrap?" + wrap);
			}

			if (endIndex > 0 && outputLine.length() > 0) {
				outputLine.append(space);
			}

			if (endIndex == 0 && outputLine.length() == 0) {
				endIndex = 1;
			}

			if (wrap && ptWordSize.x < printArea.width && !bWordLargerThanWidth) {
				// whole word is excess
				return 0;
			}

			outputLine.append(word.substring(0, endIndex));
			if (!wrap) {
				int len = outputLine.length();
				if (len == 0) {
					if (word.length() > 0) {
						outputLine.append(word.charAt(0));
					} else if (sLine.length() > 0) {
						outputLine.append(sLine.charAt(0));
					}
				} else if (len > 1) {
					outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
					cutoff = true;
				}
			}
			//drawLine(gc, outputLine, swtFlags, rectDraw);
			if (DEBUG) {
				System.out.println("excess " + word.substring(endIndex));
			}
			return endIndex;
		}

		lineInfo.width += ptWordSize.x;
		if (lineInfo.width > printArea.width) {
			if (space.length() > 0) {
				space.delete(0, space.length());
			}

			if (!wrap) {
				int len = outputLine.length();
				if (len == 0) {
					if (word.length() > 0) {
						outputLine.append(word.charAt(0));
					} else if (sLine.length() > 0) {
						outputLine.append(sLine.charAt(0));
					}
				} else if (len > 1) {
					outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
					cutoff = true;
				}
				return -1;
			} else {
				return 0;
			}
			//drawLine(gc, outputLine, swtFlags, rectDraw);
		}

		if (outputLine.length() > 0) {
			outputLine.append(space);
		}
		outputLine.append(word);
		if (space.length() > 0) {
			space.delete(0, space.length());
		}
		space.append(' ');

		return -1;
	}

	/**
	 * printArea is updated to the position of the next row
	 * 
	 * @param gc
	 * @param outputLine
	 * @param swtFlags
	 * @param printArea
	 * @param noDraw 
	 */
	private void drawLine(GC gc, LineInfo lineInfo, int swtFlags,
			Rectangle printArea, boolean noDraw) {
		String text = lineInfo.lineOutputed;
		// TODO: ensure width and height have values
		if (lineInfo.width == 0 || lineInfo.height == 0) {
			Point gcExtent = gc.stringExtent(text);;
			if (lineInfo.width == 0) {
				lineInfo.width = gcExtent.x;
			}
			if (lineInfo.height == 0) {
				lineInfo.height = gcExtent.y;
			}
		}
		Point drawSize = new Point(lineInfo.width, lineInfo.height);
		
		int x0;
		if ((swtFlags & SWT.RIGHT) > 0) {
			x0 = printArea.x + printArea.width - drawSize.x;
		} else if ((swtFlags & SWT.CENTER) > 0) {
			x0 = printArea.x + (printArea.width - drawSize.x) / 2;
		} else {
			x0 = printArea.x;
		}

		int y0 = printArea.y;

		int lineInfoRelEndPos = lineInfo.relStartPos
				+ lineInfo.lineOutputed.length();
		int relStartPos = lineInfo.relStartPos;
		int lineStartPos = 0;

		URLInfo urlInfo = null;
		boolean drawURL = hasHitUrl();

		if (drawURL) {
			URLInfo[] hitUrlInfo = getHitUrlInfo();
			int nextHitUrlInfoPos = 0;

			while (drawURL) {
				drawURL = false;
				for (int i = nextHitUrlInfoPos; i < hitUrlInfo.length; i++) {
					urlInfo = hitUrlInfo[i];

					drawURL = (urlInfo.relStartPos < lineInfoRelEndPos)
							&& (urlInfo.relStartPos + urlInfo.titleLength > relStartPos)
							&& (relStartPos >= lineInfo.relStartPos)
							&& (relStartPos < lineInfoRelEndPos);
					if (drawURL) {
						nextHitUrlInfoPos = i + 1;
						break;
					}
				}

				if (!drawURL) {
					break;
				}

				//int numHitUrlsAlready = urlInfo.hitAreas == null ? 0 : urlInfo.hitAreas.size();

				// draw text before url
				int i = lineStartPos + urlInfo.relStartPos - relStartPos;
				//System.out.println("numHitUrlsAlready = " + numHitUrlsAlready + ";i=" + i);
				if (i > 0 && i > lineStartPos && i <= text.length()) {
					String s = text.substring(lineStartPos, i);
					//gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
					x0 += drawText(gc, s, x0, y0, lineInfo.height, null, noDraw).x;

					relStartPos += (i - lineStartPos);
					lineStartPos += (i - lineStartPos);
					//System.out.println("|" + s + "|" + textExtent.x);
				}

				// draw url text
				int end = i + urlInfo.titleLength;
				if (i < 0) {
					i = 0;
				}
				//System.out.println("end=" + end + ";" + text.length() + ";titlelen=" + urlInfo.titleLength);
				if (end > text.length()) {
					end = text.length();
				}
				String s = text.substring(i, end);
				relStartPos += (end - i);
				lineStartPos += (end - i);
				Point pt = null;
				//System.out.println("|" + s + "|");
				Color fgColor = null;
				if (!noDraw) {
					fgColor = gc.getForeground();
					if (urlInfo.urlColor != null) {
						gc.setForeground(urlInfo.urlColor);
					} else if (urlColor != null) {
						gc.setForeground(urlColor);
					}
				}
				if (urlInfo.hitAreas == null) {
					urlInfo.hitAreas = new ArrayList(1);
				}
				pt = drawText(gc, s, x0, y0, lineInfo.height, urlInfo.hitAreas, noDraw);
				if (!noDraw) {
					gc.setForeground(fgColor);
				}

				if (urlInfo.hitAreas == null) {
					urlInfo.hitAreas = new ArrayList(1);
				}
				//gc.drawRectangle(new Rectangle(x0, y0, pt.x, lineInfo.height));

				x0 += pt.x;
			}
		}

		// draw text after url
		if (lineStartPos < text.length()) {
			String s = text.substring(lineStartPos);
			if (!noDraw) {
				drawText(gc, s, x0, y0, lineInfo.height, null, noDraw);
			}
		}
		printArea.y += drawSize.y;
	}
	
	private Point drawText(GC gc, String s, int x, int y, int height,
			List hitAreas, boolean nodraw) {
		Point textExtent;

		if (images != null) {
  		int pctPos = s.indexOf('%');
  		int lastPos = 0;
  		int w = 0;
  		int h = 0;
  		while (pctPos >= 0) {
    		if (pctPos >= 0 && s.length() > pctPos + 1) {
    			int imgIdx = s.charAt(pctPos + 1) - '0';
    			
    			if (imgIdx >= images.length || imgIdx < 0 || images[imgIdx] == null) {
      			String sStart = s.substring(lastPos, pctPos + 1);
    				textExtent = gc.textExtent(sStart);
    				int centerY = y + (height / 2 - textExtent.y / 2); 
        		if (hitAreas != null) {
        			hitAreas.add(new Rectangle(x, centerY, textExtent.x, textExtent.y));
        		}
      			if (!nodraw) {
      				gc.drawText(sStart, x, centerY, true);
      			}
      			x += textExtent.x;
      			w += textExtent.x;
      			h = Math.max(h, textExtent.y);

      			lastPos = pctPos + 1;
        		pctPos = s.indexOf('%', pctPos + 1);
    				continue;
    			}
    			
    			String sStart = s.substring(lastPos, pctPos);
    			textExtent = gc.textExtent(sStart);
  				int centerY = y + (height / 2 - textExtent.y / 2); 
    			if (!nodraw) {
    				gc.drawText(sStart, x, centerY, true);
    			}
    			x += textExtent.x;
    			w += textExtent.x;
    			h = Math.max(h, textExtent.y);
    			if (hitAreas != null) {
    				hitAreas.add(new Rectangle(x, centerY, textExtent.x, textExtent.y));
    			}

    			//System.out.println("drawimage: " + x + "x" + y + ";idx=" + imgIdx);
    			Rectangle imgBounds = images[imgIdx].getBounds();
    			float scale = 1.0f;
  				if (imageScales != null && imageScales.length > imgIdx) {
  					scale = imageScales[imgIdx];
  				}
  				int scaleImageWidth = (int) (imgBounds.width * scale);
					int scaleImageHeight = (int) (imgBounds.height * scale);

    			
  				centerY = y + (height / 2 - scaleImageHeight / 2); 
    			if (hitAreas != null) {
    				hitAreas.add(new Rectangle(x, centerY, scaleImageWidth, scaleImageHeight));
    			}
    			if (!nodraw) {
    				//gc.drawImage(images[imgIdx], x, centerY);
    				gc.drawImage(images[imgIdx], 0, 0, imgBounds.width,
								imgBounds.height, x, centerY, scaleImageWidth, scaleImageHeight);
    			}
    			x += scaleImageWidth;
    			w += scaleImageWidth;
    			
    			h = Math.max(h, scaleImageHeight);
    		}
    		lastPos = pctPos + 2;
    		pctPos = s.indexOf('%', lastPos);
  		}

  		if (s.length() >= lastPos) {
    		String sEnd = s.substring(lastPos);
  			textExtent = gc.textExtent(sEnd);
				int centerY = y + (height / 2 - textExtent.y / 2); 
  			if (hitAreas != null) {
  				hitAreas.add(new Rectangle(x, centerY, textExtent.x, textExtent.y));
  			}
  			if (!nodraw) {
  				gc.drawText(sEnd, x, centerY, true);
  			}
  			x += textExtent.x;
  			w += textExtent.x;
  			h = Math.max(h, textExtent.y);
  		}
  		return new Point(w, h);
		}


		if (!nodraw) {
			gc.drawText(s, x, y, true);
		}
		textExtent = gc.textExtent(s);
		if (hitAreas != null) {
			hitAreas.add(new Rectangle(x, y, textExtent.x, textExtent.y));
		}
		return textExtent;
	}

	/**
	 * 
	 */
	public GCStringPrinter(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly, int swtFlags) {
		this.gc = gc;
		this.string = string;
		this.printArea = printArea;
		this.swtFlags = swtFlags;

		printFlags = 0;
		if (skipClip) {
			printFlags |= FLAG_SKIPCLIP;
		}
		if (fullLinesOnly) {
			printFlags |= FLAG_FULLLINESONLY;
		}
	}

	public GCStringPrinter(GC gc, String string, Rectangle printArea,
			int printFlags, int swtFlags) {
		this.gc = gc;
		this.string = string;
		this.printArea = printArea;
		this.swtFlags = swtFlags;
		this.printFlags = printFlags;
	}

	public boolean printString() {
		return _printString();
	}

	public boolean printString(int printFlags) {
		int oldPrintFlags = this.printFlags;
		printFlags |= printFlags;
		boolean b = _printString();
		this.printFlags = oldPrintFlags;
		return b;
	}

	public void calculateMetrics() {
		int oldPrintFlags = printFlags;
		printFlags |= FLAG_NODRAW;
		_printString();
		printFlags = oldPrintFlags;
	}

	/**
	 * @param rectangle
	 *
	 * @since 3.0.4.3
	 */
	public void printString(GC gc, Rectangle rectangle, int swtFlags) {
		this.gc = gc;
		int printFlags = this.printFlags;
		if (printArea.width == rectangle.width) {
			printFlags |= FLAG_KEEP_URL_INFO;
		}
		printArea = rectangle;
		this.swtFlags = swtFlags;
		printString(printFlags);
	}

	public Point getCalculatedSize() {
		return size;
	}

	public Color getUrlColor() {
		return urlColor;
	}

	public void setUrlColor(Color urlColor) {
		this.urlColor = urlColor;
	}

	public URLInfo getHitUrl(int x, int y) {
		if (listUrlInfo == null || listUrlInfo.size() == 0) {
			return null;
		}
		for (Iterator iter = listUrlInfo.iterator(); iter.hasNext();) {
			URLInfo urlInfo = (URLInfo) iter.next();
			if (urlInfo.hitAreas != null) {
				for (Iterator iter2 = urlInfo.hitAreas.iterator(); iter2.hasNext();) {
					Rectangle r = (Rectangle) iter2.next();
					if (r.contains(x, y)) {
						return urlInfo;
					}
				}
			}
		}
		return null;
	}

	public URLInfo[] getHitUrlInfo() {
		if (listUrlInfo == null) {
			return new URLInfo[0];
		}
		return (URLInfo[]) listUrlInfo.toArray(new URLInfo[0]);
	}

	public boolean hasHitUrl() {
		return listUrlInfo != null && listUrlInfo.size() > 0;
	}

	public boolean isCutoff() {
		return cutoff;
	}
	
	public void setImages(Image[] images) {
		this.images = images;
	}

	public float[] getImageScales() {
		return imageScales;
	}

	public void setImageScales(float[] imageScales) {
		this.imageScales = imageScales;
	}

	
}
