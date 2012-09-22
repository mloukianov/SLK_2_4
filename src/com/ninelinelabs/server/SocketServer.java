/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: SocketServer.java 97 2011-05-28 07:15:24Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Dec 27, 2008 mloukianov Created
 * Jan 18, 2009 mloukianov Fixed JavaDoc to the common standard
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 */
package com.ninelinelabs.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import com.ninelinelabs.server.config.ServerConfig;
import com.ninelinelabs.server.startup.CleanupAgent;

/**
 * An class representing socket server for lottery server infrastructure.
 * Implementation of the protocol adapter for TCP/IP transport.
 *
 * For example:
 * <pre>
 *	try {
 *		SocketServer server = new SocketServer("standalone.server.8080", 9111, 50);
 *		server.start();
 *	} catch (IOException ioe) {
 *		logger.log(Level.SEVERE, "IOException thrown when starting up SocketServer: " + ioe);
 *	}
 * </pre>
 *
 * @author <a href="mailto:mloukianov@austin.rr.com">Max Loukianov</a>
 * @version $Revision: 97 $ $Date: 2011-05-28 02:15:24 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class SocketServer {
	private Thread _thread;
	private SocketServerRunnable _runnable;

	private final String _name;
	private final int _port;
	private final int _poolsize;

	@SuppressWarnings("unused")
	private static final int FLASH_PORT = 843;

	private static final Logger logger = Logger.getLogger(SocketServer.class.getName());

	/**
	 * Main entry point for instantiating SocketServer instance
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ServerConfig.loadConfig("");
			SocketServer server = new SocketServer("standalone.server.8080", 9111, 400);
			server.start();
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, "Unable to start socket server", ioe);
		}
	}


	/**
	 * Public contructor for SocketServer.
	 *
	 * @param name  server name
	 * @param port  port number the server is listening on
	 * @param poolsize  pool size for the thread pool
	 *
	 * @throws IOException
	 */
	public SocketServer(final String name, final int port, final int poolsize) {
		_poolsize = poolsize;

		String ver = SocketServer.class.getPackage().getImplementationVersion();

		System.out.println("Starting GameApplicationServer version " + ver);

		ServerConfig config = ServerConfig.loadConfig("");

		String logLevel = config.getLevel();

		Logger rootLogger = Logger.getLogger("com.ninelinelabs");
		@SuppressWarnings("unused")
		Logger apacheLogger = Logger.getLogger("org.apache");

		if (logLevel.equals("SEVERE")) {
			rootLogger.setLevel(Level.SEVERE);
			//apacheLogger.setLevel(Level.SEVERE);
		} if (logLevel.equals("ALL")) {
			rootLogger.setLevel(Level.ALL);
			//apacheLogger.setLevel(Level.ALL);
		} if (logLevel.equals("INFO")) {
			rootLogger.setLevel(Level.INFO);
			//apacheLogger.setLevel(Level.INFO);
		} if (logLevel.equals("OFF")) {
			rootLogger.setLevel(Level.OFF);
			//apacheLogger.setLevel(Level.OFF);
		} if (logLevel.equals("WARNING")) {
			rootLogger.setLevel(Level.WARNING);
			//apacheLogger.setLevel(Level.WARNING);
		} if (logLevel.equals("FINE")) {
			rootLogger.setLevel(Level.FINE);
			//apacheLogger.setLevel(Level.FINE);
		} if (logLevel.equals("FINER")) {
			rootLogger.setLevel(Level.FINER);
			//apacheLogger.setLevel(Level.FINER);
		}

		System.out.println("---------------------------------------");
		System.out.println("  Configuring logging level to " + logLevel);
		System.out.println("---------------------------------------");

		_name = ServerConfig.getInstance().getName();
		_port = ServerConfig.getInstance().getPort();
	}

	public String getName() {
		return _name;
	}

	public int getPort() {
		return _port;
	}

	public int getPoolsize() {
		return _poolsize;
	}

	public void init() {

		try {
			GameRegistry.getInstance().init();
			CleanupAgent cleanup = new CleanupAgent();
			cleanup.cleanup();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Exception performing server cleanup", e);
		}
	}

	public void start() throws IOException {
		try {
			logger.log(Level.INFO, "Starting " + ServerConfig.getInstance().getProduct() + " ver. " + ServerConfig.getInstance().getVersion() +
															" (build " + ServerConfig.getInstance().getBuild() + ") on port " + _port);
			_runnable = new SocketServerRunnable(_port, _poolsize, this);
			SessionManager sessionmanager = SessionManager.getSessionManager();
			sessionmanager.start(false);
			_thread = new Thread(_runnable);
			_thread.start();
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, "IOException when starting listener for the socket server", ioe);
			throw ioe;
		}
	}

	public void stop() {
		try {
			logger.log(Level.INFO, "Stopping socket server...");
			_runnable.stop();
			while (!_runnable.isTerminated()) {
				Thread.sleep(100);
			}
		} catch (SecurityException se) {
			System.out.println("SecurityException caught at stopping socket server: " + se);
			se.printStackTrace();
		} catch(InterruptedException ie) {
			System.out.println("InterruptedException caught while shutting down socket server: " + ie);
			ie.printStackTrace();
		} finally {
			_thread.interrupt();
		}
		logger.log(Level.INFO, "Socket server stopped");
	}
}


class SocketServerPort843Runnable implements Runnable {

	private ServerSocket listener;
	private ExecutorService pool;

	private final static Logger logger = Logger.getLogger(SocketServerPort843Runnable.class.getName());

	public SocketServerPort843Runnable(int port) {
		try {
			this.listener = new ServerSocket(port);
			this.pool = Executors.newFixedThreadPool(100);
			logger.log(Level.INFO, "Started listening on port 843");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Can not start listening on port 843", e);
		}
	}

	public void run() {
		try {
			while (true) {
				pool.execute(new FlashPolicyFileProtocol(listener.accept()));
			}
		}
		catch(IOException ioe)
		{
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
			pool.shutdown();
		}
	}

	public void stop() {
		pool.shutdownNow();
	}

	public boolean isTerminated() {
		return pool.isTerminated();
	}
}


class FlashPolicyFileProtocol implements Runnable {
	private final Socket socket;

	private final static Logger logger = Logger.getLogger(FlashPolicyFileProtocol.class.getName());

	public FlashPolicyFileProtocol(Socket socket) {
		logger.log(Level.INFO, "Accepted connection on port 843");
		this.socket = socket;
	}

	public void run() {
		try {
			DataInputStream ds = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			String policyRequestString = "";
			char nextChar = ' ';

			while (nextChar != '\0') {
				nextChar = (char)ds.read();
				policyRequestString += nextChar;
			}

			if (policyRequestString.equals("<policy-file-request/>\0")) {
				logger.log(Level.INFO, "Received policy-request from Flash player");
				String policyResponse = "";
				policyResponse += "<cross-domain-policy>";
				policyResponse += "<allow-access-from domain=\"*\" to-ports=\"*\"/>";
				policyResponse += "</cross-domain-policy>\0";

				byte[] b = policyResponse.getBytes();

				dos.write(b);
				dos.flush();
			}
		} catch(IOException e) {
			logger.log(Level.INFO, "Error sending policy file back to FLash Player", e);
		}
	}
}



class SocketServerRunnable implements Runnable {
	private final ServerSocket listener;
	private final ExecutorService pool;

	private static final Logger logger = Logger.getLogger(SocketServerRunnable.class.getName());

	@SuppressWarnings("unused")
	private SocketServer _parent;
	private final int _port;
	private final int _poolsize;

	public SocketServerRunnable(int port, int poolsize, SocketServer parent) throws IOException {
		_parent = parent;
		_port = port;
		_poolsize = poolsize;

		if (ServerConfig.getInstance().isSsl()) {
			logger.log(Level.INFO, "Creating SSL server socket on port " + _port);
			ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
			listener = ssocketFactory.createServerSocket(_port);
			logger.log(Level.INFO, "Created SSL server socket on port " + _port);
		} else {
			logger.log(Level.INFO, "Creating server socket on port " + _port);
			listener = new ServerSocket(_port);
		}
		pool = Executors.newFixedThreadPool(_poolsize);
	}

	public void run() {
		try {
			while (true) {
				pool.execute(new ConnectionRunnable(listener.accept()));
				logger.log(Level.FINE, "Socket server accepted connection on port " + _port);
			}
		}
		catch(IOException ioe)
		{
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
			pool.shutdown();
		}
	}

	public void stop() {
		pool.shutdownNow();
	}

	public boolean isTerminated() {
		return pool.isTerminated();
	}
}
