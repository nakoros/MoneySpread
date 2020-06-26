package com.nakoros.MoneySpread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

enum RecdCond { OK, RECIEVED, NOT_EXIST }


public class MoneySplashManager {
	public static String executeSplash(String userId, String roomId, int totalMoney, int personnel) {
		// 토큰 생성 & DB저장 토큰, 뿌린 사람, 뿌린 방, 뿌린 시각, 뿌린 금액
		boolean success = false;
		String token = null;
		String query = "INSERT INTO t_splash (TOKEN, OWNER, ROOM, TIME, TOTAL_MONEY) " + "values (@TOKEN@," + userId
				+ "," + roomId + ",now()," + totalMoney + ",";

		while (!success) {
			// DB에서 생성 후 token을 받는 방식으로 수정 필요
			token = generateToken();
			success = DBManager.executeUpdate(query.replace("@TOKEN@", token));
		}

		// 랜덤 돈 분배
		Random rd=new Random();
		totalMoney-=personnel;
		
		// DB 테이블 받아간 정보 토큰, 받아간 사람, 받아간 금액
		StringBuilder sb=new StringBuilder();
		sb.append("INSERT INTO t_recieve_info (TOKEN, ID, RECIEVER, MONEY) VALUES ");
		for(int i=0;i<personnel-1;i++) {
			int tMoney=rd.nextInt(totalMoney);
			String value="('"+token+"', "+i+",NULL,"+(1+tMoney)+"),";
			sb.append(value);
			totalMoney-=tMoney;
		}
		String value="('"+token+"', "+(personnel-1)+",NULL,"+(1+totalMoney)+")";
		sb.append(value);
		DBManager.executeUpdate(sb.toString());
		
		return token;
	}

	public static RecdCond isRecieveCond(String token, String userId, String roomId) {
		String query = "SELECT COUNT(*) FROM t_splash " + "WHERE token'" + token + "' AND room='" + roomId
				+ "' AND time> ADDTIME(now(),'-00:10:00)'";

		SQLResult res = DBManager.executeQuery(query);
		if (res.result == 1) {// token과 roomId가 맞고 10분 이내인지 확인
			query = "SELECT COUNT(*) FROM t_recieve_info WHERE token='" + token + "' AND reciever='" + userId + "'";
			res = DBManager.executeQuery(query);
			if (res.result == 1) {// 이미 받았는지 확인
				return RecdCond.RECIEVED;
			}
			return RecdCond.OK;
		}
		return RecdCond.NOT_EXIST;
	}

	public static int recieveMoney(String token, String userId) {
		// 받기
		try {
			while (true) {
				String getIdQuery = "SELECT id, money FROM t_recieve_info WHERE token='" + token
						+ "' AND ISNULL(reciever) limit 1";
				SQLResult res = DBManager.executeQuery(getIdQuery);
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
				String query = "UPDATE t_recieve_info SET reciever='" + userId + "' WHERE token='" + token
						+ "' AND ISNULL(reciever) AND id=" + id;
				if (DBManager.executeUpdate(query)) {
					return Integer.parseInt(money);
				}
			}
		} catch (Exception e) {

		}
		return 0;
	}
		
	public static Map<String,Object> getSplashInfo(String token) {
		Map<String, Object> resMap=new HashMap<String, Object>();
		String query = "SELECT * FROM t_splash WHERE token='"+token+"'";
		SQLResult res=DBManager.executeQuery(query);
		if(res.result==1) {
			//뿌린 시각, 뿌린 금액, 받기 완료된 금액, 받기 완료된 정보 ([받은 금액, 받은사용자 아이디] 리스트
			List<Map<String,Object>> recieveInfoList=getRecieveInfo(token);
			
			Vector<String> vTime=(Vector<String>)res.resultMap.get("TIME");
			Vector<String> vTotalMoney=(Vector<String>)res.resultMap.get("TOTAL_MONEY");
			Vector<String> vOwner=(Vector<String>)res.resultMap.get("OWNER");
			
			int totalMoney=Integer.parseInt(vTotalMoney.get(0));
			int remainMoney=totalMoney;
			for(Map<String,Object> info:recieveInfoList) {
				int money=(int)info.get("money");
				remainMoney-=money;
			}
			
			resMap.put("owner", vOwner.get(0));
			resMap.put("time", vTime.get(0));
			resMap.put("total_money", totalMoney);
			resMap.put("remain_money", remainMoney);
			resMap.put("recieve_list", recieveInfoList);
		}
		return resMap;
	}
	public static List<Map<String,Object>> getRecieveInfo(String token){
		List<Map<String,Object>> resList=new ArrayList<Map<String,Object>>();
		String query = "SELECT * FROM t_recieve_info WHERE token='"+token+"' AND !ISNULL(reciever)";
		SQLResult res=DBManager.executeQuery(query);
		
		if(res.result==1) {
			Vector<String> vReciever=(Vector<String>)res.resultMap.get("RECIEVER");
			Vector<String> vMoney=(Vector<String>)res.resultMap.get("MONEY");
			int size=vReciever.size();
			for(int i=0;i<size;i++) {
				Map<String,Object> infoMap=new HashMap<String,Object>();
				infoMap.put("id", vReciever.get(i));
				infoMap.put("money", Integer.parseInt(vMoney.get(i)));
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
