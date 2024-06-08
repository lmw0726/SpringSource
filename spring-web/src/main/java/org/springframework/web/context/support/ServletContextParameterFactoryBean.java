/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.context.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

/**
 * {@link FactoryBean} 用于检索特定的 ServletContext 初始化参数（即 {@code web.xml} 中定义的 "context-param"）。
 * 当用作 bean 引用时，会公开该 ServletContext 初始化参数，从而使其作为命名的 Spring bean 实例可用。
 *
 * <p><b>注意：</b> 从 Spring 3.0 开始，您还可以使用类型为 Map 的 "contextParameters" 默认 bean，
 * 并使用 "#{contextParameters.myKey}" 表达式取消引用它，以按名称访问特定参数。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.context.WebApplicationContext#CONTEXT_PARAMETERS_BEAN_NAME
 * @see ServletContextAttributeFactoryBean
 * @since 1.2.4
 */
public class ServletContextParameterFactoryBean implements FactoryBean<String>, ServletContextAware {
	/**
	 * 初始化参数名称
	 */
	@Nullable
	private String initParamName;

	/**
	 * 参数值
	 */
	@Nullable
	private String paramValue;


	/**
	 * 设置要公开的 ServletContext 初始化参数的名称。
	 */
	public void setInitParamName(String initParamName) {
		this.initParamName = initParamName;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		// 如果 初始化参数名称 为空，抛出 IllegalArgumentException 异常
		if (this.initParamName == null) {
			throw new IllegalArgumentException("initParamName is required");
		}
		// 从 Servlet上下文 获取初始化参数的值
		this.paramValue = servletContext.getInitParameter(this.initParamName);
		if (this.paramValue == null) {
			// 如果初始化参数的值为空，抛出 IllegalStateException 异常
			throw new IllegalStateException("No ServletContext init parameter '" + this.initParamName + "' found");
		}
	}


	@Override
	@Nullable
	public String getObject() {
		return this.paramValue;
	}

	@Override
	public Class<String> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
