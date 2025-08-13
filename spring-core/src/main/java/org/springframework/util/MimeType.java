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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 表示 MIME 类型，最初在 RFC 2046 中定义，后来被包括 HTTP 在内的其他互联网协议采用。
 *
 * <p>不过，该类不支持 HTTP 内容协商中使用的 q 参数。
 * 这些功能可在 {@code spring-web} 模块中的子类 {@code org.springframework.http.MediaType} 找到。
 *
 * <p>由一个 {@linkplain #getType() 类型} 和一个 {@linkplain #getSubtype() 子类型} 组成。
 * 还提供了通过 {@link #valueOf(String)} 从字符串解析 MIME 类型值的功能。
 * 更多解析选项见 {@link MimeTypeUtils}。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 * @see MimeTypeUtils
 */
public class MimeType implements Comparable<MimeType>, Serializable {

	private static final long serialVersionUID = 4085923477777865903L;


	protected static final String WILDCARD_TYPE = "*";

	private static final String PARAM_CHARSET = "charset";

	private static final BitSet TOKEN;

	static {
		// 变量名参考RFC 2616，2.2节
		BitSet ctl = new BitSet(128);
		for (int i = 0; i <= 31; i++) {
			ctl.set(i);
		}
		ctl.set(127);

		BitSet separators = new BitSet(128);
		separators.set('(');
		separators.set(')');
		separators.set('<');
		separators.set('>');
		separators.set('@');
		separators.set(',');
		separators.set(';');
		separators.set(':');
		separators.set('\\');
		separators.set('\"');
		separators.set('/');
		separators.set('[');
		separators.set(']');
		separators.set('?');
		separators.set('=');
		separators.set('{');
		separators.set('}');
		separators.set(' ');
		separators.set('\t');

		TOKEN = new BitSet(128);
		TOKEN.set(0, 128);
		TOKEN.andNot(ctl);
		TOKEN.andNot(separators);
	}


	private final String type;

	private final String subtype;

	private final Map<String, String> parameters;

	@Nullable
	private transient Charset resolvedCharset;

	@Nullable
	private volatile String toStringValue;


	/**
	 * 创建一个新的 {@code MimeType}，仅指定主类型。
	 * <p>{@linkplain #getSubtype() 子类型} 设置为 <code>"*"</code>，
	 * 参数为空。
	 * @param type 主类型
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MimeType(String type) {
		this(type, WILDCARD_TYPE);
	}

	/**
	 * 创建一个新的 {@code MimeType}，指定主类型和子类型。
	 * <p>参数为空。
	 * @param type 主类型
	 * @param subtype 子类型
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MimeType(String type, String subtype) {
		this(type, subtype, Collections.emptyMap());
	}

	/**
	 * 创建一个新的 {@code MimeType}，指定类型、子类型和字符集。
	 * @param type 主类型
	 * @param subtype 子类型
	 * @param charset 字符集
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MimeType(String type, String subtype, Charset charset) {
		this(type, subtype, Collections.singletonMap(PARAM_CHARSET, charset.name()));
		this.resolvedCharset = charset;
	}

	/**
	 * 复制构造函数，复制给定 {@code MimeType} 的类型、子类型和参数，
	 * 并允许设置指定的字符集。
	 * @param other 另一个 MimeType
	 * @param charset 字符集
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 * @since 4.3
	 */
	public MimeType(MimeType other, Charset charset) {
		this(other.getType(), other.getSubtype(), addCharsetParameter(charset, other.getParameters()));
		this.resolvedCharset = charset;
	}

	/**
	 * 复制构造函数，复制给定 {@code MimeType} 的类型和子类型，
	 * 并允许使用不同的参数。
	 * @param other 另一个 MimeType
	 * @param parameters 参数（可以为 {@code null}）
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(MimeType other, @Nullable Map<String, String> parameters) {
		this(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * 创建新的 {@code MimeType}，指定类型、子类型和参数。
	 * @param type 主类型
	 * @param subtype 子类型
	 * @param parameters 参数（可以为 {@code null}）
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MimeType(String type, String subtype, @Nullable Map<String, String> parameters) {
		Assert.hasLength(type, "'type' must not be empty");
		Assert.hasLength(subtype, "'subtype' must not be empty");
		checkToken(type);
		checkToken(subtype);
		this.type = type.toLowerCase(Locale.ENGLISH);
		this.subtype = subtype.toLowerCase(Locale.ENGLISH);
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> map = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ENGLISH);
			parameters.forEach((parameter, value) -> {
				checkParameters(parameter, value);
				map.put(parameter, value);
			});
			this.parameters = Collections.unmodifiableMap(map);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}

	/**
	 * 复制构造函数，复制给定 {@code MimeType} 的类型、子类型和参数，
	 * 并跳过其他构造函数中执行的检查。
	 * @param other 另一个 MimeType
	 * @since 5.3
	 */
	protected MimeType(MimeType other) {
		this.type = other.type;
		this.subtype = other.subtype;
		this.parameters = other.parameters;
		this.resolvedCharset = other.resolvedCharset;
		this.toStringValue = other.toStringValue;
	}

	/**
	 * 检查给定的 token 字符串是否包含非法字符，依据 RFC 2616 第2.2节定义。
	 * @throws IllegalArgumentException 如果存在非法字符
	 * @see <a href="https://tools.ietf.org/html/rfc2616#section-2.2">HTTP 1.1，第2.2节</a>
	 */
	private void checkToken(String token) {
		for (int i = 0; i < token.length(); i++) {
			char ch = token.charAt(i);
			if (!TOKEN.get(ch)) {
				throw new IllegalArgumentException("Invalid token character '" + ch + "' in token \"" + token + "\"");
			}
		}
	}

	protected void checkParameters(String parameter, String value) {
		Assert.hasLength(parameter, "'parameter' must not be empty");
		Assert.hasLength(value, "'value' must not be empty");
		checkToken(parameter);
		if (PARAM_CHARSET.equals(parameter)) {
			if (this.resolvedCharset == null) {
				this.resolvedCharset = Charset.forName(unquote(value));
			}
		}
		else if (!isQuotedString(value)) {
			checkToken(value);
		}
	}

	private boolean isQuotedString(String s) {
		if (s.length() < 2) {
			return false;
		}
		else {
			return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
		}
	}

	protected String unquote(String s) {
		return (isQuotedString(s) ? s.substring(1, s.length() - 1) : s);
	}

	/**
	 * 指示 {@linkplain #getType() 类型} 是否为通配符字符 <code>*</code>。
	 */
	public boolean isWildcardType() {
		return WILDCARD_TYPE.equals(getType());
	}

	/**
	 * 指示 {@linkplain #getSubtype() 子类型} 是否为通配符字符 <code>*</code>，
	 * 或者是带后缀的通配符字符（例如 <code>*+xml</code>）。
	 * @return 是否为通配符子类型
	 */
	public boolean isWildcardSubtype() {
		return WILDCARD_TYPE.equals(getSubtype()) || getSubtype().startsWith("*+");
	}

	/**
	 * 指示此 MIME 类型是否为具体类型，即类型和子类型都不是通配符字符 <code>*</code>。
	 * @return 是否为具体的 MIME 类型
	 */
	public boolean isConcrete() {
		return !isWildcardType() && !isWildcardSubtype();
	}

	/**
	 * 返回主类型（primary type）。
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * 返回子类型（subtype）。
	 */
	public String getSubtype() {
		return this.subtype;
	}

	/**
	 * 返回根据 RFC 6839 定义的子类型后缀。
	 * @since 5.3
	 */
	@Nullable
	public String getSubtypeSuffix() {
		int suffixIndex = this.subtype.lastIndexOf('+');
		if (suffixIndex != -1 && this.subtype.length() > suffixIndex) {
			return this.subtype.substring(suffixIndex + 1);
		}
		return null;
	}

	/**
	 * 返回由 {@code charset} 参数指示的字符集（如果有）。
	 * @return 字符集，如果不可用则返回 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public Charset getCharset() {
		return this.resolvedCharset;
	}

	/**
	 * 返回指定参数名的通用参数值。
	 * @param name 参数名称
	 * @return 参数值，如果不存在则返回 {@code null}
	 */
	@Nullable
	public String getParameter(String name) {
		return this.parameters.get(name);
	}

	/**
	 * 返回所有通用参数值。
	 * @return 只读映射（可能为空，但永不为 {@code null}）
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * 指示此 MIME 类型是否包含给定的 MIME 类型。
	 * <p>例如，{@code text/*} 包含 {@code text/plain} 和 {@code text/html}，
	 * {@code application/*+xml} 包含 {@code application/soap+xml} 等。
	 * 此方法不是对称的。
	 * @param other 用于比较的参考 MIME 类型
	 * @return 如果此 MIME 类型包含给定的 MIME 类型，则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean includes(@Nullable MimeType other) {
		if (other == null) {
			return false;
		}
		if (isWildcardType()) {
			// */* 包含所有类型
			return true;
		}
		else if (getType().equals(other.getType())) {
			if (getSubtype().equals(other.getSubtype())) {
				return true;
			}
			if (isWildcardSubtype()) {
				// 带后缀的通配符，例如 application/*+xml
				int thisPlusIdx = getSubtype().lastIndexOf('+');
				if (thisPlusIdx == -1) {
					return true;
				}
				else {
					// application/*+xml 包含 application/soap+xml
					int otherPlusIdx = other.getSubtype().lastIndexOf('+');
					if (otherPlusIdx != -1) {
						String thisSubtypeNoSuffix = getSubtype().substring(0, thisPlusIdx);
						String thisSubtypeSuffix = getSubtype().substring(thisPlusIdx + 1);
						String otherSubtypeSuffix = other.getSubtype().substring(otherPlusIdx + 1);
						if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 判断此 MIME 类型是否与给定的 MIME 类型兼容。
	 * <p>例如，{@code text/*} 与 {@code text/plain}、{@code text/html} 兼容，反之亦然。
	 * 实际上，此方法类似于 {@link #includes}，但它是对称的。
	 * @param other 用于比较的参考 MIME 类型
	 * @return 如果此 MIME 类型与给定的 MIME 类型兼容，则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean isCompatibleWith(@Nullable MimeType other) {
		if (other == null) {
			return false;
		}
		if (isWildcardType() || other.isWildcardType()) {
			return true;
		}
		else if (getType().equals(other.getType())) {
			if (getSubtype().equals(other.getSubtype())) {
				return true;
			}
			if (isWildcardSubtype() || other.isWildcardSubtype()) {
				String thisSuffix = getSubtypeSuffix();
				String otherSuffix = other.getSubtypeSuffix();
				if (getSubtype().equals(WILDCARD_TYPE) || other.getSubtype().equals(WILDCARD_TYPE)) {
					return true;
				}
				else if (isWildcardSubtype() && thisSuffix != null) {
					return (thisSuffix.equals(other.getSubtype()) || thisSuffix.equals(otherSuffix));
				}
				else if (other.isWildcardSubtype() && otherSuffix != null) {
					return (this.getSubtype().equals(otherSuffix) || otherSuffix.equals(thisSuffix));
				}
			}
		}
		return false;
	}

	/**
	 * 类似于 {@link #equals(Object)}，但仅基于类型和子类型比较，
	 * 即忽略参数部分。
	 * @param other 要比较的另一个 MIME 类型
	 * @return 两个 MIME 类型的类型和子类型是否相同
	 * @since 5.1.4
	 */
	public boolean equalsTypeAndSubtype(@Nullable MimeType other) {
		if (other == null) {
			return false;
		}
		return this.type.equalsIgnoreCase(other.type) && this.subtype.equalsIgnoreCase(other.subtype);
	}

	/**
	 * 与依赖 {@link MimeType#equals(Object)} 的 {@link Collection#contains(Object)} 不同，
	 * 此方法只检查类型和子类型，忽略参数部分。
	 * @param mimeTypes 要检查的 MIME 类型列表
	 * @return 列表中是否包含给定的 MIME 类型
	 * @since 5.1.4
	 */
	public boolean isPresentIn(Collection<? extends MimeType> mimeTypes) {
		for (MimeType mimeType : mimeTypes) {
			if (mimeType.equalsTypeAndSubtype(this)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MimeType)) {
			return false;
		}
		MimeType otherType = (MimeType) other;
		return (this.type.equalsIgnoreCase(otherType.type) &&
				this.subtype.equalsIgnoreCase(otherType.subtype) &&
				parametersAreEqual(otherType));
	}

	/**
	 * 判断此 {@code MimeType} 与另一个 {@code MimeType} 的参数是否相等，
	 * 对 {@link Charset} 进行不区分大小写的比较。
	 * @since 4.2
	 */
	private boolean parametersAreEqual(MimeType other) {
		if (this.parameters.size() != other.parameters.size()) {
			return false;
		}

		for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
			String key = entry.getKey();
			if (!other.parameters.containsKey(key)) {
				return false;
			}
			if (PARAM_CHARSET.equals(key)) {
				if (!ObjectUtils.nullSafeEquals(getCharset(), other.getCharset())) {
					return false;
				}
			}
			else if (!ObjectUtils.nullSafeEquals(entry.getValue(), other.parameters.get(key))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = this.type.hashCode();
		result = 31 * result + this.subtype.hashCode();
		result = 31 * result + this.parameters.hashCode();
		return result;
	}

	@Override
	public String toString() {
		String value = this.toStringValue;
		if (value == null) {
			StringBuilder builder = new StringBuilder();
			appendTo(builder);
			value = builder.toString();
			this.toStringValue = value;
		}
		return value;
	}

	protected void appendTo(StringBuilder builder) {
		builder.append(this.type);
		builder.append('/');
		builder.append(this.subtype);
		appendTo(this.parameters, builder);
	}

	private void appendTo(Map<String, String> map, StringBuilder builder) {
		map.forEach((key, val) -> {
			builder.append(';');
			builder.append(key);
			builder.append('=');
			builder.append(val);
		});
	}

	/**
	 * 按字母顺序比较此 MIME 类型与另一个 MIME 类型。
	 * @param other 要比较的 MIME 类型
	 * @see MimeTypeUtils#sortBySpecificity(List)
	 */
	@Override
	public int compareTo(MimeType other) {
		int comp = getType().compareToIgnoreCase(other.getType());
		if (comp != 0) {
			return comp;
		}
		comp = getSubtype().compareToIgnoreCase(other.getSubtype());
		if (comp != 0) {
			return comp;
		}
		comp = getParameters().size() - other.getParameters().size();
		if (comp != 0) {
			return comp;
		}

		TreeSet<String> thisAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		thisAttributes.addAll(getParameters().keySet());
		TreeSet<String> otherAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		otherAttributes.addAll(other.getParameters().keySet());
		Iterator<String> thisAttributesIterator = thisAttributes.iterator();
		Iterator<String> otherAttributesIterator = otherAttributes.iterator();

		while (thisAttributesIterator.hasNext()) {
			String thisAttribute = thisAttributesIterator.next();
			String otherAttribute = otherAttributesIterator.next();
			comp = thisAttribute.compareToIgnoreCase(otherAttribute);
			if (comp != 0) {
				return comp;
			}
			if (PARAM_CHARSET.equals(thisAttribute)) {
				Charset thisCharset = getCharset();
				Charset otherCharset = other.getCharset();
				if (thisCharset != otherCharset) {
					if (thisCharset == null) {
						return -1;
					}
					if (otherCharset == null) {
						return 1;
					}
					comp = thisCharset.compareTo(otherCharset);
					if (comp != 0) {
						return comp;
					}
				}
			}
			else {
				String thisValue = getParameters().get(thisAttribute);
				String otherValue = other.getParameters().get(otherAttribute);
				if (otherValue == null) {
					otherValue = "";
				}
				comp = thisValue.compareTo(otherValue);
				if (comp != 0) {
					return comp;
				}
			}
		}

		return 0;
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖默认序列化，仅在反序列化后初始化状态。
		ois.defaultReadObject();

		// 初始化 transient 字段。
		String charsetName = getParameter(PARAM_CHARSET);
		if (charsetName != null) {
			this.resolvedCharset = Charset.forName(unquote(charsetName));
		}
	}


	/**
	 * 将给定字符串解析为 {@code MimeType} 对象，
	 * 此方法名遵循 'valueOf' 命名规范（由 {@link org.springframework.core.convert.ConversionService} 支持）。
	 * @see MimeTypeUtils#parseMimeType(String)
	 */
	public static MimeType valueOf(String value) {
		return MimeTypeUtils.parseMimeType(value);
	}

	private static Map<String, String> addCharsetParameter(Charset charset, Map<String, String> parameters) {
		Map<String, String> map = new LinkedHashMap<>(parameters);
		map.put(PARAM_CHARSET, charset.name());
		return map;
	}


	/**
	 * 用于按特异性排序 {@link MimeType MimeTypes} 的比较器。
	 *
	 * @param <T> 可由此比较器比较的 MIME 类型
	 */
	public static class SpecificityComparator<T extends MimeType> implements Comparator<T> {

		@Override
		public int compare(T mimeType1, T mimeType2) {
			if (mimeType1.isWildcardType() && !mimeType2.isWildcardType()) {  // */* < audio/*
				return 1;
			}
			else if (mimeType2.isWildcardType() && !mimeType1.isWildcardType()) {  // audio/* > */*
				return -1;
			}
			else if (!mimeType1.getType().equals(mimeType2.getType())) {  // audio/basic == text/html
				return 0;
			}
			else {  // mediaType1.getType().equals(mediaType2.getType())
				if (mimeType1.isWildcardSubtype() && !mimeType2.isWildcardSubtype()) {  // audio/* < audio/basic
					return 1;
				}
				else if (mimeType2.isWildcardSubtype() && !mimeType1.isWildcardSubtype()) {  // audio/basic > audio/*
					return -1;
				}
				else if (!mimeType1.getSubtype().equals(mimeType2.getSubtype())) {  // audio/basic == audio/wave
					return 0;
				}
				else {  // mediaType2.getSubtype().equals(mediaType2.getSubtype())
					return compareParameters(mimeType1, mimeType2);
				}
			}
		}

		protected int compareParameters(T mimeType1, T mimeType2) {
			int paramsSize1 = mimeType1.getParameters().size();
			int paramsSize2 = mimeType2.getParameters().size();
			return Integer.compare(paramsSize2, paramsSize1);  // audio/basic;level=1 < audio/basic
		}
	}

}
