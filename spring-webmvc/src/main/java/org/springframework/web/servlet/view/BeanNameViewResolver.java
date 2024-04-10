/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

/**
 * {@link org.springframework.web.servlet.ViewResolver}的简单实现，它将视图名称解释为当前应用程序上下文中的bean名称，
 * 通常在执行{@code DispatcherServlet}的XML文件或相应的配置类中。
 *
 * <p>注意: 这个{@code ViewResolver}实现了{@link Ordered}接口，以便灵活参与{@code ViewResolver}链的处理。
 * 例如，一些特殊的视图可以通过这个{@code ViewResolver}定义（将其"order"值设为0），而所有其余的视图可以由
 * {@link UrlBasedViewResolver}来解析。
 *
 * @author Juergen Hoeller
 * @see UrlBasedViewResolver
 * @since 18.06.2003
 */
public class BeanNameViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered {

	/**
	 * 默认排序: 与非有序相同
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * 设置此ViewResolver bean的顺序值。
	 * <p>默认值是{@code Ordered.LOWEST_PRECEDENCE}，意味着非有序。
	 *
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws BeansException {
		ApplicationContext context = obtainApplicationContext();
		if (!context.containsBean(viewName)) {
			// 允许进行ViewResolver链处理...
			// 如果应用上下文没有该视图名称，则返回null
			return null;
		}
		if (!context.isTypeMatch(viewName, View.class)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found bean named '" + viewName + "' but it does not implement View");
			}
			// 由于我们在这里查看通用的ApplicationContext，
			// 因此让我们将其视为不匹配并且允许链接...
			return null;
		}
		// 获取该视图名称的视图解析器实例
		return context.getBean(viewName, View.class);
	}

}
