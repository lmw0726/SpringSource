package com.lmw.springmvc.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截器示例
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-06 21:48
 */
public class Hello2Interceptor implements HandlerInterceptor {
	private final Logger log = LoggerFactory.getLogger(Hello2Interceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		log.info("_______ Hello2Interceptor # preHandle _______");
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		log.info("_______ Hello2Interceptor # postHandle _______");
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		log.info("_______ Hello2Interceptor # afterCompletion _______");
	}
}
