package com.nakoros.MoneySpread;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MoneySpreadController {

	@PostMapping("/splash")
	public String requestSplash(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("money") int totalMoney, @RequestParam("personnel") int personnel) {
		String userId = request.getHeader("X-USER-ID");
		String roomId = request.getHeader("X-ROOM-ID");

		return MoneySplashManager.executeSplash(userId, roomId, totalMoney, personnel);
	}

	@PostMapping("/recieve")
	public int moneyRecieve(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("token") String token) {
		int recvMoney = 0;
		try {
			String userId = request.getHeader("X-USER-ID");
			String roomId = request.getHeader("X-ROOM-ID");
			RecdCond cond = MoneySplashManager.isRecieveCond(token, userId, roomId);
			switch (cond) {
			case OK:
				recvMoney = MoneySplashManager.recieveMoney(token, userId);
				break;
			case RECIEVED:
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "This user has already received.");
				break;
			case NOT_EXIST:
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This token has expired or is invalid.");
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return recvMoney;
	}

	@GetMapping("/splash")
	public Map<String, Object> getSplashInfo(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("token") String token) {
		Map<String, Object> returnMap = MoneySplashManager.getSplashInfo(token);
		try {
			String userId = request.getHeader("X-USER-ID");
			if (userId.equals(returnMap.get("owner"))) {
				returnMap.remove("owner");
			} else {
				if (returnMap.isEmpty()) {// 데이터가 없을때 (7일 지남 or 잘못된 token)
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "There is no data for the token.");
				} else {// 뿌린사람 아닐때
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "The user is not a token owner.");
				}
				return new HashMap<String, Object>();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return returnMap;
	}
}
