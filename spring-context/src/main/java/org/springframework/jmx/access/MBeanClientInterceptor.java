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

package org.springframework.jmx.access;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXServiceURL;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} 实现，将调用路由到
 * 运行在所提供的 {@code MBeanServerConnection} 上的 MBean。
 * 适用于本地和远程 {@code MBeanServerConnection}。
 *
 * <p>默认情况下，{@code MBeanClientInterceptor} 会在启动时连接到
 * {@code MBeanServer} 并缓存 MBean 元数据。当连接的是远程 {@code MBeanServer}
 * 而该服务器在应用启动时可能尚未运行时，这种行为可能不太理想。通过将
 * {@link #setConnectOnStartup(boolean) connectOnStartup} 属性设置为 "false"，
 * 可以将此过程推迟到首次通过代理进行调用时执行。
 *
 * <p>此功能通常通过 {@link MBeanProxyFactoryBean} 使用。
 * 详情请参阅该类的 javadoc。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanProxyFactoryBean
 * @see #setConnectOnStartup
 */
public class MBeanClientInterceptor
		implements MethodInterceptor, BeanClassLoaderAware, InitializingBean, DisposableBean {

	/** 子类可用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private MBeanServerConnection server;

	@Nullable
	private JMXServiceURL serviceUrl;

	@Nullable
	private Map<String, ?> environment;

	@Nullable
	private String agentId;

	private boolean connectOnStartup = true;

	private boolean refreshOnConnectFailure = false;

	@Nullable
	private ObjectName objectName;

	private boolean useStrictCasing = true;

	@Nullable
	private Class<?> managementInterface;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private final ConnectorDelegate connector = new ConnectorDelegate();

	@Nullable
	private MBeanServerConnection serverToUse;

	@Nullable
	private MBeanServerInvocationHandler invocationHandler;

	private Map<String, MBeanAttributeInfo> allowedAttributes = Collections.emptyMap();

	private Map<MethodCacheKey, MBeanOperationInfo> allowedOperations = Collections.emptyMap();

	private final Map<Method, String[]> signatureCache = new HashMap<>();

	private final Object preparationMonitor = new Object();


	/**
	 * 设置用于连接到所有调用路由到的 MBean 的
	 * {@code MBeanServerConnection}。
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * 设置远程 {@code MBeanServer} 的服务 URL。
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * 指定 JMX 连接器的环境属性。
	 * @see javax.management.remote.JMXConnectorFactory#connect(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	public void setEnvironment(@Nullable Map<String, ?> environment) {
		this.environment = environment;
	}

	/**
	 * 允许通过 Map 方式访问为连接器设置的环境属性，
	 * 并可选择添加或覆盖特定的条目。
	 * <p>适合直接指定条目，例如通过
	 * "environment[myKey]" 的方式。在子 bean 定义中
	 * 添加或覆盖条目时特别有用。
	 */
	@Nullable
	public Map<String, ?> getEnvironment() {
		return this.environment;
	}

	/**
	 * 设置要定位的 {@code MBeanServer} 的代理 ID。
	 * <p>默认值为无。如果指定了此属性，将会尝试定位
	 * 附带的 MBeanServer，除非已设置
	 * {@link #setServiceUrl "serviceUrl"} 属性。
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 * <p>指定空字符串表示平台 MBeanServer。
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/**
	 * 设置代理是在创建时（"true"）还是在首次调用时（"false"）连接到 {@code MBeanServer}。
	 * 默认值为 "true"。
	 */
	public void setConnectOnStartup(boolean connectOnStartup) {
		this.connectOnStartup = connectOnStartup;
	}

	/**
	 * 设置连接失败时是否刷新 MBeanServer 连接。
	 * 默认值为 "false"。
	 * <p>可以开启此选项以允许 JMX 服务器热重启，
	 * 在发生 IOException 时自动重新连接并重试。
	 */
	public void setRefreshOnConnectFailure(boolean refreshOnConnectFailure) {
		this.refreshOnConnectFailure = refreshOnConnectFailure;
	}

	/**
	 * 设置调用所路由到的 MBean 的 {@code ObjectName}，
	 * 可以是 {@code ObjectName} 实例或 {@code String}。
	 */
	public void setObjectName(Object objectName) throws MalformedObjectNameException {
		this.objectName = ObjectNameManager.getInstance(objectName);
	}

	/**
	 * 设置属性是否使用严格命名规则。默认启用。
	 * <p>使用严格命名规则时，具有 {@code getFoo()} 等 getter 的 JavaBean 属性
	 * 将转换为名为 {@code Foo} 的属性。
	 * 禁用严格命名规则时，{@code getFoo()} 将转换为 {@code foo}。
	 */
	public void setUseStrictCasing(boolean useStrictCasing) {
		this.useStrictCasing = useStrictCasing;
	}

	/**
	 * 设置目标 MBean 的管理接口，暴露 bean 属性的 setter 和 getter
	 * 用于 MBean 属性，以及常规 Java 方法用于 MBean 操作。
	 */
	public void setManagementInterface(@Nullable Class<?> managementInterface) {
		this.managementInterface = managementInterface;
	}

	/**
	 * 返回目标 MBean 的管理接口，
	 * 如果未指定则返回 {@code null}。
	 */
	@Nullable
	protected final Class<?> getManagementInterface() {
		return this.managementInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 如果 "connectOnStartup" 已开启（默认为开启），则准备 {@code MBeanServerConnection}。
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.server != null && this.refreshOnConnectFailure) {
			throw new IllegalArgumentException("'refreshOnConnectFailure' does not work when setting " +
					"a 'server' reference. Prefer 'serviceUrl' etc instead.");
		}
		if (this.connectOnStartup) {
			prepare();
		}
	}

	/**
	 * 确保已配置 {@code MBeanServerConnection}，如果未提供则尝试检测本地连接。
	 */
	public void prepare() {
		synchronized (this.preparationMonitor) {
			if (this.server != null) {
				this.serverToUse = this.server;
			}
			else {
				this.serverToUse = null;
				this.serverToUse = this.connector.connect(this.serviceUrl, this.environment, this.agentId);
			}
			this.invocationHandler = null;
			if (this.useStrictCasing) {
				Assert.state(this.objectName != null, "No ObjectName set");
				// 使用 JDK 自带的 MBeanServerInvocationHandler，特别是为了原生 MXBean 支持。
				this.invocationHandler = new MBeanServerInvocationHandler(this.serverToUse, this.objectName,
						(this.managementInterface != null && JMX.isMXBeanInterface(this.managementInterface)));
			}
			else {
				// 非严格命名只能通过自定义调用处理来实现。
				// 仅提供部分 MXBean 支持！
				retrieveMBeanInfo(this.serverToUse);
			}
		}
	}
	/**
	 * 将已配置 MBean 的管理接口信息加载到缓存中。
	 * 代理在确定调用是否匹配受管资源管理接口上的有效操作或属性时会使用此信息。
	 */
	private void retrieveMBeanInfo(MBeanServerConnection server) throws MBeanInfoRetrievalException {
		try {
			MBeanInfo info = server.getMBeanInfo(this.objectName);

			MBeanAttributeInfo[] attributeInfo = info.getAttributes();
			this.allowedAttributes = CollectionUtils.newHashMap(attributeInfo.length);
			for (MBeanAttributeInfo infoEle : attributeInfo) {
				this.allowedAttributes.put(infoEle.getName(), infoEle);
			}

			MBeanOperationInfo[] operationInfo = info.getOperations();
			this.allowedOperations = CollectionUtils.newHashMap(operationInfo.length);
			for (MBeanOperationInfo infoEle : operationInfo) {
				Class<?>[] paramTypes = JmxUtils.parameterInfoToTypes(infoEle.getSignature(), this.beanClassLoader);
				this.allowedOperations.put(new MethodCacheKey(infoEle.getName(), paramTypes), infoEle);
			}
		}
		catch (ClassNotFoundException ex) {
			throw new MBeanInfoRetrievalException("Unable to locate class specified in method signature", ex);
		}
		catch (IntrospectionException ex) {
			throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName + "]", ex);
		}
		catch (InstanceNotFoundException ex) {
			// 如果执行到这里，这不应该发生，但以防万一...
			throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName +
					"]: it is likely that this bean was unregistered during the proxy creation process",
					ex);
		}
		catch (ReflectionException ex) {
			throw new MBeanInfoRetrievalException("Unable to read MBean info for bean [ " + this.objectName + "]", ex);
		}
		catch (IOException ex) {
			throw new MBeanInfoRetrievalException("An IOException occurred when communicating with the " +
					"MBeanServer. It is likely that you are communicating with a remote MBeanServer. " +
					"Check the inner exception for exact details.", ex);
		}
	}

	/**
	 * 返回此客户端拦截器是否已经准备好，
	 * 即是否已经查找了服务器并缓存了所有元数据。
	 */
	protected boolean isPrepared() {
		synchronized (this.preparationMonitor) {
			return (this.serverToUse != null);
		}
	}


	/**
	 * 将调用路由到已配置的受管资源。
	 * @param invocation 要重新路由的 {@code MethodInvocation}
	 * @return 重新路由调用后返回的值
	 * @throws Throwable 传播给用户的调用错误
	 * @see #doInvoke
	 * @see #handleConnectFailure
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// 如果需要，延迟连接到 MBeanServer。
		synchronized (this.preparationMonitor) {
			if (!isPrepared()) {
				prepare();
			}
		}
		try {
			return doInvoke(invocation);
		}
		catch (MBeanConnectFailureException | IOException ex) {
			return handleConnectFailure(invocation, ex);
		}
	}

	/**
	 * 刷新连接并尽可能重试 MBean 调用。
	 * <p>如果未配置连接失败时刷新，此方法将直接重新抛出原始异常。
	 * @param invocation 失败的调用
	 * @param ex 远程调用引发的异常
	 * @return 新调用的结果值（如果成功）
	 * @throws Throwable 新调用引发的异常（如果也失败了）
	 * @see #setRefreshOnConnectFailure
	 * @see #doInvoke
	 */
	@Nullable
	protected Object handleConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshOnConnectFailure) {
			String msg = "Could not connect to JMX server - retrying";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn(msg);
			}
			prepare();
			return doInvoke(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 将调用路由到已配置的受管资源。正确地将 JavaBean 属性访问
	 * 路由到 {@code MBeanServerConnection.get/setAttribute}，将方法调用
	 * 路由到 {@code MBeanServerConnection.invoke}。
	 * @param invocation 要重新路由的 {@code MethodInvocation}
	 * @return 重新路由调用后返回的值
	 * @throws Throwable 传播给用户的调用错误
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		try {
			Object result;
			if (this.invocationHandler != null) {
				result = this.invocationHandler.invoke(invocation.getThis(), method, invocation.getArguments());
			}
			else {
				PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
				if (pd != null) {
					result = invokeAttribute(pd, invocation);
				}
				else {
					result = invokeOperation(method, invocation.getArguments());
				}
			}
			return convertResultValueIfNecessary(result, new MethodParameter(method, -1));
		}
		catch (MBeanException ex) {
			throw ex.getTargetException();
		}
		catch (RuntimeMBeanException ex) {
			throw ex.getTargetException();
		}
		catch (RuntimeErrorException ex) {
			throw ex.getTargetError();
		}
		catch (RuntimeOperationsException ex) {
			// 此异常仅由 JMX 1.2 RI 抛出，而非 JDK 1.5 JMX 代码。
			RuntimeException rex = ex.getTargetException();
			if (rex instanceof RuntimeMBeanException) {
				throw ((RuntimeMBeanException) rex).getTargetException();
			}
			else if (rex instanceof RuntimeErrorException) {
				throw ((RuntimeErrorException) rex).getTargetError();
			}
			else {
				throw rex;
			}
		}
		catch (OperationsException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new InvalidInvocationException(ex.getMessage());
			}
		}
		catch (JMException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new InvocationFailureException("JMX access failed", ex);
			}
		}
		catch (IOException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new MBeanConnectFailureException("I/O failure during JMX access", ex);
			}
		}
	}

	@Nullable
	private Object invokeAttribute(PropertyDescriptor pd, MethodInvocation invocation)
			throws JMException, IOException {

		Assert.state(this.serverToUse != null, "No MBeanServerConnection available");

		String attributeName = JmxUtils.getAttributeName(pd, this.useStrictCasing);
		MBeanAttributeInfo inf = this.allowedAttributes.get(attributeName);
		// 如果未返回属性，则说明它未在管理接口中定义。
		if (inf == null) {
			throw new InvalidInvocationException(
					"Attribute '" + pd.getName() + "' is not exposed on the management interface");
		}

		if (invocation.getMethod().equals(pd.getReadMethod())) {
			if (inf.isReadable()) {
				return this.serverToUse.getAttribute(this.objectName, attributeName);
			}
			else {
				throw new InvalidInvocationException("Attribute '" + attributeName + "' is not readable");
			}
		}
		else if (invocation.getMethod().equals(pd.getWriteMethod())) {
			if (inf.isWritable()) {
				this.serverToUse.setAttribute(this.objectName, new Attribute(attributeName, invocation.getArguments()[0]));
				return null;
			}
			else {
				throw new InvalidInvocationException("Attribute '" + attributeName + "' is not writable");
			}
		}
		else {
			throw new IllegalStateException(
					"Method [" + invocation.getMethod() + "] is neither a bean property getter nor a setter");
		}
	}

	/**
	 * 将方法调用（非属性 get/set）路由到受管资源上的相应操作。
	 * @param method 对应受管资源操作的方法
	 * @param args 调用参数
	 * @return 方法调用返回的值。
	 */
	private Object invokeOperation(Method method, Object[] args) throws JMException, IOException {
		Assert.state(this.serverToUse != null, "No MBeanServerConnection available");

		MethodCacheKey key = new MethodCacheKey(method.getName(), method.getParameterTypes());
		MBeanOperationInfo info = this.allowedOperations.get(key);
		if (info == null) {
			throw new InvalidInvocationException("Operation '" + method.getName() +
					"' is not exposed on the management interface");
		}

		String[] signature;
		synchronized (this.signatureCache) {
			signature = this.signatureCache.get(method);
			if (signature == null) {
				signature = JmxUtils.getMethodSignature(method);
				this.signatureCache.put(method, signature);
			}
		}

		return this.serverToUse.invoke(this.objectName, method.getName(), args, signature);
	}

	/**
	 * 将给定的结果对象（来自属性访问或操作调用）转换为指定的目标类，
	 * 以便从代理方法返回。
	 * @param result {@code MBeanServer} 返回的结果对象
	 * @param parameter 已调用代理方法的方法参数
	 * @return 转换后的结果对象，如果不需要转换则返回传入的对象
	 */
	@Nullable
	protected Object convertResultValueIfNecessary(@Nullable Object result, MethodParameter parameter) {
		Class<?> targetClass = parameter.getParameterType();
		try {
			if (result == null) {
				return null;
			}
			if (ClassUtils.isAssignableValue(targetClass, result)) {
				return result;
			}
			if (result instanceof CompositeData) {
				Method fromMethod = targetClass.getMethod("from", CompositeData.class);
				return ReflectionUtils.invokeMethod(fromMethod, null, result);
			}
			else if (result instanceof CompositeData[]) {
				CompositeData[] array = (CompositeData[]) result;
				if (targetClass.isArray()) {
					return convertDataArrayToTargetArray(array, targetClass);
				}
				else if (Collection.class.isAssignableFrom(targetClass)) {
					Class<?> elementType =
							ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
					if (elementType != null) {
						return convertDataArrayToTargetCollection(array, targetClass, elementType);
					}
				}
			}
			else if (result instanceof TabularData) {
				Method fromMethod = targetClass.getMethod("from", TabularData.class);
				return ReflectionUtils.invokeMethod(fromMethod, null, result);
			}
			else if (result instanceof TabularData[]) {
				TabularData[] array = (TabularData[]) result;
				if (targetClass.isArray()) {
					return convertDataArrayToTargetArray(array, targetClass);
				}
				else if (Collection.class.isAssignableFrom(targetClass)) {
					Class<?> elementType =
							ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
					if (elementType != null) {
						return convertDataArrayToTargetCollection(array, targetClass, elementType);
					}
				}
			}
			throw new InvocationFailureException(
					"Incompatible result value [" + result + "] for target type [" + targetClass.getName() + "]");
		}
		catch (NoSuchMethodException ex) {
			throw new InvocationFailureException(
					"Could not obtain 'from(CompositeData)' / 'from(TabularData)' method on target type [" +
							targetClass.getName() + "] for conversion of MXBean data structure [" + result + "]");
		}
	}

	private Object convertDataArrayToTargetArray(Object[] array, Class<?> targetClass) throws NoSuchMethodException {
		Class<?> targetType = targetClass.getComponentType();
		Method fromMethod = targetType.getMethod("from", array.getClass().getComponentType());
		Object resultArray = Array.newInstance(targetType, array.length);
		for (int i = 0; i < array.length; i++) {
			Array.set(resultArray, i, ReflectionUtils.invokeMethod(fromMethod, null, array[i]));
		}
		return resultArray;
	}

	private Collection<?> convertDataArrayToTargetCollection(Object[] array, Class<?> collectionType, Class<?> elementType)
			throws NoSuchMethodException {

		Method fromMethod = elementType.getMethod("from", array.getClass().getComponentType());
		Collection<Object> resultColl = CollectionFactory.createCollection(collectionType, Array.getLength(array));
		for (int i = 0; i < array.length; i++) {
			resultColl.add(ReflectionUtils.invokeMethod(fromMethod, null, array[i]));
		}
		return resultColl;
	}


	@Override
	public void destroy() {
		this.connector.close();
	}


	/**
	 * 方法名及其签名的简单包装类。
	 * 用作缓存方法时的键。
	 */
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final String name;

		private final Class<?>[] parameterTypes;

		/**
		 * 使用提供的方法名和参数列表创建 {@code MethodCacheKey} 的新实例。
		 * @param name 方法名
		 * @param parameterTypes 方法签名中的参数类型
		 */
		public MethodCacheKey(String name, @Nullable Class<?>[] parameterTypes) {
			this.name = name;
			this.parameterTypes = (parameterTypes != null ? parameterTypes : new Class<?>[0]);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof MethodCacheKey)) {
				return false;
			}
			MethodCacheKey otherKey = (MethodCacheKey) other;
			return (this.name.equals(otherKey.name) && Arrays.equals(this.parameterTypes, otherKey.parameterTypes));
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return this.name + "(" + StringUtils.arrayToCommaDelimitedString(this.parameterTypes) + ")";
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.name.compareTo(other.name);
			if (result != 0) {
				return result;
			}
			if (this.parameterTypes.length < other.parameterTypes.length) {
				return -1;
			}
			if (this.parameterTypes.length > other.parameterTypes.length) {
				return 1;
			}
			for (int i = 0; i < this.parameterTypes.length; i++) {
				result = this.parameterTypes[i].getName().compareTo(other.parameterTypes[i].getName());
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
