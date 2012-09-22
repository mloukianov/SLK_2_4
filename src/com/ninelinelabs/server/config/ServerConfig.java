/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: ServerConfig.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Sep 12, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.server.config;

import java.io.IOException;
import java.io.InputStream;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class ServerConfig {
	
	private static final Logger logger = Logger.getLogger(ServerConfig.class.getName());
	
	private String name;
	
	private String product;
	private String version;
	private String build;
	private String copyright;
	
	private String cluster;
	private boolean master;
	private String alone;
	
	private int port;
	private boolean ssl;
	
	private boolean so_linger;
	
	private int pingperiod;
	private int pingretries;
	
	private String[] keys;
	
	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * @param level the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	private String level;
	
	private static ServerConfig INSTANCE;
	
	public ServerConfig() {}
	
	public static ServerConfig loadConfig(String filename) {
		try {
			Digester digester = new Digester();
			
			digester.setValidating(false);
			digester.addObjectCreate("server", ServerConfig.class);

			digester.addBeanPropertySetter("server/name", "name");

			digester.addBeanPropertySetter("server/bootstrap/product", "product");
			digester.addBeanPropertySetter("server/bootstrap/version", "version");
			digester.addBeanPropertySetter("server/bootstrap/build", "build");
			digester.addBeanPropertySetter("server/bootstrap/copyright", "copyright");
			
			digester.addBeanPropertySetter("server/cluster/name", "cluster");
			digester.addBeanPropertySetter("server/cluster/master", "master");
			digester.addBeanPropertySetter("server/cluster/alone", "alone");
			
			digester.addBeanPropertySetter("server/port", "port");
			digester.addBeanPropertySetter("server/usessl", "ssl");
			
			digester.addBeanPropertySetter("server/keepalive/ping/sec", "pingperiod");
			digester.addBeanPropertySetter("server/keepalive/ping/retries", "pingretries");
			digester.addBeanPropertySetter("server/keepalive/so_linger", "so_linger");
			
			digester.addBeanPropertySetter("server/logger/level", "level");
			
			InputStream is = ServerConfig.class.getClassLoader().getResourceAsStream("server-config.xml");
			
			ServerConfig config = (ServerConfig)digester.parse(is);
			
			logger.log(Level.INFO, "Parsed config file for " + config.getProduct());
			
			INSTANCE = config;
			
			return config;
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Problem reading configuration file server-config.xml", e);
		} catch (SAXException e) {
			logger.log(Level.SEVERE, "Problem parsing configuration file server-config.xml", e);
		}
		
		return null;
	}
	
	public static ServerConfig getInstance() { return INSTANCE; }

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the product
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * @param product the product to set
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the build
	 */
	public String getBuild() {
		return build;
	}

	/**
	 * @param build the build to set
	 */
	public void setBuild(String build) {
		this.build = build;
	}

	/**
	 * @return the copyright
	 */
	public String getCopyright() {
		return copyright;
	}

	/**
	 * @param copyright the copyright to set
	 */
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	/**
	 * @return the cluster
	 */
	public String getCluster() {
		return cluster;
	}

	/**
	 * @param cluster the cluster to set
	 */
	public void setCluster(String cluster) {
		this.cluster = cluster;
	}

	/**
	 * @return the master
	 */
	public boolean isMaster() {
		return master;
	}

	/**
	 * @param master the master to set
	 */
	public void setMaster(boolean master) {
		this.master = master;
	}

	/**
	 * @return the alone
	 */
	public String getAlone() {
		return alone;
	}

	/**
	 * @param alone the alone to set
	 */
	public void setAlone(String alone) {
		this.alone = alone;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the ssl
	 */
	public boolean isSsl() {
		return ssl;
	}

	/**
	 * @param ssl the ssl to set
	 */
	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	/**
	 * @return the so_linger
	 */
	public boolean isSo_linger() {
		return so_linger;
	}

	/**
	 * @param so_linger the so_linger to set
	 */
	public void setSo_linger(boolean so_linger) {
		this.so_linger = so_linger;
	}

	/**
	 * @return the pingperiod
	 */
	public int getPingperiod() {
		return pingperiod;
	}

	/**
	 * @param pingperiod the pingperiod to set
	 */
	public void setPingperiod(int pingperiod) {
		this.pingperiod = pingperiod;
	}

	/**
	 * @return the pingretries
	 */
	public int getPingretries() {
		return pingretries;
	}

	/**
	 * @param pingretries the pingretries to set
	 */
	public void setPingretries(int pingretries) {
		this.pingretries = pingretries;
	}

	/**
	 * @return the keys
	 */
	public String[] getKeys() {
		return keys;
	}
}
