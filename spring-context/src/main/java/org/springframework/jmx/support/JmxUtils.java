/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jmx.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.management.*;
import java.beans.PropertyDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;

/**
 * 支持 Spring JMX 的通用工具方法集合。
 * 包含用于定位 MBeanServer 的便捷方法。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #locateMBeanServer
 */
public abstract class JmxUtils {

	/**
	 * 用于在扩展已有 {@link ObjectName} 时添加的键，
	 * 值为对应托管资源的身份哈希码（identity hash code）。
	 */
	public static final String IDENTITY_OBJECT_NAME_KEY = "identity";

	/**
	 * 用于标识 MBean 接口的后缀。
	 */
	private static final String MBEAN_SUFFIX = "MBean";


	private static final Log logger = LogFactory.getLog(JmxUtils.class);


	/**
	 * 尝试查找本地运行的 {@code MBeanServer}。如果找不到
	 * {@code MBeanServer} 则会失败。如果找到多个 {@code MBeanServer}，
	 * 则记录警告日志并返回列表中的第一个。
	 * @return 找到的 {@code MBeanServer}
	 * @throws MBeanServerNotFoundException 如果找不到 {@code MBeanServer}
	 * @see javax.management.MBeanServerFactory#findMBeanServer
	 */
	public static MBeanServer locateMBeanServer() throws MBeanServerNotFoundException {
		return locateMBeanServer(null);
	}

	/**
	 * 尝试查找本地运行的 {@code MBeanServer}。如果找不到
	 * {@code MBeanServer} 则会失败。如果找到多个 {@code MBeanServer}，
	 * 则记录警告日志并返回列表中的第一个。
	 * @param agentId 要检索的 MBeanServer 的代理标识符。
	 * 如果此参数为 {@code null}，则考虑所有已注册的 MBeanServer。
	 * 如果传入空字符串，则返回平台 MBeanServer。
	 * @return 找到的 {@code MBeanServer}
	 * @throws MBeanServerNotFoundException 如果找不到 {@code MBeanServer}
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	public static MBeanServer locateMBeanServer(@Nullable String agentId) throws MBeanServerNotFoundException {
		MBeanServer server = null;

		// null 表示任意已注册的服务器，而 "" 特指平台服务器
		if (!"".equals(agentId)) {
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(agentId);
			if (!CollectionUtils.isEmpty(servers)) {
				// 检查是否注册了 MBeanServer。
				if (servers.size() > 1 && logger.isInfoEnabled()) {
					logger.info("Found more than one MBeanServer instance" +
							(agentId != null ? " with agent id [" + agentId + "]" : "") +
							". Returning first from list.");
				}
				server = servers.get(0);
			}
		}

		if (server == null && !StringUtils.hasLength(agentId)) {
			// 尝试加载平台 MBeanServer。
			try {
				server = ManagementFactory.getPlatformMBeanServer();
			}
			catch (SecurityException ex) {
				throw new MBeanServerNotFoundException("No specific MBeanServer found, " +
						"and not allowed to obtain the Java platform MBeanServer", ex);
			}
		}

		if (server == null) {
			throw new MBeanServerNotFoundException(
					"Unable to locate an MBeanServer instance" +
					(agentId != null ? " with agent id [" + agentId + "]" : ""));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found MBeanServer: " + server);
		}
		return server;
	}

	/**
	 * 将 {@code MBeanParameterInfo} 数组转换为对应的参数 {@code Class} 实例数组。
	 * @param paramInfo JMX 参数信息
	 * @return 以 Class 形式表示的参数类型
	 * @throws ClassNotFoundException 如果无法解析参数类型
	 */
	@Nullable
	public static Class<?>[] parameterInfoToTypes(@Nullable MBeanParameterInfo[] paramInfo)
			throws ClassNotFoundException {

		return parameterInfoToTypes(paramInfo, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 将 {@code MBeanParameterInfo} 数组转换为对应的参数 {@code Class} 实例数组。
	 * @param paramInfo JMX 参数信息
	 * @param classLoader 用于加载参数类型的 ClassLoader
	 * @return 以 Class 形式表示的参数类型
	 * @throws ClassNotFoundException 如果无法解析参数类型
	 */
	@Nullable
	public static Class<?>[] parameterInfoToTypes(
			@Nullable MBeanParameterInfo[] paramInfo, @Nullable ClassLoader classLoader)
			throws ClassNotFoundException {

		Class<?>[] types = null;
		if (paramInfo != null && paramInfo.length > 0) {
			types = new Class<?>[paramInfo.length];
			for (int x = 0; x < paramInfo.length; x++) {
				types[x] = ClassUtils.forName(paramInfo[x].getType(), classLoader);
			}
		}
		return types;
	}

	/**
	 * 创建一个表示方法参数签名的 {@code String[]}。数组中的每个元素
	 * 是方法签名中对应参数的全限定类名。
	 * @param method 要构建参数签名的方法
	 * @return 由参数类型组成的签名数组
	 */
	public static String[] getMethodSignature(Method method) {
		Class<?>[] types = method.getParameterTypes();
		String[] signature = new String[types.length];
		for (int x = 0; x < types.length; x++) {
			signature[x] = types[x].getName();
		}
		return signature;
	}

	/**
	 * 返回给定 JavaBeans 属性对应的 JMX 属性名称。
	 * <p>使用严格命名约定时，getter 方法为 {@code getFoo()} 的
	 * JavaBean 属性对应的属性名为 {@code Foo}。禁用严格命名约定时，
	 * {@code getFoo()} 将对应为 {@code foo}。
	 * @param property JavaBeans 属性描述符
	 * @param useStrictCasing 是否使用严格命名约定
	 * @return 要使用的 JMX 属性名
	 */
	public static String getAttributeName(PropertyDescriptor property, boolean useStrictCasing) {
		if (useStrictCasing) {
			return StringUtils.capitalize(property.getName());
		}
		else {
			return property.getName();
		}
	}

	/**
	 * 向现有的 {@link ObjectName} 追加一个额外的键值对，其中键为固定值
	 * {@code identity}，值为暴露在所给 {@link ObjectName} 上的托管资源的身份哈希码。
	 * 这可用于为特定 bean 或类的每个不同实例提供唯一的 {@link ObjectName}。
	 * 在运行时基于 {@link org.springframework.jmx.export.naming.ObjectNamingStrategy}
	 * 提供的模板值为一组托管资源生成 {@link ObjectName} 时非常有用。
	 * @param objectName 原始的 JMX ObjectName
	 * @param managedResource MBean 实例
	 * @return 添加了 MBean 身份标识的 ObjectName
	 * @throws MalformedObjectNameException 如果对象名称规范无效
	 * @see org.springframework.util.ObjectUtils#getIdentityHexString(Object)
	 */
	public static ObjectName appendIdentityToObjectName(ObjectName objectName, Object managedResource)
			throws MalformedObjectNameException {

		Hashtable<String, String> keyProperties = objectName.getKeyPropertyList();
		keyProperties.put(IDENTITY_OBJECT_NAME_KEY, ObjectUtils.getIdentityHexString(managedResource));
		return ObjectNameManager.getInstance(objectName.getDomain(), keyProperties);
	}

	/**
	 * 返回给定 bean 要暴露的类或接口。
	 * 这是将被搜索属性和操作的类（例如检查注解）。
	 * <p>此实现对 CGLIB 代理返回其父类，
	 * 对其他情况（JDK 代理或普通 bean 类）返回给定 bean 的类。
	 * @param managedBean bean 实例（可能是 AOP 代理）
	 * @return 要暴露的 bean 类
	 * @see org.springframework.util.ClassUtils#getUserClass(Object)
	 */
	public static Class<?> getClassToExpose(Object managedBean) {
		return ClassUtils.getUserClass(managedBean);
	}

	/**
	 * 返回给定 bean 类要暴露的类或接口。
	 * 这是将被搜索属性和操作的类（例如检查注解）。
	 * <p>此实现对 CGLIB 代理返回其父类，
	 * 对其他情况（JDK 代理或普通 bean 类）返回给定 bean 的类。
	 * @param clazz bean 类（可能是 AOP 代理类）
	 * @return 要暴露的 bean 类
	 * @see org.springframework.util.ClassUtils#getUserClass(Class)
	 */
	public static Class<?> getClassToExpose(Class<?> clazz) {
		return ClassUtils.getUserClass(clazz);
	}

	/**
	 * 判断给定的 bean 类是否直接符合 MBean 的条件。
	 * <p>此实现检查 {@link javax.management.DynamicMBean} 类以及
	 * 具有对应 "*MBean" 接口的类（标准 MBean）或
	 * 具有对应 "*MXBean" 接口的类（Java MXBean）。
	 * @param clazz 要分析的 bean 类
	 * @return 该类是否符合 MBean 的条件
	 * @see org.springframework.jmx.export.MBeanExporter#isMBean(Class)
	 */
	public static boolean isMBean(@Nullable Class<?> clazz) {
		return (clazz != null &&
				(DynamicMBean.class.isAssignableFrom(clazz) ||
						(getMBeanInterface(clazz) != null || getMXBeanInterface(clazz) != null)));
	}

	/**
	 * 返回给定类的标准 MBean 接口（如果存在），
	 * 即接口名与给定类的类名相同但后缀为 "MBean" 的接口。
	 * @param clazz 要检查的类
	 * @return 给定类的标准 MBean 接口
	 */
	@Nullable
	public static Class<?> getMBeanInterface(@Nullable Class<?> clazz) {
		if (clazz == null || clazz.getSuperclass() == null) {
			return null;
		}
		String mbeanInterfaceName = clazz.getName() + MBEAN_SUFFIX;
		Class<?>[] implementedInterfaces = clazz.getInterfaces();
		for (Class<?> iface : implementedInterfaces) {
			if (iface.getName().equals(mbeanInterfaceName)) {
				return iface;
			}
		}
		return getMBeanInterface(clazz.getSuperclass());
	}

	/**
	 * 返回给定类的 Java MXBean 接口（如果存在），
	 * 即名称以 "MXBean" 结尾和/或带有合适 MXBean 注解的接口。
	 * @param clazz 要检查的类
	 * @return 给定类是否存在 MXBean 接口
	 */
	@Nullable
	public static Class<?> getMXBeanInterface(@Nullable Class<?> clazz) {
		if (clazz == null || clazz.getSuperclass() == null) {
			return null;
		}
		Class<?>[] implementedInterfaces = clazz.getInterfaces();
		for (Class<?> iface : implementedInterfaces) {
			if (JMX.isMXBeanInterface(iface)) {
				return iface;
			}
		}
		return getMXBeanInterface(clazz.getSuperclass());
	}

}
