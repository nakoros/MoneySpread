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
	public int moneyReceive(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("token") String token) {
		int recvMoney = 0;
		try {
			String userId = request.getHeader("X-USER-ID");
			String roomId = request.getHeader("X-ROOM-ID");
			RecdCond cond = MoneySplashManager.isReceiveCond(token, userId, roomId);
			switch (cond) {
			case OK:
				recvMoney = MoneySplashManager.receiveMoney(token, userId);
				break;
			case RECEIVED:
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "This user has already received.");
				break;
			case NOT_EXIST:
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This token has expired or is invalid.");
				break;
			case IS_OWNER:
				response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Token owners are not receive.");
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
		String userId = request.getHeader("X-USER-ID");
		Map<String, Object> returnMap = MoneySplashManager.getSplashInfo(token,userId);
		try {
			String reason=(String)returnMap.get("reason");
			if (reason!=null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "The token is invalid or is not owner of token.");
				return new HashMap<String, Object>();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return returnMap;
	}
}
