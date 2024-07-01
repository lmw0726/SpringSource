package com.lmw.springmvc.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * 自定义Filter
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-01 20:11
 */
@WebFilter("/hello")
public class HelloWorldFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		System.out.println("hello world filter");
		chain.doFilter(request, response);
	}
}
