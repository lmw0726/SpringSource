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

package org.springframework.beans.factory.xml;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * 用于读取 XML Bean 定义的 Bean 定义读取器。
 * 实际的 XML 文档读取操作由 {@link BeanDefinitionDocumentReader} 接口的实现类完成。
 *
 * <p>通常应用于
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * 或 {@link org.springframework.context.support.GenericApplicationContext}。
 *
 * <p>该类会加载 DOM 文档并将其交给 BeanDefinitionDocumentReader 处理。
 * 文档读取器会将每个 Bean 定义注册到指定的 BeanFactory，
 * 并通过该 BeanFactory 对 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 接口的实现进行操作。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @see #setDocumentReaderClass
 * @see BeanDefinitionDocumentReader
 * @see DefaultBeanDefinitionDocumentReader
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @since 26.11.2003
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * 禁用验证模式
	 */
	public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

	/**
	 * 自动获取验证模式
	 */
	public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

	/**
	 * DTD 验证模式
	 */
	public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

	/**
	 * XSD 验证模式
	 */
	public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;


	/**
	 * 该类的常量实例。
	 */
	private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

	/**
	 * 验证模式，默认为自动模式
	 */
	private int validationMode = VALIDATION_AUTO;

	private boolean namespaceAware = false;

	/**
	 * documentReader的类，负责解析XML标签
	 */
	private Class<? extends BeanDefinitionDocumentReader> documentReaderClass =
			DefaultBeanDefinitionDocumentReader.class;

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private ReaderEventListener eventListener = new EmptyReaderEventListener();

	private SourceExtractor sourceExtractor = new NullSourceExtractor();

	@Nullable
	private NamespaceHandlerResolver namespaceHandlerResolver;

	private DocumentLoader documentLoader = new DefaultDocumentLoader();

	@Nullable
	private EntityResolver entityResolver;

	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	/**
	 * XML验证模式探测器
	 */
	private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

	/**
	 * 当前线程，正在加载的EncodeResource集合
	 */
	private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
			new NamedThreadLocal<Set<EncodedResource>>("XML bean definition resources currently being loaded") {
				@Override
				protected Set<EncodedResource> initialValue() {
					return new HashSet<>(4);
				}
			};


	/**
	 * 为给定的 BeanFactory 创建新的 XmlBeanDefinitionReader。
	 *
	 * @param registry 要加载 Bean 定义的 BeanFactory，
	 *                 以 BeanDefinitionRegistry 形式提供
	 */
	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * 设置是否使用 XML 验证。默认值为 {@code true}。
	 * <p>如果关闭验证，该方法会开启命名空间感知，
	 * 以便在此情况下仍能正确处理模式命名空间。
	 *
	 * @see #setValidationMode
	 * @see #setNamespaceAware
	 */
	public void setValidating(boolean validating) {
		this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
		this.namespaceAware = !validating;
	}

	/**
	 * 按名称设置要使用的验证模式。默认值为 {@link #VALIDATION_AUTO}。
	 *
	 * @see #setValidationMode
	 */
	public void setValidationModeName(String validationModeName) {
		setValidationMode(constants.asNumber(validationModeName).intValue());
	}

	/**
	 * 设置要使用的验证模式。默认为 {@link #VALIDATION_AUTO}。
	 * <p> 请注意，这仅会激活或停用验证本身。
	 * 如果要关闭模式文件的验证，则可能需要显式激活模式名称空间支持: 请参阅 {@link #setNamespaceAware}。
	 */
	public void setValidationMode(int validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * 返回要使用的验证模式
	 */
	public int getValidationMode() {
		return this.validationMode;
	}

	/**
	 * 设置 XML 解析器是否应支持 XML 命名空间。
	 * 默认值为 "false"。
	 * <p>当启用模式验证时，通常不需要设置此项。
	 * 但是在未启用验证的情况下，需要将其设置为 "true"，
	 * 以正确处理模式命名空间。
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * 返回XML解析器是否应该是XML命名空间感知的。
	 */
	public boolean isNamespaceAware() {
		return this.namespaceAware;
	}

	/**
	 * 指定要使用的 {@link org.springframework.beans.factory.parsing.ProblemReporter}。
	 * <p>默认实现为 {@link org.springframework.beans.factory.parsing.FailFastProblemReporter}，
	 * 采用快速失败的行为。外部工具可以提供替代实现，
	 * 将错误和警告收集以在工具 UI 中显示。
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * 指定要使用的 {@link ReaderEventListener}。
	 * <p>默认实现为 EmptyReaderEventListener，会丢弃所有事件通知。
	 * 外部工具可以提供替代实现，用于监控注册到 BeanFactory 的组件。
	 */
	public void setEventListener(@Nullable ReaderEventListener eventListener) {
		this.eventListener = (eventListener != null ? eventListener : new EmptyReaderEventListener());
	}

	/**
	 * 指定要使用的 {@link SourceExtractor}。
	 * <p>默认实现为 {@link NullSourceExtractor}，仅返回 {@code null} 作为源对象。
	 * 这意味着在正常运行时，不会向 Bean 配置元数据附加额外的源信息。
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new NullSourceExtractor());
	}

	/**
	 * 指定要使用的 {@link NamespaceHandlerResolver}。
	 * <p>如果未指定，将通过 {@link #createDefaultNamespaceHandlerResolver()} 创建默认实例。
	 */
	public void setNamespaceHandlerResolver(@Nullable NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}

	/**
	 * 指定要使用的 {@link DocumentLoader}。
	 * <p>默认实现为 {@link DefaultDocumentLoader}，使用 JAXP 加载 {@link Document} 实例。
	 */
	public void setDocumentLoader(@Nullable DocumentLoader documentLoader) {
		this.documentLoader = (documentLoader != null ? documentLoader : new DefaultDocumentLoader());
	}

	/**
	 * 设置用于解析的 SAX 实体解析器。
	 * <p>默认使用 {@link ResourceEntityResolver}，可以覆盖以实现自定义实体解析，
	 * 例如相对于某个特定的基础路径。
	 */
	public void setEntityResolver(@Nullable EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * 返回要使用的EntityResolver，如果未指定，则构建默认解析器。
	 */
	protected EntityResolver getEntityResolver() {
		if (this.entityResolver == null) {
			// 指定要使用的默认实体解析器。
			ResourceLoader resourceLoader = getResourceLoader();
			if (resourceLoader != null) {
				this.entityResolver = new ResourceEntityResolver(resourceLoader);
			} else {
				this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
			}
		}
		return this.entityResolver;
	}

	/**
	 * 设置 {@code org.xml.sax.ErrorHandler} 接口的实现，
	 * 用于自定义处理 XML 解析中的错误和警告。
	 * <p>如果未设置，将使用默认的 SimpleSaxErrorHandler，
	 * 仅使用视图类的日志记录器记录警告，并重新抛出错误以中断 XML 转换。
	 *
	 * @see SimpleSaxErrorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 指定要使用的 {@link BeanDefinitionDocumentReader} 实现类，
	 * 负责实际读取 XML Bean 定义文档。
	 * <p>默认使用 {@link DefaultBeanDefinitionDocumentReader}。
	 *
	 * @param documentReaderClass 所需的 BeanDefinitionDocumentReader 实现类
	 */
	public void setDocumentReaderClass(Class<? extends BeanDefinitionDocumentReader> documentReaderClass) {
		this.documentReaderClass = documentReaderClass;
	}


	/**
	 * 从指定的XML文件加载bean定义
	 *
	 * @param resource XML文件的资源描述符
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * 从指定的XML文件加载bean定义。
	 *
	 * @param encodedResource XML文件的资源描述符，允许指定用于解析文件的编码
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Loading XML bean definitions from " + encodedResource);
		}
		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();

		if (!currentResources.add(encodedResource)) {
			// 如果当前正在加载的资源已经在当前的资源集合中，抛出BeanDefinitionStoreException异常，提示编码资源重复加载。
			throw new BeanDefinitionStoreException(
					"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
		}

		try (InputStream inputStream = encodedResource.getResource().getInputStream()) {
			InputSource inputSource = new InputSource(inputStream);
			if (encodedResource.getEncoding() != null) {
				//设置编码格式
				inputSource.setEncoding(encodedResource.getEncoding());
			}
			return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
		} catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		} finally {
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				//如果当前加载的资源为空，回收TreadLocal，防止内存溢出。
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}

	/**
	 * 从指定的 XML 文件加载 Bean 定义。
	 *
	 * @param inputSource 要读取的 SAX InputSource
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 加载或解析错误时抛出
	 */
	public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
	}

	/**
	 * 从指定的 XML 文件加载 Bean 定义。
	 *
	 * @param inputSource         要读取的 SAX InputSource
	 * @param resourceDescription 资源描述（可以为 {@code null} 或空）
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 加载或解析错误时抛出
	 */
	public int loadBeanDefinitions(InputSource inputSource, @Nullable String resourceDescription)
			throws BeanDefinitionStoreException {

		return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
	}


	/**
	 * 从指定的XML文件实际加载bean定义的函数
	 *
	 * @param inputSource 要读取的SAX输入源
	 * @param resource    XML文件的资源描述符
	 * @return 已经找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 * @see #doLoadDocument
	 * @see #registerBeanDefinitions
	 */
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {

		try {
			//获取XML Document实例
			Document doc = doLoadDocument(inputSource, resource);
			//根据Document实例，注册Bean信息
			int count = registerBeanDefinitions(doc, resource);
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + resource);
			}
			return count;
		} catch (BeanDefinitionStoreException ex) {
			throw ex;
		} catch (SAXParseException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		} catch (SAXException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"XML document from " + resource + " is invalid", ex);
		} catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Parser configuration exception parsing XML from " + resource, ex);
		} catch (IOException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"IOException parsing XML document from " + resource, ex);
		} catch (Throwable ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Unexpected exception parsing XML document from " + resource, ex);
		}
	}

	/**
	 * 实际使用配置的DocumentLoader加载指定的文档。
	 *
	 * @param inputSource 要读取的SAX输入源
	 * @param resource    XML文件的资源描述符
	 * @return DOM Document实例
	 * @throws Exception 当从DocumentLoader抛出异常
	 * @see #setDocumentLoader
	 * @see DocumentLoader#loadDocument
	 */
	protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
		return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
				getValidationModeForResource(resource), isNamespaceAware());
	}

	/**
	 * 决定了 {@link Resource} 的验证模式。
	 * 如果未显式地配置验证模式，则验证模式将从给定资源中获取 {@link #detectValidationMode detected}。
	 * <p>如果您希望完全控制验证模式，即使设置了 {@link #VALIDATION_AUTO} 以外的内容，也可以覆盖此方法。
	 *
	 * @see #detectValidationMode
	 */
	protected int getValidationModeForResource(Resource resource) {
		//获取正在使用的校验模式
		int validationModeToUse = getValidationMode();
		//如果手动指定验证模式，非自动验证模式，直接返回
		if (validationModeToUse != VALIDATION_AUTO) {
			return validationModeToUse;
		}
		//其次，自动获取验证模式
		int detectedMode = detectValidationMode(resource);
		if (detectedMode != VALIDATION_AUTO) {
			return detectedMode;
		}
		//最后，使用XSD作为默认
		return VALIDATION_XSD;
	}


	/**
	 * 检测要对所提供的 {@link Resource} 标识的XML文件执行哪种验证。
	 * 如果文件具有 {@code DOCTYPE} 定义，则使用DTD验证，否则假定XSD验证。
	 * <p> 如果要自定义 {@link #VALIDATION_AUTO} 模式的分辨率，请覆盖此方法。
	 *
	 * @param resource 资源
	 * @return 验证模式
	 */
	protected int detectValidationMode(Resource resource) {
		//如果资源被其他流打开（不可读），抛出BeanDefinitionStoreException 异常
		if (resource.isOpen()) {
			throw new BeanDefinitionStoreException(
					"Passed-in Resource [" + resource + "] contains an open stream: " +
							"cannot determine validation mode automatically. Either pass in a Resource " +
							"that is able to create fresh streams, or explicitly specify the validationMode " +
							"on your XmlBeanDefinitionReader instance.");
		}

		InputStream inputStream;
		try {
			inputStream = resource.getInputStream();
		} catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Unable to determine validation mode for [" + resource + "]: cannot open InputStream. " +
							"Did you attempt to load directly from a SAX InputSource without specifying the " +
							"validationMode on your XmlBeanDefinitionReader instance?", ex);
		}

		try {
			//获取相应的验证模式
			return this.validationModeDetector.detectValidationMode(inputStream);
		} catch (IOException ex) {
			throw new BeanDefinitionStoreException("Unable to determine validation mode for [" +
					resource + "]: an error occurred whilst reading from the InputStream.", ex);
		}
	}

	/**
	 * 注册给定DOM文档中包含的bean定义。由 {@code loadBeanDefinitions} 调用。
	 * <p> 创建解析器类的新实例，并在其上调用 {@code registerBeanDefinitions}。
	 *
	 * @param doc      DOM文档
	 * @param resource 资源描述符 (用于上下文信息)
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在解析错误的情况下
	 * @see #loadBeanDefinitions
	 * @see #setDocumentReaderClass
	 * @see BeanDefinitionDocumentReader#registerBeanDefinitions
	 */
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		//创建Bean定义文档阅读器
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		//获取已注册的Bean定义数量
		int countBefore = getRegistry().getBeanDefinitionCount();
		//创建XML阅读上下文，并注册Bean定义
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		//返回当前bean定义的数量
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}

	/**
	 * 创建 {@link BeanDefinitionDocumentReader}，用于从XML文档中实际读取bean定义。
	 * <p> 默认实现实例化指定的 “documentReaderClass”。
	 *
	 * @see #setDocumentReaderClass
	 */
	protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
		return BeanUtils.instantiateClass(this.documentReaderClass);
	}

	/**
	 * 创建 {@link XmlReaderContext} 以传递给文档阅读器。
	 */
	public XmlReaderContext createReaderContext(Resource resource) {
		return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
				this.sourceExtractor, this, getNamespaceHandlerResolver());
	}

	/**
	 * 懒加载地创建一个默认的命名空间处理解析器 (如果之前没有设置)。
	 *
	 * @see #createDefaultNamespaceHandlerResolver()
	 */
	public NamespaceHandlerResolver getNamespaceHandlerResolver() {
		if (this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return this.namespaceHandlerResolver;
	}

	/**
	 * 如果未指定，则创建 {@link NamespaceHandlerResolver} 的默认实现。
	 * <p> 默认实现返回 {@link DefaultNamespaceHandlerResolver} 的实例。
	 *
	 * @see DefaultNamespaceHandlerResolver#DefaultNamespaceHandlerResolver(ClassLoader)
	 */
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		//获取类加载器
		ClassLoader cl = (getResourceLoader() == null ? getBeanClassLoader() : getResourceLoader().getClassLoader());
		//创建默认命名空间处理解析器
		return new DefaultNamespaceHandlerResolver(cl);
	}

}
