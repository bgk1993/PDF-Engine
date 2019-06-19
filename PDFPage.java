package com.document.pdf;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.text.TextPosition;

/**
 * Represents a page in the PDF.
 * @author bkulkarni
 */
public class PDFPage {

	public static class PDFWord {
		
		// Single word in the PDF line.
		private String word;
		private List<TextPosition> positions;
		
		/**
		 * Constructor
		 * @param text
		 * @param textPositions
		 */
		public PDFWord(String text, List<TextPosition> textPositions) {
			this.word = text;
			this.positions = new ArrayList<TextPosition>(textPositions);
		}
		
		/**
		 * Return word
		 * @return word
		 */
		public String getWord() {
			return word;
		}
		
		/**
		 * Return word positions
		 * @return positions
		 */
		public List<TextPosition> getPositions() {
			return this.positions;
		}
		
		/**
		 * Return length of word.
		 * @return word length
		 */
		public int length() {
			return this.word.length();
		}
		
		public void optimize(PDFLine pdfLine) {

			StringBuilder builder = new StringBuilder();
			List<TextPosition> textPositions = new ArrayList<TextPosition>();
			int wordIndex = 0;

			for (int index = 0; index < this.positions.size(); index++) {

				TextPosition textPosition = this.positions.get(index);
				String normalizedData = PDFPage.normalizeWord(textPosition.getUnicode());
				normalizedData = normalizedData.isEmpty() ? textPosition.getUnicode() : normalizedData;

				for (int pos = 0; pos < normalizedData.length(); pos++) {
					
					// Adding text position irrespective of its empty or not.
					// Acts as metadata for space.
					textPositions.add(textPosition);
					
					// Create a new word if white space is encountered.
					if (Character.isWhitespace(this.word.charAt(wordIndex))) {
						PDFWord newWord = new PDFWord(builder.toString(), textPositions);
						pdfLine.newWord(newWord);
						builder.setLength(0);
						textPositions.clear();
					} else {
						builder.append(this.word.charAt(wordIndex));
					}
					wordIndex++;
				}
			}
			
			// Usually this is last word in the line
			if (builder.length() > 0) {
				PDFWord newWord = new PDFWord(builder.toString(), textPositions);
				pdfLine.newWord(newWord);
			}
		}

		@Override
		public String toString() {
			return "PDFWord [word=" + this.word + " description(x,y,font-size)=(" + this.positions.get(0).getX() +
					"," + this.positions.get(0).getY() + "," + this.positions.get(0).getFontSize() + ")]";
		}
	}
	
	/**
	 * Represents a line in the PDF.
	 * @author bkulkarni
	 */
	public static class PDFLine {
		
		// Words in the PDF line.
		private List<PDFWord> line;
		
		/**
		 * Constructor.
		 */
		public PDFLine() {
			this.line = new ArrayList<PDFWord>();
		}
		
		/**
		 * Add new word to the PDF line.
		 * @param text
		 */
		public void newWord(PDFWord text) {
			this.line.add(text);
		}
		
		public void optimize() {
			List<PDFWord> nonOptimizedLine = new ArrayList<PDFWord>(this.line);
			this.line.clear();
			nonOptimizedLine.forEach(word -> word.optimize(this));
		}
		
		List<PDFWord> get() {
			return line;
		}

		@Override
		public String toString() {
			return "PDFLine [line=" + line + "] \n";
		}
	}
	
	// Lines in a PDF page.
	private List<PDFLine> pageLines;

	// All the words in page.
	private List<PDFWord> pageWords;
	
	/**
	 * Constructor.
	 */
	public PDFPage() {
		pageLines = new ArrayList<PDFLine>();
		pageWords = new ArrayList<PDFWord>();
	}
	
	/**
	 * Add a new line in PDF page
	 * @return line
	 */
	public PDFLine newLine() {
		PDFLine line = new PDFLine();
		pageLines.add(line);
		return line;
	}
	
	/**
	 * Add new text to the PDF page
	 * @param text
	 * @param textPositions
	 */
	public void newText(String text, List<TextPosition> textPositions) {
		PDFLine currentLine = pageLines.get(pageLines.size()-1);
		currentLine.newWord(new PDFWord(text, textPositions));
	}
	
	/**
	 * Return all words present in the PDF page.
	 * @return pageWords.
	 */
	public List<PDFWord> get() {
		return pageWords;
	}
	
	public void optimize() {
		// Optimize each line.
		pageLines.forEach(line -> line.optimize());
		
		// Store the references to all words for processing purpose.
		pageLines.forEach(line -> pageWords.addAll(line.get()));
	}

	@Override
	public String toString() {
		return "\n PDFPage [pageLines=" + pageLines + "]";
	}
	
	/**
	 * Normalize word
	 * @param word
	 * @return builder.
	 */
	public static String normalizeWord(String word)
	{
		StringBuilder builder = new StringBuilder();
		int p = 0;
		int q = 0;
		int strLength = word.length();
		for (; q < strLength; q++)
		{
			// We only normalize if the code point is in a given range.
			// Otherwise, NFKC converts too many things that would cause
			// confusion. For example, it converts the micro symbol in
			// extended Latin to the value in the Greek script. We normalize
			// the Unicode Alphabetic and Arabic A&B Presentation forms.
			char c = word.charAt(q);
			if (0xFB00 <= c && c <= 0xFDFF || 0xFE70 <= c && c <= 0xFEFF)
			{
				builder.append(word.substring(p, q));
				// Some fonts map U+FDF2 differently than the Unicode spec.
				// They add an extra U+0627 character to compensate.
				// This removes the extra character for those fonts.
				if (c == 0xFDF2 && q > 0
						&& (word.charAt(q - 1) == 0x0627 || word.charAt(q - 1) == 0xFE8D))
				{
					builder.append("\u0644\u0644\u0647");
				}
				else
				{
					// Trim because some decompositions have an extra space, such as U+FC5E
					builder.append(Normalizer.normalize(word.substring(q, q + 1), Normalizer.Form.NFKC).trim());
				}
				p = q + 1;
			}
		}
		return builder.toString();
	}
}
