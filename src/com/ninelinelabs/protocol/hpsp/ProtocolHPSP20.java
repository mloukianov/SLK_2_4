package com.ninelinelabs.protocol.hpsp;

public interface ProtocolHPSP20 {

	public static final String HPSP_20_CONN_REQ = "HPSP/2.0 CONN_REQ";	// Connection request
	public static final String HPSP_20_CONN_RES = "HPSP/2.0 CONN_RES";	// Connection response
	public static final String HPSP_20_CONN_ERR = "HPSP/2.0 CONN_ERR";	// Connection error response

	public static final String UNSUPPORTED = "UNSUPPORTED";		// Response sent when this protocol is not supported

	public static final String HPSP_20_RECN_REQ = "HPSP/2.0 RECN_REQ";	// Reconnect request
	public static final String HPSP_20_RECN_RES = "HPSP/2.0 RECN_RES";	// Reconnect response
	public static final String HPSP_20_RECN_ERR = "HPSP/2.0 RECN_ERR";	// Reconnect error response

	public static final String HPSP_20_PLAY_REQ = "HPSP/2.0 PLAY_REQ";	// Play request
	public static final String HPSP_20_PLAY_RES = "HPSP/2.0 PLAY_RES";	// Play response

	public static final String HPSP_20_AUTH_REQ = "HPSP/2.0 AUTH_REQ";	// Authentication request
	public static final String HPSP_20_AUTH_RES = "HPSP/2.0 AUTH_RES";	// Authentication response

	public static final String HPSP_20_CASH_REQ = "HPSP/2.0 CASH_REQ";	// Deposit escrow win to credits request
	public static final String HPSP_20_CASH_RES = "HPSP/2.0 CASH_RES";	// Deposit escrow win to credits response

	public static final String HPSP_20_DEPO_REQ = "HPSP/2.0 DEPO_REQ";	// BNA cash deposit request
	public static final String HPSP_20_DEPO_RES = "HPSP/2.0 DEPO_RES";	// BNA cash deposit response
	public static final String HPSP_20_DEPO_ERR = "HPSP/2.0 DEPO_ERR";	// BNA cash deposit - wrong banknote denomination

	public static final String HPSP_20_COUT_REQ = "HPSP/2.0 COUT_REQ";	// Cash out request
	public static final String HPSP_20_COUT_RES = "HPSP/2.0 COUT_RES";	// Cash out response

	public static final String HPSP_20_UMSG_RES = "HPSP/2.0 UMSG_RES";	// Unknown message response

	public static final String HPSP_20_DUBL_REQ = "HPSP/2.0 DUBL_REQ";	// Double-up play request
	public static final String HPSP_20_DUBL_RES = "HPSP/2.0 DUBL_RES";	// Double-up play response

	public static final String HPSP_20_BNUS_REQ = "HPSP/2.0 BNUS_REQ";	// Bonus request
	public static final String HPSP_20_BNUS_RES = "HPSP/2.0 BNUS_RES";	// Bonus response

	public static final String HPSP_20_TIKT_REQ = "HPSP/2.0 TIKT_REQ";	// Buy ticket request
	public static final String HPSP_20_TIKT_RES = "HPSP/2.0 TIKT_RES";	// Buy ticket response
	public static final String HPSP_20_TIKT_END = "HPSP/2.0 TIKT_END";	// ????
	public static final String HPSP_20_TIKT_ERR = "HPSP/2.0 TIKT_ERR";	// Error message sent back when this paper ticket number is marked as previously sold

	public static final String HPSP_20_PRNT_REQ = "HPSP/2.0 PRNT_REQ";	// Print request (new for 2.0)
	public static final String HPSP_20_PRNT_RES = "HPSP/2.0 PRNT_RES";	// Print response (new for 2.0)

	public static final String HPSP_20_PRNT_CNF = "HPSP/2.0 PRNT_CNF";	// Print confirmation (new for 2.0)

	public static final String HPSP_20_EXIT_REQ = "HPSP/2.0 EXIT_REQ";	// Exit game request
	public static final String HPSP_20_EXIT_RES = "HPSP/2.0 EXIT_RES";	// Exit game response

	public static final String HPSP_20_PING_REQ = "HPSP/2.0 PING_REQ";	// Ping request
	public static final String HPSP_20_PING_RES = "HPSP/2.0 PING_RES";	// Ping response

	public static final String HPSP_20_TCRD_RES = "HPSP/2.0 TCRD_RES";	// Tech card response (paired with AUTH_REQ)

	public static final String HPSP_20_NFND_RES = "HPSP/2.0 NFND_RES";	// Card not found response (paired with AUTH_REQ)

	public static final String HPSP_20_TLOG_REQ = "HPSP/2.0 TLOG_REQ";	// Log message request

	public static final String HPSP_20_ENDL_RES = "HPSP/2.0 ENDL_RES";	// ?????

	public static final String PINNO = "PINNO";			// PIN authentication
	public static final String CCARD = "CCARD";			// Credit card authentication
	public static final String SMART = "SMART";   		// Smart card authentication

	public static final String LONGTICKET      = "LONGTICKET";		// Long ticket / no bonus type
	public static final String SHORTTICKET     = "SHORTTICKET";		// Short ticket / no bonus type
	public static final String SLOT            = "SLOT";			// Slot non-lottery
	public static final String LONGTICKETBONUS = "LOGNTICKETBONUS";	// Long ticket / bonus type
	public static final String IGRASOFT        = "IGRASOFT";		// Igrasoft type

	public static final String HPSP_20_COUNT_REQ = "HPSP/2.0 COUNT_REQ";	// Terminal BNA count request
	public static final String HPSP_20_COUNT_RES = "HPSP/2.0 COUNT_RES";	// Terminal BNA count response

	public static final String HPSP_20_ENDCT_REQ = "HPSP/2.0 ENDCT_REQ";	// Terminal BNA count complete request
	public static final String HPSP_20_ENDCT_RES = "HPSP/2.0 ENDCT_RES";	// Terminal BNA count complete response

	public static final String ORIGINAL = "ORIGINAL";	// ORIGINAL presentation
	public static final String SCRATCH  = "SCRATCH";	// SCRATCH presentation
	public static final String BALLS    = "BALLS";   	// BALLS presentation
	public static final String BINGO    = "BINGO";		// BINGO presentation

	public static final String HPSP_20_LIST_LOT = "HPSP/2.0 LIST_LOT";	// List of available lotteries and denominations (ticket prices)

	public static final String HPSP_20_REEL_REQ = "HPSP/2.0 REEL_REQ";	// Request game reels
	public static final String HPSP_20_REEL_RES = "HPSP/2.0 REEL_RES";	// Response with game reels

	public static final String HPSP_20_COMM_ERR = "HPSP/2.0 COMM_ERR";	// Communication error

	public static final String HPSP_20_ROLL_TKT = "HPSP/2.0 ROLL_TKT";		// Approve paper ticket sale
	public static final String HPSP_20_ROLL_READY = "HPSP/2.0 ROLL_READY";	// Paper ticket sale approved

	public static final String HPSP_20_ROLL_END = "HPSP/2.0 ROLL_END";		// Terminal is out of paper tickets
	public static final String HPSP_20_ROLL_ERR = "HPSP/2.0 ROLL_ERR";		// Terminal encountered error processing paper roll

	public static final String HPSP_20_ROLL_INIT = "HPSP/2.0 ROLL_INIT";		// Roll inserted into the terminal (for paper tickets)
	public static final String HPSP_20_ROLL_RES = "HPSP/2.0 ROLL_RES";		// Response to roll inserted

	public static final String HPSP_20_SHUT_RES = "HPSP/2.0 SHUT_RES";		// Shutdown response sent to terminal

	public static final String HPSP_20_BNUS_END = "HPSP/2.0 BNUS_END";		// End of bonus games (Sharky, etc)

	public static final String HPSP_20_CARD_REQ = "HPSP/2.0 CARD_REQ";		// Dealer card request; used in Igrasoft
	public static final String HPSP_20_CARD_RES = "HPSP/2.0 CARD_RES";		// Dealer card response; used in Igrasoft

	public static final String HPSP_20_PARM_PUT = "HPSP/2.0 PARM_PUT";		// Save parameter request
	public static final String HPSP_20_PARM_GET = "HPSP/2.0 PARM_GET";		// Retrieve parameter request
	public static final String HPSP_20_PARM_RES = "HPSP/2.0 PARM_RES";		// Retrieve parameter response
}
