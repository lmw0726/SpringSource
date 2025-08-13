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

package org.springframework.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

/**
 * {@link ResourcePatternResolver} 的实现类，能够将指定的资源位置路径解析成一个或多个匹配的资源。
 * 源路径可以是简单路径（与目标 {@link org.springframework.core.io.Resource} 一一对应），
 * 也可以包含特殊的 "{@code classpath*:}" 前缀和/或内部的 Ant 风格正则表达式
 * （通过 Spring 的 {@link org.springframework.util.AntPathMatcher} 工具进行匹配）。
 * 后两者均为通配符的表现形式。
 *
 * <p><b>无通配符情况：</b>
 *
 * <p>在简单情况下，如果指定的位置路径不以 {@code "classpath*:}"} 前缀开头，且不包含 PathMatcher 模式，
 * 该解析器将直接通过底层 {@code ResourceLoader} 的 {@code getResource()} 调用返回单个资源。
 * 例如，真实的 URL 如 "{@code file:C:/context.xml}"，伪 URL 如 "{@code classpath:/context.xml}"，
 * 以及简单的无前缀路径如 "{@code /WEB-INF/context.xml}"。
 * 后者的解析依赖底层 {@code ResourceLoader}，例如 {@code WebApplicationContext} 会返回 {@code ServletContextResource}。
 *
 * <p><b>Ant 风格模式：</b>
 *
 * <p>当路径包含 Ant 风格模式时，例如：
 * <pre class="code">
 * /WEB-INF/*-context.xml
 * com/mycompany/**&#47;applicationContext.xml
 * file:C:/some/path/*-context.xml
 * classpath:com/mycompany/**&#47;applicationContext.xml</pre>
 * 解析器遵循更复杂但定义明确的流程尝试解析通配符。
 * 它会生成路径中最后一个非通配符段对应的 {@code Resource}，并从中获取一个 {@code URL}。
 * 如果此 URL 不是 "{@code jar:}" 或容器特定变体（如 WebLogic 的 "{@code zip:}"，
 * WebSphere 的 "{@code wsjar}" 等），
 * 则会将其转为 {@code java.io.File}，并通过文件系统遍历来解析通配符。
 * 如果是 jar URL，解析器会获取 {@code java.net.JarURLConnection}，或手动解析 jar URL，
 * 以遍历 jar 文件内容来解析通配符。
 *
 * <p><b>关于可移植性的影响：</b>
 *
 * <p>如果指定路径已经是文件 URL（显式或隐式，因为底层 {@code ResourceLoader} 是文件系统实现），
 * 则通配符解析可以保证完全可移植。
 *
 * <p>如果指定路径是类路径位置，解析器必须通过 {@code Classloader.getResource()} 调用
 * 获取最后一个非通配符路径段的 URL。
 * 由于这是路径节点（而非最终文件），
 * JDK 文档未定义具体返回何种 URL。
 * 通常是 {@code java.io.File}（当类路径资源指向文件系统位置时）或某种 jar URL（当指向 jar 时）。
 * 该操作存在一定可移植性问题。
 *
 * <p>如果最后非通配符段获得的是 jar URL，解析器必须能够获取 {@code java.net.JarURLConnection}，
 * 或手动解析 jar URL，以遍历 jar 内容解析通配符。
 * 这在大多数环境下可用，但部分环境可能失败，建议在特定环境中充分测试 jar 资源的通配符解析。
 *
 * <p><b>{@code classpath*:} 前缀：</b>
 *
 * <p>特别支持通过 "{@code classpath*:}" 前缀检索多个同名类路径资源。
 * 例如，"{@code classpath*:META-INF/beans.xml}" 会找到类路径下所有名为 "beans.xml" 的文件，
 * 包括 "classes" 目录和 jar 文件内。
 * 这对自动检测每个 jar 中相同位置的配置文件特别有用。
 * 内部通过 {@code ClassLoader.getResources()} 调用实现，完全可移植。
 *
 * <p>"{@code classpath*:}" 前缀也可以与 PathMatcher 模式结合使用，
 * 例如 "classpath*:META-INF/*-beans.xml"。
 * 在这种情况下，解析策略较为简单：
 * 使用 {@code ClassLoader.getResources()} 对最后一个非通配符路径段获取所有匹配资源，
 * 然后对每个资源对通配符子路径使用前述 PathMatcher 解析策略。
 *
 * <p><b>其他说明：</b>
 *
 * <p><b>警告：</b> 当 "{@code classpath*:}" 与 Ant 风格模式结合使用时，
 * 除非实际目标文件位于文件系统，否则必须至少在模式开始前有一个根目录，
 * 否则如 "{@code classpath*:*.xml}" 不会从 jar 根目录检索文件，
 * 只会从展开目录根目录检索。
 * 这是由于 JDK 的 {@code ClassLoader.getResources()} 方法对空字符串（表示搜索根）只返回文件系统位置的限制。
 * 该 {@code ResourcePatternResolver} 试图通过 {@link URLClassLoader} 反射和
 * "java.class.path" 清单评估来缓解 jar 根目录查找限制，但不保证可移植性。
 *
 * <p><b>警告：</b> 使用 "classpath:" 资源的 Ant 风格模式时，
 * 若根包在多个类路径位置存在，不保证能找到所有匹配资源。
 * 因为如资源
 * <pre class="code">
 *     com/mycompany/package1/service-context.xml
 * </pre>
 * 可能仅存在于一个位置，
 * 但用路径
 * <pre class="code">
 *     classpath:com/mycompany/**&#47;service-context.xml
 * </pre>
 * 解析时，解析器只会基于 {@code getResource("com/mycompany")} 返回的第一个 URL 工作，
 * 如果根包存在多个类加载器位置，实际资源可能不在该路径下。
 * 因此，在此情况下推荐使用 "{@code classpath*:}" 加同样的 Ant 风格模式，
 * 以搜索所有包含根包的类路径位置。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Marius Bogoevici
 * @author Costin Leau
 * @author Phillip Webb
 * @see #CLASSPATH_ALL_URL_PREFIX
 * @see org.springframework.util.AntPathMatcher
 * @see org.springframework.core.io.ResourceLoader#getResource(String)
 * @see ClassLoader#getResources(String)
 * @since 1.0.2
 */
public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(PathMatchingResourcePatternResolver.class);

	@Nullable
	private static Method equinoxResolveMethod;

	static {
		try {
			//检测Equinox OSGi(例如在WebSphere 6.1上)
			Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
					PathMatchingResourcePatternResolver.class.getClassLoader());
			equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
			logger.trace("Found Equinox FileLocator for OSGi bundle URL resolution");
		} catch (Throwable ex) {
			equinoxResolveMethod = null;
		}
	}

	/**
	 * 资源加载器，实际加载资源的加载器
	 */
	private final ResourceLoader resourceLoader;

	/**
	 * 负责解析Ant风格路径的资源
	 */
	private PathMatcher pathMatcher = new AntPathMatcher();


	public PathMatchingResourcePatternResolver() {
		this.resourceLoader = new DefaultResourceLoader();
	}


	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}


	public PathMatchingResourcePatternResolver(@Nullable ClassLoader classLoader) {
		this.resourceLoader = new DefaultResourceLoader(classLoader);
	}

	/**
	 * 返回此模式解析器使用的ResourceLoader。
	 *
	 * @return 资源加载器
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * 获取类加载器
	 *
	 * @return 类加载器
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return getResourceLoader().getClassLoader();
	}

	/**
	 * 设置路径匹配器，默认是Ant风格的路径匹配器
	 *
	 * @param pathMatcher 路径匹配器
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回此资源模式解析器使用的路径匹配器。
	 *
	 * @return 路径匹配器
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 根据资源的位置获取资源
	 *
	 * @param location 资源的位置
	 * @return 资源
	 */
	@Override
	public Resource getResource(String location) {
		return getResourceLoader().getResource(location);
	}

	/**
	 * 根据位置模式获取多个资源
	 *
	 * @param locationPattern 位置模式
	 * @return 多个资源
	 * @throws IOException IO异常
	 */
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		//如果该位置模式以classpath*:开头
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			//截取classpath*:后面的字符串，判断该字符串是否是Ant风格的路径
			if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
				// 如果是Ant风格的路径，调用findPathMatchingResources方法
				return findPathMatchingResources(locationPattern);
			} else {
				//如果不是Ant风格的路径，调用findAllClassPathResources解析classpath*:后面的字符串。
				return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
			}
		} else {
			//如果是位置模式是war:开头，找到*/后面的字符串。否则，找：后面的字符串
			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
					locationPattern.indexOf(':') + 1);
			//后面的路径是Ant风格的路径
			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
				//获取该资源对应的多个文件。
				return findPathMatchingResources(locationPattern);
			} else {
				//不是Ant风格的路径，获取单个资源的。
				return new Resource[]{getResourceLoader().getResource(locationPattern)};
			}
		}
	}

	/**
	 * 通过 ClassLoader 查找具有给定位置的所有类位置资源。实际调用doFindAllClassPathResources方法
	 *
	 * @param location 指定位置
	 * @return 多个资源
	 * @throws IOException IO异常
	 */
	protected Resource[] findAllClassPathResources(String location) throws IOException {
		String path = location;
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		Set<Resource> result = doFindAllClassPathResources(path);
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved classpath location [" + location + "] to resources " + result);
		}
		return result.toArray(new Resource[0]);
	}


	/**
	 * 根据ClassLoader 查找具有给定路径的所有类位置资源。
	 *
	 * @param path 绝对路径，不是/开头
	 * @return 多个资源
	 * @throws IOException IO异常
	 */
	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
		Set<Resource> result = new LinkedHashSet<>(16);
		ClassLoader cl = getClassLoader();
		//如果类加载器不为空，通过类加载器获取该路径对应的URL资源，否则获取该路径对应的系统资源。
		Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
		while (resourceUrls.hasMoreElements()) {
			//遍历所有URL，并将URL转为Resource
			URL url = resourceUrls.nextElement();
			result.add(convertClassLoaderURL(url));
		}
		//如果位置为空，添加所有类加载器Jar根目录资源。
		if (!StringUtils.hasLength(path)) {
			addAllClassLoaderJarRoots(cl, result);
		}
		return result;
	}

	/**
	 * 将URL转为Resource
	 *
	 * @param url 统一资源定位符
	 * @return 资源
	 */
	protected Resource convertClassLoaderURL(URL url) {
		return new UrlResource(url);
	}

	/**
	 * 以所有URL所在的类加载器搜索jar文件，并将它们添加到指定的结果集中。
	 *
	 * @param classLoader 类加载器
	 * @param result      结果集
	 */
	protected void addAllClassLoaderJarRoots(@Nullable ClassLoader classLoader, Set<Resource> result) {
		//如果类加载器是URLClassLoader
		if (classLoader instanceof URLClassLoader) {
			try {
				//转为URLClassLoader获取URL数组并遍历它们
				for (URL url : ((URLClassLoader) classLoader).getURLs()) {
					try {
						//如果该URL的协议头是jar协议头，转为UrlResource。否则在它的前后添加jar协议头并转为UrlResource。
						UrlResource jarResource = (ResourceUtils.URL_PROTOCOL_JAR.equals(url.getProtocol()) ?
								new UrlResource(url) :
								new UrlResource(ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR));
						//判断这个UrlResource是否存在，存在就添加进result结果集中。
						if (jarResource.exists()) {
							result.add(jarResource);
						}
					} catch (MalformedURLException ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Cannot search for matching files underneath [" + url +
									"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
						}
					}
				}
			} catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
							"] does not support 'getURLs()': " + ex);
				}
			}
		}
		//该类加载器是系统类加载器
		if (classLoader == ClassLoader.getSystemClassLoader()) {
			//“java.class.path”清单评估...，添加类路径清单
			addClassPathManifestEntries(result);
		}

		if (classLoader != null) {
			try {
				//层次遍历，获取父的类加载器继续遍历添加进集合中。
				addAllClassLoaderJarRoots(classLoader.getParent(), result);
			} catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
							"] does not support 'getParent()': " + ex);
				}
			}
		}
	}


	/**
	 * 从“java.class.path”清单属性确定 jar 文件引用。并将它们以指向jar文件内容根的指针的形式添加到给定的资源集。
	 *
	 * @param result 添加到Jar根的资源集合
	 */
	protected void addClassPathManifestEntries(Set<Resource> result) {
		try {
			//获取Java类路径属性
			String javaClassPathProperty = System.getProperty("java.class.path");
			//以文件分隔符分割该类路径
			String[] properties = StringUtils.delimitedListToStringArray(
					javaClassPathProperty, System.getProperty("path.separator"));
			for (String path : properties) {
				try {
					//获取绝对路径
					String filePath = new File(path).getAbsolutePath();
					int prefixIndex = filePath.indexOf(':');
					if (prefixIndex == 1) {
						//可能是 Windows 上的“c:”驱动器前缀，大写以进行正确的重复检测
						filePath = StringUtils.capitalize(filePath);
					}
					//可以出现在目录文件名中，java.net.URL 不应将其视为分片
					filePath = StringUtils.replace(filePath, "#", "%23");
					// 构建指向 jar 文件根目录的 URL
					UrlResource jarResource = new UrlResource(ResourceUtils.JAR_URL_PREFIX +
							ResourceUtils.FILE_URL_PREFIX + filePath + ResourceUtils.JAR_URL_SEPARATOR);
					//可能与上面的 URLClassLoader.getURLs() 结果重叠！
					if (!result.contains(jarResource) && !hasDuplicate(filePath, result) && jarResource.exists()) {
						result.add(jarResource);
					}
				} catch (MalformedURLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Cannot search for matching files underneath [" + path +
								"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
					}
				}
			}
		} catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
			}
		}
	}


	/**
	 * 检查给定文件路径在现有结果中是否有重复但结构不同的条目，即有或没有前导斜杠。
	 *
	 * @param filePath 文件路径（有或没有前导斜杠的路径）
	 * @param result   当前结果集
	 * @return 如果重复了就返回false，不重复改回true
	 */
	private boolean hasDuplicate(String filePath, Set<Resource> result) {
		if (result.isEmpty()) {
			return false;
		}
		String duplicatePath = (filePath.startsWith("/") ? filePath.substring(1) : "/" + filePath);
		try {
			return result.contains(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX +
					duplicatePath + ResourceUtils.JAR_URL_SEPARATOR));
		} catch (MalformedURLException ex) {
			//忽略：仅用于针对重复项进行测试。
			return false;
		}
	}


	/**
	 * 通过 Ant 风格的路径匹配器 查找与给定位置模式匹配的所有资源。支持 jar 文件和 zip 文件以及文件系统中的资源。
	 *
	 * @param locationPattern 位置模式
	 * @return 与Ant风格路径匹配的资源
	 * @throws IOException IO异常
	 */
	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
		//寻找根文件夹路径
		String rootDirPath = determineRootDir(locationPattern);
		//截取子模式
		String subPattern = locationPattern.substring(rootDirPath.length());
		//根据根文件夹路径获取对应的资源
		Resource[] rootDirResources = getResources(rootDirPath);
		Set<Resource> result = new LinkedHashSet<>(16);
		for (Resource rootDirResource : rootDirResources) {
			//解析根文件夹资源
			rootDirResource = resolveRootDirResource(rootDirResource);
			//获取根文件夹对应的URL
			URL rootDirUrl = rootDirResource.getURL();
			//如果equinox解析方法存在，且URL以bundle开头，通过反射调用equinox解析方法解析URL
			if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
				URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
				if (resolvedUrl != null) {
					rootDirUrl = resolvedUrl;
				}
				rootDirResource = new UrlResource(rootDirUrl);
			}
			//如果该URL协议头以vfs开头，用VfsResourceMatchingDelegate的findMatchingResources解析该URL并添加进集合中。
			if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
			} else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
				//如果该URL是JarURL，或者是Jar资源，以Jar资源形式解析，并添加进集合中
				result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
			} else {
				//否则就以文件资源进行解析，并添加进集合中。
				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
		}
		//返回由第一个资源构成的数组。
		return result.toArray(new Resource[0]);
	}

	/**
	 * 确定给定位置的根目录。用于确定文件匹配的起点，将根目录位置解析为 java.io.File 并将其传递给retrieveMatchingFiles，并将位置的其余部分作为模式。
	 * 比如/WEB-INF/*.xml将返回/WEB-INF/
	 *
	 * @param location 检查位置
	 * @return 表示根目录的位置部分
	 */
	protected String determineRootDir(String location) {
		//找到：后的第一个位置
		int prefixEnd = location.indexOf(':') + 1;
		int rootDirEnd = location.length();
		//如果结束位置大于开始位置，并且他们之间的字符串是Ant风格的路径。
		while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
			//不断缩减结束位置和开始位置的字符串。
			rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
		}
		if (rootDirEnd == 0) {
			rootDirEnd = prefixEnd;
		}
		//截取该字符串的前面部分。
		return location.substring(0, rootDirEnd);
	}


	/**
	 * 解析指定资源进行路径匹配。默认情况下，Equinox OSGi "bundleresource:" "bundleentry:" URL 将被解析为标准 jar 文件 URL，
	 * 使用 Spring 的标准 jar 文件遍历算法进行遍历。对于任何先前的自定义解析，重写此方法并相应地替换资源句柄。
	 * @param original 原来的资源
	 * @return 解析后的资源
	 * @throws IOException IO异常
	 */
	protected Resource resolveRootDirResource(Resource original) throws IOException {
		return original;
	}

	/**
	 * 判断给定的资源句柄是否指示一个 jar 资源，
	 * 并且该资源可以被 {@code doFindPathMatchingJarResources} 方法处理。
	 * <p>默认情况下，协议为 "jar"、"zip"、"vfszip" 和 "wsjar" 的 URL 会被视为 jar 资源。
	 * 该模板方法允许通过资源句柄类型的 {@code instanceof} 检查，检测更多类型的 jar 资源。
	 *
	 * @param resource 待检查的资源句柄（通常是路径匹配的起始根目录）
	 * @see #doFindPathMatchingJarResources
	 * @see org.springframework.util.ResourceUtils#isJarURL
	 */
	protected boolean isJarResource(Resource resource) throws IOException {
		return false;
	}

	/**
	 * 在 jar 文件中查找所有匹配给定位置模式（通过 Ant 风格 PathMatcher）的资源。
	 *
	 * @param rootDirResource 根目录资源
	 * @param rootDirURL 预解析的根目录 URL
	 * @param subPattern 需要匹配的子路径模式（相对于根目录）
	 * @return 可变的匹配到的 Resource 集合
	 * @throws IOException 发生 I/O 错误时抛出
	 * @see java.net.JarURLConnection
	 * @see org.springframework.util.PathMatcher
	 * @since 4.3
	 */
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirURL, String subPattern)
			throws IOException {

		URLConnection con = rootDirURL.openConnection();
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean closeJarFile;

		if (con instanceof JarURLConnection) {
			// 通常传统 JAR 文件会是这种情况
			JarURLConnection jarCon = (JarURLConnection) con;
			ResourceUtils.useCachesIfNecessary(jarCon);
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
			closeJarFile = !jarCon.getUseCaches();
		} else {
			// 非 JarURLConnection，需要手动解析 URL 文件路径
			// 假设格式为 "jar:path!/entry"，协议不限，但需符合此格式
			// 兼容带或不带 "file:" 前缀的路径
			String urlFile = rootDirURL.getFile();
			try {
				int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
				if (separatorIndex == -1) {
					separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
				}
				if (separatorIndex != -1) {
					jarFileUrl = urlFile.substring(0, separatorIndex);
					rootEntryPath = urlFile.substring(separatorIndex + 2); // 分隔符长度均为2
					jarFile = getJarFile(jarFileUrl);
				} else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				closeJarFile = true;
			} catch (ZipException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping invalid jar classpath entry [" + urlFile + "]");
				}
				return Collections.emptySet();
			}
		}

		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}
			if (StringUtils.hasLength(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// 根路径必须以斜杠结尾以保证匹配正确
				// Sun JRE 不会返回斜杠，BEA JRockit 会
				rootEntryPath = rootEntryPath + "/";
			}
			Set<Resource> result = new LinkedHashSet<>(8);
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (getPathMatcher().match(subPattern, relativePath)) {
						result.add(rootDirResource.createRelative(relativePath));
					}
				}
			}
			return result;
		} finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

	/**
	 * 将给定的 jar 文件 URL 解析为 JarFile 对象。
	 */
	protected JarFile getJarFile(String jarFileUrl) throws IOException {
		if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			try {
				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
			} catch (URISyntaxException ex) {
				// 作为回退处理，针对非有效 URI 的 URL（几乎不会发生）
				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
			}
		} else {
			return new JarFile(jarFileUrl);
		}
	}

	/**
	 * 通过 Ant 风格的 PathMatcher，查找文件系统中匹配给定路径模式的所有资源。
	 *
	 * @param rootDirResource 根目录资源
	 * @param subPattern      需要匹配的子路径模式（相对于根目录）
	 * @return 可变的匹配到的 Resource 集合
	 * @throws IOException 发生 I/O 错误时抛出
	 * @see #retrieveMatchingFiles
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		File rootDir;
		try {
			rootDir = rootDirResource.getFile().getAbsoluteFile();
		} catch (FileNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot search for matching files underneath " + rootDirResource +
						" in the file system: " + ex.getMessage());
			}
			return Collections.emptySet();
		} catch (Exception ex) {
			if (logger.isInfoEnabled()) {
				logger.info("Failed to resolve " + rootDirResource + " in the file system: " + ex);
			}
			return Collections.emptySet();
		}
		return doFindMatchingFileSystemResources(rootDir, subPattern);
	}

	/**
	 * 通过 Ant 风格的 PathMatcher，查找文件系统中匹配给定路径模式的所有资源。
	 *
	 * @param rootDir    文件系统中的根目录
	 * @param subPattern 需要匹配的子路径模式（相对于根目录）
	 * @return 可变的匹配到的 Resource 集合
	 * @throws IOException 发生 I/O 错误时抛出
	 * @see #retrieveMatchingFiles
	 * @see org.springframework.util.PathMatcher
	 */
	protected Set<Resource> doFindMatchingFileSystemResources(File rootDir, String subPattern) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Looking for matching resources in directory tree [" + rootDir.getPath() + "]");
		}
		Set<File> matchingFiles = retrieveMatchingFiles(rootDir, subPattern);
		Set<Resource> result = new LinkedHashSet<>(matchingFiles.size());
		for (File file : matchingFiles) {
			result.add(new FileSystemResource(file));
		}
		return result;
	}

	/**
	 * 检索匹配给定路径模式的文件，
	 * 会检查指定目录及其所有子目录。
	 *
	 * @param rootDir 起始目录
	 * @param pattern 需要匹配的路径模式，基于 rootDir 的相对路径
	 * @return 可变的匹配文件集合
	 * @throws IOException 如果无法获取目录内容则抛出
	 */
	protected Set<File> retrieveMatchingFiles(File rootDir, String pattern) throws IOException {
		if (!rootDir.exists()) {
			// 静默跳过不存在的目录
			if (logger.isDebugEnabled()) {
				logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
			}
			return Collections.emptySet();
		}
		if (!rootDir.isDirectory()) {
			// 目录不存在，但不是目录则打印日志
			if (logger.isInfoEnabled()) {
				logger.info("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
			}
			return Collections.emptySet();
		}
		if (!rootDir.canRead()) {
			if (logger.isInfoEnabled()) {
				logger.info("Skipping search for matching files underneath directory [" + rootDir.getAbsolutePath() +
						"] because the application is not allowed to read the directory");
			}
			return Collections.emptySet();
		}
		String fullPattern = StringUtils.replace(rootDir.getAbsolutePath(), File.separator, "/");
		if (!pattern.startsWith("/")) {
			fullPattern += "/";
		}
		fullPattern = fullPattern + StringUtils.replace(pattern, File.separator, "/");
		Set<File> result = new LinkedHashSet<>(8);
		doRetrieveMatchingFiles(fullPattern, rootDir, result);
		return result;
	}

	/**
	 * 递归检索匹配给定模式的文件，
	 * 并将匹配的文件添加到传入的结果集合中。
	 *
	 * @param fullPattern 带根目录路径的完整匹配模式
	 * @param dir         当前目录
	 * @param result      匹配文件集合，用于收集结果
	 * @throws IOException 如果无法读取目录内容则抛出
	 */
	protected void doRetrieveMatchingFiles(String fullPattern, File dir, Set<File> result) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Searching directory [" + dir.getAbsolutePath() +
					"] for files matching pattern [" + fullPattern + "]");
		}
		for (File content : listDirectory(dir)) {
			String currPath = StringUtils.replace(content.getAbsolutePath(), File.separator, "/");
			if (content.isDirectory() && getPathMatcher().matchStart(fullPattern, currPath + "/")) {
				if (!content.canRead()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping subdirectory [" + dir.getAbsolutePath() +
								"] because the application is not allowed to read the directory");
					}
				} else {
					doRetrieveMatchingFiles(fullPattern, content, result);
				}
			}
			if (getPathMatcher().match(fullPattern, currPath)) {
				result.add(content);
			}
		}
	}

	/**
	 * 获取指定目录下的文件列表，并按字母顺序排序。
	 *
	 * @param dir 要检查的目录
	 * @return 排序后的文件数组（默认按文件名字母顺序）
	 * @see File#listFiles()
	 * @since 5.1
	 */
	protected File[] listDirectory(File dir) {
		File[] files = dir.listFiles();
		if (files == null) {
			if (logger.isInfoEnabled()) {
				logger.info("Could not retrieve contents of directory [" + dir.getAbsolutePath() + "]");
			}
			return new File[0];
		}
		Arrays.sort(files, Comparator.comparing(File::getName));
		return files;
	}


	/**
	 * 内部代理类，用于避免在运行时对 JBoss VFS API 的硬依赖。
	 */
	private static class VfsResourceMatchingDelegate {

		public static Set<Resource> findMatchingResources(
				URL rootDirURL, String locationPattern, PathMatcher pathMatcher) throws IOException {

			Object root = VfsPatternUtils.findRoot(rootDirURL);
			PatternVirtualFileVisitor visitor =
					new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
			VfsPatternUtils.visit(root, visitor);
			return visitor.getResources();
		}
	}


	/**
	 * 用于 VFS 路径匹配的访问者，基于动态代理实现。
	 */
	@SuppressWarnings("unused")
	private static class PatternVirtualFileVisitor implements InvocationHandler {

		private final String subPattern;

		private final PathMatcher pathMatcher;

		private final String rootPath;

		private final Set<Resource> resources = new LinkedHashSet<>();

		public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
			this.subPattern = subPattern;
			this.pathMatcher = pathMatcher;
			this.rootPath = (rootPath.isEmpty() || rootPath.endsWith("/") ? rootPath : rootPath + "/");
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (Object.class == method.getDeclaringClass()) {
				if (methodName.equals("equals")) {
					// 只有当代理对象完全相同时才认为相等。
					return (proxy == args[0]);
				} else if (methodName.equals("hashCode")) {
					return System.identityHashCode(proxy);
				}
			} else if ("getAttributes".equals(methodName)) {
				return getAttributes();
			} else if ("visit".equals(methodName)) {
				visit(args[0]);
				return null;
			} else if ("toString".equals(methodName)) {
				return toString();
			}

			throw new IllegalStateException("Unexpected method invocation: " + method);
		}

		public void visit(Object vfsResource) {
			if (this.pathMatcher.match(this.subPattern,
					VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
				this.resources.add(new VfsResource(vfsResource));
			}
		}

		@Nullable
		public Object getAttributes() {
			return VfsPatternUtils.getVisitorAttributes();
		}

		public Set<Resource> getResources() {
			return this.resources;
		}

		public int size() {
			return this.resources.size();
		}

		@Override
		public String toString() {
			return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
		}
	}

}
