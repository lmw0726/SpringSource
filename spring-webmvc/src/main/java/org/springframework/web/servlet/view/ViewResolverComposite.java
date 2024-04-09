/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 一个委托给其他视图解析器的 {@link org.springframework.web.servlet.ViewResolver}。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ViewResolverComposite implements ViewResolver, Ordered, InitializingBean,
		ApplicationContextAware, ServletContextAware {

	/**
	 * 视图解析器列表
	 */
	private final List<ViewResolver> viewResolvers = new ArrayList<>();

	/**
	 * 解析视图的顺序
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 设置要委托的视图解析器列表。
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers.clear();
		if (!CollectionUtils.isEmpty(viewResolvers)) {
			this.viewResolvers.addAll(viewResolvers);
		}
	}

	/**
	 * 返回要委托的视图解析器列表。
	 */
	public List<ViewResolver> getViewResolvers() {
		return Collections.unmodifiableList(this.viewResolvers);
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		for (ViewResolver viewResolver : this.viewResolvers) {
			if (viewResolver instanceof ApplicationContextAware) {
				// 如果视图解析器是 应用上下文感知器 实例，则设置应用上下文
				((ApplicationContextAware) viewResolver).setApplicationContext(applicationContext);
			}
		}
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		for (ViewResolver viewResolver : this.viewResolvers) {
			if (viewResolver instanceof ServletContextAware) {
				// 如果视图解析器是 Servlet上下文感知器 的实例，则设置Servlet上下文
				((ServletContextAware) viewResolver).setServletContext(servletContext);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (ViewResolver viewResolver : this.viewResolvers) {
			if (viewResolver instanceof InitializingBean) {
				// 如果视图解析器是 初始化Bean实例，则调用它的 afterPropertiesSet 方法
				((InitializingBean) viewResolver).afterPropertiesSet();
			}
		}
	}

	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		for (ViewResolver viewResolver : this.viewResolvers) {
			// 遍历视图解析器，并解析出视图
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				// 如果视图不为空，则返回该视图
				return view;
			}
		}
		return null;
	}

}
