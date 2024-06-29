package com.lmw.springmvc.sse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;

/**
 * SSE服务端发送事件，第一种服务器推送
 */
@Controller
public class SseController {

	@RequestMapping(value = "/push", produces = "text/event-stream;charset=UTF-8")
	@ResponseBody
	public String push() {
		Random random = new Random();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "data:Testing 1,2,3" + random.nextInt() + "\n\n";
	}

	@RequestMapping("/sse")
	public String turnToSse() {
		return "sse";
	}
}
