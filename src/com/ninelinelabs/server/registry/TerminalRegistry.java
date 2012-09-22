package com.ninelinelabs.server.registry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ninelinelabs.util.database.ConnectionDispenser;

public class TerminalRegistry {

	public static int getTerminalOperday(String terminal) throws SQLException {

		Connection conn = ConnectionDispenser.getConnection();

		PreparedStatement ps = conn.prepareStatement("SELECT OPERDAY FROM TERMINAL WHERE HALLNAME= ?");

		ps.setString(1, terminal);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {
			int operday = rs.getInt(1);
			return operday;
		}

		return -1;
	}

}
