/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: $
 *
 * Date Author Changes
 * Oct 15, 2008 mloukianov Created
 * Jul 14, 2011 mloukianov Changed field for win per line from short to int (some wins are greater then 25,535)
 *
 */
package com.ninelinelabs.lottery.generator.vo.bor;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A lottery ticket
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: $ $Date: $
 * @see
 */
public class BorLongTicketVO implements Serializable {

	private static final long serialVersionUID = -8546583570710039004L;

	private static final Logger logger = Logger.getLogger(BorLongTicketVO.class.getName());

	public static final String REGULAR = "S";
	public static final String BONUS   = "B";

	private BorLongTicketSpinVO[] spins;

	private int size = 0;
	private int pointer = 0;
	private int remainingDoubleUp = 0;
	private boolean isBonus = false;

	String ticketno = null;
	String faketicketno = null;

	private int percentage;

	private int bonusPointer = 0;

	public BorLongTicketVO() {}


	public static BorLongTicketVO readTicket(DataInputStream dis) throws IOException {

		int size = dis.readInt();

		BorLongTicketVO vo = new BorLongTicketVO(size);

		for (int i = 0; i < size; i++) {
			vo.setSpin(i, vo.readTicketSpin(dis));
		}

		return vo;
	}


	public BorLongTicketVO(int size) {
		this.size = size;
		spins = new BorLongTicketSpinVO[size];
	}


	public void setTicketNo(String ticketno) {
		this.ticketno = ticketno;
	}


	public String getTicketNo() {
		return this.ticketno;
	}

	public synchronized int getPointer() {
		return pointer;
	}


	public synchronized void movePointer() {
		pointer++;
	}


	public boolean isBonus() {
		return isBonus;
	}


	public void setBonus(boolean bonus) {
		isBonus = bonus;
	}


	public synchronized int getRemainingDoubleUp() {
		return remainingDoubleUp;
	}


	public synchronized void useDoubleUp() {
		remainingDoubleUp--;
	}


	public int size() {
		return size;
	}


	public void setSpin(int step, BorLongTicketSpinVO spin) {
		spins[step] = spin;
	}


	public BorLongTicketSpinVO getSpin(int lines, int bet, int step) {
		return spins[step];
	}


	public byte[] getTicketBytes() {

		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(os);

			dos.writeInt(spins.length);

			for (int i = 0; i < spins.length; i++) {
				writeTicketSpin(dos, BorLongTicketVO.REGULAR, spins[i]);
			}

			return os.toByteArray();
		}
		catch(Exception e) {
			logger.log(Level.SEVERE, "Can not get ticket bytes for " + ticketno, e);
		}

		return null;
	}


	public void writeTicketSpin(DataOutputStream dos, String type, BorLongTicketSpinVO spin) throws IOException {

		dos.writeUTF(type);
		writeUnsignedByteArray(dos, spin.getStops());
		writeIntArray(dos, spin.getLines());
		dos.writeShort(spin.getScatter());
		dos.writeByte(spin.getDoubleup());
		dos.writeShort(spin.getSpecialSymbol());
		dos.writeShort(spin.getSpecialSymbolWin());
		dos.writeShort(spin.getCurrentFreeGamesCount());
		dos.writeShort(spin.getFreeGamesNumber());
		dos.writeShort(spin.getReelsetno());
		writeShortArray(dos, spin.getBonuses());

		writeTicketSpin1(dos, BorLongTicketVO.BONUS, spin);
	}


	private void writeTicketSpin1(DataOutputStream dos, String type, BorLongTicketSpinVO spin) throws IOException {

		if (spin.getFreeGamesNumber() > 0) {

			for (BorLongTicketSpinVO bonusSpin : spin.getBonusSpins()) {

				dos.writeUTF(type);
				writeUnsignedByteArray(dos, bonusSpin.getStops());
				writeIntArray(dos, bonusSpin.getLines());
				dos.writeShort(bonusSpin.getScatter());
				dos.writeByte(bonusSpin.getDoubleup());
				dos.writeShort(bonusSpin.getSpecialSymbol());
				dos.writeShort(bonusSpin.getSpecialSymbolWin());
				dos.writeShort(bonusSpin.getCurrentFreeGamesCount());
				dos.writeShort(bonusSpin.getFreeGamesNumber());
				dos.writeShort(bonusSpin.getReelsetno());

				writeShortArray(dos, bonusSpin.getBonuses());

			}
		}
	}


	public void writeShortArray(DataOutputStream dos, int[] array) throws IOException {

		dos.writeShort(array.length);
		for (int i = 0 ; i < array.length; i++) {
			dos.writeShort(array[i]);
		}
	}


	public void writeIntArray(DataOutputStream dos, int[] array) throws IOException {

		dos.writeShort(array.length);
		for (int i = 0 ; i < array.length; i++) {
			dos.writeInt(array[i]);
		}
	}


	public void writeUnsignedByteArray(DataOutputStream dos, int[] array) throws IOException {

		dos.writeShort(array.length);
		for (int i = 0 ; i < array.length; i++) {
			dos.writeByte(array[i]);
		}
	}


	public BorLongTicketSpinVO readTicketSpin(DataInputStream dis) throws IOException {

		BorLongTicketSpinVO vo = null;

		@SuppressWarnings("unused")

		String type = dis.readUTF();

		int[] stops = readUnsignedByteArray(dis);
		int[] lines = readIntArray(dis);
		int scatter = dis.readShort();
		int doubleup = dis.readUnsignedByte();
		int specialSymbol = dis.readShort();
		int specialSymbolWin = dis.readShort();
		int currentFreeGamesCount = dis.readShort();
		int freeGamesNumber = dis.readShort();
		int reelsetno = dis.readShort();

		int[] bonuses = readShortArray(dis);

		vo = new BorLongTicketSpinVO(stops, lines, scatter, doubleup, specialSymbol, specialSymbolWin, currentFreeGamesCount, freeGamesNumber, reelsetno, bonuses);

		readTicketSpin1(dis, vo);

		return vo;
	}


	private void readTicketSpin1(DataInputStream dis, BorLongTicketSpinVO vo) throws IOException {

		if (vo.getFreeGamesNumber() > 0) {
			// sentinel condition
			int bonusCurrentFreeGamesCount = 0;
			int bonusFreeGamesNumber = 0;

			// bonus games container
			ArrayList<BorLongTicketSpinVO> bonusSpins = new ArrayList<BorLongTicketSpinVO>();

			do {
				@SuppressWarnings("unused")
				String bonusType = dis.readUTF();

				int[] bonusStops = readUnsignedByteArray(dis);
				int[] bonusLines = readIntArray(dis);
				int bonusScatter = dis.readShort();
				int bonusDoubleup = dis.readUnsignedByte();
				int bonusSpecialSymbol = dis.readShort();
				int bonusSpecialSymbolWin = dis.readShort();
				bonusCurrentFreeGamesCount = dis.readShort();
				bonusFreeGamesNumber = dis.readShort();
				int reelsetno = dis.readShort();

				int[] bonuses = readShortArray(dis);

				BorLongTicketSpinVO bonusVO = new BorLongTicketSpinVO(bonusStops, bonusLines, bonusScatter, bonusDoubleup, bonusSpecialSymbol, bonusSpecialSymbolWin, bonusCurrentFreeGamesCount, bonusFreeGamesNumber, reelsetno, bonuses);

				bonusSpins.add(bonusVO);

			} while (bonusCurrentFreeGamesCount != bonusFreeGamesNumber);

			vo.setBonusSpins(bonusSpins.toArray(new BorLongTicketSpinVO[]{}));
		}
	}


	public int[] readShortArray(DataInputStream dis) throws IOException {
		int length = dis.readShort();
		int[] arr = new int[length];

		for (int i = 0; i < length; i ++) {
			arr[i] = dis.readShort();
		}

		return arr;
	}


	public int[] readIntArray(DataInputStream dis) throws IOException {
		int length = dis.readShort();
		int[] arr = new int[length];

		for (int i = 0; i < length; i ++) {
			arr[i] = dis.readInt();
		}

		return arr;
	}


	public int[] readUnsignedByteArray(DataInputStream dis) throws IOException {
		int length = dis.readShort();
		int[] arr = new int[length];

		for (int i = 0; i < length; i ++) {
			arr[i] = dis.readUnsignedByte();
		}

		return arr;
	}

    public synchronized int getBonusPointer() {
    	return this.bonusPointer;
    }


    public synchronized void moveBonusPointer() {
    	this.bonusPointer++;
    }


    public synchronized void resetBonusPointer() {
    	this.bonusPointer = 0;
    }


	public void setFakeTicketNo(String faketicket) {
		this.faketicketno = faketicket;
	}

	public String getFakeTicketNo() {
		return this.faketicketno;
	}

	public void setPercentage(int percentage) {
		this.percentage = percentage;
	}

	public int getPercentage() {
		return this.percentage;
	}
}
