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

package org.springframework.mail.javamail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于填充 {@link javax.mail.internet.MimeMessage} 的辅助类。
 *
 * <p>镜像了 {@link org.springframework.mail.SimpleMailMessage} 的简单 setter 方法，
 * 直接将值应用到底层 MimeMessage。允许为整个消息定义字符编码，
 * 该编码将被本辅助类的所有方法自动应用。
 *
 * <p>支持 HTML 文本内容、内联元素（如图片）以及典型的邮件附件。
 * 还支持伴随邮件地址的个人名称。请注意，
 * 高级设置仍然可以直接应用到底层 MimeMessage 对象！
 *
 * <p>通常用于 {@link MimeMessagePreparator} 实现或
 * {@link JavaMailSender} 客户端代码：只需将其实例化为 MimeMessage 包装器，
 * 在包装器上调用 setter，然后使用底层 MimeMessage 发送邮件。
 * 也可在 {@link JavaMailSenderImpl} 内部使用。
 *
 * <p>带有内联图片和 PDF 附件的 HTML 邮件示例代码：
 *
 * <pre class="code">
 * mailSender.send(new MimeMessagePreparator() {
 *   public void prepare(MimeMessage mimeMessage) throws MessagingException {
 *     MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
 *     message.setFrom("me@mail.com");
 *     message.setTo("you@mail.com");
 *     message.setSubject("my subject");
 *     message.setText("my text &lt;img src='cid:myLogo'&gt;", true);
 *     message.addInline("myLogo", new ClassPathResource("img/mylogo.gif"));
 *     message.addAttachment("myDocument.pdf", new ClassPathResource("doc/myDocument.pdf"));
 *   }
 * });</pre>
 *
 * 考虑使用 {@link MimeMailMessage}（它实现了通用的
 * {@link org.springframework.mail.MailMessage} 接口，就像
 * {@link org.springframework.mail.SimpleMailMessage} 一样）在此辅助类之上，
 * 以便消息填充代码可以通过通用接口与简单消息或 MIME 消息进行交互。
 *
 * <p><b>关于多部分邮件的警告：</b>仅包含 HTML 文本但没有内联元素或附件的简单 MIME 消息
 * 可以在几乎所有支持 HTML 渲染的电子邮件客户端上正常工作。然而，
 * 内联元素和附件仍然是电子邮件客户端之间的主要兼容性问题：
 * 几乎不可能让内联元素和附件在 Microsoft Outlook、Lotus Notes 和 Mac Mail 上都能正常工作。
 * 请考虑根据您的需求选择特定的多部分模式：MULTIPART_MODE 常量的 javadoc
 * 包含更详细的信息。
 *
 * @author Juergen Hoeller
 * @since 19.01.2004
 * @see #setText(String, boolean)
 * @see #setText(String, String)
 * @see #addInline(String, org.springframework.core.io.Resource)
 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
 * @see #MULTIPART_MODE_MIXED_RELATED
 * @see #MULTIPART_MODE_RELATED
 * @see #getMimeMessage()
 * @see JavaMailSender
 */
public class MimeMessageHelper {

	/**
	 * 表示非多部分消息的常量。
	 */
	public static final int MULTIPART_MODE_NO = 0;

	/**
	 * 表示具有单个根多部分元素（类型为 "mixed"）的多部分消息的常量。
	 * 文本、内联元素和附件都将被添加到该根元素中。
	 * <p>这是 Spring 1.0 的默认行为。已知在 Outlook 上可以正常工作。
	 * 但是，其他邮件客户端往往会将内联元素误解为附件，和/或也会以内联方式显示附件。
	 */
	public static final int MULTIPART_MODE_MIXED = 1;

	/**
	 * 表示具有单个根多部分元素（类型为 "related"）的多部分消息的常量。
	 * 文本、内联元素和附件都将被添加到该根元素中。
	 * <p>这是从 Spring 1.1 到 1.2 正式版的默认行为。
	 * 这是"Microsoft 多部分模式"，即 Outlook 原生发送的方式。
	 * 已知在 Outlook、Outlook Express、Yahoo Mail 上可以正常工作，
	 * 在很大程度上在 Mac Mail 上也可以正常工作（内联元素会额外列出一个附件，
	 * 尽管内联元素也会以内联方式显示）。
	 * 在 Lotus Notes 上无法正常工作（附件不会在那里显示）。
	 */
	public static final int MULTIPART_MODE_RELATED = 2;

	/**
	 * 表示具有根多部分元素 "mixed" 加上嵌套多部分元素（类型为 "related"）的多部分消息的常量。
	 * 文本和内联元素将被添加到嵌套的 "related" 元素中，
	 * 而附件将被添加到 "mixed" 根元素中。
	 * <p>这是自 Spring 1.2.1 以来的默认值。根据 MIME 规范，这可以说是
	 * 最正确的 MIME 结构：已知在 Outlook、Outlook Express、Yahoo Mail 和
	 * Lotus Notes 上可以正常工作。在 Mac Mail 上无法正常工作。
	 * 如果您针对 Mac Mail 或在 Outlook 上遇到特定邮件的问题，
	 * 请考虑改用 MULTIPART_MODE_RELATED。
	 */
	public static final int MULTIPART_MODE_MIXED_RELATED = 3;


	private static final String MULTIPART_SUBTYPE_MIXED = "mixed";

	private static final String MULTIPART_SUBTYPE_RELATED = "related";

	private static final String MULTIPART_SUBTYPE_ALTERNATIVE = "alternative";

	private static final String CONTENT_TYPE_ALTERNATIVE = "text/alternative";

	private static final String CONTENT_TYPE_HTML = "text/html";

	private static final String CONTENT_TYPE_CHARSET_SUFFIX = ";charset=";

	private static final String HEADER_PRIORITY = "X-Priority";


	private final MimeMessage mimeMessage;

	@Nullable
	private MimeMultipart rootMimeMultipart;

	@Nullable
	private MimeMultipart mimeMultipart;

	@Nullable
	private final String encoding;

	private FileTypeMap fileTypeMap;

	private boolean encodeFilenames = false;

	private boolean validateAddresses = false;


	/**
	 * 为给定的 MimeMessage 创建一个新的 MimeMessageHelper，
	 * 假设为简单文本消息（无多部分内容，
	 * 即无替代文本且无内联元素或附件）。
	 * <p>消息的字符编码将从传入的 MimeMessage 对象中获取（如果其中包含的话）。
	 * 否则，将使用 JavaMail 的默认编码。
	 * @param mimeMessage 要处理的 mime 消息
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, boolean)
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage) {
		this(mimeMessage, null);
	}

	/**
	 * 为给定的 MimeMessage 创建一个新的 MimeMessageHelper，
	 * 假设为简单文本消息（无多部分内容，
	 * 即无替代文本且无内联元素或附件）。
	 * @param mimeMessage 要处理的 mime 消息
	 * @param encoding 消息使用的字符编码
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, boolean)
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, @Nullable String encoding) {
		this.mimeMessage = mimeMessage;
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}

	/**
	 * 为给定的 MimeMessage 创建一个新的 MimeMessageHelper，
	 * 如果请求的话，在多部分模式下（支持替代文本、内联元素和附件）。
	 * <p>考虑使用 MimeMessageHelper 构造函数，
	 * 该函数接受一个 multipartMode 参数来选择特定的多部分模式，
	 * 而不是 MULTIPART_MODE_MIXED_RELATED。
	 * <p>消息的字符编码将从传入的 MimeMessage 对象中获取（如果其中包含的话）。
	 * 否则，将使用 JavaMail 的默认编码。
	 * @param mimeMessage 要处理的 mime 消息
	 * @param multipart 是否创建支持替代文本、内联元素和附件的多部分消息
	 * （对应于 MULTIPART_MODE_MIXED_RELATED）
	 * @throws MessagingException 如果多部分创建失败
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, int)
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart) throws MessagingException {
		this(mimeMessage, multipart, null);
	}

	/**
	 * 为给定的 MimeMessage 创建一个新的 MimeMessageHelper，
	 * 如果请求的话，在多部分模式下（支持替代文本、内联元素和附件）。
	 * <p>考虑使用 MimeMessageHelper 构造函数，
	 * 该函数接受一个 multipartMode 参数来选择特定的多部分模式，
	 * 而不是 MULTIPART_MODE_MIXED_RELATED。
	 * @param mimeMessage 要处理的 mime 消息
	 * @param multipart 是否创建支持替代文本、内联元素和附件的多部分消息
	 * （对应于 MULTIPART_MODE_MIXED_RELATED）
	 * @param encoding 消息使用的字符编码
	 * @throws MessagingException 如果多部分创建失败
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, int, String)
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart, @Nullable String encoding)
			throws MessagingException {

		this(mimeMessage, (multipart ? MULTIPART_MODE_MIXED_RELATED : MULTIPART_MODE_NO), encoding);
	}

	/**
	 * 为给定的 MimeMessage 创建一个新的 MimeMessageHelper，
	 * 如果请求的话，在多部分模式下（支持替代文本、内联元素和附件）。
	 * <p>消息的字符编码将从传入的 MimeMessage 对象中获取（如果其中包含的话）。
	 * 否则，将使用 JavaMail 的默认编码。
	 * @param mimeMessage 要处理的 mime 消息
	 * @param multipartMode 创建哪种类型的多部分消息
	 * （MIXED、RELATED、MIXED_RELATED 或 NO）
	 * @throws MessagingException 如果多部分创建失败
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		this(mimeMessage, multipartMode, null);
	}

	/**
	 * 为给定的 MimeMessage 创建一个新的 MimeMessageHelper，
	 * 如果请求的话，在多部分模式下（支持替代文本、内联元素和附件）。
	 * @param mimeMessage 要处理的 mime 消息
	 * @param multipartMode 创建哪种类型的多部分消息
	 * （MIXED、RELATED、MIXED_RELATED 或 NO）
	 * @param encoding 消息使用的字符编码
	 * @throws MessagingException 如果多部分创建失败
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode, @Nullable String encoding)
			throws MessagingException {

		this.mimeMessage = mimeMessage;
		createMimeMultiparts(mimeMessage, multipartMode);
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}


	/**
	 * 返回底层的 MimeMessage 对象。
	 */
	public final MimeMessage getMimeMessage() {
		return this.mimeMessage;
	}


	/**
	 * 确定要使用的 MimeMultipart 对象，这些对象将用于
	 * 一方面存储附件，另一方面存储文本和内联元素。
	 * <p>文本和内联元素可以存储在根元素本身中
	 * （MULTIPART_MODE_MIXED、MULTIPART_MODE_RELATED），
	 * 也可以存储在嵌套元素中而不是直接存储在根元素中
	 * （MULTIPART_MODE_MIXED_RELATED）。
	 * <p>默认情况下，根 MimeMultipart 元素的类型为 "mixed"
	 * （MULTIPART_MODE_MIXED）或 "related"（MULTIPART_MODE_RELATED）。
	 * 主多部分元素将作为类型为 "related" 的嵌套元素添加
	 * （MULTIPART_MODE_MIXED_RELATED），或者与根元素本身相同
	 * （MULTIPART_MODE_MIXED、MULTIPART_MODE_RELATED）。
	 * @param mimeMessage 要添加根 MimeMultipart 对象的 MimeMessage 对象
	 * @param multipartMode 多部分模式，如传递给构造函数的那样
	 * （MIXED、RELATED、MIXED_RELATED 或 NO）
	 * @throws MessagingException 如果多部分创建失败
	 * @see #setMimeMultiparts
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 */
	protected void createMimeMultiparts(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		switch (multipartMode) {
			case MULTIPART_MODE_NO:
				setMimeMultiparts(null, null);
				break;
			case MULTIPART_MODE_MIXED:
				MimeMultipart mixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(mixedMultipart);
				setMimeMultiparts(mixedMultipart, mixedMultipart);
				break;
			case MULTIPART_MODE_RELATED:
				MimeMultipart relatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				mimeMessage.setContent(relatedMultipart);
				setMimeMultiparts(relatedMultipart, relatedMultipart);
				break;
			case MULTIPART_MODE_MIXED_RELATED:
				MimeMultipart rootMixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(rootMixedMultipart);
				MimeMultipart nestedRelatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				MimeBodyPart relatedBodyPart = new MimeBodyPart();
				relatedBodyPart.setContent(nestedRelatedMultipart);
				rootMixedMultipart.addBodyPart(relatedBodyPart);
				setMimeMultiparts(rootMixedMultipart, nestedRelatedMultipart);
				break;
			default:
				throw new IllegalArgumentException("Only multipart modes MIXED_RELATED, RELATED and NO supported");
		}
	}

	/**
	 * 设置此 MimeMessageHelper 使用的给定 MimeMultipart 对象。
	 * @param root 根 MimeMultipart 对象，附件将被添加到该对象中；
	 * 或 {@code null} 表示完全没有多部分
	 * @param main 主 MimeMultipart 对象，文本和内联元素将被添加到该对象中
	 * （可以与根多部分对象相同，也可以是嵌套在根多部分元素下面的元素）
	 */
	protected final void setMimeMultiparts(@Nullable MimeMultipart root, @Nullable MimeMultipart main) {
		this.rootMimeMultipart = root;
		this.mimeMultipart = main;
	}

	/**
	 * 返回此助手是否处于多部分模式，
	 * 即是否持有个多部分消息。
	 * @see #MimeMessageHelper(MimeMessage, boolean)
	 */
	public final boolean isMultipart() {
		return (this.rootMimeMultipart != null);
	}

	/**
	 * 返回根 MIME "multipart/mixed" 对象（如果有的话）。
	 * 可用于手动添加附件。
	 * <p>在多部分邮件的情况下，
	 * 这将是 MimeMessage 的直接内容。
	 * @throws IllegalStateException 如果此助手不处于多部分模式
	 * @see #isMultipart
	 * @see #getMimeMessage
	 * @see javax.mail.internet.MimeMultipart#addBodyPart
	 */
	public final MimeMultipart getRootMimeMultipart() throws IllegalStateException {
		if (this.rootMimeMultipart == null) {
			throw new IllegalStateException("Not in multipart mode - " +
					"create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
					"if you need to set alternative texts or add inline elements or attachments.");
		}
		return this.rootMimeMultipart;
	}

	/**
	 * 返回底层的 MIME "multipart/related" 对象（如果有的话）。
	 * 可用于手动添加正文部分、内联元素等。
	 * <p>在多部分邮件的情况下，
	 * 这将嵌套在根 MimeMultipart 内部。
	 * @throws IllegalStateException 如果此助手不处于多部分模式
	 * @see #isMultipart
	 * @see #getRootMimeMultipart
	 * @see javax.mail.internet.MimeMultipart#addBodyPart
	 */
	public final MimeMultipart getMimeMultipart() throws IllegalStateException {
		if (this.mimeMultipart == null) {
			throw new IllegalStateException("Not in multipart mode - " +
					"create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
					"if you need to set alternative texts or add inline elements or attachments.");
		}
		return this.mimeMultipart;
	}


	/**
	 * 确定给定 MimeMessage 的默认编码。
	 * @param mimeMessage 传入的 MimeMessage
	 * @return 与 MimeMessage 关联的默认编码，
	 * 如果未找到则返回 {@code null}
	 */
	@Nullable
	protected String getDefaultEncoding(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			return ((SmartMimeMessage) mimeMessage).getDefaultEncoding();
		}
		return null;
	}

	/**
	 * 返回此消息使用的特定字符编码（如果有的话）。
	 */
	@Nullable
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * 确定给定 MimeMessage 的默认 Java Activation FileTypeMap。
	 * @param mimeMessage 传入的 MimeMessage
	 * @return 与 MimeMessage 关联的默认 FileTypeMap，
	 * 如果消息未找到则返回默认的 ConfigurableMimeFileTypeMap
	 * @see ConfigurableMimeFileTypeMap
	 */
	protected FileTypeMap getDefaultFileTypeMap(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			FileTypeMap fileTypeMap = ((SmartMimeMessage) mimeMessage).getDefaultFileTypeMap();
			if (fileTypeMap != null) {
				return fileTypeMap;
			}
		}
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		return fileTypeMap;
	}

	/**
	 * 设置用于确定添加到消息中的内联内容和附件的内容类型的
	 * Java Activation Framework {@code FileTypeMap}。
	 * <p>默认值是底层 MimeMessage 携带的 {@code FileTypeMap}（如果有的话），
	 * 否则是 Activation Framework 的默认 {@code FileTypeMap} 实例。
	 * @see #addInline
	 * @see #addAttachment
	 * @see #getDefaultFileTypeMap(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultFileTypeMap
	 * @see javax.activation.FileTypeMap#getDefaultFileTypeMap
	 * @see ConfigurableMimeFileTypeMap
	 */
	public void setFileTypeMap(@Nullable FileTypeMap fileTypeMap) {
		this.fileTypeMap = (fileTypeMap != null ? fileTypeMap : getDefaultFileTypeMap(getMimeMessage()));
	}

	/**
	 * 返回此 MimeMessageHelper 使用的 {@code FileTypeMap}。
	 * @see #setFileTypeMap
	 */
	public FileTypeMap getFileTypeMap() {
		return this.fileTypeMap;
	}


	/**
	 * 设置是否对此助手的 {@code #addAttachment} 方法传入的附件文件名进行编码。
	 * <p>默认值为 {@code false} 以实现标准 MIME 行为；将此值设为
	 * {@code true} 可与较旧的电子邮件客户端兼容。另外，请查看
	 * JavaMail 的 {@code mail.mime.encodefilename} 系统属性。
	 * <p><b>注意：</b>默认值在 5.3 中更改为 {@code false}，
	 * 以支持 JavaMail 的标准 {@code mail.mime.encodefilename} 系统属性。
	 * @since 5.2.9
	 * @see #addAttachment(String, DataSource)
	 * @see MimeBodyPart#setFileName(String)
	 */
	public void setEncodeFilenames(boolean encodeFilenames) {
		this.encodeFilenames = encodeFilenames;
	}

	/**
	 * 返回是否对此助手的 {@code #addAttachment} 方法传入的附件文件名进行编码。
	 * @since 5.2.9
	 * @see #setEncodeFilenames
	 */
	public boolean isEncodeFilenames() {
		return this.encodeFilenames;
	}

	/**
	 * 设置是否验证传递给此助手的所有地址。
	 * <p>默认值为 {@code false}。
	 * @see #validateAddress
	 */
	public void setValidateAddresses(boolean validateAddresses) {
		this.validateAddresses = validateAddresses;
	}

	/**
	 * 返回此助手是否将验证传递给它的所有地址。
	 * @see #setValidateAddresses
	 */
	public boolean isValidateAddresses() {
		return this.validateAddresses;
	}

	/**
	 * 验证给定的邮件地址。
	 * 由 MimeMessageHelper 的所有地址设置器和添加器调用。
	 * <p>默认实现调用 {@link InternetAddress#validate()}，
	 * 前提是助手实例已激活地址验证。
	 * @param address 要验证的地址
	 * @throws AddressException 如果验证失败
	 * @see #isValidateAddresses()
	 * @see javax.mail.internet.InternetAddress#validate()
	 */
	protected void validateAddress(InternetAddress address) throws AddressException {
		if (isValidateAddresses()) {
			address.validate();
		}
	}

	/**
	 * 验证所有给定的邮件地址。
	 * <p>默认实现仅为每个地址委托给 {@link #validateAddress}。
	 * @param addresses 要验证的地址
	 * @throws AddressException 如果验证失败
	 * @see #validateAddress(InternetAddress)
	 */
	protected void validateAddresses(InternetAddress[] addresses) throws AddressException {
		for (InternetAddress address : addresses) {
			validateAddress(address);
		}
	}


	public void setFrom(InternetAddress from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		validateAddress(from);
		this.mimeMessage.setFrom(from);
	}

	public void setFrom(String from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(parseAddress(from));
	}

	public void setFrom(String from, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(getEncoding() != null ?
			new InternetAddress(from, personal, getEncoding()) : new InternetAddress(from, personal));
	}

	public void setReplyTo(InternetAddress replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		validateAddress(replyTo);
		this.mimeMessage.setReplyTo(new InternetAddress[] {replyTo});
	}

	public void setReplyTo(String replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		setReplyTo(parseAddress(replyTo));
	}

	public void setReplyTo(String replyTo, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		InternetAddress replyToAddress = (getEncoding() != null) ?
				new InternetAddress(replyTo, personal, getEncoding()) : new InternetAddress(replyTo, personal);
		setReplyTo(replyToAddress);
	}


	public void setTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.setRecipient(Message.RecipientType.TO, to);
	}

	public void setTo(InternetAddress[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		validateAddresses(to);
		this.mimeMessage.setRecipients(Message.RecipientType.TO, to);
	}

	public void setTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		setTo(parseAddress(to));
	}

	public void setTo(String[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		InternetAddress[] addresses = new InternetAddress[to.length];
		for (int i = 0; i < to.length; i++) {
			addresses[i] = parseAddress(to[i]);
		}
		setTo(addresses);
	}

	public void addTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.addRecipient(Message.RecipientType.TO, to);
	}

	public void addTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		addTo(parseAddress(to));
	}

	public void addTo(String to, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(to, "To address must not be null");
		addTo(getEncoding() != null ?
			new InternetAddress(to, personal, getEncoding()) :
			new InternetAddress(to, personal));
	}


	public void setCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.setRecipient(Message.RecipientType.CC, cc);
	}

	public void setCc(InternetAddress[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		validateAddresses(cc);
		this.mimeMessage.setRecipients(Message.RecipientType.CC, cc);
	}

	public void setCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		setCc(parseAddress(cc));
	}

	public void setCc(String[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[cc.length];
		for (int i = 0; i < cc.length; i++) {
			addresses[i] = parseAddress(cc[i]);
		}
		setCc(addresses);
	}

	public void addCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.addRecipient(Message.RecipientType.CC, cc);
	}

	public void addCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(parseAddress(cc));
	}

	public void addCc(String cc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(getEncoding() != null ?
			new InternetAddress(cc, personal, getEncoding()) :
			new InternetAddress(cc, personal));
	}


	public void setBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.setRecipient(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(InternetAddress[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		validateAddresses(bcc);
		this.mimeMessage.setRecipients(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		setBcc(parseAddress(bcc));
	}

	public void setBcc(String[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[bcc.length];
		for (int i = 0; i < bcc.length; i++) {
			addresses[i] = parseAddress(bcc[i]);
		}
		setBcc(addresses);
	}

	public void addBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.addRecipient(Message.RecipientType.BCC, bcc);
	}

	public void addBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(parseAddress(bcc));
	}

	public void addBcc(String bcc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(getEncoding() != null ?
			new InternetAddress(bcc, personal, getEncoding()) :
			new InternetAddress(bcc, personal));
	}

	private InternetAddress parseAddress(String address) throws MessagingException {
		InternetAddress[] parsed = InternetAddress.parse(address);
		if (parsed.length != 1) {
			throw new AddressException("Illegal address", address);
		}
		InternetAddress raw = parsed[0];
		try {
			return (getEncoding() != null ?
					new InternetAddress(raw.getAddress(), raw.getPersonal(), getEncoding()) : raw);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessagingException("Failed to parse embedded personal name to correct encoding", ex);
		}
	}


	/**
	 * 设置消息的优先级（"X-Priority" 头）。
	 * @param priority 优先级值；
	 * 通常在 1（最高）和 5（最低）之间
	 * @throws MessagingException 如果发生错误
	 */
	public void setPriority(int priority) throws MessagingException {
		this.mimeMessage.setHeader(HEADER_PRIORITY, Integer.toString(priority));
	}

	/**
	 * 设置消息的发送日期。
	 * @param sentDate 要设置的日期（不能为 {@code null}）
	 * @throws MessagingException 如果发生错误
	 */
	public void setSentDate(Date sentDate) throws MessagingException {
		Assert.notNull(sentDate, "Sent date must not be null");
		this.mimeMessage.setSentDate(sentDate);
	}

	/**
	 * 使用正确的编码设置消息的主题。
	 * @param subject 主题文本
	 * @throws MessagingException 如果发生错误
	 */
	public void setSubject(String subject) throws MessagingException {
		Assert.notNull(subject, "Subject must not be null");
		if (getEncoding() != null) {
			this.mimeMessage.setSubject(subject, getEncoding());
		}
		else {
			this.mimeMessage.setSubject(subject);
		}
	}


	/**
	 * 将给定的文本直接设置为非多部分模式下的内容，
	 * 或设置为多部分模式下的默认正文部分。
	 * 始终应用默认内容类型 "text/plain"。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@link #addInline}；
	 * 否则，邮件阅读器可能无法正确解析内联引用。
	 * @param text 消息的文本
	 * @throws MessagingException 如果发生错误
	 */
	public void setText(String text) throws MessagingException {
		setText(text, false);
	}

	/**
	 * 将给定的文本直接设置为非多部分模式下的内容，
	 * 或设置为多部分模式下的默认正文部分。
	 * "html" 标志决定要应用的内容类型。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@link #addInline}；
	 * 否则，邮件阅读器可能无法正确解析内联引用。
	 * @param text 消息的文本
	 * @param html 是否为 HTML 邮件应用内容类型 "text/html"，
	 * 否则使用默认内容类型（"text/plain"）
	 * @throws MessagingException 如果发生错误
	 */
	public void setText(String text, boolean html) throws MessagingException {
		Assert.notNull(text, "Text must not be null");
		MimePart partToUse;
		if (isMultipart()) {
			partToUse = getMainPart();
		}
		else {
			partToUse = this.mimeMessage;
		}
		if (html) {
			setHtmlTextToMimePart(partToUse, text);
		}
		else {
			setPlainTextToMimePart(partToUse, text);
		}
	}

	/**
	 * 将给定的纯文本和 HTML 文本设置为替代项，
	 * 向电子邮件客户端提供两个选项。需要多部分模式。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@link #addInline}；
	 * 否则，邮件阅读器可能无法正确解析内联引用。
	 * @param plainText 消息的纯文本
	 * @param htmlText 消息的 HTML 文本
	 * @throws MessagingException 如果发生错误
	 */
	public void setText(String plainText, String htmlText) throws MessagingException {
		Assert.notNull(plainText, "Plain text must not be null");
		Assert.notNull(htmlText, "HTML text must not be null");

		MimeMultipart messageBody = new MimeMultipart(MULTIPART_SUBTYPE_ALTERNATIVE);
		getMainPart().setContent(messageBody, CONTENT_TYPE_ALTERNATIVE);

		// 创建消息的纯文本部分。
		MimeBodyPart plainTextPart = new MimeBodyPart();
		setPlainTextToMimePart(plainTextPart, plainText);
		messageBody.addBodyPart(plainTextPart);

		// 创建消息的 HTML 文本部分。
		MimeBodyPart htmlTextPart = new MimeBodyPart();
		setHtmlTextToMimePart(htmlTextPart, htmlText);
		messageBody.addBodyPart(htmlTextPart);
	}

	private MimeBodyPart getMainPart() throws MessagingException {
		MimeMultipart mimeMultipart = getMimeMultipart();
		MimeBodyPart bodyPart = null;
		for (int i = 0; i < mimeMultipart.getCount(); i++) {
			BodyPart bp = mimeMultipart.getBodyPart(i);
			if (bp.getFileName() == null) {
				bodyPart = (MimeBodyPart) bp;
			}
		}
		if (bodyPart == null) {
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeMultipart.addBodyPart(mimeBodyPart);
			bodyPart = mimeBodyPart;
		}
		return bodyPart;
	}

	private void setPlainTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setText(text, getEncoding());
		}
		else {
			mimePart.setText(text);
		}
	}

	private void setHtmlTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setContent(text, CONTENT_TYPE_HTML + CONTENT_TYPE_CHARSET_SUFFIX + getEncoding());
		}
		else {
			mimePart.setContent(text, CONTENT_TYPE_HTML);
		}
	}


	/**
	 * 向 MimeMessage 添加内联元素，内容取自
	 * {@code javax.activation.DataSource}。
	 * <p>注意，DataSource 实现返回的 InputStream
	 * 需要是<i>每次调用时都是全新的</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * <p><b>注意：</b>请在 {@link #setText} <i>之后</i>调用 {@code addInline}；
	 * 否则，邮件阅读器可能无法正确解析内联引用。
	 * @param contentId 要使用的内容 ID。将作为 "Content-ID" 头
	 * 出现在正文部分中，用尖括号包围：例如 "myId" &rarr; "&lt;myId&gt;"。
	 * 可以通过 src="cid:myId" 表达式在 HTML 源码中引用。
	 * @param dataSource 要从中获取内容的 {@code javax.activation.DataSource}，
	 * 用于确定 InputStream 和内容类型
	 * @throws MessagingException 如果发生错误
	 * @see #addInline(String, java.io.File)
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 */
	public void addInline(String contentId, DataSource dataSource) throws MessagingException {
		Assert.notNull(contentId, "Content ID must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setDisposition(MimeBodyPart.INLINE);
		mimeBodyPart.setContentID("<" + contentId + ">");
		mimeBodyPart.setDataHandler(new DataHandler(dataSource));
		getMimeMultipart().addBodyPart(mimeBodyPart);
	}

	/**
	 * 向 MimeMessage 添加内联元素，内容取自
	 * {@code java.io.File}。
	 * <p>内容类型将由给定内容文件的名称决定。
	 * 请不要将此方法用于文件名任意的临时文件
	 * （可能是以 ".tmp" 等结尾的文件名）！
	 * <p><b>注意：</b>请在 {@link #setText} <i>之后</i>调用 {@code addInline}；
	 * 否则，邮件阅读器可能无法正确解析内联引用。
	 * @param contentId 要使用的内容 ID。将作为 "Content-ID" 头
	 * 出现在正文部分中，用尖括号包围：例如 "myId" &rarr; "&lt;myId&gt;"。
	 * 可以通过 src="cid:myId" 表达式在 HTML 源码中引用。
	 * @param file 要从中获取内容的 File 资源
	 * @throws MessagingException 如果发生错误
	 * @see #setText
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addInline(contentId, dataSource);
	}

	/**
	 * 向 MimeMessage 添加内联元素，内容取自
	 * {@code org.springframework.core.io.Resource}。
	 * <p>内容类型将由给定内容文件的名称决定。
	 * 请不要将此方法用于文件名任意的临时文件
	 * （可能是以 ".tmp" 等结尾的文件名）！
	 * <p>注意，Resource 实现返回的 InputStream
	 * 需要是<i>每次调用时都是全新的</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * <p><b>注意：</b>请在 {@link #setText} <i>之后</i>调用 {@code addInline}；
	 * 否则，邮件阅读器可能无法正确解析内联引用。
	 * @param contentId 要使用的内容 ID。将作为 "Content-ID" 头
	 * 出现在正文部分中，用尖括号包围：例如 "myId" &rarr; "&lt;myId&gt;"。
	 * 可以通过 src="cid:myId" 表达式在 HTML 源码中引用。
	 * @param resource 要从中获取内容的资源
	 * @throws MessagingException 如果发生错误
	 * @see #setText
	 * @see #addInline(String, java.io.File)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, Resource resource) throws MessagingException {
		Assert.notNull(resource, "Resource must not be null");
		String contentType = getFileTypeMap().getContentType(resource.getFilename());
		addInline(contentId, resource, contentType);
	}

	/**
	 * 向 MimeMessage 添加内联元素，内容取自
	 * {@code org.springframework.core.InputStreamResource}，
	 * 并显式指定内容类型。
	 * <p>你可以通过 Java Activation Framework 的 FileTypeMap
	 * 来确定任何给定文件名的内容类型，例如本助手持有的 FileTypeMap。
	 * <p>注意，InputStreamSource 实现返回的 InputStream
	 * 需要是<i>每次调用时都是全新的</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@code addInline}；
	 * 否则，邮件阅读器可能无法正确解析内联引用。
	 * @param contentId 要使用的内容 ID。将作为 "Content-ID" 头
	 * 出现在正文部分中，用尖括号包围：例如 "myId" &rarr; "&lt;myId&gt;"。
	 * 可以通过 src="cid:myId" 表达式在 HTML 源码中引用。
	 * @param inputStreamSource 要从中获取内容的资源
	 * @param contentType 用于该元素的内容类型
	 * @throws MessagingException 如果发生错误
	 * @see #setText
	 * @see #getFileTypeMap
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, InputStreamSource inputStreamSource, String contentType)
			throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, "inline");
		addInline(contentId, dataSource);
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自
	 * {@code javax.activation.DataSource}。
	 * <p>注意，DataSource 实现返回的 InputStream
	 * 需要是<i>每次调用时都是全新的</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * @param attachmentFilename 附件的名称，将显示在邮件中
	 * （内容类型将由此决定）
	 * @param dataSource 要从中获取内容的 {@code javax.activation.DataSource}，
	 * 用于确定 InputStream 和内容类型
	 * @throws MessagingException 如果发生错误
	 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
	 * @see #addAttachment(String, java.io.File)
	 */
	public void addAttachment(String attachmentFilename, DataSource dataSource) throws MessagingException {
		Assert.notNull(attachmentFilename, "Attachment filename must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		try {
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
			mimeBodyPart.setFileName(isEncodeFilenames() ?
					MimeUtility.encodeText(attachmentFilename) : attachmentFilename);
			mimeBodyPart.setDataHandler(new DataHandler(dataSource));
			getRootMimeMultipart().addBodyPart(mimeBodyPart);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessagingException("Failed to encode attachment filename", ex);
		}
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自
	 * {@code java.io.File}。
	 * <p>内容类型将由给定内容文件的名称决定。
	 * 请不要将此方法用于文件名任意的临时文件
	 * （可能是以 ".tmp" 等结尾的文件名）！
	 * @param attachmentFilename 附件的名称，将显示在邮件中
	 * @param file 要从中获取内容的 File 资源
	 * @throws MessagingException 如果发生错误
	 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 */
	public void addAttachment(String attachmentFilename, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自
	 * {@code org.springframework.core.io.InputStreamResource}。
	 * <p>内容类型将由给定附件的文件名决定。
	 * 因此，任何内容源都可以，包括文件名任意的临时文件。
	 * <p>注意，InputStreamSource 实现返回的 InputStream
	 * 需要是<i>每次调用时都是全新的</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * @param attachmentFilename 附件的名称，将显示在邮件中
	 * @param inputStreamSource 要从中获取内容的资源
	 * （Spring 的所有 Resource 实现都可以传入此处）
	 * @throws MessagingException 如果发生错误
	 * @see #addAttachment(String, java.io.File)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 * @see org.springframework.core.io.Resource
	 */
	public void addAttachment(String attachmentFilename, InputStreamSource inputStreamSource)
			throws MessagingException {

		String contentType = getFileTypeMap().getContentType(attachmentFilename);
		addAttachment(attachmentFilename, inputStreamSource, contentType);
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自
	 * {@code org.springframework.core.io.InputStreamResource}。
	 * <p>注意，InputStreamSource 实现返回的 InputStream
	 * 需要是<i>每次调用时都是全新的</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * @param attachmentFilename 附件的名称，将显示在邮件中
	 * @param inputStreamSource 要从中获取内容的资源
	 * （Spring 的所有 Resource 实现都可以传入此处）
	 * @param contentType 用于该元素的内容类型
	 * @throws MessagingException 如果发生错误
	 * @see #addAttachment(String, java.io.File)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 * @see org.springframework.core.io.Resource
	 */
	public void addAttachment(
			String attachmentFilename, InputStreamSource inputStreamSource, String contentType)
			throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, attachmentFilename);
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * 为给定的 InputStreamSource 创建一个 Activation Framework DataSource。
	 * @param inputStreamSource InputStreamSource（通常是 Spring Resource）
	 * @param contentType 内容类型
	 * @param name DataSource 的名称
	 * @return Activation Framework DataSource
	 */
	protected DataSource createDataSource(
		final InputStreamSource inputStreamSource, final String contentType, final String name) {

		return new DataSource() {
			@Override
			public InputStream getInputStream() throws IOException {
				return inputStreamSource.getInputStream();
			}
			@Override
			public OutputStream getOutputStream() {
				throw new UnsupportedOperationException("Read-only javax.activation.DataSource");
			}
			@Override
			public String getContentType() {
				return contentType;
			}
			@Override
			public String getName() {
				return name;
			}
		};
	}

}
