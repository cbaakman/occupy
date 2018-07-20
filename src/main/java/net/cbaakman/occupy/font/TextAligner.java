package net.cbaakman.occupy.font;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.cbaakman.occupy.errors.TextAlignError;
import net.cbaakman.occupy.font.enums.HorizontalTextAlignment;
import net.cbaakman.occupy.font.enums.VerticalTextAlignment;

public class TextAligner {
	
	public static TextAlignment getAlignment(Font font, String text, float maxLineWidth) throws TextAlignError {
		TextAlignment alignment = new TextAlignment(font, maxLineWidth);
		
		for (String forcedLineString : patternNewline.split(text)) {
			alignment.getLines().addAll(getLines(font, forcedLineString, maxLineWidth));
		}
		
		return alignment;
	}
	
	private static List<TextLine> getLines(Font font, String text, float maxLineWidth) {
		List<TextLine> lines = new ArrayList<TextLine>();
		
		Matcher matcher = patternWord.matcher(text);
		TextLine currentLine = new TextLine(font);
		int prevWordEnd = 0;
		while (matcher.find()) {
			
			// First word has no preceding spaces.
			if (currentLine.getWords().size() <= 0)
				prevWordEnd = matcher.start();
			
			TextSpaceWithWord word = new TextSpaceWithWord(font, text.substring(prevWordEnd, matcher.end()));
			
			currentLine.getWords().add(word);
			if (currentLine.getWidth() > maxLineWidth) {
				// remove last added
				currentLine.getWords().remove(currentLine.getWords().size() - 1);
				
				lines.add(currentLine);
				currentLine = new TextLine(font);
				currentLine.getWords().add(new TextSpaceWithWord(font, text.substring(matcher.start(), matcher.end())));
			}
			
			prevWordEnd = matcher.end();
		}
		
		return lines;
	}

	private static Pattern patternNewline = Pattern.compile("$");
	private static Pattern patternWord = Pattern.compile("[^\\s]+");

}
