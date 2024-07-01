package com.lmw.springmvc.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

/**
 * 自定义Servlet
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-01 20:08
 */
public class HelloWorld3Servlet extends HttpServlet implements Serializable {

	private static final long serialVersionUID = 6812317322570854000L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setHeader("Content-Type", "text/html;charset=UTF-8");
		resp.getWriter().write("Hello World3");
	}
}
