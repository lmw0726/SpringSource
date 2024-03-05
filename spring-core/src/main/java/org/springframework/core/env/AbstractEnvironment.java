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

package org.springframework.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.security.AccessControlException;
import java.util.*;

/**
 * {@link Environment}实现的抽象基类。
 * 支持保留的默认配置文件名的概念，并通过{@link #ACTIVE_PROFILES_PROPERTY_NAME}和{@link #DEFAULT_PROFILES_PROPERTY_NAME}属性指定活动和默认配置文件。
 *
 * <p>具体的子类主要区别在于它们默认添加哪些{@link PropertySource}对象。
 * {@code AbstractEnvironment}不添加任何属性源。
 * 子类应通过受保护的{@link #customizePropertySources(MutablePropertySources)}挂钩贡献属性源，
 * 而客户端应使用{@link ConfigurableEnvironment#getPropertySources()}进行自定义，并使用{@link MutablePropertySources} API进行操作。
 * 有关用法示例，请参阅{@link ConfigurableEnvironment} javadoc。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @see ConfigurableEnvironment
 * @see StandardEnvironment
 * @since 3.1
 */
public abstract class AbstractEnvironment implements ConfigurableEnvironment {

	/**
	 * 指示Spring忽略系统环境变量的系统属性，即永远不尝试通过{@link System#getenv()}检索这样的变量。
	 * <p>默认值为“false”，如果Spring环境属性（例如配置字符串中的占位符）在其他情况下不可解析，则回退到系统环境变量检查。如果您从Spring收到{@code getenv}调用的日志警告（例如，在WebSphere上采用严格的SecurityManager设置和AccessControlExceptions警告），请考虑将此标志切换为“true”。
	 *
	 * @see #suppressGetenvAccess()
	 */
	public static final String IGNORE_GETENV_PROPERTY_NAME = "spring.getenv.ignore";

	/**
	 * 要设置以指定活动配置文件的属性名称: {@value}。值可以用逗号分隔。
	 * <p>
	 * 请注意，某些shell环境 (例如Bash) 不允许在变量名称中使用句点字符。
	 * 假设正在使用Spring的 {@link SystemEnvironmentPropertySource}，则此属性可以指定为 {@code SPRING_PROFILES_ACTIVE} 作为环境变量。
	 *
	 * @see ConfigurableEnvironment#setActiveProfiles
	 */
	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active";

	/**
	 * 用于指定默认情况下活动配置文件的属性名称：{@value}。值可以用逗号分隔。
	 * <p>请注意，某些shell环境（例如Bash）不允许在变量名中使用句点字符。
	 * 假设正在使用Spring的{@link SystemEnvironmentPropertySource}，则可以将此属性指定为环境变量，
	 * 例如{@code SPRING_PROFILES_DEFAULT}。
	 *
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 */
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profiles.default";

	/**
	 * 保留的默认配置文件名称的名称：{@value}。
	 * 如果没有显式设置默认配置文件名称并且没有显式设置活动配置文件名称，
	 * 则此配置文件将默认情况下自动激活。
	 *
	 * @see #getReservedDefaultProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	protected static final String RESERVED_DEFAULT_PROFILE_NAME = "default";


	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 活动的环境配置，这里是有序的
	 */
	private final Set<String> activeProfiles = new LinkedHashSet<>();
	/**
	 * 默认的环境配置，这里是有序的
	 */
	private final Set<String> defaultProfiles = new LinkedHashSet<>(getReservedDefaultProfiles());

	/**
	 * 可变属性源
	 */
	private final MutablePropertySources propertySources;

	/**
	 * 可配置的属性解析器
	 */
	private final ConfigurablePropertyResolver propertyResolver;


	/**
	 * 创建一个新的{@code Environment}实例，在构造过程中调用{@link #customizePropertySources(MutablePropertySources)}以允许子类根据需要贡献或操作{@link PropertySource}实例。
	 *
	 * @see #customizePropertySources(MutablePropertySources)
	 */
	public AbstractEnvironment() {
		this(new MutablePropertySources());
	}

	/**
	 * 创建一个新的{@code Environment}实例，并使用特定的{@link MutablePropertySources}实例，
	 * 在构造过程中调用{@link #customizePropertySources(MutablePropertySources)}以允许子类根据需要贡献或操作{@link PropertySource}实例。
	 *
	 * @param propertySources 要使用的属性源
	 * @see #customizePropertySources(MutablePropertySources)
	 * @since 5.3.4
	 */
	protected AbstractEnvironment(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
		this.propertyResolver = createPropertyResolver(propertySources);
		customizePropertySources(propertySources);
	}


	/**
	 * 工厂方法用于创建Environment使用的{@link ConfigurablePropertyResolver}实例。
	 *
	 * @see #getPropertyResolver()
	 * @since 5.3.4
	 */
	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
		return new PropertySourcesPropertyResolver(propertySources);
	}

	/**
	 * 返回由{@link Environment}使用的{@link ConfigurablePropertyResolver}。
	 *
	 * @see #createPropertyResolver(MutablePropertySources)
	 * @since 5.3.4
	 */
	protected final ConfigurablePropertyResolver getPropertyResolver() {
		return this.propertyResolver;
	}

	/**
	 * 自定义在调用{@link #getProperty(String)}和相关方法时由此{@code Environment}搜索的{@link PropertySource}对象集合。
	 *
	 * <p>鼓励覆盖此方法的子类使用{@link MutablePropertySources#addLast(PropertySource)}添加属性源，以便进一步的子类调用{@code super.customizePropertySources()}具有可预测的结果。例如：
	 *
	 * <pre class="code">
	 * public class Level1Environment extends AbstractEnvironment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // 基类中无操作
	 *         propertySources.addLast(new PropertySourceA(...));
	 *         propertySources.addLast(new PropertySourceB(...));
	 *     }
	 * }
	 *
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // 添加从超类继承的所有
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>在这种安排下，属性将按照顺序A、B、C、D解析。也就是说，属性源“A”优先于属性源“D”。如果{@code Level2Environment}子类希望给属性源C和D优先于A和B，它可以简单地在添加自己的属性源之后而不是之前调用{@code super.customizePropertySources}：
	 *
	 * <pre class="code">
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *         super.customizePropertySources(propertySources); // 添加从超类继承的所有
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>现在搜索顺序是C、D、A、B，如所需。
	 *
	 * <p>除了这些建议，子类可以使用{@link MutablePropertySources}暴露的任何{@code add*}、{@code remove}或{@code replace}方法，以创建所需的属性源排列。
	 *
	 * <p>基类实现不注册任何属性源。
	 *
	 * <p>请注意，任何{@link ConfigurableEnvironment}的客户端都可以通过{@link #getPropertySources()}访问器进一步自定义属性源，通常在{@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}中。例如：
	 *
	 * <pre class="code">
	 * ConfigurableEnvironment env = new StandardEnvironment();
	 * env.getPropertySources().addLast(new PropertySourceX(...));
	 * </pre>
	 *
	 * <h2>关于实例变量访问的警告</h2>
	 * <p>子类中声明并具有默认初始值的实例变量不应从此方法中访问。由于Java对象创建生命周期约束，当{@link #AbstractEnvironment()}构造函数调用此回调时，任何初始值尚未分配，这可能导致{@code NullPointerException}或其他问题。如果需要访问实例变量的默认值，请将此方法保留为无操作，并直接在子类构造函数中执行属性源操作和实例变量访问。请注意，<em>分配</em>值给实例变量不是问题；只有尝试读取默认值必须避免。
	 *
	 * @see MutablePropertySources
	 * @see PropertySourcesPropertyResolver
	 * @see org.springframework.context.ApplicationContextInitializer
	 */
	protected void customizePropertySources(MutablePropertySources propertySources) {
	}

	/**
	 * 返回保留的默认配置文件名称集合。此实现返回{@value #RESERVED_DEFAULT_PROFILE_NAME}。
	 * 子类可以覆盖以定制保留名称集合。
	 *
	 * @see #RESERVED_DEFAULT_PROFILE_NAME
	 * @see #doGetDefaultProfiles()
	 */
	protected Set<String> getReservedDefaultProfiles() {
		return Collections.singleton(RESERVED_DEFAULT_PROFILE_NAME);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableEnvironment interface
	//---------------------------------------------------------------------

	@Override
	public String[] getActiveProfiles() {
		return StringUtils.toStringArray(doGetActiveProfiles());
	}

	/**
	 * 返回通过{@link #setActiveProfiles} 显式设置的活动配置文件集，
	 * 或者如果当前活动配置文件集为空，请检查是否存在 {@link #doGetActiveProfilesProperty()}，并将其值分配给活动配置文件集。
	 *
	 * @see #getActiveProfiles()
	 * @see #doGetActiveProfilesProperty()
	 */
	protected Set<String> doGetActiveProfiles() {
		synchronized (this.activeProfiles) {
			if (this.activeProfiles.isEmpty()) {
				//如果活动的环境配置为空，获取激活的环境配置属性
				String profiles = doGetActiveProfilesProperty();
				if (StringUtils.hasText(profiles)) {
					//以逗号分隔的字符串，并设置为活动的配置
					setActiveProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.activeProfiles;
		}
	}

	/**
	 * 返回活动配置文件的属性值。
	 *
	 * @see #ACTIVE_PROFILES_PROPERTY_NAME
	 * @since 5.3.4
	 */
	@Nullable
	protected String doGetActiveProfilesProperty() {
		return getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Override
	public void setActiveProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profiles " + Arrays.toString(profiles));
		}
		// 同步操作以确保线程安全
		synchronized (this.activeProfiles) {
			// 清除当前活动的配置文件列表
			this.activeProfiles.clear();
			// 遍历传入的配置文件列表
			for (String profile : profiles) {
				// 验证配置文件是否合法
				validateProfile(profile);
				// 将配置文件添加到活动配置文件列表中
				this.activeProfiles.add(profile);
			}
		}
	}

	@Override
	public void addActiveProfile(String profile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profile '" + profile + "'");
		}
		// 验证配置文件是否合法
		validateProfile(profile);
		// 获取当前活动的配置文件列表
		doGetActiveProfiles();
		// 同步操作以确保线程安全
		synchronized (this.activeProfiles) {
			// 将配置文件添加到活动配置文件列表中
			this.activeProfiles.add(profile);
		}
	}


	@Override
	public String[] getDefaultProfiles() {
		return StringUtils.toStringArray(doGetDefaultProfiles());
	}

	/**
	 * 返回通过{@link #setDefaultProfiles(String...)}明确设置的默认配置文件集，或者如果当前默认配置文件集仅由{@linkplain #getReservedDefaultProfiles()保留的默认配置文件}组成，
	 * 则检查{@link #doGetActiveProfilesProperty()}的存在并将其值（如果有）分配给默认配置文件集。
	 *
	 * @see #AbstractEnvironment()
	 * @see #getDefaultProfiles()
	 * @see #getReservedDefaultProfiles()
	 * @see #doGetDefaultProfilesProperty()
	 */
	protected Set<String> doGetDefaultProfiles() {
		// 同步操作以确保线程安全
		synchronized (this.defaultProfiles) {
			// 如果默认配置文件与保留的默认配置文件相同
			if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
				// 获取默认配置文件属性值
				String profiles = doGetDefaultProfilesProperty();
				// 如果属性值非空
				if (StringUtils.hasText(profiles)) {
					// 将属性值转换为字符串数组，并设置为默认配置文件
					setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			// 返回默认配置文件列表
			return this.defaultProfiles;
		}
	}

	/**
	 * 返回默认配置文件的属性值。
	 *
	 * @see #DEFAULT_PROFILES_PROPERTY_NAME
	 * @since 5.3.4
	 */
	@Nullable
	protected String doGetDefaultProfilesProperty() {
		return getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
	}

	/**
	 * 指定要默认激活的配置文件集，如果没有通过 {@link #setActiveProfiles} 显式激活其他配置文件。
	 * <p>调用此方法会覆盖在环境构建过程中添加的任何保留的默认配置文件。
	 *
	 * @param profiles 要设置为默认激活的配置文件集
	 * @see #AbstractEnvironment()
	 * @see #getReservedDefaultProfiles()
	 */
	@Override
	public void setDefaultProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		// 同步操作以确保线程安全
		synchronized (this.defaultProfiles) {
			// 清空默认配置文件列表
			this.defaultProfiles.clear();
			// 遍历传入的配置文件列表
			for (String profile : profiles) {
				// 验证配置文件的有效性
				validateProfile(profile);
				// 将验证通过的配置文件添加到默认配置文件列表中
				this.defaultProfiles.add(profile);
			}
		}
	}

	@Override
	@Deprecated
	public boolean acceptsProfiles(String... profiles) {
		Assert.notEmpty(profiles, "Must specify at least one profile");
		for (String profile : profiles) {
			if (StringUtils.hasLength(profile) && profile.charAt(0) == '!') {
				//如果profile有长度，且profile的第一个字符是“!”，
				if (!isProfileActive(profile.substring(1))) {
					//如果profile第一个字符后的字符串是非激活的，则返回true
					return true;
				}
			} else if (isProfileActive(profile)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean acceptsProfiles(Profiles profiles) {
		Assert.notNull(profiles, "Profiles must not be null");
		return profiles.matches(this::isProfileActive);
	}

	/**
	 * 返回给定的配置文件是否处于活动状态，或者如果活动配置文件为空，则默认情况下该配置文件是否处于活动状态。
	 *
	 * @throws IllegalArgumentException 详见 {@link #validateProfile(String)}
	 */
	protected boolean isProfileActive(String profile) {
		//校验profile是否是非法参数
		validateProfile(profile);
		//获取激活的环境列表
		Set<String> currentActiveProfiles = doGetActiveProfiles();
		return (currentActiveProfiles.contains(profile)
				//如果激活的环境列表包含profile，则返回true
				||
				//如果激活的环境列表为空，获取默认的激活环境列表后，再进一步判断是否含有profile。
				(currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile)));
	}

	/**
	 * 验证给定的配置文件，在添加到活动或默认配置文件集之前内部调用。
	 * <p> 子类可能会覆盖以对配置文件语法施加进一步限制。
	 *
	 * @throws IllegalArgumentException 如果配置文件为null，为空，仅空白或以配置文件NOT运算符 (!) 开头。
	 * @see #acceptsProfiles
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 */
	protected void validateProfile(String profile) {
		if (!StringUtils.hasText(profile)) {
			//profile为空，抛出IllegalArgumentException
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must contain text");
		}
		if (profile.charAt(0) == '!') {
			//profile的第一个字符为“!”，抛出IllegalArgumentException
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not begin with ! operator");
		}
	}

	@Override
	public MutablePropertySources getPropertySources() {
		return this.propertySources;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemProperties() {
		try {
			// 尝试获取系统属性并返回
			return (Map) System.getProperties();
		} catch (AccessControlException ex) {
			// 如果没有权限获取系统属性，则返回只读的系统属性映射
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				// 重写方法以获取系统属性
				protected String getSystemAttribute(String attributeName) {
					try {
						// 尝试获取系统属性值
						return System.getProperty(attributeName);
					} catch (AccessControlException ex) {
						// 如果获取系统属性时抛出AccessControlException，记录错误信息并返回null
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system property '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemEnvironment() {
		if (suppressGetenvAccess()) {
			return Collections.emptyMap();
		}
		try {
			return (Map) System.getenv();
		} catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getenv(attributeName);
					} catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system environment variable '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	/**
	 * Determine whether to suppress {@link System#getenv()}/{@link System#getenv(String)}
	 * access for the purposes of {@link #getSystemEnvironment()}.
	 * <p>If this method returns {@code true}, an empty dummy Map will be used instead
	 * of the regular system environment Map, never even trying to call {@code getenv}
	 * and therefore avoiding security manager warnings (if any).
	 * <p>The default implementation checks for the "spring.getenv.ignore" system property,
	 * returning {@code true} if its value equals "true" in any case.
	 *
	 * @see #IGNORE_GETENV_PROPERTY_NAME
	 * @see SpringProperties#getFlag
	 */
	protected boolean suppressGetenvAccess() {
		return SpringProperties.getFlag(IGNORE_GETENV_PROPERTY_NAME);
	}

	@Override
	public void merge(ConfigurableEnvironment parent) {
		for (PropertySource<?> ps : parent.getPropertySources()) {
			if (!this.propertySources.contains(ps.getName())) {
				this.propertySources.addLast(ps);
			}
		}
		String[] parentActiveProfiles = parent.getActiveProfiles();
		if (!ObjectUtils.isEmpty(parentActiveProfiles)) {
			synchronized (this.activeProfiles) {
				Collections.addAll(this.activeProfiles, parentActiveProfiles);
			}
		}
		String[] parentDefaultProfiles = parent.getDefaultProfiles();
		if (!ObjectUtils.isEmpty(parentDefaultProfiles)) {
			synchronized (this.defaultProfiles) {
				this.defaultProfiles.remove(RESERVED_DEFAULT_PROFILE_NAME);
				Collections.addAll(this.defaultProfiles, parentDefaultProfiles);
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurablePropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public ConfigurableConversionService getConversionService() {
		return this.propertyResolver.getConversionService();
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		this.propertyResolver.setConversionService(conversionService);
	}

	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
	}

	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
	}

	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.propertyResolver.setValueSeparator(valueSeparator);
	}

	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		this.propertyResolver.setRequiredProperties(requiredProperties);
	}

	@Override
	public void validateRequiredProperties() throws MissingRequiredPropertiesException {
		this.propertyResolver.validateRequiredProperties();
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsProperty(String key) {
		return this.propertyResolver.containsProperty(key);
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		//委托给属性解析器处理
		return this.propertyResolver.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return this.propertyResolver.getProperty(key, defaultValue);
	}

	@Override
	@Nullable
	public <T> T getProperty(String key, Class<T> targetType) {
		return this.propertyResolver.getProperty(key, targetType);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return this.propertyResolver.getProperty(key, targetType, defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key);
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key, targetType);
	}

	@Override
	public String resolvePlaceholders(String text) {
		return this.propertyResolver.resolvePlaceholders(text);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return this.propertyResolver.resolveRequiredPlaceholders(text);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " {activeProfiles=" + this.activeProfiles +
				", defaultProfiles=" + this.defaultProfiles + ", propertySources=" + this.propertySources + "}";
	}

}
