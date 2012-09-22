/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: SessionManager.java 93 2011-05-28 07:13:21Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.jgroups.Address;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import com.ninelinelabs.server.config.ServerConfig;


/**
 * Session manager for the session information
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 93 $ $Date: 2011-05-28 02:13:21 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class SessionManager extends ReceiverAdapter {

	private static SessionManager INSTANCE = new SessionManager();
	private final Logger logger = Logger.getLogger(SessionManager.class.getName());
	private JChannel jchannel;
	private boolean slave;

	private Map<String, Map<String, java.io.Serializable>> sessions = Collections.synchronizedMap(new HashMap<String, Map<String, java.io.Serializable>>());


	// ------ JGroups support for clustering -------
	public void viewAccepted(View view) {
		System.out.println("View accepted: " + view.toString());

		Vector<Address> members = view.getMembers();

		if (members.size() == 1) {
			logger.log(Level.INFO, "Server is the only one in the cluster");
			String alone = ServerConfig.getInstance().getAlone();

			if (alone.equals("master")) {
				logger.log(Level.INFO, "Switching server to alone/master configuration");
			}
		}

		Address creator = view.getCreator();

		logger.log(Level.INFO, "Creator's address: " + creator);

		Iterator<Address> iter = members.iterator();

		while (iter.hasNext()) {
			Address address = iter.next();

			logger.log(Level.INFO, "Member address: " + address);
		}
	}

    public void receive(Message msg) {
		System.out.println("Message recieved: " + msg.getSrc() + " with payload " + msg.getObject());

		if (slave) {
			SessionUpdate update = (SessionUpdate)msg.getObject();

			if (update.remove) {
				this.removeParam(update.session, update.key);
			} else {
				this.setParam(update.session, update.key, update.value);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void setState(byte[] state) {
		try {
			Map<String, Map<String, java.io.Serializable>> map = (Map<String, Map<String, java.io.Serializable>>)Util.objectFromByteBuffer(state);

			synchronized(sessions) {
				sessions = map;
			}
			logger.log(Level.INFO, "Received cluster state transfer");
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Cluster state transfer failed", e);
		}
	}

	public byte[] getState() {
		try {
			logger.log(Level.INFO, "Received cluster state transfer request");
			synchronized(sessions) {
				return Util.objectToByteBuffer(sessions);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Getting state for cluster state transfer failed", e);
		}

		return null;
	}


	public static SessionManager getSessionManager() {
		return INSTANCE;
	}


	private SessionManager() {}

	public void start(boolean slave) {
		// this.slave = slave;

		try {

			String cluster = ServerConfig.getInstance().getCluster();
			boolean master = ServerConfig.getInstance().isMaster();

			jchannel = new JChannel();
			jchannel.setReceiver(this);
			jchannel.connect(cluster);

			this.slave = !master;

			if (slave) {
				logger.log(Level.INFO, "Starting server is slave configuration; requesting state transfer from the master");
				jchannel.getState(null, 10000);
			}

		} catch (ChannelException e) {
			logger.log(Level.SEVERE, "Can not connect to the cluster; clustering disabled", e);
		}
	}

	private void sendMessage(java.io.Serializable value) {
		try {
			Message msg = new Message(null, null, value);
			jchannel.send(msg);
			logger.log(Level.INFO, "Sent session state update message to the cluster; content = " + value);
		} catch(NullPointerException e) {
			// do nothing; JGroups have not initialized, most likely
		} catch(Exception e) {
			logger.log(Level.INFO, "Exception when sending message to the cluster", e);
		}
	}


	public void setParamNoClusterUpdate(String session, String key, Serializable value) {

		logger.log(Level.INFO, "Setting param: session = " + session + " ; key = " + key + " ; value = " + value);

		// get should not hurt anybody; besides we are the only thread that is accessing this particular session
		// FIXME: we might want to store reference to this session in the ConnectionRunnable instance for the duration of the session
		Map<String, Serializable> params = sessions.get(session);

		// if no session information found, create it
		if (params == null) {
			params = Collections.synchronizedMap(new HashMap<String, java.io.Serializable>());
			sessions.put(session, params);
		}

		params.put(key, value);
	}


	public void setParam(String session, String key, java.io.Serializable value) {

		logger.log(Level.INFO, "Setting param: session = " + session + " ; key = " + key + " ; value = " + value);

		// get should not hurt anybody; besides we are the only thread that is accessing this particular session
		// FIXME: we might want to store reference to this session in the ConnectionRunnable instance for the duration of the session
		Map<String, Serializable> params = sessions.get(session);

		// if no session information found, create it
		if (params == null) {
			params = Collections.synchronizedMap(new HashMap<String, java.io.Serializable>());
			sessions.put(session, params);
		}

		// if this server is not slave, send updates to the cluster
		if (!slave) {
			SessionUpdate update = new SessionUpdate();
			update.session = session;
			update.key = key;
			update.value = value;

			// FIXME: do we really need to send this one if we are the only one in the cluster?
			sendMessage(update);
		}

		params.put(key, value);
	}


	public void removeParam(String session, String key) {
		logger.log(Level.INFO, "Removing param: session = " + session + " ; key = " + key );

		synchronized(sessions) {

			Map <String, java.io.Serializable> params = sessions.get(session);

			if (params == null) {
			} else {
				synchronized(params) {
					params.remove(key);
				}
			}

			if (!slave) {
				SessionUpdate update = new SessionUpdate();
				update.session = session;
				update.key = key;
				update.remove = true;

				sendMessage(update);
			}
		}
	}


	public java.io.Serializable getParam(String session, String key) {
		synchronized(sessions) {

			Map<String, java.io.Serializable> params = sessions.get(session);

			if (params == null) {
				params = Collections.synchronizedMap(new HashMap<String, java.io.Serializable>());
				sessions.put(session, params);
			}

			synchronized(params) {
				return params.get(key);
			}
		}
	}
}
