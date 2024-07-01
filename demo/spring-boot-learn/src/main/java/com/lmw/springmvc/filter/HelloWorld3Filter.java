package com.lmw.springmvc.filter;

import javax.servlet.*;
import java.io.IOException;

/**
 * 自定义Filter
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-01 20:11
 */
public class HelloWorld3Filter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		System.out.println("hello world filter2");
		chain.doFilter(request, response);
	}
}
