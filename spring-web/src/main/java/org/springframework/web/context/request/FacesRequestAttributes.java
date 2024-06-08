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

package org.springframework.web.context.request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * JSF {@link javax.faces.context.FacesContext}的{@link RequestAttributes}适配器。
 * 在JSF环境中用作默认值，包装当前的FacesContext。
 *
 * <p><b>注意：</b>与{@link ServletRequestAttributes}不同，这种变体<i>不</i>支持作用域属性的销毁回调，
 * 无论是请求作用域还是会话作用域。如果您依赖此类隐式销毁回调，请考虑在{@code web.xml}中定义一个Spring {@link RequestContextListener}。
 *
 * <p>需要JSF 2.0或更高版本，从Spring 4.0开始。
 *
 * @author Juergen Hoeller
 * @see javax.faces.context.FacesContext#getExternalContext()
 * @see javax.faces.context.ExternalContext#getRequestMap()
 * @see javax.faces.context.ExternalContext#getSessionMap()
 * @see RequestContextHolder#currentRequestAttributes()
 * @since 2.5.2
 */
public class FacesRequestAttributes implements RequestAttributes {

	/**
	 * 我们会创建很多这些对象，所以我们不希望每次都创建一个新的日志记录器。
	 */
	private static final Log logger = LogFactory.getLog(FacesRequestAttributes.class);
	/**
	 * Faces上下文
	 */
	private final FacesContext facesContext;


	/**
	 * 为给定的FacesContext创建一个新的FacesRequestAttributes适配器。
	 *
	 * @param facesContext 当前的FacesContext
	 * @see javax.faces.context.FacesContext#getCurrentInstance()
	 */
	public FacesRequestAttributes(FacesContext facesContext) {
		Assert.notNull(facesContext, "FacesContext must not be null");
		this.facesContext = facesContext;
	}


	/**
	 * 返回此适配器操作的JSF FacesContext。
	 */
	protected final FacesContext getFacesContext() {
		return this.facesContext;
	}

	/**
	 * 返回此适配器操作的JSF ExternalContext。
	 *
	 * @see javax.faces.context.FacesContext#getExternalContext()
	 */
	protected final ExternalContext getExternalContext() {
		return getFacesContext().getExternalContext();
	}

	/**
	 * 返回指定作用域的JSF属性Map。
	 *
	 * @param scope 表示请求或会话作用域的常量
	 * @return 指定作用域内属性的Map表示
	 * @see #SCOPE_REQUEST
	 * @see #SCOPE_SESSION
	 */
	protected Map<String, Object> getAttributeMap(int scope) {
		// 如果作用域是请求作用域
		if (scope == SCOPE_REQUEST) {
			// 返回外部上下文中的请求参数映射
			return getExternalContext().getRequestMap();
		} else {
			// 否则，返回外部上下文中的会话参数映射
			return getExternalContext().getSessionMap();
		}
	}


	@Override
	public Object getAttribute(String name, int scope) {
		return getAttributeMap(scope).get(name);
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		getAttributeMap(scope).put(name, value);
	}

	@Override
	public void removeAttribute(String name, int scope) {
		getAttributeMap(scope).remove(name);
	}

	@Override
	public String[] getAttributeNames(int scope) {
		return StringUtils.toStringArray(getAttributeMap(scope).keySet());
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (logger.isWarnEnabled()) {
			logger.warn("Could not register destruction callback [" + callback + "] for attribute '" + name +
					"' because FacesRequestAttributes does not support such callbacks");
		}
	}

	@Override
	public Object resolveReference(String key) {
		// 如果 key 是 "request"
		if (REFERENCE_REQUEST.equals(key)) {
			// 返回当前请求对象
			return getExternalContext().getRequest();
		} else if (REFERENCE_SESSION.equals(key)) {
			// 如果 key 是 "session"，返回当前会话对象
			return getExternalContext().getSession(true);
		} else if ("application".equals(key)) {
			// 如果 key 是 "application"，返回当前应用程序上下文
			return getExternalContext().getContext();
		} else if ("requestScope".equals(key)) {
			// 如果 key 是 "requestScope"，返回当前请求范围的映射
			return getExternalContext().getRequestMap();
		} else if ("sessionScope".equals(key)) {
			// 如果 key 是 "sessionScope"，返回当前会话范围的映射
			return getExternalContext().getSessionMap();
		} else if ("applicationScope".equals(key)) {
			// 如果 key 是 "applicationScope"，返回当前应用程序范围的映射
			return getExternalContext().getApplicationMap();
		} else if ("facesContext".equals(key)) {
			// 如果 key 是 "facesContext"，返回当前的 FacesContext
			return getFacesContext();
		} else if ("cookie".equals(key)) {
			// 如果 key 是 "cookie"，返回请求中的 Cookie 映射
			return getExternalContext().getRequestCookieMap();
		} else if ("header".equals(key)) {
			// 如果 key 是 "header"，返回请求中的 Header 映射
			return getExternalContext().getRequestHeaderMap();
		} else if ("headerValues".equals(key)) {
			// 如果 key 是 "headerValues"，返回请求中 Header 的值映射
			return getExternalContext().getRequestHeaderValuesMap();
		} else if ("param".equals(key)) {
			// 如果 key 是 "param"，返回请求中的参数映射
			return getExternalContext().getRequestParameterMap();
		} else if ("paramValues".equals(key)) {
			// 如果 key 是 "paramValues"，返回请求中参数的值映射
			return getExternalContext().getRequestParameterValuesMap();
		} else if ("initParam".equals(key)) {
			// 如果 key 是 "initParam"，返回初始化参数的映射
			return getExternalContext().getInitParameterMap();
		} else if ("view".equals(key)) {
			// 如果 key 是 "view"，返回当前视图根节点
			return getFacesContext().getViewRoot();
		} else if ("viewScope".equals(key)) {
			// 如果 key 是 "viewScope"，返回当前视图范围的映射
			return getFacesContext().getViewRoot().getViewMap();
		} else if ("flash".equals(key)) {
			// 如果 key 是 "flash"，返回当前 Flash 对象
			return getExternalContext().getFlash();
		} else if ("resource".equals(key)) {
			// 如果 key 是 "resource"，返回资源处理器
			return getFacesContext().getApplication().getResourceHandler();
		} else {
			// 对于其他的 key，返回 null
			return null;
		}
	}

	@Override
	public String getSessionId() {
		// 获取当前外部上下文中的Session对象，若没有则创建一个新的
		Object session = getExternalContext().getSession(true);
		try {
			// 获取HttpSession对象的getId()方法
			Method getIdMethod = session.getClass().getMethod("getId");
			// 通过反射调用getId()方法并返回其结果的字符串表示
			return String.valueOf(ReflectionUtils.invokeMethod(getIdMethod, session));
		} catch (NoSuchMethodException ex) {
			// 如果Session对象没有getId()方法，抛出异常
			throw new IllegalStateException("Session object [" + session + "] does not have a getId() method");
		}
	}

	@Override
	public Object getSessionMutex() {
		// 首先强制会话的存在，以允许监听器创建互斥属性
		// 获取外部上下文
		ExternalContext externalContext = getExternalContext();
		// 获取会话，如果不存在则创建一个新的会话
		Object session = externalContext.getSession(true);
		// 从会话映射中获取互斥属性
		Object mutex = externalContext.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			// 如果互斥属性为 null，并且会话对象不为 null，则将互斥属性设置为会话对象
			// 否则设置为外部上下文对象
			mutex = (session != null ? session : externalContext);
		}
		// 返回互斥对象
		return mutex;
	}

}
