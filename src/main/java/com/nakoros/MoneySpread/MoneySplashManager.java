package com.nakoros.MoneySpread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

enum RecdCond { OK, RECEIVED, NOT_EXIST, IS_OWNER }


public class MoneySplashManager {
	public static String executeSplash(String userId, String roomId, int totalMoney, int personnel) {
		// 토큰 생성 & DB저장 토큰, 뿌린 사람, 뿌린 방, 뿌린 시각, 뿌린 금액
		boolean success = false;
		String token = null;
		String query = "INSERT INTO t_splash (TOKEN, OWNER, ROOM, TIME, TOTAL_MONEY) values (@TOKEN@," + userId
				+ "," + roomId + ",now()," + totalMoney + ",";

		while (!success) {
			// DB에서 생성 후 token을 받는 방식으로 수정 필요
			token = generateToken();
			success = DBManager.getInstance().executeUpdate(query.replace("@TOKEN@", token));
		}

		// 랜덤 돈 분배
		Random rd=new Random();
		totalMoney-=personnel;
		
		// DB 테이블 받아간 정보 토큰, 받아간 사람, 받아간 금액
		StringBuilder sb=new StringBuilder();
		sb.append("INSERT INTO t_receive_info (TOKEN, ID, RECEIVER, MONEY) VALUES ");
		for(int i=0;i<personnel-1;i++) {
			int tMoney=rd.nextInt(totalMoney);
			String value="('"+token+"', "+i+",NULL,"+(1+tMoney)+"),";
			sb.append(value);
			totalMoney-=tMoney;
		}
		String value="('"+token+"', "+(personnel-1)+",NULL,"+(1+totalMoney)+")";
		sb.append(value);
		DBManager.getInstance().executeUpdate(sb.toString());
		
		return token;
	}

	public static RecdCond isReceiveCond(String token, String userId, String roomId) {
		String query = "SELECT * FROM t_splash " + "WHERE token'" + token + "' AND room='" + roomId
				+ "' AND time> ADDTIME(now(),'-00:10:00)'";

		SQLResult res = DBManager.getInstance().executeQuery(query);
		if (res.result == 1) {// token과 roomId가 맞고 10분 이내인지 확인
			Vector<String> vOwner=(Vector<String>)res.resultMap.get("OWNER");
			if(userId.equals(vOwner.get(0))) {
				return RecdCond.IS_OWNER;
			}
			query = "SELECT COUNT(*) FROM t_receive_info WHERE token='" + token + "' AND receiver='" + userId + "'";
			res = DBManager.getInstance().executeQuery(query);
			if (res.result == 1) {// 이미 받았는지 확인
				return RecdCond.RECEIVED;
			}
			return RecdCond.OK;
		}
		return RecdCond.NOT_EXIST;
	}

	public static int receiveMoney(String token, String userId) {
		// 받기
		try {
			while (true) {
				String getIdQuery = "SELECT id, money FROM t_receive_info WHERE token='" + token
						+ "' AND ISNULL(receiver) limit 1";
				SQLResult res = DBManager.getInstance().executeQuery(getIdQuery);
				String id;
				String money;
				if (res.result == 1) {
					Vector<String> vId = (Vector<String>) res.resultMap.get("ID");
					Vector<String> vMoney = (Vector<String>) res.resultMap.get("MONEY");
					id = vId.get(0);
					money = vMoney.get(0);
				} else {
					break;
				}
				String query = "UPDATE t_receive_info SET receiver='" + userId + "' WHERE token='" + token
						+ "' AND ISNULL(receiver) AND id=" + id;
				if (DBManager.getInstance().executeUpdate(query)) {
					return Integer.parseInt(money);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
		
	public static Map<String,Object> getSplashInfo(String token, String userId) {
		Map<String, Object> resMap=new HashMap<String, Object>();
		String query = "SELECT * FROM t_splash WHERE token='"+token+"' AND owner='"+userId+"'";
		SQLResult res=DBManager.getInstance().executeQuery(query);
		if(res.result==1) {
			//뿌린 시각, 뿌린 금액, 받기 완료된 금액, 받기 완료된 정보 ([받은 금액, 받은사용자 아이디] 리스트
			List<Map<String,Object>> receiveInfoList=getReceiveInfo(token);
			
			Vector<String> vTime=(Vector<String>)res.resultMap.get("TIME");
			Vector<String> vTotalMoney=(Vector<String>)res.resultMap.get("TOTAL_MONEY");
			
			int totalMoney=Integer.parseInt(vTotalMoney.get(0));
			int remainMoney=totalMoney;
			for(Map<String,Object> info:receiveInfoList) {
				int money=(int)info.get("money");
				remainMoney-=money;
			}
			
			resMap.put("time", vTime.get(0));
			resMap.put("total_money", totalMoney);
			resMap.put("remain_money", remainMoney);
			resMap.put("receive_list", receiveInfoList);
		}else {
			//필요에 따라 error reason 다양화
			resMap.put("reason", "NO_DATA");
		}
		return resMap;
	}
	public static List<Map<String,Object>> getReceiveInfo(String token){
		List<Map<String,Object>> resList=new ArrayList<Map<String,Object>>();
		String query = "SELECT * FROM t_receive_info WHERE token='"+token+"' AND !ISNULL(receiver)";
		SQLResult res=DBManager.getInstance().executeQuery(query);
		
		if(res.result==1) {
			Vector<String> vReceiver=(Vector<String>)res.resultMap.get("RECEIVER");
			Vector<String> vMoney=(Vector<String>)res.resultMap.get("MONEY");
			int size=vReceiver.size();
			for(int i=0;i<size;i++) {
				Map<String,Object> infoMap=new HashMap<String,Object>();
				infoMap.put("id", vReceiver.get(i));
				infoMap.put("money", Integer.parseInt(vMoney.get(i)));
				resList.add(infoMap);
			}
		}
		
		return resList;
	}
	private static String generateToken() {
		StringBuffer temp = new StringBuffer();
		Random rnd = new Random();
		for (int i = 0; i < 3; i++) {
			int rIndex = rnd.nextInt(3);
			switch (rIndex) {
			case 0:
				// a-z
				temp.append((char) ((int) (rnd.nextInt(26)) + 97));
				break;
			case 1:
				// A-Z
				temp.append((char) ((int) (rnd.nextInt(26)) + 65));
				break;
			case 2:
				// 0-9
				temp.append((rnd.nextInt(10)));
				break;
			}
		}
		return temp.toString();
	}
	//scheduled function
	public static void deleteExpiredData() {
		String query="DELETE FROM t_splash WHERE time<ADDTIME(now(), '-07 00:00:00')";
		DBManager.getInstance().executeUpdate(query);
	}
	public static void main(String[] args) {
		int totalMoney=1000;
		int personnel=5;
		Random rd=new Random();
		int[] splitMoney=new int[personnel];
		totalMoney-=personnel;
		for(int i=0;i<personnel-1;i++) {
			int tMoney=rd.nextInt(totalMoney);
			splitMoney[i]=1+tMoney;
			totalMoney-=tMoney;
		}
		splitMoney[personnel-1]=1+totalMoney;
		for(int i=0;i<personnel;i++) {
			System.out.println(splitMoney[i]);
		}
	}
}
