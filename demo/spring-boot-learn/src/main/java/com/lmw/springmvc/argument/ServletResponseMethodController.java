package com.lmw.springmvc.argument;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletResponseMethodArgumentResolver;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * ServletResponse、OutputStream、Writer参数解析器示例
 * 这个控制器是 {@link ServletResponseMethodArgumentResolver} 参数解析器
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-09 21:58
 */
@Controller
@RequestMapping("/argument")
public class ServletResponseMethodController {
	/**
	 * ServletResponse示例
	 *
	 * @param response Servlet响应
	 */
	@GetMapping("/servletResponse")
	public void servletResponse(ServletResponse response) {
		response.setContentType("text/html; charset=UTF-8");
		try (PrintWriter writer = response.getWriter()) {
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			writer.println("解析结果为：" + response.getClass().getName());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * OutputStream参数示例
	 *
	 * @param outputStream 输出流
	 */
	@GetMapping("/outputStream")
	public void outputStream(HttpServletResponse response, OutputStream outputStream) {
		// 设置响应的字符集为 UTF-8
		response.setCharacterEncoding("UTF-8");

		// 设置响应的内容类型和字符集
		response.setContentType("text/plain; charset=UTF-8");

		String s = "解析结果为：" + outputStream.getClass().getName();
		try {
			outputStream.write(s.getBytes(StandardCharsets.UTF_8));
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writer示例
	 *
	 * @param writer 写入接口
	 */
	@GetMapping("/writer")
	public void writer(Writer writer) {
		try {
			writer.write("argument type is " + writer.getClass().getName());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
