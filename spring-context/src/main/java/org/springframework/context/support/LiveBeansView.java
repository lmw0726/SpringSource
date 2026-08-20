/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于实时Bean视图暴露的适配器，构建当前Bean及其依赖关系的快照，
 * 来源于本地 {@code ApplicationContext}（带有本地 {@code LiveBeansView} Bean定义）或所有已注册的ApplicationContexts
 * （由 {@value #MBEAN_DOMAIN_PROPERTY_NAME} 环境属性驱动）。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 * @see #getSnapshotAsJson()
 * @see org.springframework.web.context.support.LiveBeansViewServlet
 * @deprecated 自5.3版本起，建议使用Spring Boot actuators来满足此类需求
 */
@Deprecated
public class LiveBeansView implements LiveBeansViewMBean, ApplicationContextAware {

	/**
	 * "MBean Domain" 属性名称。
	 */
	public static final String MBEAN_DOMAIN_PROPERTY_NAME = "spring.liveBeansView.mbeanDomain";

	/**
	 * MBean 应用程序键。
	 */
	public static final String MBEAN_APPLICATION_KEY = "application";

	private static final Set<ConfigurableApplicationContext> applicationContexts = new LinkedHashSet<>();

	@Nullable
	private static String applicationName;


	static void registerApplicationContext(ConfigurableApplicationContext applicationContext) {
		String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
		if (mbeanDomain != null) {
			synchronized (applicationContexts) {
				if (applicationContexts.isEmpty()) {
					try {
						MBeanServer server = ManagementFactory.getPlatformMBeanServer();
						applicationName = applicationContext.getApplicationName();
						server.registerMBean(new LiveBeansView(),
								new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
					}
					catch (Throwable ex) {
						throw new ApplicationContextException("Failed to register LiveBeansView MBean", ex);
					}
				}
				applicationContexts.add(applicationContext);
			}
		}
	}

	static void unregisterApplicationContext(ConfigurableApplicationContext applicationContext) {
		synchronized (applicationContexts) {
			if (applicationContexts.remove(applicationContext) && applicationContexts.isEmpty()) {
				try {
					MBeanServer server = ManagementFactory.getPlatformMBeanServer();
					String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
					if (mbeanDomain != null) {
						server.unregisterMBean(new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
					}
				}
				catch (Throwable ex) {
					throw new ApplicationContextException("Failed to unregister LiveBeansView MBean", ex);
				}
				finally {
					applicationName = null;
				}
			}
		}
	}


	@Nullable
	private ConfigurableApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}


	/**
	 * 生成当前Bean及其依赖关系的JSON快照，
	 * 通过 {@link #findApplicationContexts()} 查找所有活动的ApplicationContexts，
	 * 然后委托给 {@link #generateJson(java.util.Set)}。
	 */
	@Override
	public String getSnapshotAsJson() {
		Set<ConfigurableApplicationContext> contexts;
		if (this.applicationContext != null) {
			contexts = Collections.singleton(this.applicationContext);
		}
		else {
			contexts = findApplicationContexts();
		}
		return generateJson(contexts);
	}

	/**
	 * 查找当前应用程序的所有适用的ApplicationContexts。
	 * <p>如果未为此LiveBeansView设置特定的ApplicationContext，则调用此方法。
	 * @return ApplicationContexts集合
	 */
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		synchronized (applicationContexts) {
			return new LinkedHashSet<>(applicationContexts);
		}
	}

	/**
	 * 实际生成给定ApplicationContexts中Bean的JSON快照。
	 * <p>此实现不使用任何JSON解析库以避免第三方库依赖。它生成一个上下文描述对象数组，
	 * 每个对象包含context和parent属性，以及带有嵌套Bean描述对象的beans属性。
	 * 每个Bean对象包含bean、scope、type和resource属性，以及dependencies属性，
	 * 其中包含当前Bean所依赖的Bean名称的嵌套数组。
	 * @param contexts ApplicationContexts集合
	 * @return JSON文档
	 */
	protected String generateJson(Set<ConfigurableApplicationContext> contexts) {
		StringBuilder result = new StringBuilder("[\n");
		for (Iterator<ConfigurableApplicationContext> it = contexts.iterator(); it.hasNext();) {
			ConfigurableApplicationContext context = it.next();
			result.append("{\n\"context\": \"").append(context.getId()).append("\",\n");
			if (context.getParent() != null) {
				result.append("\"parent\": \"").append(context.getParent().getId()).append("\",\n");
			}
			else {
				result.append("\"parent\": null,\n");
			}
			result.append("\"beans\": [\n");
			ConfigurableListableBeanFactory bf = context.getBeanFactory();
			String[] beanNames = bf.getBeanDefinitionNames();
			boolean elementAppended = false;
			for (String beanName : beanNames) {
				BeanDefinition bd = bf.getBeanDefinition(beanName);
				if (isBeanEligible(beanName, bd, bf)) {
					if (elementAppended) {
						result.append(",\n");
					}
					result.append("{\n\"bean\": \"").append(beanName).append("\",\n");
					result.append("\"aliases\": ");
					appendArray(result, bf.getAliases(beanName));
					result.append(",\n");
					String scope = bd.getScope();
					if (!StringUtils.hasText(scope)) {
						scope = BeanDefinition.SCOPE_SINGLETON;
					}
					result.append("\"scope\": \"").append(scope).append("\",\n");
					Class<?> beanType = bf.getType(beanName);
					if (beanType != null) {
						result.append("\"type\": \"").append(beanType.getName()).append("\",\n");
					}
					else {
						result.append("\"type\": null,\n");
					}
					result.append("\"resource\": \"").append(getEscapedResourceDescription(bd)).append("\",\n");
					result.append("\"dependencies\": ");
					appendArray(result, bf.getDependenciesForBean(beanName));
					result.append("\n}");
					elementAppended = true;
				}
			}
			result.append("]\n");
			result.append('}');
			if (it.hasNext()) {
				result.append(",\n");
			}
		}
		result.append(']');
		return result.toString();
	}

	/**
	 * 确定指定Bean是否适合包含在LiveBeansView JSON快照中。
	 * @param beanName Bean名称
	 * @param bd 对应的Bean定义
	 * @param bf 包含的Bean工厂
	 * @return 如果Bean应被包含则返回 {@code true}；否则返回 {@code false}
	 */
	protected boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
		return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE &&
				(!bd.isLazyInit() || bf.containsSingleton(beanName)));
	}

	/**
	 * 确定给定Bean定义的资源描述并应用基本的JSON转义（反斜杠、双引号）。
	 * @param bd 要构建资源描述的Bean定义
	 * @return JSON转义后的资源描述
	 */
	@Nullable
	protected String getEscapedResourceDescription(BeanDefinition bd) {
		String resourceDescription = bd.getResourceDescription();
		if (resourceDescription == null) {
			return null;
		}
		StringBuilder result = new StringBuilder(resourceDescription.length() + 16);
		for (int i = 0; i < resourceDescription.length(); i++) {
			char character = resourceDescription.charAt(i);
			if (character == '\\') {
				result.append('/');
			}
			else if (character == '"') {
				result.append("\\").append('"');
			}
			else {
				result.append(character);
			}
		}
		return result.toString();
	}

	private void appendArray(StringBuilder result, String[] arr) {
		result.append('[');
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(StringUtils.arrayToDelimitedString(arr, "\", \""));
		if (arr.length > 0) {
			result.append('\"');
		}
		result.append(']');
	}

}
