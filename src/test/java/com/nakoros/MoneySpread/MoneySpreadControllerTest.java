package com.nakoros.MoneySpread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class MoneySpreadControllerTest {

	@Mock
	DBManager db = mock(DBManager.class);
	
	MoneySplashManager mng=new MoneySplashManager();
	
	@Before
	public void before() {
		new DBManager(db);
	}

	@Test
	public void recieveTestNormal() {// 정상받기
		
		String testToken="abc";
		String testUserId="user1";
		
		String getIdQuery = "SELECT id, money FROM t_receive_info WHERE token='" + testToken+ "' AND ISNULL(receiver) limit 1";
		SQLResult res = new SQLResult();
		res.result = 1;
		res.count = 1;
		Vector<String> vId = new Vector<String>();
		vId.add("1");
		Vector<String> vMoney = new Vector<String>();
		vMoney.add("666");
		res.resultMap.put("ID", vId);
		res.resultMap.put("MONEY", vMoney);

		when(db.executeQuery(getIdQuery)).thenReturn(res);
		
		SQLResult res2 = new SQLResult();
		res2.result = 0;
		res2.count = 0;
		String query = "UPDATE t_receive_info SET receiver='" + testUserId + "' WHERE token='" + testToken
				+ "' AND ISNULL(receiver) AND id=" + "1";
		when(db.executeUpdate(query)).thenReturn(true);
		
		int actual=mng.receiveMoney(testToken, testUserId);
		assertEquals(666, actual);
	}

	@Test
	public void recieveTestAbnormal_AlreadyRecieved() {// 이미 받음
		String testToken="abc";
		String testUserId="user2";
		String testRoomId="room1";
		
		SQLResult res = new SQLResult();
		res.result = 1;
		res.count = 1;
		Vector<String> vOwner = new Vector<String>();
		vOwner.add("user1");
		res.resultMap.put("OWNER", vOwner);

		String query = "SELECT * FROM t_splash " + "WHERE token'" + testToken + "' AND room='" + testRoomId
				+ "' AND time> ADDTIME(now(),'-00:10:00)'";
		when(db.executeQuery(query)).thenReturn(res);
		
		query = "SELECT COUNT(*) FROM t_receive_info WHERE token='" + testToken + "' AND receiver='" + testUserId + "'";
		SQLResult res2 = new SQLResult();
		//이미 받았기 때문에 t_receive_info에 데이터가 존재
		res2.result = 1;
		res2.count = 1;
		when(db.executeQuery(query)).thenReturn(res2);
		
		RecdCond actual=mng.isReceiveCond(testToken, testUserId, testRoomId);
		assertEquals(RecdCond.RECEIVED, actual);
	}

	@Test
	public void recieveTestAbnormal_TokenOwner() {//뿌린사람이 받으려고 함
		String testToken="abc";
		String testUserId="user1";
		String testRoomId="room2";
		SQLResult res = new SQLResult();
		res.result = 1;
		res.count = 1;
		
		//요청하는 ID와 DB에서 return되는 Owner ID가 동일
		Vector<String> vOwner = new Vector<String>();
		vOwner.add("user1");
		res.resultMap.put("OWNER", vOwner);

		String query = "SELECT * FROM t_splash " + "WHERE token'" + testToken + "' AND room='" + testRoomId
				+ "' AND time> ADDTIME(now(),'-00:10:00)'";
		when(db.executeQuery(query)).thenReturn(res);
		
		
		RecdCond actual=mng.isReceiveCond(testToken, testUserId, testRoomId);
		assertEquals(RecdCond.IS_OWNER, actual);
	}

	@Test
	public void recieveTestAbnormal_NotExist() {// 다른 대화방 or 10분 넘을 경우 DB데이터 없음
		String testToken="abc";
		String testUserId="user1";
		String testRoomId="room2";
		SQLResult res = new SQLResult();
		res.result = 0;
		res.count = 0;
		
		String query = "SELECT * FROM t_splash " + "WHERE token'" + testToken + "' AND room='" + testRoomId
				+ "' AND time> ADDTIME(now(),'-00:10:00)'";
		when(db.executeQuery(query)).thenReturn(res);
		
		RecdCond actual=mng.isReceiveCond(testToken, testUserId, testRoomId);
		assertEquals(RecdCond.NOT_EXIST, actual);
	}

	@Test
	public void getSplashTestNormal() {//
		String testToken="abc";
		String testUserId="user1";
		
		SQLResult res = new SQLResult();
		res.result = 1;
		res.count = 1;
		Vector<String> vTime = new Vector<String>();
		Vector<String> vTotMoney = new Vector<String>();
		vTime.add("2020-06-26 23:11:00");
		vTotMoney.add("1000");
		res.resultMap.put("TIME", vTime);
		res.resultMap.put("TOTAL_MONEY", vTotMoney);
		String query = "SELECT * FROM t_splash WHERE token='"+testToken+"' AND owner='"+testUserId+"'";
		when(db.executeQuery(query)).thenReturn(res);
		
		SQLResult res2 = new SQLResult();
		res2.result = 1;
		res2.count = 1;
		Vector<String> vReceiver = new Vector<String>();
		Vector<String> vMoney = new Vector<String>();
		vReceiver.add("user2");
		vMoney.add("666");
		res2.resultMap.put("RECEIVER", vReceiver);
		res2.resultMap.put("MONEY", vMoney);
		
		query = "SELECT * FROM t_receive_info WHERE token='"+testToken+"' AND !ISNULL(receiver)";
		when(db.executeQuery(query)).thenReturn(res2);
		
		Map<String,Object> actualMap=mng.getSplashInfo(testToken, testUserId);
		//다른 정보는 mock이기 때문에 remain_money가 정상 계산되는지만 확인.
		assertEquals(1000-666, (int)actualMap.get("remain_money"));
	}

	@Test
	public void getSplashTestAbnormal_NoData() {// 토큰 정보가 없거나 오너아니면 DB데이터 없음
		String testToken="abc";
		String testUserId="user1";
		SQLResult res = new SQLResult();
		res.result = 0;
		res.count = 0;
		String query = "SELECT * FROM t_splash WHERE token='"+testToken+"' AND owner='"+testUserId+"'";
		when(db.executeQuery(query)).thenReturn(res);
		
		Map<String,Object> actualMap=mng.getSplashInfo(testToken, testUserId);
		assertEquals("NO_DATA", actualMap.get("reason"));
	}



}
