package com.lmw.springmvc.returnValue;

import org.reactivestreams.Publisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitterReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;

/**
 * ResponseBodyEmitter类型的返回值解析示例
 * 这个控制器是 {@link ResponseBodyEmitterReturnValueHandler} 返回值解析器的一个示例。
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-17 21:58
 */
@Controller
@RequestMapping("/returnValue")
public class ResponseBodyEmitterController {


	/**
	 * SseEmitter类型返回值解析示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/sseEmitter")
	public SseEmitter sseEmitter() {
		SseEmitter sseEmitter = new SseEmitter();
		try {
			for (int i = 0; i < 10; i++) {
				sseEmitter.send(SseEmitter.event().name("msg").data("发送的第 " + (i + 1) + " 消息。"));
			}
			sseEmitter.complete();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sseEmitter;
	}

	/**
	 * Publisher类型返回值示例
	 *
	 * @return 解析结果
	 */
	@GetMapping("/publisher")
	public ResponseEntity<Publisher<String>> publisher() {
		// 创建一个Flux，它每秒钟发出一个字符串
		Flux<String> flux = Flux.interval(Duration.ofMillis(200))
				.map(sequence -> "Message " + sequence + "\n")
				// 只取5个元素
				.take(5);
		return ResponseEntity.ok(flux);
	}
}
