/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: LotteryBatchDAO.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 06, 2009 mloukianov Created
 * Nov 06, 2009 mloukianov Fixed compiler warnings
 *
 */
package com.ninelinelabs.server.lottery.dao;

import java.util.ArrayList;

/**
 * Data Access Object for lottery batch information stored in the database.
 *
 * For example:
 * <pre>
 *
 * </pre>
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 119 $ $Date: 2011-05-28 02:40:43 -0500 (Sat, 28 May 2011) $
 * @see
 */
public interface LotteryBatchDAO {
	public int insertLotteryBatch(LotteryBatch batch);

	public boolean deleteLotteryBatch(String lottery, String batch);

	public LotteryBatch findLotteryBatch(String lottery, String batch);

	public LotteryBatch findByFilename(String filename);

	public int updateLotteryBatch(LotteryBatch batch);

	public int setLastTicket(String lottery, String batch, String ticket);

	public ArrayList<LotteryBatch> listByStatus(String status);

	public ArrayList<LotteryBatch> listAll();
}
