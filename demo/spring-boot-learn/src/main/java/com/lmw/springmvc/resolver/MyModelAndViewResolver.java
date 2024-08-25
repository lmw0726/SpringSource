package com.lmw.springmvc.resolver;

import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 自定义ModelAndViewResolver解析器
 *
 * @author LMW
 * @version 1.0
 * @since 2024-08-25 21:08
 */
public class MyModelAndViewResolver implements ModelAndViewResolver {
	@Override
	public ModelAndView resolveModelAndView(Method handlerMethod, Class<?> handlerType, Object returnValue,
											ExtendedModelMap implicitModel, NativeWebRequest webRequest) {

		if (returnValue instanceof MySpecialArg) {
			return new ModelAndView(new View() {
				@Override
				public String getContentType() {
					return "text/html";
				}
				@Override
				public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
						throws Exception {
					response.getWriter().write("myValue");
				}
			});
		}
		return UNRESOLVED;
	}
}
