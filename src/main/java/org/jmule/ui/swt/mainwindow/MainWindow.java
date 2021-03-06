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
package org.jmule.ui.swt.mainwindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jmule.core.*;
import org.jmule.core.servermanager.ServerManagerException;
import org.jmule.ui.JMuleUIComponent;
import org.jmule.ui.JMuleUIManager;
import org.jmule.ui.JMuleUIManagerException;
import org.jmule.ui.localizer.Localizer;
import org.jmule.ui.swt.*;
import org.jmule.ui.swt.common.NightlyBuildWarningWindow;
import org.jmule.ui.swt.maintabs.AbstractTab;
import org.jmule.ui.swt.maintabs.AbstractTab.JMULE_TABS;
import org.jmule.ui.swt.maintabs.kad.KadTab;
import org.jmule.ui.swt.maintabs.logs.LogsTab;
import org.jmule.ui.swt.maintabs.search.SearchTab;
import org.jmule.ui.swt.maintabs.serverlist.ServerListTab;
import org.jmule.ui.swt.maintabs.shared.SharedTab;
import org.jmule.ui.swt.maintabs.statistics.StatisticsTab;
import org.jmule.ui.swt.maintabs.transfers.TransfersTab;
import org.jmule.ui.swt.updaterwindow.UpdaterWindow;
import org.jmule.ui.utils.FileFormatter;
import org.jmule.updater.JMUpdater;
import org.jmule.updater.JMUpdaterException;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author binary256
 * @version $$Revision: 1.11 $$
 * Last changed by $$Author: javajox $$ on $$Date: 2010/01/07 15:25:30 $$
 */
public class MainWindow implements JMuleUIComponent {

	private List<AbstractTab> tab_list = new LinkedList<AbstractTab>();
	private ScrolledComposite window_content;
	private Shell shell;
	
	private JMuleCore _core;
	private Toolbar toolbar;
	private MainMenu main_menu;
	private StatusBar status_bar;
	
	private static Logger logger;
	
	public MainWindow() {	
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
	public void initUIComponents() {
		final Display display = SWTThread.getDisplay();
		final MainWindow instance = this;
		display.asyncExec(new JMRunnable() {
            public void JMRun() {
		
            	shell = new Shell(display);
            	
				shell.setSize(800, 500); 	
            	Utils.centreWindow(shell);
            	
            	shell.setText(JMConstants.JMULE_FULL_NAME);
            	shell.setImage(SWTImageRepository.getImage("jmule.png"));
            	
            	GridLayout gridLayout = new GridLayout(1,true);
            	gridLayout.marginHeight = 2;
            	gridLayout.marginWidth = 2;
            	shell.setLayout(gridLayout);
            	//Setup main_menu
            	main_menu = new MainMenu(shell,instance);
            	//Setup tool bar
            	toolbar = new Toolbar(shell,_core,instance);
		
            	window_content = new ScrolledComposite(shell,SWT.NONE);
            	window_content.setExpandHorizontal(true);
            	window_content.setExpandVertical(true);
            	window_content.setLayout(new FillLayout());
            	GridData gridData = new GridData(GridData.FILL_BOTH);
            	window_content.setLayoutData(gridData);
		
            	new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            	
            	status_bar = new StatusBar(shell,_core);
            	
            	LogsTab logs_tab = new LogsTab(window_content);
            	logger = logs_tab;
            	tab_list.add(logs_tab);
            	tab_list.add(new ServerListTab(window_content,_core));
            	tab_list.add(new KadTab(window_content,_core));
            	tab_list.add(new TransfersTab(window_content,_core));
            	tab_list.add(new SearchTab(window_content,_core));
            	tab_list.add(new SharedTab(window_content,_core));
            	tab_list.add(new StatisticsTab(window_content));
		
            	setTab(AbstractTab.JMULE_TABS.SERVERLIST);
            	
            	if (!SWTPreferences.getInstance().isStatusBarVisible())
        			statusBarToogleVisibility();
            	
        		if (!SWTPreferences.getInstance().isToolBarVisible())
        			toolbarToogleVisibility();

            	shell.open ();

            	if (SWTPreferences.getInstance().isConnectAtStartup()) {
	            	try {
						_core.getServerManager().connect();
					} catch (ServerManagerException e1) {
						e1.printStackTrace();
					}	
            	}
            	
            	if (JMConstants.IS_NIGHTLY_BUILD) 
            		if (SWTPreferences.getInstance().isNightlyBuildWarning()){
            			NightlyBuildWarningWindow warning_window = new NightlyBuildWarningWindow(shell);
            			warning_window.getCoreComponents();
            			warning_window.initUIComponents();
            	}
            	
            	// show log messages
            	new JMThread(new JMRunnable() {
            		public void JMRun() {
            			logger.fine(Localizer.getString("mainwindow.logtab.message_jmule_started",  JMConstants.JMULE_FULL_NAME));
            			logger.fine(Localizer.getString("mainwindow.logtab.message_servers_loaded", _core.getServerManager().getServersCount() + ""));
            			logger.fine(Localizer.getString("mainwindow.logtab.message_shared_loaded", _core.getSharingManager().getFileCount() + ""));
            			logger.fine(Localizer.getString("mainwindow.logtab.message_partial_loaded", _core.getDownloadManager().getDownloadCount() + ""));
            		}}).start();
            	
            	_core.addEventListener(new JMuleCoreEventListener() {
					public void eventOccured(JMuleCoreEvent event, final EventDescriptor eventDescriptor) {
						if( event == JMuleCoreEvent.NOT_ENOUGH_SPACE ) {
							display.syncExec(new JMRunnable() {
								NotEnoughSpaceDownloadingFile nes = (NotEnoughSpaceDownloadingFile)eventDescriptor;
								public void JMRun() {
									Utils.showErrorMessage(shell, 
											Localizer.getString("mainwindow.not_enough_space_dialog.title"),
											Localizer.getString("mainwindow.not_enough_space_dialog.message",
													nes.getFileName(),
													FileFormatter.formatFileSize( nes.getTotalSpace() ),
													FileFormatter.formatFileSize( nes.getFreeSpace() )));
								}
							});
						}
					}
            	});
            	// Update checker
            	if (SWTPreferences.getInstance().updateCheckAtStartup()) {
            		new JMThread(new JMRunnable() {
            			public void JMRun() {
            				try {
								JMUpdater.getInstance().checkForUpdates();
								if (JMUpdater.getInstance().isNewVersionAvailable())
									SWTThread.getDisplay().asyncExec(new JMRunnable() {
				            			public void JMRun() {
				            				if (JMUpdater.getInstance().isNewVersionAvailable()) {
												if (Utils.showConfirmMessage(shell, Localizer.getString("mainwindow.new_version_available.title"), Localizer.getString("mainwindow.new_version_available"))) {
													UpdaterWindow window = new UpdaterWindow();
													window.getCoreComponents();
													window.initUIComponents();
												} }
				            		}});
							} catch (JMUpdaterException e) {}
            			}
            		}).start();
            	}
            	shell.addListener(SWT.Close, new Listener() {
					public void handleEvent(Event arg0) {
						boolean exit = SWTPreferences.getInstance().promptOnExit() ? 
								Utils.showConfirmMessage(shell, Localizer.getString("mainwindow.exit_prompt_title"), Localizer.getString("mainwindow.exit_prompt")) : true;
						arg0.doit = exit;
					}
            	});
            	shell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent arg0) {
						new JMThread(new JMRunnable() {
							public void JMRun() {
								try {
									JMuleUIManager.getSingleton().shutdown();
								} catch (JMuleUIManagerException e) {
									e.printStackTrace();
								}
							}
						}).start();
					}
            	});
            }});
		
	} 
	
	public void statusBarToogleVisibility() {
		status_bar.toogleVisibility();
		shell.layout();
	}
	
	public void toolbarToogleVisibility() {
		toolbar.toogleVisibility();
		shell.layout();
	}
	
	public Shell getShell() {
		return this.shell;
	}
	
	public void setTab(JMULE_TABS tabID) {
		toolbar.setSelection(tabID);
		if (window_content.getContent()!=null) {
		for(AbstractTab tab : tab_list) {
			if (window_content.getContent().equals(tab)) {
				tab.lostFocus();
			}
		} }
		for(AbstractTab tab : tab_list) {
			if (tab.getTabType()==tabID) {
				window_content.setContent(tab);
				tab.obtainFocus();
				main_menu.setSelectedTab(tab.getTabType());
			}
			else {
			}
		}
	}

	public void close() {
		shell.close();
	}
	
	public void getCoreComponents() {
		_core = JMuleCoreFactory.getSingleton();
	}

}
