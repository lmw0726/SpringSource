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

package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 由命令行参数支持的 {@link PropertySource} 实现的抽象基类。参数化类型 {@code T}
 * 表示命令行选项的底层源。这可能像 {@link SimpleCommandLinePropertySource} 情况下
 * 的简单字符串数组一样简单，或者特定于特定 API，例如 {@link JOptCommandLinePropertySource}
 * 情况下的 JOpt 的 {@code OptionSet}。
 *
 * <h3>目的和一般用法</h3>
 * <p>
 * 用于独立的基于 Spring 的应用程序，即那些通过传统的 {@code main} 方法引导的应用程序，
 * 该方法从命令行接受 {@code String[]} 参数。在许多情况下，直接在 {@code main} 方法中
 * 处理命令行参数可能就足够了，但在其他情况下，可能希望将参数作为值注入到 Spring Bean 中。
 * 正是在后一种情况下，{@code CommandLinePropertySource} 变得有用。
 * {@code CommandLinePropertySource} 通常会被添加到 Spring {@code ApplicationContext} 的
 * {@link Environment} 中，此时所有命令行参数都可通过 {@link Environment#getProperty(String)}
 * 系列方法获得。例如：
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 * <p>
 * 使用上述引导逻辑，{@code AppConfig} 类可以 {@code @Inject} Spring {@code Environment}
 * 并直接查询属性：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public void DataSource dataSource() {
 *         MyVendorDataSource dataSource = new MyVendorDataSource();
 *         dataSource.setHostname(env.getProperty("db.hostname", "localhost"));
 *         dataSource.setUsername(env.getRequiredProperty("db.username"));
 *         dataSource.setPassword(env.getRequiredProperty("db.password"));
 *         // ...
 *         return dataSource;
 *     }
 * }</pre>
 * <p>
 * 因为 {@code CommandLinePropertySource} 是使用 {@code #addFirst} 方法添加到
 * {@code Environment} 的 {@link MutablePropertySources} 集合中的，它具有最高的搜索优先级，
 * 这意味着虽然 "db.hostname" 和其他属性可能存在于其他属性源（如系统环境变量）中，
 * 但会首先从命令行属性源中选择。这是一种合理的方法，因为在命令行上指定的参数
 * 自然比作为环境变量指定的参数更具体。
 *
 * <p>作为注入 {@code Environment} 的替代方案，可以使用 Spring 的 {@code @Value}
 * 注解来注入这些属性，前提是已注册 {@link PropertySourcesPropertyResolver} Bean，
 * 直接注册或通过使用 {@code <context:property-placeholder>} 元素注册。例如：
 *
 * <pre class="code">
 * &#064;Component
 * public class MyComponent {
 *
 *     &#064;Value("my.property:defaultVal")
 *     private String myProperty;
 *
 *     public void getMyProperty() {
 *         return this.myProperty;
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>处理选项参数</h3>
 *
 * <p>单个命令行参数通过常用的 {@link PropertySource#getProperty(String)} 和
 * {@link PropertySource#containsProperty(String)} 方法表示为属性。例如，给定以下命令行：
 *
 * <pre class="code">--o1=v1 --o2</pre>
 * <p>
 * 'o1' 和 'o2' 被视为"选项参数"，以下断言将评估为真：
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("o3") == false;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("");
 * assert ps.getProperty("o3") == null;
 * </pre>
 * <p>
 * 请注意，'o2' 选项没有参数，但 {@code getProperty("o2")} 解析为空字符串 ({@code ""})
 * 而不是 {@code null}，而 {@code getProperty("o3")} 解析为 {@code null}，
 * 因为它没有被指定。这种行为与所有 {@code PropertySource} 实现应遵循的一般契约一致。
 *
 * <p>还要注意，虽然上面的示例中使用了 "--" 来表示选项参数，但这种语法在各个命令行参数库中
 * 可能有所不同。例如，基于 JOpt 或 Commons CLI 的实现可能允许单破折号 ("-") "短" 选项参数等。
 *
 * <h3>处理非选项参数</h3>
 *
 * <p>通过这种抽象也支持非选项参数。任何没有选项样式前缀（如 "-" 或 "--"）的参数都被视为
 * "非选项参数"，并可通过特殊的 {@linkplain #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME "nonOptionArgs"}
 * 属性获得。如果指定了多个非选项参数，此属性的值将是包含所有参数的逗号分隔字符串。
 * 这种方法确保了来自 {@code CommandLinePropertySource} 的所有属性具有简单且一致的返回类型 (String)，
 * 同时在与 Spring {@link Environment} 及其内置 {@code ConversionService} 结合使用时有助于转换。
 * 考虑以下示例：
 *
 * <pre class="code">--o1=v1 --o2=v2 /path/to/file1 /path/to/file2</pre>
 * <p>
 * 在此示例中，"o1" 和 "o2" 将被视为"选项参数"，而两个文件系统路径则被视为"非选项参数"。
 * 因此，以下断言将评估为真：
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("nonOptionArgs") == true;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("v2");
 * assert ps.getProperty("nonOptionArgs").equals("/path/to/file1,/path/to/file2");
 * </pre>
 *
 * <p>如上所述，当与 Spring {@code Environment} 抽象结合使用时，这个逗号分隔的字符串
 * 可以很容易地转换为字符串数组或列表：
 *
 * <pre class="code">
 * Environment env = applicationContext.getEnvironment();
 * String[] nonOptionArgs = env.getProperty("nonOptionArgs", String[].class);
 * assert nonOptionArgs[0].equals("/path/to/file1");
 * assert nonOptionArgs[1].equals("/path/to/file2");
 * </pre>
 *
 * <p>特殊"非选项参数"属性的名称可以通过 {@link #setNonOptionArgsPropertyName(String)}
 * 方法自定义。建议这样做，因为它为非选项参数提供了适当的语义价值。例如，如果文件系统路径
 * 被指定为非选项参数，那么将其称为类似 "file.locations" 的名称可能比默认的 "nonOptionArgs" 更合适：
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     clps.setNonOptionArgsPropertyName("file.locations");
 *
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * <h3>限制</h3>
 * <p>
 * 这种抽象并不打算暴露底层命令行解析 API（如 JOpt 或 Commons CLI）的全部功能。
 * 它的意图恰恰相反：在命令行参数被解析<em>之后</em>，为访问命令行参数提供最简单的抽象。
 * 因此，典型的情况将涉及完全配置底层命令行解析 API，解析进入 main 方法的 {@code String[]} 参数，
 * 然后简单地将解析结果提供给 {@code CommandLinePropertySource} 的实现。此时，
 * 所有参数都可以被视为"选项"或"非选项"参数，如上所述，可以通过正常的 {@code PropertySource}
 * 和 {@code Environment} API 访问。
 *
 * @param <T> 源类型
 * @author Chris Beams
 * @see PropertySource
 * @see SimpleCommandLinePropertySource
 * @see JOptCommandLinePropertySource
 * @since 3.1
 */
public abstract class CommandLinePropertySource<T> extends EnumerablePropertySource<T> {

	/**
	 * 给予 {@link CommandLinePropertySource} 实例的默认名称：{@value}。
	 */
	public static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

	/**
	 * 表示非选项参数的属性的默认名称：{@value}。
	 */
	public static final String DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME = "nonOptionArgs";


	private String nonOptionArgsPropertyName = DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME;


	/**
	 * 创建一个具有默认名称 {@value #COMMAND_LINE_PROPERTY_SOURCE_NAME}
	 * 且由给定源对象支持的新 {@code CommandLinePropertySource}。
	 */
	public CommandLinePropertySource(T source) {
		super(COMMAND_LINE_PROPERTY_SOURCE_NAME, source);
	}

	/**
	 * 创建一个具有给定名称且由给定源对象支持的新 {@link CommandLinePropertySource}。
	 */
	public CommandLinePropertySource(String name, T source) {
		super(name, source);
	}


	/**
	 * 指定特殊"非选项参数"属性的名称。
	 * 默认值为 {@value #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME}。
	 */
	public void setNonOptionArgsPropertyName(String nonOptionArgsPropertyName) {
		this.nonOptionArgsPropertyName = nonOptionArgsPropertyName;
	}

	/**
	 * 此实现首先检查指定的名称是否为特殊的 {@linkplain #setNonOptionArgsPropertyName(String)
	 * "非选项参数"属性}，如果是，则委托给抽象的 {@link #getNonOptionArgs()} 方法，
	 * 检查它是否返回空集合。否则委托给抽象的 {@link #containsOption(String)} 方法并返回其值。
	 */
	@Override
	public final boolean containsProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			return !this.getNonOptionArgs().isEmpty();
		}
		return this.containsOption(name);
	}

	/**
	 * 此实现首先检查指定的名称是否为特殊的 {@linkplain #setNonOptionArgsPropertyName(String)
	 * "非选项参数"属性}，如果是，则委托给抽象的 {@link #getNonOptionArgs()} 方法。
	 * 如果是并且非选项参数集合为空，此方法返回 {@code null}。如果不为空，
	 * 则返回所有非选项参数的逗号分隔字符串。否则委托给抽象的 {@link #getOptionValues(String)}
	 * 方法并返回其结果。
	 */
	@Override
	@Nullable
	public final String getProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			Collection<String> nonOptionArguments = this.getNonOptionArgs();
			if (nonOptionArguments.isEmpty()) {
				return null;
			} else {
				return StringUtils.collectionToCommaDelimitedString(nonOptionArguments);
			}
		}
		Collection<String> optionValues = this.getOptionValues(name);
		if (optionValues == null) {
			return null;
		} else {
			return StringUtils.collectionToCommaDelimitedString(optionValues);
		}
	}


	/**
	 * 返回从命令行解析的选项参数集合是否包含具有给定名称的选项。
	 */
	protected abstract boolean containsOption(String name);

	/**
	 * 返回与具有给定名称的命令行选项关联的值集合。
	 * <ul>
	 * <li>如果选项存在且没有参数 (例如："--foo")，返回空集合 ({@code []})</li>
	 * <li>如果选项存在且有单个值 (例如 "--foo=bar")，返回有一个元素的集合 ({@code ["bar"]})</li>
	 * <li>如果选项存在且底层命令行解析库支持多个参数 (例如 "--foo=bar --foo=baz")，
	 * 返回每个值都有元素的集合 ({@code ["bar", "baz"]})</li>
	 * <li>如果选项不存在，返回 {@code null}</li>
	 * </ul>
	 */
	@Nullable
	protected abstract List<String> getOptionValues(String name);

	/**
	 * 返回从命令行解析的非选项参数集合。
	 * 永远不会是 {@code null}。
	 */
	protected abstract List<String> getNonOptionArgs();

}
