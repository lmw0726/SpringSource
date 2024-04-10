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

package org.springframework.web.servlet.view.document;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * PDF视图的抽象超类。应用程序特定的视图类将扩展此类。视图将保存在子类本身中，而不是模板中。
 *
 * <p>此视图实现使用Bruno Lowagie的<a href="https://www.lowagie.com/iText">iText</a> API。
 * 已知与原始iText 2.1.7及其分支<a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a>一起使用。
 * <b>我们强烈建议使用OpenPDF，因为它正在积极维护并修复了一个针对不受信任的PDF内容的重要漏洞。</b>
 *
 * <p>注意：Internet Explorer需要".pdf"扩展名，因为它并不总是尊重声明的内容类型。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @see AbstractPdfStamperView
 */
public abstract class AbstractPdfView extends AbstractView {

	/**
	 * 此构造函数设置适当的内容类型“application/pdf”。
	 * 请注意，IE对此并不关心，但我们对此无能为力。生成的文档应具有“.pdf”扩展名。
	 */
	public AbstractPdfView() {
		setContentType("application/pdf");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// IE解决方法：先写入字节数组。
		ByteArrayOutputStream baos = createTemporaryOutputStream();

		// 应用首选项并构建元数据。
		Document document = newDocument();
		PdfWriter writer = newWriter(document, baos);
		// 准备写入器
		prepareWriter(model, writer, request);
		// 构建PDF元数据
		buildPdfMetadata(model, document, request);

		// 构建PDF文档。
		document.open();
		buildPdfDocument(model, document, writer, request, response);
		document.close();

		// 刷新到HTTP响应。
		writeToResponse(response, baos);
	}

	/**
	 * 创建一个新文档以保存PDF内容。
	 * <p>默认情况下返回A4文档，但子类可以指定任何Document，可能通过在视图上定义的bean属性进行参数化。
	 * @return 新创建的iText Document实例
	 * @see com.lowagie.text.Document#Document(com.lowagie.text.Rectangle)
	 */
	protected Document newDocument() {
		return new Document(PageSize.A4);
	}

	/**
	 * 为给定的iText文档创建一个新的PdfWriter。
	 * @param document 要为其创建写入器的iText文档
	 * @param os 要写入的OutputStream
	 * @return 要使用的PdfWriter实例
	 * @throws DocumentException 创建写入器期间抛出DocumentException
	 */
	protected PdfWriter newWriter(Document document, OutputStream os) throws DocumentException {
		return PdfWriter.getInstance(document, os);
	}

	/**
	 * 准备给定的PdfWriter。在构建PDF文档之前调用，即在调用{@code Document.open()}之前。
	 * <p>例如，用于注册页面事件监听器。
	 * 默认实现设置此类的{@code getViewerPreferences()}方法返回的查看器首选项。
	 * @param model 如果必须从模型中填充元信息，则为模型
	 * @param writer 要准备的PdfWriter
	 * @param request 如果需要区域设置等，则为请求。不应查看属性。
	 * @throws DocumentException 准备写入器期间抛出的DocumentException
	 * @see com.lowagie.text.Document#open()
	 * @see com.lowagie.text.pdf.PdfWriter#setPageEvent
	 * @see com.lowagie.text.pdf.PdfWriter#setViewerPreferences
	 * @see #getViewerPreferences()
	 */
	protected void prepareWriter(Map<String, Object> model, PdfWriter writer, HttpServletRequest request)
			throws DocumentException {

		writer.setViewerPreferences(getViewerPreferences());
	}

	/**
	 * 返回PDF文件的查看器首选项。
	 * <p>默认情况下返回{@code AllowPrinting}和{@code PageLayoutSinglePage}，但可以被子类覆盖。
	 * 子类可以具有固定首选项，也可以从在视图上定义的bean属性中检索它们。
	 * @return 包含位信息的int，针对PdfWriter定义
	 * @see com.lowagie.text.pdf.PdfWriter#AllowPrinting
	 * @see com.lowagie.text.pdf.PdfWriter#PageLayoutSinglePage
	 */
	protected int getViewerPreferences() {
		return PdfWriter.ALLOW_PRINTING | PdfWriter.PageLayoutSinglePage;
	}

	/**
	 * 填充iText文档的元字段（作者、标题等）。
	 * <br>默认为空实现。子类可以覆盖此方法以添加元字段，例如标题、主题、作者、创建者、关键字等。
	 * 在为Document分配PdfWriter并在调用{@code document.open()}之后调用此方法。
	 * @param model 如果必须从模型中填充元信息，则为模型
	 * @param document 要填充的iText文档
	 * @param request 如果需要区域设置等，则为请求。不应查看属性。
	 * @see com.lowagie.text.Document#addTitle
	 * @see com.lowagie.text.Document#addSubject
	 * @see com.lowagie.text.Document#addKeywords
	 * @see com.lowagie.text.Document#addAuthor
	 * @see com.lowagie.text.Document#addCreator
	 * @see com.lowagie.text.Document#addProducer
	 * @see com.lowagie.text.Document#addCreationDate
	 * @see com.lowagie.text.Document#addHeader
	 */
	protected void buildPdfMetadata(Map<String, Object> model, Document document, HttpServletRequest request) {
	}

	/**
	 * 子类必须实现此方法以构建一个iText PDF文档，给定模型。在调用{@code Document.open()}和{@code Document.close()}之间调用。
	 * <p>请注意，传入的HTTP响应仅应用于设置cookie或其他HTTP标头。
	 * 构建的PDF文档本身将在此方法返回后自动写入响应。
	 * @param model 模型Map
	 * @param document 要添加元素的iText Document
	 * @param writer 要使用的PdfWriter
	 * @param request 如果需要区域设置等，则为请求。不应查看属性。
	 * @param response 如果需要设置cookie，则应用于设置cookie。不应写入它。
	 * @throws Exception 构建文档期间发生的任何异常
	 * @see com.lowagie.text.Document#open()
	 * @see com.lowagie.text.Document#close()
	 */
	protected abstract void buildPdfDocument(Map<String, Object> model, Document document, PdfWriter writer,
											 HttpServletRequest request, HttpServletResponse response) throws Exception;

}
