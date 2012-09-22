/*
 * Copyright (C) 2008-2011, Nine Line Labs LLC
 *
 * The program(s) herein may be used and/or copied only with
 * the written permission of Nine Line Labs LLC or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * $Id: LongTicketVO.java $
 *
 * Date Author Changes
 * Oct 12, 2008 mloukianov Created
 * Feb 05, 2009 mloukianov Updated copyright statement in header
 *
 */
package com.ninelinelabs.lottery.generator.vo;

import java.io.*;

/**
 * Class representing a lottery ticket.
 *
 * @author <a href="mailto:max.loukianov@gmail.com">Max Loukianov</a>
 * @version $Revision: 54 $ $Date: 2011-05-19 20:12:21 -0500 (Thu, 19 May 2011) $
 * @see
 */
public class LongTicketVO implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -5625265253210320525L;

	private LongTicketSpinVO[] spins;

	private int pointer = 0;
	private int remainingDoubleUp = 0;
	private boolean isBonus = false;

	String ticketno = null;

	private int size;

	public static LongTicketVO readTicket(DataInputStream dis) throws IOException {
		// in a ticket first we get the integer containing the overall number of plays in the ticket
		int numplays = dis.readInt();

		LongTicketVO vo = new LongTicketVO(numplays);

		for (int i = 0; i < numplays; i++) {
			vo.setSpin(i, vo.readTicketSpin(dis));
		}

		return vo;
	}

	public LongTicketVO(int size) {
		this.size = size;
		spins = new LongTicketSpinVO[size];
	}

	public LongTicketVO setTicketNo(String no) {
		this.ticketno = no;
		return this;
	}

	public String getTicketNo() {
		return this.ticketno;
	}
	public int getPointer() {
		return this.pointer;
	}

	public void movePointer() {
		pointer++;
	}

	public boolean isBonus() {
		return this.isBonus;
	}

	public void setBonus(boolean bonus) {
		this.isBonus = bonus;
	}

	public int getRemainingDoubleUp() {
		return this.remainingDoubleUp;
	}

	public void useDoubleUp() {
		this.remainingDoubleUp--;
	}

	public int size() {
		return size;
	}

	public void setSpin(int step, LongTicketSpinVO spin) {
		spins[step] = spin;
	}

	public LongTicketSpinVO getSpin(int lines, int bet, int step) {
		return spins[step];
	}

	public byte[] getTicketBytes() {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(os);

			dos.writeInt(spins.length);

			for (int i = 0; i < spins.length; i++) {
				writeTicketSpin(dos, "S", spins[i]);
			}

			return os.toByteArray();
		}
		catch(Exception e) {
			System.out.println("Exception: " + e);
			e.printStackTrace();
		}

		return null;
	}

	public void writeTicketSpin(DataOutputStream dos, String type, LongTicketSpinVO spin) throws IOException {
		dos.writeUTF(type);
		writeUnsignedByteArray(dos, spin.getStops());
		writeUnsignedByteArray(dos, spin.getLines());
		dos.writeShort(spin.getScatter());
		dos.writeByte(spin.getDoubleup());
		dos.writeShort(spin.getBonuses());
		if (spin.getBonuses() > 0 ) {
			for (int i = 0; i < spin.getBonuses(); i++) {
				writeTicketSpin(dos, "B", spin.getBonusSpins()[i]);
			}
		}
	}

	public void writeShortArray(DataOutputStream dos, int[] array) throws IOException {
		dos.writeShort(array.length);
		for (int i = 0 ; i < array.length; i++) {
			dos.writeShort(array[i]);
		}
	}

	public void writeUnsignedByteArray(DataOutputStream dos, int[] array) throws IOException {
		dos.writeShort(array.length);
		for (int i = 0 ; i < array.length; i++) {
			dos.writeByte(array[i]);
		}
	}

	/*
	public void readTicket(DataInputStream dis) throws IOException {
		int length = 100;

		for (int i = 0; i < length; i++) {
			setSpin(i, readTicketSpin(dis));
		}
	}
	*/

	public LongTicketSpinVO readTicketSpin(DataInputStream dis) throws IOException {
		LongTicketSpinVO vo = null;

		@SuppressWarnings("unused")
		String type = dis.readUTF();
		int[] stops = readUnsignedByteArray(dis);
		int[] lines = readUnsignedByteArray(dis);
		int scatter = dis.readShort();
		int doubleup = dis.readUnsignedByte();
		int bonuses = dis.readShort();

		vo = new LongTicketSpinVO(stops, lines, scatter, doubleup, bonuses);

		if (bonuses > 0) {
			LongTicketSpinVO[] bonusSpins = new LongTicketSpinVO[bonuses];
			for (int i = 0; i < bonuses; i++) {
				bonusSpins[i] = readTicketSpin(dis);
			}
			vo.setLongTicketSpins(bonusSpins);
		}

		return vo;
	}

	public int[] readShortArray(DataInputStream dis) throws IOException {
		int length = dis.readShort();
		int[] arr = new int[length];

		for (int i = 0; i < length; i ++) {
			arr[i] = dis.readShort();
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
}
