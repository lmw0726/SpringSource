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

package org.springframework.jmx.export.assembler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.management.Descriptor;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanNotificationInfo;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.metadata.JmxMetadataUtils;
import org.springframework.jmx.export.metadata.ManagedAttribute;
import org.springframework.jmx.export.metadata.ManagedMetric;
import org.springframework.jmx.export.metadata.ManagedNotification;
import org.springframework.jmx.export.metadata.ManagedOperation;
import org.springframework.jmx.export.metadata.ManagedOperationParameter;
import org.springframework.jmx.export.metadata.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link MBeanInfoAssembler} 接口的实现，从源级元数据中读取管理接口信息。
 *
 * <p>使用 {@link JmxAttributeSource} 策略接口，因此可以使用任何支持的实现来读取元数据。
 * 开箱即用，Spring 提供了一个基于注解的实现：
 * {@code AnnotationJmxAttributeSource}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Jennifer Hickey
 * @since 1.2
 * @see #setAttributeSource
 * @see org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource
 */
public class MetadataMBeanInfoAssembler extends AbstractReflectiveMBeanInfoAssembler
		implements AutodetectCapableMBeanInfoAssembler, InitializingBean {

	@Nullable
	private JmxAttributeSource attributeSource;


	/**
	 * 创建一个新的 {@code MetadataMBeanInfoAssembler}，需要通过 {@link #setAttributeSource} 方法进行配置。
	 */
	public MetadataMBeanInfoAssembler() {
	}

	/**
	 * 创建一个新的 {@code MetadataMBeanInfoAssembler}，使用给定的 {@code JmxAttributeSource}。
	 * @param attributeSource 要使用的 JmxAttributeSource
	 */
	public MetadataMBeanInfoAssembler(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}


	/**
	 * 设置用于从 bean 类读取元数据的 {@code JmxAttributeSource} 实现。
	 * @see org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource
	 */
	public void setAttributeSource(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.attributeSource == null) {
			throw new IllegalArgumentException("Property 'attributeSource' is required");
		}
	}

	private JmxAttributeSource obtainAttributeSource() {
		Assert.state(this.attributeSource != null, "No JmxAttributeSource set");
		return this.attributeSource;
	}


	/**
	 * 如果遇到 JDK 动态代理则抛出 IllegalArgumentException。
	 * 只能从目标类和 CGLIB 代理读取元数据！
	 */
	@Override
	protected void checkManagedBean(Object managedBean) throws IllegalArgumentException {
		if (AopUtils.isJdkDynamicProxy(managedBean)) {
			throw new IllegalArgumentException(
					"MetadataMBeanInfoAssembler does not support JDK dynamic proxies - " +
					"export the target beans directly or use CGLIB proxies instead");
		}
	}

	/**
	 * 用于 bean 的自动检测。检查 bean 的类是否具有 {@code ManagedResource} 属性。
	 * 如果是，将添加到包含的 bean 列表中。
	 * @param beanClass bean 的类
	 * @param beanName bean 工厂中 bean 的名称
	 */
	@Override
	public boolean includeBean(Class<?> beanClass, String beanName) {
		return (obtainAttributeSource().getManagedResource(getClassToExpose(beanClass)) != null);
	}

	/**
	 * 对属性访问器的包含进行投票。
	 * @param method 访问器方法
	 * @param beanKey 与 beans map 中 MBean 关联的键
	 * @return 该方法是否具有适当的元数据
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return hasManagedAttribute(method) || hasManagedMetric(method);
	}

	/**
	 * 对属性修改器的包含进行投票。
	 * @param method 修改器方法
	 * @param beanKey 与 beans map 中 MBean 关联的键
	 * @return 该方法是否具有适当的元数据
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return hasManagedAttribute(method);
	}

	/**
	 * 对操作的包含进行投票。
	 * @param method 操作方法
	 * @param beanKey 与 beans map 中 MBean 关联的键
	 * @return 该方法是否具有适当的元数据
	 */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
		return (pd != null && hasManagedAttribute(method)) || hasManagedOperation(method);
	}

	/**
	 * 检查给定的方法是否具有 {@code ManagedAttribute} 属性。
	 */
	private boolean hasManagedAttribute(Method method) {
		return (obtainAttributeSource().getManagedAttribute(method) != null);
	}

	/**
	 * 检查给定的方法是否具有 {@code ManagedMetric} 属性。
	 */
	private boolean hasManagedMetric(Method method) {
		return (obtainAttributeSource().getManagedMetric(method) != null);
	}

	/**
	 * 检查给定的方法是否具有 {@code ManagedOperation} 属性。
	 * @param method 要检查的方法
	 */
	private boolean hasManagedOperation(Method method) {
		return (obtainAttributeSource().getManagedOperation(method) != null);
	}


	/**
	 * 从源级元数据读取托管资源描述。
	 * 如果找不到描述，则返回空 {@code String}。
	 */
	@Override
	protected String getDescription(Object managedBean, String beanKey) {
		ManagedResource mr = obtainAttributeSource().getManagedResource(getClassToExpose(managedBean));
		return (mr != null ? mr.getDescription() : "");
	}

	/**
	 * 为此属性描述符对应的属性创建描述。
	 * 尝试使用 getter 或 setter 属性中的元数据来创建描述，
	 * 否则使用属性名称。
	 */
	@Override
	protected String getAttributeDescription(PropertyDescriptor propertyDescriptor, String beanKey) {
		Method readMethod = propertyDescriptor.getReadMethod();
		Method writeMethod = propertyDescriptor.getWriteMethod();

		ManagedAttribute getter =
				(readMethod != null ? obtainAttributeSource().getManagedAttribute(readMethod) : null);
		ManagedAttribute setter =
				(writeMethod != null ? obtainAttributeSource().getManagedAttribute(writeMethod) : null);

		if (getter != null && StringUtils.hasText(getter.getDescription())) {
			return getter.getDescription();
		}
		else if (setter != null && StringUtils.hasText(setter.getDescription())) {
			return setter.getDescription();
		}

		ManagedMetric metric = (readMethod != null ? obtainAttributeSource().getManagedMetric(readMethod) : null);
		if (metric != null && StringUtils.hasText(metric.getDescription())) {
			return metric.getDescription();
		}

		return propertyDescriptor.getDisplayName();
	}

	/**
	 * 从元数据中检索给定 {@code Method} 的描述。
	 * 如果元数据中没有描述，则使用方法名称。
	 */
	@Override
	protected String getOperationDescription(Method method, String beanKey) {
		PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
		if (pd != null) {
			ManagedAttribute ma = obtainAttributeSource().getManagedAttribute(method);
			if (ma != null && StringUtils.hasText(ma.getDescription())) {
				return ma.getDescription();
			}
			ManagedMetric metric = obtainAttributeSource().getManagedMetric(method);
			if (metric != null && StringUtils.hasText(metric.getDescription())) {
				return metric.getDescription();
			}
			return method.getName();
		}
		else {
			ManagedOperation mo = obtainAttributeSource().getManagedOperation(method);
			if (mo != null && StringUtils.hasText(mo.getDescription())) {
				return mo.getDescription();
			}
			return method.getName();
		}
	}

	/**
	 * 从附加到方法的 {@code ManagedOperationParameter} 属性中读取 {@code MBeanParameterInfo}。
	 * 如果没有找到属性，则返回空的 {@code MBeanParameterInfo} 数组。
	 */
	@Override
	protected MBeanParameterInfo[] getOperationParameters(Method method, String beanKey) {
		ManagedOperationParameter[] params = obtainAttributeSource().getManagedOperationParameters(method);
		if (ObjectUtils.isEmpty(params)) {
			return super.getOperationParameters(method, beanKey);
		}

		MBeanParameterInfo[] parameterInfo = new MBeanParameterInfo[params.length];
		Class<?>[] methodParameters = method.getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			ManagedOperationParameter param = params[i];
			parameterInfo[i] =
					new MBeanParameterInfo(param.getName(), methodParameters[i].getName(), param.getDescription());
		}
		return parameterInfo;
	}

	/**
	 * 从托管资源的 {@code Class} 中读取 {@link ManagedNotification} 元数据，
	 * 并生成并返回相应的 {@link ModelMBeanNotificationInfo} 元数据。
	 */
	@Override
	protected ModelMBeanNotificationInfo[] getNotificationInfo(Object managedBean, String beanKey) {
		ManagedNotification[] notificationAttributes =
				obtainAttributeSource().getManagedNotifications(getClassToExpose(managedBean));
		ModelMBeanNotificationInfo[] notificationInfos =
				new ModelMBeanNotificationInfo[notificationAttributes.length];

		for (int i = 0; i < notificationAttributes.length; i++) {
			ManagedNotification attribute = notificationAttributes[i];
			notificationInfos[i] = JmxMetadataUtils.convertToModelMBeanNotificationInfo(attribute);
		}

		return notificationInfos;
	}

	/**
	 * 将 {@code ManagedResource} 属性中的描述符字段添加到 MBean 描述符。
	 * 具体来说，如果元数据中存在 {@code currencyTimeLimit}、
	 * {@code persistPolicy}、{@code persistPeriod}、{@code persistLocation}
	 * 和 {@code persistName} 描述符字段，则添加它们。
	 */
	@Override
	protected void populateMBeanDescriptor(Descriptor desc, Object managedBean, String beanKey) {
		ManagedResource mr = obtainAttributeSource().getManagedResource(getClassToExpose(managedBean));
		if (mr == null) {
			throw new InvalidMetadataException(
					"No ManagedResource attribute found for class: " + getClassToExpose(managedBean));
		}

		applyCurrencyTimeLimit(desc, mr.getCurrencyTimeLimit());

		if (mr.isLog()) {
			desc.setField(FIELD_LOG, "true");
		}
		if (StringUtils.hasLength(mr.getLogFile())) {
			desc.setField(FIELD_LOG_FILE, mr.getLogFile());
		}

		if (StringUtils.hasLength(mr.getPersistPolicy())) {
			desc.setField(FIELD_PERSIST_POLICY, mr.getPersistPolicy());
		}
		if (mr.getPersistPeriod() >= 0) {
			desc.setField(FIELD_PERSIST_PERIOD, Integer.toString(mr.getPersistPeriod()));
		}
		if (StringUtils.hasLength(mr.getPersistName())) {
			desc.setField(FIELD_PERSIST_NAME, mr.getPersistName());
		}
		if (StringUtils.hasLength(mr.getPersistLocation())) {
			desc.setField(FIELD_PERSIST_LOCATION, mr.getPersistLocation());
		}
	}

	/**
	 * 将 {@code ManagedAttribute} 属性或 {@code ManagedMetric} 属性中的
	 * 描述符字段添加到属性描述符。
	 */
	@Override
	protected void populateAttributeDescriptor(
			Descriptor desc, @Nullable Method getter, @Nullable Method setter, String beanKey) {

		if (getter != null) {
			ManagedMetric metric = obtainAttributeSource().getManagedMetric(getter);
			if (metric != null) {
				populateMetricDescriptor(desc, metric);
				return;
			}
		}

		ManagedAttribute gma = (getter != null ? obtainAttributeSource().getManagedAttribute(getter) : null);
		ManagedAttribute sma = (setter != null ? obtainAttributeSource().getManagedAttribute(setter) : null);
		populateAttributeDescriptor(desc,
				(gma != null ? gma : ManagedAttribute.EMPTY),
				(sma != null ? sma : ManagedAttribute.EMPTY));
	}

	private void populateAttributeDescriptor(Descriptor desc, ManagedAttribute gma, ManagedAttribute sma) {
		applyCurrencyTimeLimit(desc, resolveIntDescriptor(gma.getCurrencyTimeLimit(), sma.getCurrencyTimeLimit()));

		Object defaultValue = resolveObjectDescriptor(gma.getDefaultValue(), sma.getDefaultValue());
		desc.setField(FIELD_DEFAULT, defaultValue);

		String persistPolicy = resolveStringDescriptor(gma.getPersistPolicy(), sma.getPersistPolicy());
		if (StringUtils.hasLength(persistPolicy)) {
			desc.setField(FIELD_PERSIST_POLICY, persistPolicy);
		}
		int persistPeriod = resolveIntDescriptor(gma.getPersistPeriod(), sma.getPersistPeriod());
		if (persistPeriod >= 0) {
			desc.setField(FIELD_PERSIST_PERIOD, Integer.toString(persistPeriod));
		}
	}

	private void populateMetricDescriptor(Descriptor desc, ManagedMetric metric) {
		applyCurrencyTimeLimit(desc, metric.getCurrencyTimeLimit());

		if (StringUtils.hasLength(metric.getPersistPolicy())) {
			desc.setField(FIELD_PERSIST_POLICY, metric.getPersistPolicy());
		}
		if (metric.getPersistPeriod() >= 0) {
			desc.setField(FIELD_PERSIST_PERIOD, Integer.toString(metric.getPersistPeriod()));
		}

		if (StringUtils.hasLength(metric.getDisplayName())) {
			desc.setField(FIELD_DISPLAY_NAME, metric.getDisplayName());
		}

		if (StringUtils.hasLength(metric.getUnit())) {
			desc.setField(FIELD_UNITS, metric.getUnit());
		}

		if (StringUtils.hasLength(metric.getCategory())) {
			desc.setField(FIELD_METRIC_CATEGORY, metric.getCategory());
		}

		desc.setField(FIELD_METRIC_TYPE, metric.getMetricType().toString());
	}

	/**
	 * 将 {@code ManagedAttribute} 属性中的描述符字段添加到属性描述符。
	 * 具体来说，如果元数据中存在 {@code currencyTimeLimit} 描述符字段，则添加它。
	 */
	@Override
	protected void populateOperationDescriptor(Descriptor desc, Method method, String beanKey) {
		ManagedOperation mo = obtainAttributeSource().getManagedOperation(method);
		if (mo != null) {
			applyCurrencyTimeLimit(desc, mo.getCurrencyTimeLimit());
		}
	}

	/**
	 * 确定两个 {@code int} 值中的哪一个应该用作属性描述符的值。
	 * 通常，只有 getter 或 setter 会有一个非负值，因此我们使用该值。
	 * 如果两个值都是非负的，则使用两者中较大的那个。
	 * 此方法可用于解析具有两个可能值的 {@code int} 类型描述符。
	 * @param getter 与此属性的 getter 关联的 int 值
	 * @param setter 与此属性的 setter 关联的 int 值
	 */
	private int resolveIntDescriptor(int getter, int setter) {
		return (getter >= setter ? getter : setter);
	}

	/**
	 * 根据附加到 getter 和 setter 方法的值定位描述符的值。
	 * 如果两者都提供了值，则优先使用附加到 getter 的值。
	 * @param getter 与 get 方法关联的 Object 值
	 * @param setter 与 set 方法关联的 Object 值
	 * @return 用作描述符值的适当 Object
	 */
	@Nullable
	private Object resolveObjectDescriptor(@Nullable Object getter, @Nullable Object setter) {
		return (getter != null ? getter : setter);
	}

	/**
	 * 根据附加到 getter 和 setter 方法的值定位描述符的值。
	 * 如果两者都提供了值，则优先使用附加到 getter 的值。
	 * 提供的默认值用于检查与 getter 关联的值是否已从默认值更改。
	 * @param getter 与 get 方法关联的 String 值
	 * @param setter 与 set 方法关联的 String 值
	 * @return 用作描述符值的适当 String
	 */
	@Nullable
	private String resolveStringDescriptor(@Nullable String getter, @Nullable String setter) {
		return (StringUtils.hasLength(getter) ? getter : setter);
	}

}
