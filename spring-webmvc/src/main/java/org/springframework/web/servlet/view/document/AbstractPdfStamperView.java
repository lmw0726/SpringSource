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

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * PDF视图的抽象超类，用于在具有AcroForm的现有文档上操作。应用程序特定的视图类将扩展此类以将PDF表单与模型数据合并。
 *
 * <p>此视图实现使用Bruno Lowagie的
 * <a href="https://www.lowagie.com/iText">iText</a> API。
 * 已知与原始的iText 2.1.7以及其分支
 * <a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a>一起使用。
 * <b>我们强烈推荐OpenPDF，因为它正在积极维护并修复了一些关于不受信任的PDF内容的重要漏洞。</b>
 *
 * <p>感谢Bryant Larsen提出建议并创建原型！
 *
 * @author Juergen Hoeller
 * @since 2.5.4
 * @see AbstractPdfView
 */
public abstract class AbstractPdfStamperView extends AbstractUrlBasedView {

	public AbstractPdfStamperView(){
		setContentType("application/pdf");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// IE解决方案：首先写入字节数组。
		ByteArrayOutputStream baos = createTemporaryOutputStream();
		// 构建PDF读取器
		PdfReader reader = readPdfResource();
		// 获取PDF印章
		PdfStamper stamper = new PdfStamper(reader, baos);
		// 合并到PDF文档中
		mergePdfDocument(model, stamper, request, response);
		// 关闭PDF印章
		stamper.close();

		// 写入到HTTP响应。
		writeToResponse(response, baos);
	}

	/**
	 * 将原始PDF资源读取为iText PdfReader。
	 * <p>默认实现将指定的"url"属性解析为ApplicationContext资源。
	 * @return PdfReader实例
	 * @throws IOException 如果资源访问失败
	 * @see #setUrl
	 */
	protected PdfReader readPdfResource() throws IOException {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return new PdfReader(obtainApplicationContext().getResource(url).getInputStream());
	}

	/**
	 * 子类必须实现此方法以将PDF表单与给定的模型数据合并。
	 * <p>这是您可以在此级别执行的操作：
	 * <pre class="code">
	 * //从文档中获取表单
	 * AcroFields form = stamper.getAcroFields();
	 *
	 * //在表单上设置一些值
	 * form.setField("field1", "value1");
	 * form.setField("field2", "Vvlue2");
	 *
	 * //设置位置和文件名
	 * response.setHeader("Content-disposition", "attachment; FILENAME=someName.pdf");</pre>
	 * <p>请注意，传入的HTTP响应仅应用于设置cookie或其他HTTP标头。
	 * 构建的PDF文档本身将在此方法返回后自动写入响应。
	 * @param model 模型Map
	 * @param stamper 将包含AcroFields的PdfStamper实例。
	 * 您还可以根据需要自定义此PdfStamper实例，例如设置"formFlattening"属性。
	 * @param request 以防需要区域设置等。不应查看属性。
	 * @param response 以防需要设置cookie。不应向其写入。
	 * @throws Exception 在构建文档期间发生的任何异常
	 */
	protected abstract void mergePdfDocument(Map<String, Object> model, PdfStamper stamper,
											 HttpServletRequest request, HttpServletResponse response) throws Exception;

}
