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

package org.springframework.beans.factory.groovy;

import groovy.lang.*;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * 基于Groovy的Spring bean定义读取器：类似于Groovy构建器，
 * 但更像是Spring配置的DSL。
 *
 * <p>此bean定义读取器还理解XML bean定义文件，
 * 允许与Groovy bean定义文件无缝混合和匹配。
 *
 * <p>通常应用于
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * 或{@link org.springframework.context.support.GenericApplicationContext}，
 * 但可以用于任何{@link BeanDefinitionRegistry}实现。
 *
 * <h3>示例语法</h3>
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * def reader = new GroovyBeanDefinitionReader(myApplicationContext)
 * reader.beans {
 *     dataSource(BasicDataSource) {                  // &lt;--- invokeMethod
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"                            // &lt;-- setProperty
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource                    // &lt;-- getProperty for retrieving references
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;         // &lt;-- setProperty with closure for nested bean
 *             dataSource = dataSource
 *         }
 *     }
 * }</pre>
 *
 * <p>您还可以使用{@link #loadBeanDefinitions(Resource...)}或
 * {@link #loadBeanDefinitions(String...)}方法加载包含在Groovy脚本中定义的bean的资源，
 * 脚本类似于以下内容。
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * beans {
 *     dataSource(BasicDataSource) {
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;
 *             dataSource = dataSource
 *         }
 *     }
 * }</pre>
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.context.support.GenericGroovyApplicationContext
 * @since 4.0
 */
public class GroovyBeanDefinitionReader extends AbstractBeanDefinitionReader implements GroovyObject {

	/**
	 * 使用默认设置创建的标准{@code XmlBeanDefinitionReader}，
	 * 用于从XML文件加载bean定义。
	 */
	private final XmlBeanDefinitionReader standardXmlBeanDefinitionReader;

	/**
	 * 用于通过Groovy DSL加载bean定义的Groovy DSL {@code XmlBeanDefinitionReader}，
	 * 通常配置为禁用XML验证。
	 */
	private final XmlBeanDefinitionReader groovyDslXmlBeanDefinitionReader;

	private final Map<String, String> namespaces = new HashMap<>();

	private final Map<String, DeferredProperty> deferredProperties = new HashMap<>();

	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());

	private Binding binding;

	private GroovyBeanDefinitionWrapper currentBeanDefinition;


	/**
	 * 为给定的{@link BeanDefinitionRegistry}创建新的{@code GroovyBeanDefinitionReader}。
	 *
	 * @param registry 要加载bean定义的{@code BeanDefinitionRegistry}
	 */
	public GroovyBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
		this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
		this.groovyDslXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
		this.groovyDslXmlBeanDefinitionReader.setValidating(false);
	}

	/**
	 * 基于给定的{@link XmlBeanDefinitionReader}创建新的{@code GroovyBeanDefinitionReader}，
	 * 将bean定义加载到其{@code BeanDefinitionRegistry}中，并将Groovy DSL加载委托给它。
	 * <p>提供的{@code XmlBeanDefinitionReader}通常应该
	 * 预先配置为禁用XML验证。
	 *
	 * @param xmlBeanDefinitionReader 用于派生注册表和委托Groovy DSL加载的{@code XmlBeanDefinitionReader}
	 */
	public GroovyBeanDefinitionReader(XmlBeanDefinitionReader xmlBeanDefinitionReader) {
		super(xmlBeanDefinitionReader.getRegistry());
		this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(xmlBeanDefinitionReader.getRegistry());
		this.groovyDslXmlBeanDefinitionReader = xmlBeanDefinitionReader;
	}


	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

	@Override
	public MetaClass getMetaClass() {
		return this.metaClass;
	}

	/**
	 * 设置绑定，即在{@code GroovyBeanDefinitionReader}闭包范围内
	 * 可用的Groovy变量。
	 */
	public void setBinding(Binding binding) {
		this.binding = binding;
	}

	/**
	 * 返回Groovy变量的指定绑定（如果有）。
	 */
	public Binding getBinding() {
		return this.binding;
	}


	// 传统的BEAN定义读取器方法

	/**
	 * 从指定的Groovy脚本或XML文件加载bean定义。
	 * <p>注意{@code ".xml"}文件将被解析为XML内容；所有其他类型的
	 * 资源将被解析为Groovy脚本。
	 *
	 * @param resource Groovy脚本或XML文件的资源描述符
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * 从指定的Groovy脚本或XML文件加载bean定义。
	 * <p>注意{@code ".xml"}文件将被解析为XML内容；所有其他类型的
	 * 资源将被解析为Groovy脚本。
	 *
	 * @param encodedResource Groovy脚本或XML文件的资源描述符，
	 *                        允许指定用于解析文件的编码
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		// 检查XML文件并将它们重定向到"标准"XmlBeanDefinitionReader
		String filename = encodedResource.getResource().getFilename();
		if (StringUtils.endsWithIgnoreCase(filename, ".xml")) {
			return this.standardXmlBeanDefinitionReader.loadBeanDefinitions(encodedResource);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Loading Groovy bean definitions from " + encodedResource);
		}

		@SuppressWarnings("serial")
		Closure<Object> beans = new Closure<Object>(this) {
			@Override
			public Object call(Object... args) {
				invokeBeanDefiningClosure((Closure<?>) args[0]);
				return null;
			}
		};
		Binding binding = new Binding() {
			@Override
			public void setVariable(String name, Object value) {
				if (currentBeanDefinition != null) {
					applyPropertyToBeanDefinition(name, value);
				} else {
					super.setVariable(name, value);
				}
			}
		};
		binding.setVariable("beans", beans);

		int countBefore = getRegistry().getBeanDefinitionCount();
		try {
			GroovyShell shell = new GroovyShell(getBeanClassLoader(), binding);
			shell.evaluate(encodedResource.getReader(), "beans");
		} catch (Throwable ex) {
			throw new BeanDefinitionParsingException(new Problem("Error evaluating Groovy script: " + ex.getMessage(),
					new Location(encodedResource.getResource()), null, ex));
		}

		int count = getRegistry().getBeanDefinitionCount() - countBefore;
		if (logger.isDebugEnabled()) {
			logger.debug("Loaded " + count + " bean definitions from " + encodedResource);
		}
		return count;
	}


	// 在GROOVY闭包中使用的方法

	/**
	 * 为给定的块或闭包定义一组bean。
	 *
	 * @param closure 块或闭包
	 * @return 此{@code GroovyBeanDefinitionReader}实例
	 */
	public GroovyBeanDefinitionReader beans(Closure<?> closure) {
		return invokeBeanDefiningClosure(closure);
	}

	/**
	 * 定义内部bean定义。
	 *
	 * @param type bean类型
	 * @return bean定义
	 */
	public GenericBeanDefinition bean(Class<?> type) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(type);
		return beanDefinition;
	}

	/**
	 * 定义内部bean定义。
	 *
	 * @param type bean类型
	 * @param args 构造函数参数和闭包配置器
	 * @return bean定义
	 */
	public AbstractBeanDefinition bean(Class<?> type, Object... args) {
		GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
		try {
			Closure<?> callable = null;
			Collection<Object> constructorArgs = null;
			if (!ObjectUtils.isEmpty(args)) {
				int index = args.length;
				Object lastArg = args[index - 1];
				if (lastArg instanceof Closure<?>) {
					callable = (Closure<?>) lastArg;
					index--;
				}
				constructorArgs = resolveConstructorArguments(args, 0, index);
			}
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, type, constructorArgs);
			if (callable != null) {
				callable.call(this.currentBeanDefinition);
			}
			return this.currentBeanDefinition.getBeanDefinition();
		} finally {
			this.currentBeanDefinition = current;
		}
	}

	/**
	 * 定义要使用的Spring XML命名空间定义。
	 *
	 * @param definition 命名空间定义
	 */
	public void xmlns(Map<String, String> definition) {
		if (!definition.isEmpty()) {
			for (Map.Entry<String, String> entry : definition.entrySet()) {
				String namespace = entry.getKey();
				String uri = entry.getValue();
				if (uri == null) {
					throw new IllegalArgumentException("Namespace definition must supply a non-null URI");
				}
				NamespaceHandler namespaceHandler =
						this.groovyDslXmlBeanDefinitionReader.getNamespaceHandlerResolver().resolve(uri);
				if (namespaceHandler == null) {
					throw new BeanDefinitionParsingException(new Problem("No namespace handler found for URI: " + uri,
							new Location(new DescriptiveResource(("Groovy")))));
				}
				this.namespaces.put(namespace, uri);
			}
		}
	}

	/**
	 * 从XML或Groovy源将Spring bean定义导入到
	 * 当前bean构建器实例中。
	 *
	 * @param resourcePattern 资源模式
	 */
	public void importBeans(String resourcePattern) throws IOException {
		loadBeanDefinitions(resourcePattern);
	}


	// GROOVY闭包和属性的内部处理

	/**
	 * 此方法重写方法调用，为每个接受类参数的方法名创建bean。
	 */
	@Override
	public Object invokeMethod(String name, Object arg) {
		Object[] args = (Object[]) arg;
		if ("beans".equals(name) && args.length == 1 && args[0] instanceof Closure) {
			return beans((Closure<?>) args[0]);
		} else if ("ref".equals(name)) {
			String refName;
			if (args[0] == null) {
				throw new IllegalArgumentException("Argument to ref() is not a valid bean or was not found");
			}
			if (args[0] instanceof RuntimeBeanReference) {
				refName = ((RuntimeBeanReference) args[0]).getBeanName();
			} else {
				refName = args[0].toString();
			}
			boolean parentRef = false;
			if (args.length > 1 && args[1] instanceof Boolean) {
				parentRef = (Boolean) args[1];
			}
			return new RuntimeBeanReference(refName, parentRef);
		} else if (this.namespaces.containsKey(name) && args.length > 0 && args[0] instanceof Closure) {
			GroovyDynamicElementReader reader = createDynamicElementReader(name);
			reader.invokeMethod("doCall", args);
		} else if (args.length > 0 && args[0] instanceof Closure) {
			// 抽象bean定义
			return invokeBeanDefiningMethod(name, args);
		} else if (args.length > 0 &&
				(args[0] instanceof Class || args[0] instanceof RuntimeBeanReference || args[0] instanceof Map)) {
			return invokeBeanDefiningMethod(name, args);
		} else if (args.length > 1 && args[args.length - 1] instanceof Closure) {
			return invokeBeanDefiningMethod(name, args);
		}
		MetaClass mc = DefaultGroovyMethods.getMetaClass(getRegistry());
		if (!mc.respondsTo(getRegistry(), name, args).isEmpty()) {
			return mc.invokeMethod(getRegistry(), name, args);
		}
		return this;
	}

	private boolean addDeferredProperty(String property, Object newValue) {
		if (newValue instanceof List || newValue instanceof Map) {
			this.deferredProperties.put(this.currentBeanDefinition.getBeanName() + '.' + property,
					new DeferredProperty(this.currentBeanDefinition, property, newValue));
			return true;
		}
		return false;
	}

	private void finalizeDeferredProperties() {
		for (DeferredProperty dp : this.deferredProperties.values()) {
			if (dp.value instanceof List) {
				dp.value = manageListIfNecessary((List<?>) dp.value);
			} else if (dp.value instanceof Map) {
				dp.value = manageMapIfNecessary((Map<?, ?>) dp.value);
			}
			dp.apply();
		}
		this.deferredProperties.clear();
	}

	/**
	 * 当方法参数只是一个闭包时，它是一组bean定义。
	 *
	 * @param callable 闭包参数
	 * @return 此{@code GroovyBeanDefinitionReader}实例
	 */
	protected GroovyBeanDefinitionReader invokeBeanDefiningClosure(Closure<?> callable) {
		callable.setDelegate(this);
		callable.call();
		finalizeDeferredProperties();
		return this;
	}

	/**
	 * 当调用bean定义节点时调用此方法。
	 *
	 * @param beanName 要定义的bean的名称
	 * @param args     bean的参数。第一个参数是类名，最后一个参数有时是闭包。
	 *                 中间的所有参数都是构造函数参数。
	 * @return bean定义包装器
	 */
	private GroovyBeanDefinitionWrapper invokeBeanDefiningMethod(String beanName, Object[] args) {
		boolean hasClosureArgument = (args[args.length - 1] instanceof Closure);
		if (args[0] instanceof Class) {
			Class<?> beanClass = (Class<?>) args[0];
			if (hasClosureArgument) {
				if (args.length - 1 != 1) {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
							beanName, beanClass, resolveConstructorArguments(args, 1, args.length - 1));
				} else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, beanClass);
				}
			} else {
				this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
						beanName, beanClass, resolveConstructorArguments(args, 1, args.length));
			}
		} else if (args[0] instanceof RuntimeBeanReference) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
			this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(((RuntimeBeanReference) args[0]).getBeanName());
		} else if (args[0] instanceof Map) {
			// 命名构造函数参数
			if (args.length > 1 && args[1] instanceof Class) {
				List<Object> constructorArgs =
						resolveConstructorArguments(args, 2, hasClosureArgument ? args.length - 1 : args.length);
				this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, (Class<?>) args[1], constructorArgs);
				Map<?, ?> namedArgs = (Map<?, ?>) args[0];
				for (Map.Entry<?, ?> entity : namedArgs.entrySet()) {
					String propName = (String) entity.getKey();
					setProperty(propName, entity.getValue());
				}
			}
			// 工厂方法语法
			else {
				this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
				// 第一个参数是包含factoryBean : factoryMethod的map
				Map.Entry<?, ?> factoryBeanEntry = ((Map<?, ?>) args[0]).entrySet().iterator().next();
				// 如果我们有闭包体，那将是最后一个参数。
				// 中间的是构造函数参数
				int constructorArgsTest = (hasClosureArgument ? 2 : 1);
				// 如果我们的参数数量超过这个数字，我们就有构造函数参数
				if (args.length > constructorArgsTest) {
					// 工厂方法需要参数
					int endOfConstructArgs = (hasClosureArgument ? args.length - 1 : args.length);
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null,
							resolveConstructorArguments(args, 1, endOfConstructArgs));
				} else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
				}
				this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(factoryBeanEntry.getKey().toString());
				this.currentBeanDefinition.getBeanDefinition().setFactoryMethodName(factoryBeanEntry.getValue().toString());
			}

		} else if (args[0] instanceof Closure) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
			this.currentBeanDefinition.getBeanDefinition().setAbstract(true);
		} else {
			List<Object> constructorArgs =
					resolveConstructorArguments(args, 0, hasClosureArgument ? args.length - 1 : args.length);
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null, constructorArgs);
		}

		if (hasClosureArgument) {
			Closure<?> callable = (Closure<?>) args[args.length - 1];
			callable.setDelegate(this);
			callable.setResolveStrategy(Closure.DELEGATE_FIRST);
			callable.call(this.currentBeanDefinition);
		}

		GroovyBeanDefinitionWrapper beanDefinition = this.currentBeanDefinition;
		this.currentBeanDefinition = null;
		beanDefinition.getBeanDefinition().setAttribute(GroovyBeanDefinitionWrapper.class.getName(), beanDefinition);
		getRegistry().registerBeanDefinition(beanName, beanDefinition.getBeanDefinition());
		return beanDefinition;
	}

	protected List<Object> resolveConstructorArguments(Object[] args, int start, int end) {
		Object[] constructorArgs = Arrays.copyOfRange(args, start, end);
		for (int i = 0; i < constructorArgs.length; i++) {
			if (constructorArgs[i] instanceof GString) {
				constructorArgs[i] = constructorArgs[i].toString();
			} else if (constructorArgs[i] instanceof List) {
				constructorArgs[i] = manageListIfNecessary((List<?>) constructorArgs[i]);
			} else if (constructorArgs[i] instanceof Map) {
				constructorArgs[i] = manageMapIfNecessary((Map<?, ?>) constructorArgs[i]);
			}
		}
		return Arrays.asList(constructorArgs);
	}

	/**
	 * 检查{@link Map}内部是否有任何{@link RuntimeBeanReference RuntimeBeanReferences}，
	 * 如有必要，将其转换为{@link ManagedMap}。
	 *
	 * @param map 原始Map
	 * @return 原始map或其管理副本
	 */
	private Object manageMapIfNecessary(Map<?, ?> map) {
		boolean containsRuntimeRefs = false;
		for (Object element : map.values()) {
			if (element instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}
		}
		if (containsRuntimeRefs) {
			Map<Object, Object> managedMap = new ManagedMap<>();
			managedMap.putAll(map);
			return managedMap;
		}
		return map;
	}

	/**
	 * 检查{@link List}内部是否有任何{@link RuntimeBeanReference RuntimeBeanReferences}，
	 * 如有必要，将其转换为{@link ManagedList}。
	 *
	 * @param list 原始List
	 * @return 原始list或其管理副本
	 */
	private Object manageListIfNecessary(List<?> list) {
		boolean containsRuntimeRefs = false;
		for (Object element : list) {
			if (element instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}
		}
		if (containsRuntimeRefs) {
			List<Object> managedList = new ManagedList<>();
			managedList.addAll(list);
			return managedList;
		}
		return list;
	}

	/**
	 * 此方法在{@code GroovyBeanDefinitionReader}范围内重写属性设置，
	 * 以在当前bean定义上设置属性。
	 */
	@Override
	public void setProperty(String name, Object value) {
		if (this.currentBeanDefinition != null) {
			applyPropertyToBeanDefinition(name, value);
		}
	}

	protected void applyPropertyToBeanDefinition(String name, Object value) {
		if (value instanceof GString) {
			value = value.toString();
		}
		if (addDeferredProperty(name, value)) {
			return;
		} else if (value instanceof Closure) {
			GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
			try {
				Closure<?> callable = (Closure<?>) value;
				Class<?> parameterType = callable.getParameterTypes()[0];
				if (Object.class == parameterType) {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper("");
					callable.call(this.currentBeanDefinition);
				} else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, parameterType);
					callable.call((Object) null);
				}

				value = this.currentBeanDefinition.getBeanDefinition();
			} finally {
				this.currentBeanDefinition = current;
			}
		}
		this.currentBeanDefinition.addProperty(name, value);
	}

	/**
	 * 此方法在{@code GroovyBeanDefinitionReader}范围内重写属性检索。
	 * 属性检索将：
	 * <ul>
	 * <li>如果存在，从bean构建器的绑定中检索变量
	 * <li>如果存在，为特定bean检索RuntimeBeanReference
	 * <li>否则只委托给MetaClass.getProperty，它将从{@code GroovyBeanDefinitionReader}本身解析属性
	 * </ul>
	 */
	@Override
	public Object getProperty(String name) {
		Binding binding = getBinding();
		if (binding != null && binding.hasVariable(name)) {
			return binding.getVariable(name);
		} else {
			if (this.namespaces.containsKey(name)) {
				return createDynamicElementReader(name);
			}
			if (getRegistry().containsBeanDefinition(name)) {
				GroovyBeanDefinitionWrapper beanDefinition = (GroovyBeanDefinitionWrapper)
						getRegistry().getBeanDefinition(name).getAttribute(GroovyBeanDefinitionWrapper.class.getName());
				if (beanDefinition != null) {
					return new GroovyRuntimeBeanReference(name, beanDefinition, false);
				} else {
					return new RuntimeBeanReference(name, false);
				}
			}
			// 这是为了处理属性设置器是闭包中最后一个语句的情况
			// （因此是返回值）
			else if (this.currentBeanDefinition != null) {
				MutablePropertyValues pvs = this.currentBeanDefinition.getBeanDefinition().getPropertyValues();
				if (pvs.contains(name)) {
					return pvs.get(name);
				} else {
					DeferredProperty dp = this.deferredProperties.get(this.currentBeanDefinition.getBeanName() + name);
					if (dp != null) {
						return dp.value;
					} else {
						return getMetaClass().getProperty(this, name);
					}
				}
			} else {
				return getMetaClass().getProperty(this, name);
			}
		}
	}

	private GroovyDynamicElementReader createDynamicElementReader(String namespace) {
		XmlReaderContext readerContext = this.groovyDslXmlBeanDefinitionReader.createReaderContext(
				new DescriptiveResource("Groovy"));
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		boolean decorating = (this.currentBeanDefinition != null);
		if (!decorating) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(namespace);
		}
		return new GroovyDynamicElementReader(namespace, this.namespaces, delegate, this.currentBeanDefinition, decorating) {
			@Override
			protected void afterInvocation() {
				if (!this.decorating) {
					currentBeanDefinition = null;
				}
			}
		};
	}


	/**
	 * 此类用于延迟向bean定义添加属性直到稍后。这适用于这样的情况：
	 * 您将属性分配给一个列表，该列表在分配时可能不包含bean引用，
	 * 但稍后可能包含；因此，它需要被管理。
	 */
	private static class DeferredProperty {

		private final GroovyBeanDefinitionWrapper beanDefinition;

		private final String name;

		public Object value;

		public DeferredProperty(GroovyBeanDefinitionWrapper beanDefinition, String name, Object value) {
			this.beanDefinition = beanDefinition;
			this.name = name;
			this.value = value;
		}

		public void apply() {
			this.beanDefinition.addProperty(this.name, this.value);
		}
	}


	/**
	 * 负责向运行时引用添加新属性的RuntimeBeanReference。
	 */
	private class GroovyRuntimeBeanReference extends RuntimeBeanReference implements GroovyObject {

		/**
		 * Groovy bean定义包装器
		 */
		private final GroovyBeanDefinitionWrapper beanDefinition;

		/**
		 * 元类型
		 */
		private MetaClass metaClass;

		public GroovyRuntimeBeanReference(String beanName, GroovyBeanDefinitionWrapper beanDefinition, boolean toParent) {
			super(beanName, toParent);
			this.beanDefinition = beanDefinition;
			this.metaClass = InvokerHelper.getMetaClass(this);
		}

		@Override
		public MetaClass getMetaClass() {
			return this.metaClass;
		}

		@Override
		public Object getProperty(String property) {
			if (property.equals("beanName")) {
				//如果属性名为beanName,获取bean名称
				return getBeanName();
			} else if (property.equals("source")) {
				//如果属性名为source,获取数据源
				return getSource();
			} else if (this.beanDefinition != null) {
				//如果bean定义不为空，返回Groovy属性值
				return new GroovyPropertyValue(
						property, this.beanDefinition.getBeanDefinition().getPropertyValues().get(property));
			} else {
				//否则通过元类型获取属性值
				return this.metaClass.getProperty(this, property);
			}
		}

		@Override
		public Object invokeMethod(String name, Object args) {
			return this.metaClass.invokeMethod(this, name, args);
		}

		@Override
		public void setMetaClass(MetaClass metaClass) {
			this.metaClass = metaClass;
		}

		@Override
		public void setProperty(String property, Object newValue) {
			if (!addDeferredProperty(property, newValue)) {
				//如果未能添加延迟属性，则获取属性值列表后，将其添加进属性名和属性值中。
				this.beanDefinition.getBeanDefinition().getPropertyValues().add(property, newValue);
			}
		}


		/**
		 * 包装bean定义属性，并确保对其添加的所有RuntimeBeanReference都将延迟以供以后解决。
		 */
		private class GroovyPropertyValue extends GroovyObjectSupport {

			/**
			 * 属性名称
			 */
			private final String propertyName;

			/**
			 * 属性值
			 */
			private final Object propertyValue;

			public GroovyPropertyValue(String propertyName, Object propertyValue) {
				this.propertyName = propertyName;
				this.propertyValue = propertyValue;
			}

			@SuppressWarnings("unused")
			public void leftShift(Object value) {
				InvokerHelper.invokeMethod(this.propertyValue, "leftShift", value);
				updateDeferredProperties(value);
			}

			@SuppressWarnings("unused")
			public boolean add(Object value) {
				boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "add", value);
				updateDeferredProperties(value);
				return retVal;
			}

			@SuppressWarnings("unused")
			public boolean addAll(Collection<?> values) {
				boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "addAll", values);
				for (Object value : values) {
					updateDeferredProperties(value);
				}
				return retVal;
			}

			@Override
			public Object invokeMethod(String name, Object args) {
				return InvokerHelper.invokeMethod(this.propertyValue, name, args);
			}

			@Override
			public Object getProperty(String name) {
				return InvokerHelper.getProperty(this.propertyValue, name);
			}

			@Override
			public void setProperty(String name, Object value) {
				InvokerHelper.setProperty(this.propertyValue, name, value);
			}

			private void updateDeferredProperties(Object value) {
				if (value instanceof RuntimeBeanReference) {
					deferredProperties.put(beanDefinition.getBeanName(),
							new DeferredProperty(beanDefinition, this.propertyName, this.propertyValue));
				}
			}
		}
	}

}
