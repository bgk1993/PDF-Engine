package com.document.parser.pdf;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * Engine that process PDF document into positional data units.
 * 
 * @author bkulkarni
 *
 */
public class PDFEngine extends PDFTextStripper {

	private static Logger log = Logger.getLogger(PDFEngine.class);
	
	private Stack<PDFPage> pdfPages = new Stack<PDFPage>();

	public PDFEngine() throws IOException {
		super();
	}

	public void process(PDDocument document) throws IOException {
		resetEngine();
		getText(document);
	}
	
	@Override
	protected void startDocument(PDDocument document) throws IOException {
		log.info("PDF engine started processing document "+document.getDocumentInformation().getTitle());
	}
	
	@Override
	protected void startPage(PDPage page) throws IOException {
		createNewPage();
		pdfPages.lastElement().newLine();
		super.startPage(page);
	}
	
	@Override
	protected void writeString(String text, List<TextPosition> textPositions) throws IOException
    {
		pdfPages.lastElement().newText(text, textPositions);
    }
	
	@Override
	protected void writeLineSeparator() throws IOException
	{
		pdfPages.lastElement().newLine();
	}
	
	@Override
	protected void endDocument(PDDocument document) throws IOException
    {
		log.info("Optimizing the result...");
		pdfPages.forEach(pdfPage -> pdfPage.optimize());
		log.info("PDF document processing completed.");
    }
	
	public Stack<PDFPage> getProcessedResult() {
		return pdfPages;
	}
	
	public void resetEngine() {
		
		log.info("Resetting PDF engine");
		pdfPages.clear();
	}
	
	private void createNewPage() {
		pdfPages.add(new PDFPage());
	}
}
