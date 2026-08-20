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

package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.NestedIOException;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.*;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Predicate;

/**
 * 解析一个 {@link Configuration} 类定义，并填充一组 {@link ConfigurationClass} 对象。
 * （解析一个 Configuration 类可能会生成多个 ConfigurationClass 对象，
 * 因为一个 Configuration 类可能使用 {@link Import} 注解导入其他类。）
 *
 * <p>该类的作用是将解析 Configuration 类结构的职责，
 * 与根据该结构内容注册 BeanDefinition 对象的职责进行分离
 *（唯一的例外是 {@code @ComponentScan} 注解，它们需要立即注册）。
 *
 * <p>这是一个基于 ASM 的实现，避免使用反射和类的提前加载，
 * 以便更好地与 Spring ApplicationContext 中的延迟类加载机制协同工作。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
class ConfigurationClassParser {

	private static final PropertySourceFactory DEFAULT_PROPERTY_SOURCE_FACTORY = new DefaultPropertySourceFactory();

	private static final Predicate<String> DEFAULT_EXCLUSION_FILTER = className ->
			(className.startsWith("java.lang.annotation.") || className.startsWith("org.springframework.stereotype."));

	private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
			(o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());


	private final Log logger = LogFactory.getLog(getClass());

	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;

	private final ComponentScanAnnotationParser componentScanParser;

	private final ConditionEvaluator conditionEvaluator;

	private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();

	private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();

	private final List<String> propertySourceNames = new ArrayList<>();

	private final ImportStack importStack = new ImportStack();

	private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();

	private final SourceClass objectSourceClass = new SourceClass(Object.class);


	/**
	 * 创建一个新的 {@link ConfigurationClassParser} 实例，用于填充配置类的集合。
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
			ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
			BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {

		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.componentScanParser = new ComponentScanAnnotationParser(
				environment, resourceLoader, componentScanBeanNameGenerator, registry);
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
	}


	public void parse(Set<BeanDefinitionHolder> configCandidates) {
		for (BeanDefinitionHolder holder : configCandidates) {
			// 📋 获取 Bean 定义对象
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				// 🏷️ 如果是注解 Bean 定义，使用元数据进行解析
				if (bd instanceof AnnotatedBeanDefinition) {
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				}
				// 🏭 如果是抽象 Bean 定义且有 Bean 类，使用类进行解析
				else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				// 📝 否则使用 Bean 类名进行解析
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			// 🚨 如果是 Bean 定义存储异常，直接抛出
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			// ⚠️ 其他异常包装为 Bean 定义存储异常后抛出
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}

		// ⏳ 处理延迟导入选择器
		this.deferredImportSelectorHandler.process();
	}

	protected final void parse(@Nullable String className, String beanName) throws IOException {
		Assert.notNull(className, "No bean class name for configuration class bean definition");
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
		processConfigurationClass(new ConfigurationClass(reader, beanName), DEFAULT_EXCLUSION_FILTER);
	}

	protected final void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName), DEFAULT_EXCLUSION_FILTER);
	}

	protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(metadata, beanName), DEFAULT_EXCLUSION_FILTER);
	}

	/**
	 * 校验每一个 {@link ConfigurationClass} 对象。
	 * @see ConfigurationClass#validate
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getConfigurationClasses() {
		return this.configurationClasses.keySet();
	}


	protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) throws IOException {
		// 如果根据条件评估器判断该配置类应跳过，则直接返回
		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}

		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		if (existingClass != null) {
			if (configClass.isImported()) {
				if (existingClass.isImported()) {
					// 如果原配置类和新配置类都是通过 @Import 导入的，则合并导入者信息
					existingClass.mergeImportedBy(configClass);
				}
				// 否则忽略新导入的配置类；已有的非导入类将覆盖它
				return;
			}
			else {
				// 找到了显式的 Bean 定义，可能是替代了之前的导入类
				// 删除旧的配置类，使用新的配置类
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}

		// 递归处理配置类及其父类结构
		SourceClass sourceClass = asSourceClass(configClass, filter);
		do {
			sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
		}
		while (sourceClass != null);

		this.configurationClasses.put(configClass, configClass);
	}

	/**
	 * 通过读取源类上的注解、成员和方法，应用处理逻辑并构建完整的 {@link ConfigurationClass}。
	 * 随着相关源被发现，此方法可能会被调用多次。
	 *
	 * @param configClass 正在构建的配置类
	 * @param sourceClass 一个源类
	 * @return 父类，如果未找到或已处理过则返回 {@code null}
	 */
	@Nullable
	protected final SourceClass doProcessConfigurationClass(
			ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter)
			throws IOException {

		if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
			// 如果标注有@Component注解，调用处理成员类的方进行处理。
			// 递归优先处理所有成员（嵌套）类
			processMemberClasses(configClass, sourceClass, filter);
		}

		// 处理所有 @PropertySource 注解
		for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), PropertySources.class,
				org.springframework.context.annotation.PropertySource.class)) {
			if (this.environment instanceof ConfigurableEnvironment) {
				// 如果环境是可配置环境，则处理属性源
				processPropertySource(propertySource);
			}
			else {
				logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
						"]. Reason: Environment must implement ConfigurableEnvironment");
			}
		}

		// 处理所有 @ComponentScan 注解
		Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
		if (!componentScans.isEmpty() &&
				!this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
			for (AnnotationAttributes componentScan : componentScans) {
				// 配置类被 @ComponentScan 注解 -> 立即执行扫描
				Set<BeanDefinitionHolder> scannedBeanDefinitions =
						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
				// 检查扫描到的定义中是否有配置类候选项，如有则递归解析
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
					if (bdCand == null) {
						bdCand = holder.getBeanDefinition();
					}
					if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
						parse(bdCand.getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}

		//处理@Import注解
		processImports(configClass, sourceClass, getImports(sourceClass), filter, true);

		//处理@ImportResource注解
		AnnotationAttributes importResource =
				AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
		// 📋 如果存在 @ImportResource 注解
		if (importResource != null) {
			// 📁 获取资源位置数组
			String[] resources = importResource.getStringArray("locations");

			// 📖 获取 Bean 定义读取器类型
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");

			// 🔄 遍历所有资源位置
			for (String resource : resources) {
				// 🔧 解析资源路径中的占位符
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);

				// ➕ 将解析后的资源添加到配置类的导入资源中
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}

		//处理单个Bean方法
		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
		for (MethodMetadata methodMetadata : beanMethods) {
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// 处理接口中的默认方法
		processInterfaces(configClass, sourceClass);

		// 处理父类（如果存在）
		// 🔍 检查源类是否有父类
		if (sourceClass.getMetadata().hasSuperClass()) {
			// 📝 获取父类的完整类名
			String superclass = sourceClass.getMetadata().getSuperClassName();
			// ✅ 如果父类存在且不是 Java 内置类，并且未被处理过
			if (superclass != null && !superclass.startsWith("java") &&
					!this.knownSuperclasses.containsKey(superclass)) {
				// 📋 将父类记录到已知父类映射中，避免重复处理
				this.knownSuperclasses.put(superclass, configClass);
				// 找到父类，返回其注解元数据并递归处理
				return sourceClass.getSuperClass();
			}
		}

		// 没有父类 -> 处理完成
		return null;
	}

	/**
	 * 注册作为配置类的成员（嵌套）类。
	 */
	private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass,
			Predicate<String> filter) throws IOException {

		// 🔍 获取源类的所有成员类
		Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();

		// 📋 如果存在成员类
		if (!memberClasses.isEmpty()) {
			List<SourceClass> candidates = new ArrayList<>(memberClasses.size());

			// 🔄 遍历所有成员类
			for (SourceClass memberClass : memberClasses) {
				// ✅ 检查成员类是否为配置候选且不是当前配置类本身
				if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
						!memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
					// ➕ 添加到候选列表中
					candidates.add(memberClass);
				}
			}

			// 🔢 按照 @Order 注解排序候选配置类
			OrderComparator.sort(candidates);

			// 🔄 处理所有候选配置类
			for (SourceClass candidate : candidates) {
				// 🔄 检查是否存在循环导入
				if (this.importStack.contains(configClass)) {
					// ⚠️ 报告循环导入错误
					this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
				}
				else {
					// 📚 将当前配置类推入导入栈
					this.importStack.push(configClass);
					try {
						// 🏗️ 递归处理候选配置类
						processConfigurationClass(candidate.asConfigClass(configClass), filter);
					}
					finally {
						// 🗑️ 处理完成后从导入栈中移除
						this.importStack.pop();
					}
				}
			}
		}
	}

	/**
	 * 注册配置类所实现接口中的默认方法。
	 */
	private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		// 🔄 遍历源类的所有接口
		for (SourceClass ifc : sourceClass.getInterfaces()) {
			// 🔍 检索接口中的 Bean 方法元数据
			Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
			// 🎯 遍历所有 Bean 方法
			for (MethodMetadata methodMetadata : beanMethods) {
				// ✅ 如果方法不是抽象方法（即默认方法或 Java8+ 接口方法）
				if (!methodMetadata.isAbstract()) {
					// 🏗️ 将该方法作为 Bean 方法添加到配置类中
					// 默认方法，或者是 Java8 接口上的接口方法
					configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
				}
			}
			// 🔄 递归处理接口的父接口
			processInterfaces(configClass, ifc);
		}
	}

	/**
	 * 获取所有 <code>@Bean</code> 方法的元数据。
	 */
	private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
		AnnotationMetadata original = sourceClass.getMetadata();
		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
			// 尝试通过 ASM 读取类文件，以确定方法声明的顺序...
			// 不幸的是，JVM 的标准反射返回的方法顺序是任意的，
			// 即使是在同一 JVM 上同一应用的不同运行中也可能不同。
			try {
				AnnotationMetadata asm =
						this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
				if (asmMethods.size() >= beanMethods.size()) {
					Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
					for (MethodMetadata asmMethod : asmMethods) {
						for (MethodMetadata beanMethod : beanMethods) {
							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
								selectedMethods.add(beanMethod);
								break;
							}
						}
					}
					if (selectedMethods.size() == beanMethods.size()) {
						// 反射检测到的所有方法都能在 ASM 方法集合中找到 -> 继续使用该顺序
						beanMethods = selectedMethods;
					}
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
				// 不用担心，继续使用最初的反射元数据即可...
			}
		}
		return beanMethods;
	}


	/**
	 * 处理给定的 <code>@PropertySource</code> 注解元数据。
	 * @param propertySource 找到的 <code>@PropertySource</code> 注解的元数据
	 * @throws IOException 加载属性资源失败时抛出
	 */
	private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
		// 📝 获取属性源名称，如果为空则设置为 null
		String name = propertySource.getString("name");
		if (!StringUtils.hasLength(name)) {
			name = null;
		}

		// 🔤 获取编码格式，如果为空则设置为 null
		String encoding = propertySource.getString("encoding");
		if (!StringUtils.hasLength(encoding)) {
			encoding = null;
		}

		// 📍 获取属性源位置数组
		String[] locations = propertySource.getStringArray("value");
		Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");

		// 🤷 获取是否忽略资源未找到的标志
		boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

		// 🏭 获取属性源工厂类并实例化
		Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
		PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ?
				DEFAULT_PROPERTY_SOURCE_FACTORY : BeanUtils.instantiateClass(factoryClass));

		// 🔄 遍历所有属性源位置
		for (String location : locations) {
			try {
				// 🔧 解析位置中的占位符
				String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
				// 📁 获取资源对象
				Resource resource = this.resourceLoader.getResource(resolvedLocation);
				// ➕ 创建并添加属性源
				addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
			}
			catch (IllegalArgumentException | FileNotFoundException | UnknownHostException | SocketException ex) {
				// 🚨 处理占位符无法解析或资源未找到的异常
				if (ignoreResourceNotFound) {
					if (logger.isInfoEnabled()) {
						logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
					}
				}
				else {
					// ⚠️ 否则重新抛出异常
					throw ex;
				}
			}
		}
	}

	private void addPropertySource(PropertySource<?> propertySource) {
		String name = propertySource.getName();
		MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment).getPropertySources();

		if (this.propertySourceNames.contains(name)) {
			// 我们已经添加了一个版本，我们需要扩展它
			PropertySource<?> existing = propertySources.get(name);
			if (existing != null) {
				PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ?
						((ResourcePropertySource) propertySource).withResourceName() : propertySource);
				if (existing instanceof CompositePropertySource) {
					((CompositePropertySource) existing).addFirstPropertySource(newSource);
				}
				else {
					if (existing instanceof ResourcePropertySource) {
						existing = ((ResourcePropertySource) existing).withResourceName();
					}
					CompositePropertySource composite = new CompositePropertySource(name);
					composite.addPropertySource(newSource);
					composite.addPropertySource(existing);
					propertySources.replace(name, composite);
				}
				return;
			}
		}

		if (this.propertySourceNames.isEmpty()) {
			propertySources.addLast(propertySource);
		}
		else {
			String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
			propertySources.addBefore(firstProcessed, propertySource);
		}
		this.propertySourceNames.add(name);
	}


	/**
	 * 返回 {@code @Import} 注解的类，包含所有元注解。
	 */
	private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
		Set<SourceClass> imports = new LinkedHashSet<>();
		Set<SourceClass> visited = new LinkedHashSet<>();
		collectImports(sourceClass, imports, visited);
		return imports;
	}

	/**
	 * 递归收集所有声明的 {@code @Import} 值。与大多数元注解不同，
	 * 一个类上可以声明多个不同值的 {@code @Import}；单纯返回第一个元注解的值是不够的。
	 * <p>例如，{@code @Configuration} 类通常会声明直接的 {@code @Import}，
	 * 还会包含来自 {@code @Enable} 注解的元导入。
	 * @param sourceClass 要搜索的类
	 * @param imports 当前已收集的导入集合
	 * @param visited 用于追踪已访问的类，防止无限递归
	 * @throws IOException 读取指定类的元数据时发生问题
	 */
	private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited)
			throws IOException {

		// 🔍 检查是否已访问过此源类，如果未访问则添加到已访问集合中
		if (visited.add(sourceClass)) {
			// 🏷️ 遍历源类上的所有注解
			for (SourceClass annotation : sourceClass.getAnnotations()) {
				// 📝 获取注解的类名
				String annName = annotation.getMetadata().getClassName();

				// ❌ 如果不是 @Import 注解，递归收集该注解上的导入
				if (!annName.equals(Import.class.getName())) {
					collectImports(annotation, imports, visited);
				}
			}

			// 📦 将当前源类 @Import 注解的 value 属性值添加到导入集合中
			imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
		}
	}

	private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, Predicate<String> exclusionFilter,
			boolean checkForCircularImports) {

		// 🚫 如果没有候选导入，直接返回
		if (importCandidates.isEmpty()) {
			return;
		}

		// ♻️ 如果需要检查循环导入，且当前配置类已在导入栈中，则报错
		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		}
		else {
			// 📚 将当前配置类压入导入栈，跟踪导入路径
			this.importStack.push(configClass);
			try {
				// 🔍 遍历所有候选导入类
				for (SourceClass candidate : importCandidates) {
					// 🧩 如果候选是 ImportSelector 类型，委托其决定具体导入
					if (candidate.isAssignable(ImportSelector.class)) {
						// 候选类是一个ImportSelector -> 委托给它来确定导入
						Class<?> candidateClass = candidate.loadClass();
						ImportSelector selector = ParserStrategyUtils.instantiateClass(candidateClass, ImportSelector.class,
								this.environment, this.resourceLoader, this.registry);
						// 🚫 获取并合并该 ImportSelector 的排除过滤器
						Predicate<String> selectorFilter = selector.getExclusionFilter();
						if (selectorFilter != null) {
							exclusionFilter = exclusionFilter.or(selectorFilter);
						}
						// ⏳ 特殊处理 DeferredImportSelector 类型
						if (selector instanceof DeferredImportSelector) {
							this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
						}
						else {
							// 📥 获取 ImportSelector 返回的导入类名，转换为 SourceClass 并递归处理
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);
							processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);
						}
					}
					// 📝 如果候选是 ImportBeanDefinitionRegistrar，委托其注册额外的 Bean 定义
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// 候选类是 ImportBeanDefinitionRegistrar ->
						// 委托它注册额外的 Bean 定义
						Class<?> candidateClass = candidate.loadClass();
						ImportBeanDefinitionRegistrar registrar =
								ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,
										this.environment, this.resourceLoader, this.registry);
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					}
					// 🏷️ 否则将候选类作为普通配置类处理，递归解析
					else {
						// 候选类既不是 ImportSelector 也不是 ImportBeanDefinitionRegistrar ->
						// 将其作为 @Configuration 类进行处理
						this.importStack.registerImport(
								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
						processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);
					}
				}
			}
			catch (BeanDefinitionStoreException ex) {
				// 💥 直接抛出 BeanDefinitionStoreException 异常
				throw ex;
			}
			catch (Throwable ex) {
				// ❌ 捕获其他异常，封装为 BeanDefinitionStoreException 抛出
				throw new BeanDefinitionStoreException(
						"Failed to process import candidates for configuration class [" +
						configClass.getMetadata().getClassName() + "]", ex);
			}
			finally {
				// 📤 最终弹出当前配置类，清理导入栈
				this.importStack.pop();
			}
		}
	}

	private boolean isChainedImportOnStack(ConfigurationClass configClass) {
		// 🔍 检查配置类是否已在导入栈中
		if (this.importStack.contains(configClass)) {
			// 📝 获取配置类的完整类名
			String configClassName = configClass.getMetadata().getClassName();

			// 🔗 获取导入当前配置类的类
			AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);

			// 🔄 循环检查导入链，查找是否存在循环依赖
			while (importingClass != null) {
				// 🎯 如果找到循环引用（当前类名与导入类名相同）
				if (configClassName.equals(importingClass.getClassName())) {
					// ⚠️ 存在链式导入循环，返回 true
					return true;
				}

				// 🔗 继续向上查找导入链
				importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
			}
		}

		// ✅ 没有发现循环导入
		return false;
	}

	ImportRegistry getImportRegistry() {
		return this.importStack;
	}


	/**
	 * 从 {@link ConfigurationClass} 获取 {@link SourceClass} 的工厂方法。
	 */
	private SourceClass asSourceClass(ConfigurationClass configurationClass, Predicate<String> filter) throws IOException {
		AnnotationMetadata metadata = configurationClass.getMetadata();
		if (metadata instanceof StandardAnnotationMetadata) {
			return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass(), filter);
		}
		return asSourceClass(metadata.getClassName(), filter);
	}

	/**
	 * 从 {@link Class} 获取 {@link SourceClass} 的工厂方法。
	 */
	SourceClass asSourceClass(@Nullable Class<?> classType, Predicate<String> filter) throws IOException {
		if (classType == null || filter.test(classType.getName())) {
			return this.objectSourceClass;
		}
		try {
			// 校验是否能反射读取注解（包括 Class 属性），如果不能则回退使用 ASM
			for (Annotation ann : classType.getDeclaredAnnotations()) {
				AnnotationUtils.validateAnnotation(ann);
			}
			return new SourceClass(classType);
		}
		catch (Throwable ex) {
			// 通过类名解析强制使用 ASM
			return asSourceClass(classType.getName(), filter);
		}
	}

	/**
	 * 从类名数组获取 {@link SourceClass} 集合的工厂方法。
	 */
	private Collection<SourceClass> asSourceClasses(String[] classNames, Predicate<String> filter) throws IOException {
		List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
		for (String className : classNames) {
			annotatedClasses.add(asSourceClass(className, filter));
		}
		return annotatedClasses;
	}

	/**
	 * 从类名获取 {@link SourceClass} 的工厂方法。
	 */
	SourceClass asSourceClass(@Nullable String className, Predicate<String> filter) throws IOException {
		if (className == null || filter.test(className)) {
			return this.objectSourceClass;
		}
		if (className.startsWith("java")) {
			// 核心 Java 类型永远不使用 ASM
			try {
				return new SourceClass(ClassUtils.forName(className, this.resourceLoader.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new NestedIOException("Failed to load class [" + className + "]", ex);
			}
		}
		return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
	}


	@SuppressWarnings("serial")
	private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

		private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();

		public void registerImport(AnnotationMetadata importingClass, String importedClass) {
			this.imports.add(importedClass, importingClass);
		}

		@Override
		@Nullable
		public AnnotationMetadata getImportingClassFor(String importedClass) {
			return CollectionUtils.lastElement(this.imports.get(importedClass));
		}

		@Override
		public void removeImportingClass(String importingClass) {
			for (List<AnnotationMetadata> list : this.imports.values()) {
				for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext();) {
					if (iterator.next().getClassName().equals(importingClass)) {
						iterator.remove();
						break;
					}
				}
			}
		}

		/**
		 * 给定一个堆栈（顺序如下）
		 * <ul>
		 * <li>com.acme.Foo</li>
		 * <li>com.acme.Bar</li>
		 * <li>com.acme.Baz</li>
		 * </ul>
		 * 返回 "[Foo->Bar->Baz]"。
		 */
		@Override
		public String toString() {
			StringJoiner joiner = new StringJoiner("->", "[", "]");
			for (ConfigurationClass configurationClass : this) {
				joiner.add(configurationClass.getSimpleName());
			}
			return joiner.toString();
		}
	}


	private class DeferredImportSelectorHandler {

		@Nullable
		private List<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();

		/**
		 * 处理指定的 {@link DeferredImportSelector}。
		 * 如果正在收集延迟导入选择器，则将该实例注册到列表中。
		 * 如果正在处理，则根据其 {@link DeferredImportSelector.Group} 立即处理该 {@link DeferredImportSelector}。
		 *
		 * @param configClass 源配置类
		 * @param importSelector 要处理的选择器
		 */
		public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
			// 🎁 创建延迟导入选择器持有者对象
			DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);

			// 🔍 检查是否处于立即处理模式
			if (this.deferredImportSelectors == null) {
				// 🏗️ 创建延迟导入选择器分组处理器
				DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();

				// 📋 注册持有者到处理器
				handler.register(holder);

				// ⚡ 立即处理分组导入
				handler.processGroupImports();
			}
			else {
				// 📝 添加到延迟处理队列中
				this.deferredImportSelectors.add(holder);
			}
		}

		public void process() {
			// 📝 获取延迟导入选择器列表并清空当前引用
			List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
			this.deferredImportSelectors = null;

			try {
				// 🔍 如果存在延迟导入选择器
				if (deferredImports != null) {
					// 🏗️ 创建延迟导入选择器分组处理器
					DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();

					// 🔢 对延迟导入选择器进行排序
					deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);

					// 📋 将每个延迟导入选择器注册到处理器
					deferredImports.forEach(handler::register);

					// ⚡ 处理分组导入
					handler.processGroupImports();
				}
			}
			finally {
				// 🔄 重新初始化延迟导入选择器列表
				this.deferredImportSelectors = new ArrayList<>();
			}
		}
	}


	private class DeferredImportSelectorGroupingHandler {

		private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();

		private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

		public void register(DeferredImportSelectorHolder deferredImport) {
			// 🏷️ 获取延迟导入选择器的分组类型
			Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();

			// 🔍 获取或创建对应的分组对象，如果没有指定分组则使用持有者对象作为键
			DeferredImportSelectorGrouping grouping = this.groupings.computeIfAbsent(
					(group != null ? group : deferredImport),
					key -> new DeferredImportSelectorGrouping(createGroup(group)));

			// ➕ 将延迟导入持有者添加到分组中
			grouping.add(deferredImport);

			// 📋 将配置类添加到配置类映射表中
			this.configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
					deferredImport.getConfigurationClass());
		}

		public void processGroupImports() {
			// 🔄 遍历所有分组对象
			for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {
				// 🎯 获取候选过滤器
				Predicate<String> exclusionFilter = grouping.getCandidateFilter();

				// 🔄 处理分组中的每个导入条目
				grouping.getImports().forEach(entry -> {
					// 📋 从配置类映射中获取对应的配置类
					ConfigurationClass configurationClass = this.configurationClasses.get(entry.getMetadata());

					try {
						// 🏗️ 处理导入操作，将导入类名转换为源类并进行处理
						processImports(configurationClass, asSourceClass(configurationClass, exclusionFilter),
								Collections.singleton(asSourceClass(entry.getImportClassName(), exclusionFilter)),
								exclusionFilter, false);
					}
					catch (BeanDefinitionStoreException ex) {
						// 🚨 重新抛出 Bean 定义存储异常
						throw ex;
					}
					catch (Throwable ex) {
						// ⚠️ 包装其他异常为 Bean 定义存储异常
						throw new BeanDefinitionStoreException(
								"Failed to process import candidates for configuration class [" +
										configurationClass.getMetadata().getClassName() + "]", ex);
					}
				});
			}
		}

		private Group createGroup(@Nullable Class<? extends Group> type) {
			Class<? extends Group> effectiveType = (type != null ? type : DefaultDeferredImportSelectorGroup.class);
			return ParserStrategyUtils.instantiateClass(effectiveType, Group.class,
					ConfigurationClassParser.this.environment,
					ConfigurationClassParser.this.resourceLoader,
					ConfigurationClassParser.this.registry);
		}
	}


	private static class DeferredImportSelectorHolder {

		private final ConfigurationClass configurationClass;

		private final DeferredImportSelector importSelector;

		public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
			this.configurationClass = configClass;
			this.importSelector = selector;
		}

		public ConfigurationClass getConfigurationClass() {
			return this.configurationClass;
		}

		public DeferredImportSelector getImportSelector() {
			return this.importSelector;
		}
	}


	private static class DeferredImportSelectorGrouping {

		private final DeferredImportSelector.Group group;

		private final List<DeferredImportSelectorHolder> deferredImports = new ArrayList<>();

		DeferredImportSelectorGrouping(Group group) {
			this.group = group;
		}

		public void add(DeferredImportSelectorHolder deferredImport) {
			this.deferredImports.add(deferredImport);
		}

		/**
		 * 返回该组定义的导入。
		 * @return 每个导入及其关联的配置类
		 */
		public Iterable<Group.Entry> getImports() {
			// 🔄 遍历所有延迟导入选择器持有者
			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
				// ⚙️ 让分组对象处理配置类元数据和导入选择器
				this.group.process(deferredImport.getConfigurationClass().getMetadata(),
						deferredImport.getImportSelector());
			}

			// 🎁 从分组对象中选择并返回导入条目
			return this.group.selectImports();
		}

		public Predicate<String> getCandidateFilter() {
			Predicate<String> mergedFilter = DEFAULT_EXCLUSION_FILTER;
			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
				Predicate<String> selectorFilter = deferredImport.getImportSelector().getExclusionFilter();
				if (selectorFilter != null) {
					mergedFilter = mergedFilter.or(selectorFilter);
				}
			}
			return mergedFilter;
		}
	}


	private static class DefaultDeferredImportSelectorGroup implements Group {

		private final List<Entry> imports = new ArrayList<>();

		@Override
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			// 🔄 遍历选择器选中的所有导入类名
			for (String importClassName : selector.selectImports(metadata)) {
				// ➕ 为每个导入类名创建条目并添加到导入列表中
				this.imports.add(new Entry(metadata, importClassName));
			}
		}

		@Override
		public Iterable<Entry> selectImports() {
			return this.imports;
		}
	}


	/**
	 * 简单的包装类，允许统一处理带注解的源类，
	 * 无论它们是如何被加载的。
	 */
	private class SourceClass implements Ordered {

		private final Object source;  // Class 或 MetadataReader

		private final AnnotationMetadata metadata;

		public SourceClass(Object source) {
			this.source = source;
			if (source instanceof Class) {
				this.metadata = AnnotationMetadata.introspect((Class<?>) source);
			}
			else {
				this.metadata = ((MetadataReader) source).getAnnotationMetadata();
			}
		}

		public final AnnotationMetadata getMetadata() {
			return this.metadata;
		}

		@Override
		public int getOrder() {
			Integer order = ConfigurationClassUtils.getOrder(this.metadata);
			return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
		}

		public Class<?> loadClass() throws ClassNotFoundException {
			if (this.source instanceof Class) {
				return (Class<?>) this.source;
			}
			String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
			return ClassUtils.forName(className, resourceLoader.getClassLoader());
		}

		public boolean isAssignable(Class<?> clazz) throws IOException {
			if (this.source instanceof Class) {
				return clazz.isAssignableFrom((Class<?>) this.source);
			}
			return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
		}

		public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
			if (this.source instanceof Class) {
				return new ConfigurationClass((Class<?>) this.source, importedBy);
			}
			return new ConfigurationClass((MetadataReader) this.source, importedBy);
		}

		public Collection<SourceClass> getMemberClasses() throws IOException {
			Object sourceToProcess = this.source;
			if (sourceToProcess instanceof Class) {
				Class<?> sourceClass = (Class<?>) sourceToProcess;
				try {
					Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
					List<SourceClass> members = new ArrayList<>(declaredClasses.length);
					for (Class<?> declaredClass : declaredClasses) {
						members.add(asSourceClass(declaredClass, DEFAULT_EXCLUSION_FILTER));
					}
					return members;
				}
				catch (NoClassDefFoundError err) {
					// getDeclaredClasses() 因为无法解析的依赖失败
					// -> 退回到下面的 ASM 处理
					sourceToProcess = metadataReaderFactory.getMetadataReader(sourceClass.getName());
				}
			}

			// 基于 ASM 的解析方式 — 同样适用于无法解析的类
			MetadataReader sourceReader = (MetadataReader) sourceToProcess;
			String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
			List<SourceClass> members = new ArrayList<>(memberClassNames.length);
			for (String memberClassName : memberClassNames) {
				try {
					members.add(asSourceClass(memberClassName, DEFAULT_EXCLUSION_FILTER));
				}
				catch (IOException ex) {
					// 如果无法解决，让我们跳过它-我们只是在寻找候选人
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to resolve member class [" + memberClassName +
								"] - not considering it as a configuration class candidate");
					}
				}
			}
			return members;
		}

		public SourceClass getSuperClass() throws IOException {
			if (this.source instanceof Class) {
				return asSourceClass(((Class<?>) this.source).getSuperclass(), DEFAULT_EXCLUSION_FILTER);
			}
			return asSourceClass(
					((MetadataReader) this.source).getClassMetadata().getSuperClassName(), DEFAULT_EXCLUSION_FILTER);
		}

		public Set<SourceClass> getInterfaces() throws IOException {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Class<?> ifcClass : sourceClass.getInterfaces()) {
					result.add(asSourceClass(ifcClass, DEFAULT_EXCLUSION_FILTER));
				}
			}
			else {
				for (String className : this.metadata.getInterfaceNames()) {
					result.add(asSourceClass(className, DEFAULT_EXCLUSION_FILTER));
				}
			}
			return result;
		}

		public Set<SourceClass> getAnnotations() {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Annotation ann : sourceClass.getDeclaredAnnotations()) {
					Class<?> annType = ann.annotationType();
					if (!annType.getName().startsWith("java")) {
						try {
							result.add(asSourceClass(annType, DEFAULT_EXCLUSION_FILTER));
						}
						catch (Throwable ex) {
							// JVM 类加载时忽略了类路径上不存在的注解
							// 因此这里也忽略该异常。
						}
					}
				}
			}
			else {
				for (String className : this.metadata.getAnnotationTypes()) {
					if (!className.startsWith("java")) {
						try {
							result.add(getRelated(className));
						}
						catch (Throwable ex) {
							// JVM 类加载时忽略了类路径中不存在的注解
							// 这里也同样忽略该异常。
						}
					}
				}
			}
			return result;
		}

		public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
			Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
			if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
				return Collections.emptySet();
			}
			String[] classNames = (String[]) annotationAttributes.get(attribute);
			Set<SourceClass> result = new LinkedHashSet<>();
			for (String className : classNames) {
				result.add(getRelated(className));
			}
			return result;
		}

		private SourceClass getRelated(String className) throws IOException {
			if (this.source instanceof Class) {
				try {
					Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
					return asSourceClass(clazz, DEFAULT_EXCLUSION_FILTER);
				}
				catch (ClassNotFoundException ex) {
					// 忽略该异常 -> 回退到基于 ASM 的解析，核心 Java 类型除外。
					if (className.startsWith("java")) {
						throw new NestedIOException("Failed to load class [" + className + "]", ex);
					}
					return new SourceClass(metadataReaderFactory.getMetadataReader(className));
				}
			}
			return asSourceClass(className, DEFAULT_EXCLUSION_FILTER);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof SourceClass &&
					this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
		}

		@Override
		public int hashCode() {
			return this.metadata.getClassName().hashCode();
		}

		@Override
		public String toString() {
			return this.metadata.getClassName();
		}
	}


	/**
	 * 在检测到循环 {@link Import} 时注册的 {@link Problem}。
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
			super(String.format("A circular @Import has been detected: " +
					"Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
					"already present in the current import stack %s", importStack.element().getSimpleName(),
					attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
					new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
		}
	}

}
