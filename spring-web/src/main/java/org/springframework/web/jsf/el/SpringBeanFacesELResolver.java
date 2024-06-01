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

package org.springframework.web.jsf.el;

import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;
import javax.faces.context.FacesContext;
import java.beans.FeatureDescriptor;
import java.util.Iterator;

/**
 * JSF {@code ELResolver}，委托给 Spring 根 {@code WebApplicationContext}，解析对 Spring 定义的 bean 的名称引用。
 *
 * <p>在你的 {@code faces-config.xml} 文件中配置此解析器，如下所示：
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;el-resolver&gt;org.springframework.web.jsf.el.SpringBeanFacesELResolver&lt;/el-resolver&gt;
 * &lt;/application&gt;</pre>
 *
 * 然后，所有你的 JSF 表达式都可以隐式引用 Spring 管理的服务层 bean 的名称，例如在 JSF 管理的 bean 的属性值中：
 *
 * <pre class="code">
 * &lt;managed-bean&gt;
 *   &lt;managed-bean-name&gt;myJsfManagedBean&lt;/managed-bean-name&gt;
 *   &lt;managed-bean-class&gt;example.MyJsfManagedBean&lt;/managed-bean-class&gt;
 *   &lt;managed-bean-scope&gt;session&lt;/managed-bean-scope&gt;
 *   &lt;managed-property&gt;
 *     &lt;property-name&gt;mySpringManagedBusinessObject&lt;/property-name&gt;
 *     &lt;value&gt;#{mySpringManagedBusinessObject}&lt;/value&gt;
 *   &lt;/managed-property&gt;
 * &lt;/managed-bean&gt;</pre>
 *
 * 其中 "mySpringManagedBusinessObject" 在 applicationContext.xml 中被定义为 Spring bean：
 *
 * <pre class="code">
 * &lt;bean id="mySpringManagedBusinessObject" class="example.MySpringManagedBusinessObject"&gt;
 *   ...
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @see WebApplicationContextFacesELResolver
 * @see org.springframework.web.jsf.FacesContextUtils#getRequiredWebApplicationContext
 * @since 2.5
 */
public class SpringBeanFacesELResolver extends ELResolver {

	@Override
	@Nullable
	public Object getValue(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		// 如果 base 对象为空
		if (base == null) {
			// 将 property 转换为字符串，作为 bean 的名称
			String beanName = property.toString();
			// 获取 Web应用上下文 实例
			WebApplicationContext wac = getWebApplicationContext(elContext);
			// 检查 Web应用上下文 中是否包含这个 bean
			if (wac.containsBean(beanName)) {
				// 设置 el上下文 的属性已解决
				elContext.setPropertyResolved(true);
				// 返回 bean 的实例
				return wac.getBean(beanName);
			}
		}
		// 如果 base 不为空或者没有找到指定的 bean，返回 null
		return null;
	}

	@Override
	@Nullable
	public Class<?> getType(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		// 如果 base 对象为空
		if (base == null) {
			// 将 property 转换为字符串，作为 bean 的名称
			String beanName = property.toString();
			// 获取 Web应用上下文 实例
			WebApplicationContext wac = getWebApplicationContext(elContext);
			// 检查 Web应用上下文 中是否包含这个 bean
			if (wac.containsBean(beanName)) {
				// 设置 el上下文 的属性已解决
				elContext.setPropertyResolved(true);
				// 返回 bean 的类型
				return wac.getType(beanName);
			}
		}
		// 如果 base 不为空或者没有找到指定的 bean，返回 null
		return null;
	}

	@Override
	public void setValue(ELContext elContext, @Nullable Object base, Object property, Object value) throws ELException {
		// 如果 base 对象为空
		if (base == null) {
			// 将 property 转换为字符串，作为 bean 的名称
			String beanName = property.toString();
			// 获取 Web应用上下文 实例
			WebApplicationContext wac = getWebApplicationContext(elContext);
			// 检查 Web应用上下文 中是否包含这个 bean
			if (wac.containsBean(beanName)) {
				// 如果 value 等于 Web应用上下文 中的 bean 实例
				if (value == wac.getBean(beanName)) {
					// 设置 elContext 的属性已解决，可以忽略设置相同的 bean 引用
					elContext.setPropertyResolved(true);
				} else {
					// 抛出异常，表示该变量引用的 Spring bean 不可写
					throw new PropertyNotWritableException(
							"Variable '" + beanName + "' refers to a Spring bean which by definition is not writable");
				}
			}
		}
	}

	@Override
	public boolean isReadOnly(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		// 如果 base 对象为空
		if (base == null) {
			// 将 property 转换为字符串，作为 bean 的名称
			String beanName = property.toString();
			// 获取 Web 应用程序上下文
			WebApplicationContext wac = getWebApplicationContext(elContext);
			// 检查 Web 应用程序上下文 中是否包含这个 bean
			if (wac.containsBean(beanName)) {
				// 如果存在，返回 true
				return true;
			}
		}
		// 如果 base 不为空或者 bean 不存在，返回 false
		return false;
	}

	@Override
	@Nullable
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, @Nullable Object base) {
		return null;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, @Nullable Object base) {
		return Object.class;
	}

	/**
	 * 检索用于委托 bean 名称解析的 web 应用程序上下文。
	 * <p>默认实现委托给 FacesContextUtils。
	 *
	 * @param elContext 当前的 JSF ELContext
	 * @return Spring web 应用程序上下文（从不返回 {@code null}）
	 * @see org.springframework.web.jsf.FacesContextUtils#getRequiredWebApplicationContext
	 */
	protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
		// 获取当前的 Faces上下文 实例
		FacesContext facesContext = FacesContext.getCurrentInstance();

		// 使用 Faces上下文 获取所需的 Web 应用程序上下文并返回
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
