/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.jsf.el;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.faces.context.FacesContext;
import java.beans.FeatureDescriptor;
import java.util.Iterator;

/**
 * 特殊的 JSF {@code ELResolver}，在名为 "webApplicationContext" 的变量下暴露 Spring {@code WebApplicationContext} 实例。
 *
 * <p>与 {@link SpringBeanFacesELResolver} 相比，这种 ELResolver 变体 <i>不</i> 将 JSF 变量名解析为 Spring bean 名称。
 * 它是在一个特殊的名称下暴露 Spring 的根 WebApplicationContext <i>本身</i>，并且能够将 "webApplicationContext.mySpringManagedBusinessObject"
 * 反引用解析为该应用程序上下文中定义的 Spring bean。
 *
 * <p>在你的 {@code faces-config.xml} 文件中配置此解析器，如下所示：
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;el-resolver&gt;org.springframework.web.jsf.el.WebApplicationContextFacesELResolver&lt;/el-resolver&gt;
 * &lt;/application&gt;</pre>
 *
 * @author Juergen Hoeller
 * @see SpringBeanFacesELResolver
 * @see org.springframework.web.jsf.FacesContextUtils#getWebApplicationContext
 * @since 2.5
 */
public class WebApplicationContextFacesELResolver extends ELResolver {

	/**
	 * 暴露的 WebApplicationContext 变量的名称："webApplicationContext"。
	 */
	public static final String WEB_APPLICATION_CONTEXT_VARIABLE_NAME = "webApplicationContext";


	/**
	 * 子类可用的日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	@Nullable
	public Object getValue(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		// 如果 base 对象不为空
		if (base != null) {
			// 检查 base 对象是否是 Web应用上下文 类型的实例
			if (base instanceof WebApplicationContext) {
				// 将 base 对象转换为 Web应用上下文 类型
				WebApplicationContext wac = (WebApplicationContext) base;
				// 将 property 转换为字符串，作为 bean 的名称
				String beanName = property.toString();
				// 如果日志级别是 trace，则输出 trace 信息
				if (logger.isTraceEnabled()) {
					logger.trace("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				// 检查 Web应用上下文 中是否包含这个 bean
				if (wac.containsBean(beanName)) {
					// 如果日志级别是调试，则输出调试信息
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					// 设置 el上下文 的属性已解析
					elContext.setPropertyResolved(true);
					try {
						// 返回 bean 的实例
						return wac.getBean(beanName);
					} catch (BeansException ex) {
						// 如果出现 BeansException，则抛出 ELException
						throw new ELException(ex);
					}
				} else {
					// 模仿标准的 JSF/JSP 行为，当 base 是一个 Map 时返回 null
					return null;
				}
			}
		} else {
			// 如果 base 对象为空，并且属性名称等于 webApplicationContext
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				// 设置 el上下文 的属性已解析
				elContext.setPropertyResolved(true);
				// 返回 Web应用上下文 实例
				return getWebApplicationContext(elContext);
			}
		}

		// 如果以上条件都不满足，返回 null
		return null;
	}

	@Override
	@Nullable
	public Class<?> getType(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		// 如果 base 对象不为空
		if (base != null) {
			// 检查 base 对象是否是 Web应用上下文 类型的实例
			if (base instanceof WebApplicationContext) {
				// 将 base 对象转换为 Web应用上下文 类型
				WebApplicationContext wac = (WebApplicationContext) base;
				// 将 property 转换为字符串，作为 bean 的名称
				String beanName = property.toString();
				// 如果日志级别是调试，则输出调试信息
				if (logger.isDebugEnabled()) {
					logger.debug("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				// 检查 Web应用上下文 中是否包含这个 bean
				if (wac.containsBean(beanName)) {
					// 如果日志级别是调试，则输出调试信息
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					// 设置 el上下文 的属性已解析
					elContext.setPropertyResolved(true);
					try {
						// 返回 bean 的类型
						return wac.getType(beanName);
					} catch (BeansException ex) {
						// 如果出现 BeansException，则抛出 ELException
						throw new ELException(ex);
					}
				} else {
					// 模仿标准的 JSF/JSP 行为，当 base 是一个 Map 时返回 null
					return null;
				}
			}
		} else {
			// 如果 base 对象为空，并且属性名称等于 webApplicationContext
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				// 设置 el上下文 的属性已解析
				elContext.setPropertyResolved(true);
				// 返回 Web应用上下文 类
				return WebApplicationContext.class;
			}
		}

		// 如果以上条件都不满足，返回 null
		return null;
	}

	@Override
	public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
	}

	@Override
	public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
		// 检查 base 对象是否是 Web应用上下文 类型的实例
		if (base instanceof WebApplicationContext) {
			// 如果是，设置 el上下文 的属性已解析
			elContext.setPropertyResolved(true);
			// 返回 true 表示属性只读
			return true;
		}
		// 如果 base 不是 Web应用上下文 类型的实例，返回 false
		return false;
	}

	@Override
	@Nullable
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
		return null;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
		return Object.class;
	}


	/**
	 * 检索要公开的 {@link WebApplicationContext} 引用。
	 * <p>默认实现委托给 {@link FacesContextUtils}，如果找不到 {@code WebApplicationContext}，
	 * 则返回 {@code null}。
	 *
	 * @param elContext 当前的 JSF ELContext
	 * @return Spring web 应用程序上下文
	 * @see org.springframework.web.jsf.FacesContextUtils#getWebApplicationContext
	 */
	@Nullable
	protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
		// 获取当前的 Faces上下文 实例
		FacesContext facesContext = FacesContext.getCurrentInstance();

		// 使用 Faces上下文 获取 Web 应用程序上下文
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
