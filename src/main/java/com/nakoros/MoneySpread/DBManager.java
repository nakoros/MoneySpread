package com.nakoros.MoneySpread;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Vector;

public class DBManager {

	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver"; // 드라이버
	private static final String DB_URL = "jdbc:mysql://localhost/develop?&useSSL=false&serverTimezone=UTC"; // 접속할 DB 서버

	private static final String USER_NAME = "root"; // DB에 접속할 사용자 이름을 상수로 정의
	private static final String PASSWORD = "root123"; // 사용자의 비밀번호를 상수로 정의
	private static DBManager instance=null;
	public static DBManager getInstance() {
		if(instance==null) {
			instance=new DBManager();
		}
		return instance;
	}
	public DBManager() {
	}
	public DBManager(DBManager obj) {
		instance=obj;
	}

	public boolean executeUpdate(String query) {
		Connection conn = null;
		Statement state = null;
		int res=0;
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD);
			state = conn.createStatement();
			String sql; // SQL문을 저장할 String

			res = state.executeUpdate(query); // SQL문을 전달하여 실행

			state.close();
			conn.close();
		} catch (SQLIntegrityConstraintViolationException e) {

		} catch (Exception e) {
			// 예외 발생 시 처리부분
			e.printStackTrace();
			return false;
		} finally { // 예외가 있든 없든 무조건 실행
			try {
				if (state != null)
					state.close();
			} catch (SQLException ex1) {
				ex1.printStackTrace();
			}

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException ex1) {
				ex1.printStackTrace();
			}
		}
		if(res==0) {
			return false;
		}
		return true;

	}

	public  SQLResult executeQuery(String query) {
		return executeQuery(query, false);
	}

	public SQLResult executeQuery(String query, boolean printResult) {
		SQLResult res = new SQLResult();
		Connection conn = null;
		Statement state = null;
		ResultSet rs = null;
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD);
			state = conn.createStatement();
			String sql; // SQL문을 저장할 String
//			sql = "SELECT * FROM t_test where token='bbb'";
			rs = state.executeQuery(query); // SQL문을 전달하여 실행

			String space = "    ";
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();

			String[] columnNames = new String[columnCount];

			for (int i = 1; i <= columnCount; i++) {
				columnNames[i - 1] = rsmd.getColumnName(i);
			}

			String str = "";
			
			while (rs.next()) {
				res.count++;
				for (String column : columnNames) {
					Vector<String> vCol = (Vector<String>) res.resultMap.get(column);
					if (vCol == null) {
						vCol = new Vector<String>();
						res.resultMap.put(column.toUpperCase(), vCol);
						res.columNames.add(column.toUpperCase());
					}
					String data = rs.getString(column);
					vCol.add(data);
					str += column + " : " + data + ", ";
				}
				str += "\n";
			}
			if(res.count>0) {
				res.result=1;
			}
			if (printResult) {
				System.out.println(str);
			}

			rs.close();
			state.close();
			conn.close();
		} catch (Exception e) {
			// 예외 발생 시 처리부분
			e.printStackTrace();
			res.result=-1;
		} finally { // 예외가 있든 없든 무조건 실행
			try {
				if (state != null)
					state.close();
			} catch (SQLException ex1) {
				ex1.printStackTrace();
			}

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException ex1) {
				ex1.printStackTrace();
			}
		}
		return res;
	}

	public static void main(String[] args) {
//		String query ="INSERT INTO t_test VALUES('abX', 'nakoros', 'hell', now(), 100);";
//		String query ="update t_test2 set reciever='user0' where token='abc' ;";
//		DBManager db=new DBManager();
//		db.executeUpdate(query);
//		String query = "SELECT * FROM t_test where token='aaa'";
//		SQLResult res=DBManager.executeQuery(query, true);
//		System.out.println(res.result);
//		System.out.println(res.count);
//		System.out.println(res.resultMap);
	}

}
