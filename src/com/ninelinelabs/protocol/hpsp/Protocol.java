/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: Protocol.java $
 *
 * Date Author Changes
 * Mar 26, 2009 mloukianov Created
 * Jul 28, 2009 mloukianov Added terminal BNA count messages
 * Apr 25, 2011 mloukianov Added PARM_* messages
 *
 */
package com.ninelinelabs.protocol.hpsp;

/**
 * Interface containing HPSP protocol constants
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 59 $ $Date: 2011-05-19 20:21:44 -0500 (Thu, 19 May 2011) $
 * @see
 */
public interface Protocol {

	public static final String CONN_REQ = "CONN_REQ";	// Connection request
	public static final String CONN_RES = "CONN_RES";	// Connection response
	public static final String CONN_ERR = "CONN_ERR";	// Connection error response

	public static final String RECN_REQ = "RECN_REQ";	// Reconnect request
	public static final String RECN_RES = "RECN_RES";	// Reconnect response
	public static final String RECN_ERR = "RECN_ERR";	// Reconnect error response

	public static final String PLAY_REQ = "PLAY_REQ";	// Play request
	public static final String PLAY_RES = "PLAY_RES";	// Play response

	public static final String AUTH_REQ = "AUTH_REQ";	// Authentication request
	public static final String AUTH_RES = "AUTH_RES";	// Authentication response

	public static final String CASH_REQ = "CASH_REQ";	// Deposit escrow win to credits request
	public static final String CASH_RES = "CASH_RES";	// Deposit escrow win to credits response

	public static final String DEPO_REQ = "DEPO_REQ";	// BNA cash deposit request
	public static final String DEPO_RES = "DEPO_RES";	// BNA cash deposit response
	public static final String DEPO_ERR = "DEPO_ERR";	// BNA cash deposit - wrong banknote denomination

	public static final String COUT_REQ = "COUT_REQ";	// Cash out request
	public static final String COUT_RES = "COUT_RES";	// Cash out response

	public static final String UMSG_RES = "UMSG_RES";	// Unknown message response

	public static final String DUBL_REQ = "DUBL_REQ";	// Double-up play request
	public static final String DUBL_RES = "DUBL_RES";	// Double-up play response

	public static final String BNUS_REQ = "BNUS_REQ";	// Bonus request
	public static final String BNUS_RES = "BNUS_RES";	// Bonus response

	public static final String TIKT_REQ = "TIKT_REQ";	// Buy ticket request
	public static final String TIKT_RES = "TIKT_RES";	// Buy ticket response
	public static final String TIKT_END = "TIKT_END";	// ????
	public static final String TIKT_ERR = "TIKT_ERR";	// Error message sent back when this paper ticket number is marked as previously sold

	public static final String EXIT_REQ = "EXIT_REQ";	// Exit game request
	public static final String EXIT_RES = "EXIT_RES";	// Exit game response

	public static final String PING_REQ = "PING_REQ";	// Ping request
	public static final String PING_RES = "PING_RES";	// Ping response

	public static final String TCRD_RES = "TCRD_RES";	// Tech card response (paired with AUTH_REQ)

	public static final String NFND_RES = "NFND_RES";	// Card not found response (paired with AUTH_REQ)

	public static final String TLOG_REQ = "TLOG_REQ";	// Log message request

	public static final String ENDL_RES = "ENDL_RES";	// ?????

	public static final String PINNO = "PINNO";			// PIN authentication
	public static final String CCARD = "CCARD";			// Credit card authentication
	public static final String SMART = "SMART";   		// Smart card authentication

	public static final String LONGTICKET      = "LONGTICKET";		// Long ticket / no bonus type
	public static final String SHORTTICKET     = "SHORTTICKET";		// Short ticket / no bonus type
	public static final String SLOT            = "SLOT";			// Slot non-lottery
	public static final String LONGTICKETBONUS = "LOGNTICKETBONUS";	// Long ticket / bonus type
	public static final String IGRASOFT        = "IGRASOFT";		// Igrasoft type

	public static final String COUNT_REQ = "COUNT_REQ";	// Terminal BNA count request
	public static final String COUNT_RES = "COUNT_RES";	// Terminal BNA count response

	public static final String ENDCT_REQ = "ENDCT_REQ";	// Terminal BNA count complete request
	public static final String ENDCT_RES = "ENDCT_RES";	// Terminal BNA count complete response

	public static final String ORIGINAL = "ORIGINAL";	// ORIGINAL presentation
	public static final String SCRATCH  = "SCRATCH";	// SCRATCH presentation
	public static final String BALLS    = "BALLS";   	// BALLS presentation
	public static final String BINGO    = "BINGO";		// BINGO presentation

	public static final String LIST_LOT = "LIST_LOT";	// List of available lotteries and denominations (ticket prices)

	public static final String REEL_REQ = "REEL_REQ";	// Request game reels
	public static final String REEL_RES = "REEL_RES";	// Response with game reels

	public static final String COMM_ERR = "COMM_ERR";	// Communication error

	public static final String ROLL_TKT = "ROLL_TKT";		// Approve paper ticket sale
	public static final String ROLL_READY = "ROLL_READY";	// Paper ticket sale approved

	public static final String ROLL_END = "ROLL_END";		// Terminal is out of paper tickets
	public static final String ROLL_ERR = "ROLL_ERR";		// Terminal encountered error processing paper roll

	public static final String ROLL_INIT = "ROLL_INIT";		// Roll inserted into the terminal (for paper tickets)
	public static final String ROLL_RES = "ROLL_RES";		// Response to roll inserted

	public static final String SHUT_RES = "SHUT_RES";		// Shutdown response sent to terminal

	public static final String BNUS_END = "BNUS_END";		// End of bonus games (Sharky, etc)

	public static final String CARD_REQ = "CARD_REQ";		// Dealer card request; used in Igrasoft
	public static final String CARD_RES = "CARD_RES";		// Dealer card response; used in Igrasoft

	public static final String PARM_PUT = "PARM_PUT";		// Save parameter request
	public static final String PARM_GET = "PARM_GET";		// Retrieve parameter request
	public static final String PARM_RES = "PARM_RES";		// Retrieve parameter response
}
