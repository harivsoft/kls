package com.vsoftcorp.kls.report.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRTextExporterParameter;

import org.apache.log4j.Logger;

import com.vsoftcorp.kls.business.entities.Pacs;
import com.vsoftcorp.kls.business.util.DateUtil;
import com.vsoftcorp.kls.dataaccess.IPacsDAO;
import com.vsoftcorp.kls.dataaccess.factory.KLSDataAccessFactory;
import com.vsoftcorp.kls.report.factory.KLSReportSeriveFactory;
import com.vsoftcorp.kls.report.service.data.ShareAccountBalancingReportData;
import com.vsoftcorp.kls.report.service.exception.KlsReportRuntimeException;
import com.vsoftcorp.kls.report.util.Report;
import com.vsoftcorp.kls.report.util.ReportConfig;
import com.vsoftcorp.kls.report.util.ReportUtil;
import com.vsoftcorp.kls.service.util.PropertiesUtil;
import com.vsoftcorp.time.Date;

public class ShareAccountBalancingActionServlet extends HttpServlet {

	private static final long serialVersionUID = -2156179601256068005L;
	private static final Logger logger = Logger.getLogger(ShareAccountLedgerActionServlet.class);

	public ShareAccountBalancingActionServlet() {
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		logger.info("Start: calling getShareAccountLedgerIreport() inside doPost() ");
		PrintWriter printWriter = null;
		Integer productId = Integer.parseInt(request.getParameter("txtProductId").replaceAll("/", ""));
		logger.info("productId---" + productId);
		String memberNo = request.getParameter("txtMemberNo");
		logger.info("memberNo--" + memberNo);
		String userName = request.getParameter("userName");
		String businessDate = request.getParameter("businessDate");
		String asOnDateString = request.getParameter("txtAsOnDate");
		logger.info("asOnDateString--" + asOnDateString);
		String isBankUser = request.getParameter("isBankUser");
		logger.info("isBankUser---" + isBankUser);
		//Integer pacsId = Integer.parseInt(request.getParameter("pacsId").replaceAll("/", ""));
		String bankName = request.getParameter("bankName");

		// String accountNumber = request.getParameter("accountNumber");

		Integer pacsId = 0;
		/*if (isBankUser.equalsIgnoreCase("true"))
			pacsId = Integer.parseInt(request.getParameter("txtPacsId").replaceAll("/", ""));
		else*/
		pacsId = Integer.parseInt(request.getParameter("pacsId").replaceAll("/", ""));

		logger.info("pacsId---" + pacsId);
		JRHtmlExporter exporter = null;
		JRPdfExporter pdfExporter = null;
		// Creating SBReport directory in usr folder
		ReportUtil.createSBReportDirectory();
		
		String pdfRequired= ReportConfig.getProperty("pdfrequired");
		File fileName = null;
		if(pdfRequired.equalsIgnoreCase("Y")){
			fileName= new File(PropertiesUtil.getDocumentProperty("filesLocation") + "SBReport/"
					+ ReportConfig.getProperty("ShareAccountBlancing-Report-PDF-FileName"));	
		} else  {
			
			fileName = new File(PropertiesUtil.getDocumentProperty("filesLocation") + "SBReport/"
				+ ReportConfig.getProperty("ShareAccountBlancing-Report-Txt-FileName"));
		}
		logger.info("In Try after File Util ");

		fileName = ReportUtil.getFileName(fileName);
		
		String requiredFormat=null;
		
		if(pdfRequired.equalsIgnoreCase("Y")){
			requiredFormat="pdf";
		}else{
			requiredFormat="html";
			HttpSession session = request.getSession();
			session.setAttribute("txtFileName", fileName.getPath());
			session.setAttribute("outDatName", ReportConfig.getProperty("ShareAccountBlancing-Report-Bat-FileName"));
			session.setAttribute("reportName", ReportConfig.getProperty("ShareAccountBlancing-Report-Txt-FileName"));
		}
		
		fileName = ReportUtil.getFileName(fileName);
		HttpSession session = request.getSession();
		session.setAttribute("txtFileName", fileName.getPath());
		Map<String, Object> map = null;
		String error = "Error Occured While Report Generation  ";
		try {
			map = getShareAccountLedgerIreport(requiredFormat, asOnDateString, printWriter, productId, memberNo, fileName, pacsId, userName, businessDate,bankName);
		} catch (Exception e1) {
			e1.printStackTrace();
			printWriter.println(error);
		}
		if (map != null) 
		{
			 System.out.println("======pdfinside map"+pdfRequired);
			if(pdfRequired.equalsIgnoreCase("Y")){
				pdfExporter = (JRPdfExporter) map.get("exporter");
			}else{
			exporter = (JRHtmlExporter) map.get("exporter");
			}
			if (map.containsKey(Report._ERROR)) 
			{
				error = "<h3>" + (String) map.get(Report._ERROR) + "</h3>";
			}
			
			if(pdfRequired.equalsIgnoreCase("Y")){
				if (pdfExporter == null) 
				{		
					printWriter.println(error);
				}else{
					response.setContentType("application/pdf");
					
					 String filename=PropertiesUtil.getDocumentProperty("filesLocation") + "SBReport"+ ReportConfig.getProperty("ShareAccountBlancing-Report-PDF-FileName");
						File file = new File(filename);
						FileInputStream in = new FileInputStream(file);
						ServletOutputStream out1=response.getOutputStream();
						byte[] buf = new byte[4096];
						int count = 0;
						while ((count = in.read(buf)) >= 0)
						{
						out1.write(buf, 0, count);
						}

						in.close();
						out1.close();
				}
				
			}else{
			if (exporter == null) 
			{		
				printWriter.println(error);
			}
			else 
			{
				
				//PrintWriter writer = response.getWriter();

				response.setContentType("text/html");
				exporter.setParameter(JRHtmlExporterParameter.OUTPUT_WRITER,
						response.getWriter());
				try {
					exporter.exportReport();
				} catch (JRException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		} else {
			printWriter=response.getWriter();
			printWriter.println("Data not found for Report");
		}
		logger.info("End:  inside doPost() ");

	}

	public Map<String, Object> getShareAccountLedgerIreport(String reportType, String asOnDateString, PrintWriter printWriter, Integer productId,
			String memberNo, File filename, Integer pacsId, String userName, String businessDate, String bankName) throws ParseException {
		logger.info("Start: Calling ShareAccountLedgerReportService in getShareAccountLedgerIreport()");

		HashMap<String, Object> parameters = new HashMap<String, Object>();
		Pacs pac = null;

		Pacs pacs = null;
		if (pacsId != null && pacsId != 0) {
			IPacsDAO pDao = KLSDataAccessFactory.getPacsDAO();
			pacs = pDao.getPacs(pacsId);
		}

		if (pacs != null) {
			parameters.put("pacName", pacs.getName());
			
		} else {
			parameters.put("pacName", "All");
			parameters.put("bankName", "All");
		}
		parameters.put("bankName", bankName);
		parameters.put("asOnDate", asOnDateString);
		parameters.put("memberNo", memberNo);
		parameters.put("productId", productId);
		parameters.put("userName", userName);
		parameters.put("businessDate", ReportUtil.getUtilDateByString(businessDate));

		Date asOnDate = Date.valueOf(DateUtil.getFormattedDate(asOnDateString));
		try {
			
			if (reportType.equals("pdf")) {
				
				List<ShareAccountBalancingReportData> list = KLSReportSeriveFactory.getShareAccountBalancingReportService()
						.getShareAccountBalancingReport(pacsId, productId, memberNo, asOnDate);

				logger.info("StockRegister Item list Size::::" + list.size());
				if (list.isEmpty())
					return null;
				JasperPrint jasperPrint = ReportUtil.getJasperPrint(ReportUtil.class.getResourceAsStream(ReportConfig
						.getProperty("ShareAccountBlancing-Report-Jrxml")), new JRBeanCollectionDataSource(list), parameters);
				
				
				JRPdfExporter pdfexporter = new JRPdfExporter();
				pdfexporter.setParameter(JRExporterParameter.JASPER_PRINT,
						jasperPrint);
				
                JasperExportManager.exportReportToPdfFile(jasperPrint, PropertiesUtil.getDocumentProperty("filesLocation") + "SBReport/"
						+ ReportConfig.getProperty("ShareAccountBlancing-Report-PDF-FileName"));
                HashMap<String, Object> myMap = new HashMap<String, Object>();
				myMap.put("exporter", pdfexporter);

				return myMap;
			}
			
			if (reportType.equals("html")) {
				List<ShareAccountBalancingReportData> list = KLSReportSeriveFactory.getShareAccountBalancingReportService()
						.getShareAccountBalancingReport(pacsId, productId, memberNo, asOnDate);
				if (list.isEmpty())
					return null;
				logger.info("shareAcclList:" + list.size());
				JasperPrint jasperPrint = ReportUtil.getJasperPrint(
						ReportUtil.class.getResourceAsStream(ReportConfig.getProperty("ShareAccountBlancing-Report-Jrxml")),
						new JRBeanCollectionDataSource(list), parameters);
				Map<String, List<String>> map = new HashMap<String, List<String>>();

				List<String> list2 = new ArrayList<String>();
				list2.add(pacs.getName());
				map.put("pacName", list2);

				List<String> list3 = new ArrayList<String>();
				list3.add(asOnDateString);
				map.put("fromDate", list3);

				JRHtmlExporter exporter = ReportUtil.getExportHtm(jasperPrint);

				exporter.setParameter(JRHtmlExporterParameter.SIZE_UNIT, JRHtmlExporterParameter.SIZE_UNIT_PIXEL);

				exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE);

				exporter.setParameter(JRHtmlExporterParameter.IS_WRAP_BREAK_WORD, Boolean.TRUE);

				exporter.setParameter(JRHtmlExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);

				exporter.setParameter(JRHtmlExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

				exporter.setParameter(JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.FALSE);

				exporter.setParameter(JRTextExporterParameter.CHARACTER_HEIGHT, new Integer(16));

				exporter.setParameter(JRTextExporterParameter.CHARACTER_WIDTH, new Integer(16));

				exporter.setParameter(JRHtmlExporterParameter.OFFSET_X, -300);
				exporter.setParameter(JRTextExporterParameter.PAGE_HEIGHT, 80);
				exporter.setParameter(JRTextExporterParameter.PAGE_WIDTH, 160);

				// exporter.setParameter(JRHtmlExporterParameter.OUTPUT_WRITER,printWriter);
				exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, false);
				ReportUtil.exportText(130, 80, jasperPrint, filename.toString());
				String footerHtml = ReportUtil.getFastPrint(map, ReportConfig.getProperty("ShareAccountBlancing-FPReport-JSP"), true, null);

				exporter.setParameter(JRHtmlExporterParameter.HTML_FOOTER, footerHtml);

				HashMap<String, Object> myMap = new HashMap<String, Object>();
				myMap.put("exporter", exporter);
				return myMap;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error:Inside getShareAccountLedgerIreport method");
			throw new KlsReportRuntimeException("Problem occured while generating report");
		}
		logger.info("End: Calling ShareAccountLedgerReportService in getShareAccountLedgerIreport()");

		return null;
	}

	public static String callFastPrint(String txtFileName) {
		logger.info("Start: inside callFastPrint() ");
		String printerName = PropertiesUtil.getProperty("IP_ADDRESS") + PropertiesUtil.getProperty("PRINTER_NAME");
		String printerCommand = ReportConfig.getProperty("printCommand");
		if (printerName != null) {
			String IPAddress = PropertiesUtil.getProperty("IP_ADDRESS");
			String printName = PropertiesUtil.getProperty("PRINTER_NAME");
			printerName = printName + " -h " + IPAddress + " -o raw ";
			printerCommand = ReportConfig.getProperty("printCommand-Form") + " " + printerName;
		}
		String txtName = ReportUtil.writeToFile(ReportConfig.getProperty("ShareAccountBlancing-Report-Bat-FileName"), printerCommand, txtFileName);
		logger.info("End: inside callFastPrint() ");
		return txtName;

	}
}
