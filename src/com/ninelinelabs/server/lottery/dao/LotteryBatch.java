/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: LotteryBatch.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 07, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server.lottery.dao;

import java.io.Serializable;

/**
 * Value object class for lottery batch information in the database.
 * Lottery batch is associated with the file containing ticket information
 * stored on the file system.
 *
 * Database script:
 * <pre>
 *  CREATE TABLE TICKET_BATCH (
 *  LOTTERY_ID VARCHAR(32) PRIMARY KEY,
 *  BATCH_ID VARCHAR(32) NOT NULL,
 *  TICKET_PRICE INT NOT NULL,
 *  TICKETS_FILE VARCHAR(256) NOT NULL,
 *  LAST_TICKET VARCHAR(32),
 *  STATUS ENUM('ACTIVE', 'ON_HOLD', 'COMPLETED') NOT NULL
 *  )  ENGINE=INNODB;
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 119 $ $Date: 2011-05-28 02:40:43 -0500 (Sat, 28 May 2011) $
 * @see
 */
public class LotteryBatch implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1311281607246094012L;
	private final String lottery;
	private final String batch;
	private final int price;
	private final String file;
	private final String ticket;
	private final String status;

	public LotteryBatch(String lottery, String batch, int price, String file, String ticket, String status) {
		this.lottery = lottery;
		this.batch = batch;
		this.price = price;
		this.file = file;
		this.ticket = ticket;
		this.status = status;
	}

	public String getLottery() {
		return this.lottery;
	}

	public String getBatch() {
		return this.batch;
	}

	public int getPrice() {
		return this.price;
	}

	public String getFile() {
		return this.file;
	}

	public String getTicket() {
		return this.ticket;
	}

	public String getStatus() {
		return this.status;
	}
}
