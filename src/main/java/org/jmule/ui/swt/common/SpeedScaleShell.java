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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.jmule.core.JMConstants;
import org.jmule.ui.swt.SWTThread;
import org.jmule.ui.utils.SpeedFormatter;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Cheap ugly slider shell
 * Created on Aug 22, 2008
 * @author TuxPaper
 * @author binary256
 * @version $Revision: 1.2 $
 * Last changed by $Author: binary255 $ on $Date: 2009/11/12 18:38:49 $
 */
public class SpeedScaleShell {

	private static final boolean MOUSE_ONLY_UP_EXITS = true;

	private static final int OPTION_HEIGHT = 15;

	private static final int TEXT_HEIGHT = 32;

	private static final int SCALER_HEIGHT = 20;

	private int HEIGHT = TEXT_HEIGHT + SCALER_HEIGHT;

	private static final int WIDTH = 120;

	private static final int PADDING_X0 = 10;

	private static final int PADDING_X1 = 10;

	private static final int WIDTH_NO_PADDING = WIDTH - PADDING_X0 - PADDING_X1;

	private static final int TYPED_TEXT_ALPHA = 80;

	private static final long CLOSE_DELAY = 600;

	private long value;

	private boolean cancelled;

	private long minValue;

	private long maxValue;

	private long maxTextValue;

	private long pageIncrement;

	private long bigPageIncrement;

	private Shell shell;

	private LinkedHashMap mapOptions = new LinkedHashMap();

	private String sValue = "";

	private String title = "";
	
	private Composite composite;

	private boolean menuChosen;

	protected boolean lastMoveHadMouseDown;

	private boolean assumeInitiallyDown;

	public static void main(String[] args) {
		SpeedScaleShell speedScaleWidget = new SpeedScaleShell("Test") {
			public String getStringValue() {
				return getValue() + "b/s";
			}
		};
		speedScaleWidget.setMaxValue(10000);
		speedScaleWidget.setMaxTextValue(15000);
		speedScaleWidget.addOption("AutoSpeed", -1);
		speedScaleWidget.addOption("Preset: 10b/s", 10);
//		speedScaleWidget.addOption("Preset: 20b/s", 20);
//		speedScaleWidget.addOption("Preset: 1b/s", 1);
//		speedScaleWidget.addOption("Preset: 1000b/s", 1000);
		speedScaleWidget.addOption("Preset: A really long preset", 2000);
		System.out.println("returns "
				+ speedScaleWidget.open(1000, JMConstants.isWindows) + " w/"
				+ speedScaleWidget.getValue());
	}

	public SpeedScaleShell(String title) {
		minValue = 0;
		maxValue = -1;
		maxTextValue = -1;
		pageIncrement = 10;
		bigPageIncrement = 100;
		cancelled = true;
		menuChosen = false;
		this.title = title;
	}

	/**
	 * Borks with 0 or -1 maxValue
	 * 
	 * @param startValue
	 * @param assumeInitiallyDown 
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	public boolean open(final long startValue, boolean _assumeInitiallyDown) {
		value = startValue;
		this.assumeInitiallyDown = _assumeInitiallyDown;
		cancelled = true;

		shell = new Shell(SWTThread.getDisplay(), SWT.DOUBLE_BUFFERED | SWT.ON_TOP);
		shell.setLayout(new FillLayout());
		final Display display = shell.getDisplay();

		composite = new Composite(shell, SWT.DOUBLE_BUFFERED);

		final Point firstMousePos = display.getCursorLocation();

		composite.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					setCancelled(true);
					shell.dispose();
				} else if (e.detail == SWT.TRAVERSE_ARROW_NEXT) {
					setValue(value + 1);
				} else if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
					setValue(value - 1);
				} else if (e.detail == SWT.TRAVERSE_PAGE_NEXT) {
					setValue(value + bigPageIncrement);
				} else if (e.detail == SWT.TRAVERSE_PAGE_PREVIOUS) {
					setValue(value - bigPageIncrement);
				} else if (e.detail == SWT.TRAVERSE_RETURN) {
					setCancelled(false);
					shell.dispose();
				}
			}
		});

		composite.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.PAGE_DOWN && e.stateMask == 0) {
					setValue(value + pageIncrement);
				} else if (e.keyCode == SWT.PAGE_UP && e.stateMask == 0) {
					setValue(value - pageIncrement);
				} else if (e.keyCode == SWT.HOME) {
					setValue(minValue);
				} else if (e.keyCode == SWT.END) {
					if (maxValue != -1) {
						setValue(maxValue);
					}
				}
			}
		});

		composite.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				lastMoveHadMouseDown = false;
				boolean hasButtonDown = (e.stateMask & SWT.BUTTON_MASK) > 0
						|| assumeInitiallyDown;
				if (hasButtonDown) {
					if (e.y > HEIGHT - SCALER_HEIGHT) {
						lastMoveHadMouseDown = true;
						setValue(getValueFromMousePos(e.x));
					}
					composite.redraw();
				} else {
					composite.redraw();
				}
			}
		});

		composite.addMouseTrackListener(new MouseTrackListener() {
			boolean mouseIsOut = false;

			private boolean exitCancelled = false;

			public void mouseHover(MouseEvent e) {
			}

			public void mouseExit(MouseEvent e) {
			if (!exitCancelled) {
						shell.dispose();
				} else {
				exitCancelled = false;
				}
			
			}

			public void mouseEnter(MouseEvent e) {
				if (mouseIsOut) {
					exitCancelled = true;
				}
				mouseIsOut = false;
			}
		});

		composite.addMouseListener(new MouseListener() {
			boolean bMouseDown = false;

			public void mouseUp(MouseEvent e) {
				if (assumeInitiallyDown) {
					assumeInitiallyDown = false;
				}
				if (MOUSE_ONLY_UP_EXITS) {
					if (lastMoveHadMouseDown) {
						Point mousePos = display.getCursorLocation();
						if (mousePos.equals(firstMousePos)) {
							lastMoveHadMouseDown = false;
							return;
						}
					}
					bMouseDown = true;
				}
				if (bMouseDown) {
					if (e.y > HEIGHT - SCALER_HEIGHT) {
						setValue(getValueFromMousePos(e.x));
						setCancelled(false);
						if (lastMoveHadMouseDown) {
							shell.dispose();
						}
					} else if (e.y > TEXT_HEIGHT) {
						int idx = (e.y - TEXT_HEIGHT) / OPTION_HEIGHT;
						Iterator iterator = mapOptions.keySet().iterator();
						long newValue;
						do {
							newValue = ((Long) iterator.next()).intValue();
							idx--;
						} while (idx >= 0);
						value = newValue; // ignore min/max
						setCancelled(false);
						setMenuChosen(true);
						shell.dispose();
					}
				}
			}

			public void mouseDown(MouseEvent e) {
				if (e.count > 1) {
					lastMoveHadMouseDown = true;
					return;
				}
				Point mousePos = display.getCursorLocation();
				if (e.y > HEIGHT - SCALER_HEIGHT) {
					bMouseDown = true;
					setValue(getValueFromMousePos(e.x));
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}

		});

		composite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				int x = (int)(WIDTH_NO_PADDING * value / maxValue);
				if (x < 0) {
					x = 0;
				} else if (x > WIDTH_NO_PADDING) {
					x = WIDTH_NO_PADDING;
				}
				int startX = (int)(WIDTH_NO_PADDING * startValue / maxValue);
				if (startX < 0) {
					startX = 0;
				} else if (startX > WIDTH_NO_PADDING) {
					startX = WIDTH_NO_PADDING;
				}
				int baseLinePos = getBaselinePos();

				try {
					e.gc.setAdvanced(true);
					e.gc.setAntialias(SWT.ON);
				} catch (Exception ex) {
					// aw
				}

				e.gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
				// left
				e.gc.drawLine(PADDING_X0, baseLinePos - 6, PADDING_X0, baseLinePos + 6);
				// right
				e.gc.drawLine(PADDING_X0 + WIDTH_NO_PADDING, baseLinePos - 6,
						PADDING_X0 + WIDTH_NO_PADDING, baseLinePos + 6);
				// baseline
				e.gc.drawLine(PADDING_X0, baseLinePos, PADDING_X0 + WIDTH_NO_PADDING,
						baseLinePos);

				e.gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
				// start value marker
				e.gc.drawLine(PADDING_X0 + startX, baseLinePos - 5,
						PADDING_X0 + startX, baseLinePos + 5);
				// current value marker
				e.gc.fillRoundRectangle(PADDING_X0 + x - 2, baseLinePos - 5, 5, 10, 10,
						10);

				// Current Value Text
				e.gc.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

				e.gc.fillRectangle(0, 0, WIDTH, TEXT_HEIGHT);

				GCStringPrinter.printString(e.gc,title+"\n"+ _getStringValue(), new Rectangle(0,
						0, WIDTH, HEIGHT), true, false, SWT.CENTER | SWT.TOP | SWT.WRAP);

				e.gc.drawLine(0, TEXT_HEIGHT - 1, WIDTH, TEXT_HEIGHT - 1);

				// options list
				int y = TEXT_HEIGHT;
				Point mousePos = composite.toControl(display.getCursorLocation());
				for (Iterator iter = mapOptions.keySet().iterator(); iter.hasNext();) {
					long value = (Long) iter.next();
					String text = (String) mapOptions.get(value);

					Rectangle area = new Rectangle(0, y, WIDTH, OPTION_HEIGHT);
					Color bg;
					if (area.contains(mousePos)) {
						bg = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
						e.gc.setBackground(bg);
						e.gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
						e.gc.fillRectangle(area);
					} else {
						bg = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
						e.gc.setBackground(bg);
						e.gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
					}

					int ovalSize = OPTION_HEIGHT - 6;
					if (getValue() == value) {
						Color saveColor = e.gc.getBackground();
						e.gc.setBackground(e.gc.getForeground());
						e.gc.fillOval(4, y + 5, ovalSize - 3, ovalSize - 3);
						e.gc.setBackground(saveColor);
					}
					if (JMConstants.isLinux) {
						// Hack: on linux, drawing oval seems to draw a line from last pos
						// to start of oval.. drawing a point (anywhere) seems to clear the
						// path
						Color saveColor = e.gc.getForeground();
						e.gc.setForeground(bg);
						e.gc.drawPoint(2, y + 3);
						e.gc.setForeground(saveColor);
					}
					e.gc.drawOval(2, y + 3, ovalSize, ovalSize);

					GCStringPrinter.printString(e.gc, text, new Rectangle(OPTION_HEIGHT,
							y, WIDTH - OPTION_HEIGHT, OPTION_HEIGHT), true, false, SWT.LEFT);
					y += OPTION_HEIGHT;
				}

				// typed value
				if (sValue.length() > 0) {
					Point extent = e.gc.textExtent(sValue);
					if (extent.x > WIDTH - 10) {
						extent.x = WIDTH - 10;
					}
					Rectangle rect = new Rectangle(WIDTH - 8 - extent.x, 14,
							extent.x + 5, extent.y + 4 + 14 > TEXT_HEIGHT ? TEXT_HEIGHT - 15
									: extent.y + 4);
					e.gc.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					e.gc.fillRectangle(rect);

					try {
						e.gc.setAlpha(TYPED_TEXT_ALPHA);
					} catch (Exception ex) {
					}
					e.gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
					e.gc.setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
					//e.gc.drawRectangle(rect);

					GCStringPrinter.printString(e.gc, sValue , new Rectangle(rect.x + 2,
							rect.y + 2, WIDTH - 5, OPTION_HEIGHT), true, false, SWT.LEFT
							| SWT.BOTTOM);
				}
			}
		});

		Point location = display.getCursorLocation();

		location.y -= getBaselinePos();
		int x = (int) (WIDTH_NO_PADDING * (value > maxValue ? 1 : (double) value
				/ maxValue));
		location.x -= PADDING_X0 + x;

		Rectangle bounds = new Rectangle(location.x, location.y, WIDTH, HEIGHT);
		Monitor mouseMonitor = shell.getMonitor();
		Monitor[] monitors = display.getMonitors();
		for (int i = 0; i < monitors.length; i++) {
			Monitor monitor = monitors[i];
			if (monitor.getBounds().contains(location)) {
				mouseMonitor = monitor;
				break;
			}
		}
		Rectangle monitorBounds = mouseMonitor.getBounds();
		Rectangle intersection = monitorBounds.intersection(bounds);
		if (intersection.width != bounds.width) {
			bounds.x = monitorBounds.x + monitorBounds.width - WIDTH;
			bounds.width = WIDTH;
		}
		if (intersection.height != bounds.height) {
			bounds.y = monitorBounds.y + monitorBounds.height - HEIGHT;
			bounds.height = HEIGHT;
		}

		shell.setBounds(bounds);
		if (!bounds.contains(firstMousePos)) {
			// should never happen, which means it probably will, so handle it badly
			shell.setLocation(firstMousePos.x - (bounds.width / 2), firstMousePos.y
					- bounds.height + 2);
		}

		shell.open();
		// must be after, for OSX
		composite.setFocus();

		try {
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (Throwable t) {
			
		}


		return !cancelled;
	}

	/**
	 * @param x
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	protected long getValueFromMousePos(int x) {
		int x0 = x + 1;
		if (x < PADDING_X0) {
			x0 = PADDING_X0;
		} else if (x > PADDING_X0 + WIDTH_NO_PADDING) {
			x0 = PADDING_X0 + WIDTH_NO_PADDING;
		}

		return (x0 - PADDING_X0) * maxValue / WIDTH_NO_PADDING;
	}

	public long getValue() {
		return value;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public long getMinValue() {
		return minValue;
	}

	public void setMinValue(long minValue) {
		this.minValue = minValue;
	}

	public long getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(long maxValue) {
		this.maxValue = maxValue;
	}

	public void setValue(long value) {
		//System.out.println("sv " + value + ";" + Debug.getCompressedStackTrace());
		if (value > maxValue) {
			value = maxValue;
		} else if (value < minValue) {
			value = minValue;
		}
		this.value = value;
		if (composite != null && !composite.isDisposed()) {
			composite.redraw();
		}
	}

	public String _getStringValue() {
		String name = (String) mapOptions.get(new Long(value));
		return getStringValue(value, name);
	}

	public String getStringValue(long value, String sValue) {
		if (sValue != null) {
			return sValue + "";
		}
		return SpeedFormatter.formatByteCountToKiBEtcPerSec(value * 1024,true);
	}

	private int getBaselinePos() {
		return HEIGHT - (SCALER_HEIGHT / 2);
	}

	public void addOption(String id, long value) {
		mapOptions.put(new Long(value), id);
		HEIGHT += OPTION_HEIGHT;
	}

	public long getMaxTextValue() {
		return maxTextValue;
	}

	public void setMaxTextValue(long maxTextValue) {
		this.maxTextValue = maxTextValue;
	}

	public boolean wasMenuChosen() {
		return menuChosen;
	}

	public void setMenuChosen(boolean menuChosen) {
		this.menuChosen = menuChosen;
	}
	
}
