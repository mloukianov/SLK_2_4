/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: GameServiceImpl.java 119 2011-05-28 07:40:43Z max.loukianov@gmail.com $
 *
 * Date Author Changes
 * Mar 18, 2009 mloukianov Created
 *
 */
package com.ninelinelabs.services;

import com.ninelinelabs.game.slots.vo.GameResult;

/**
 * A class representing
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
public class GameServiceImpl implements GameService {
	private final String gameid;
	private final String gametype;
	private final String name;
	private final int denom;


	public GameServiceImpl(String gameid, String gametype, String name, int denom) {
		this.gameid = gameid;
		this.gametype = gametype;
		this.name = name;
		this.denom = denom;
	}

	public String getId() {
		return this.gameid;
	}

	public String getType() {
		return this.gametype;
	}

	public String getName() {
		return this.name;
	}

	public int getDenom() {
		return this.denom;
	}


	/* (non-Javadoc)
	 * @see com.ninelinelabs.services.GameService#play(java.lang.String, int, int[])
	 */
	public GameResult play(String gameId, int bet, int[] lines) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void setRandomSource(RandomSource source) {
		
	}

}
