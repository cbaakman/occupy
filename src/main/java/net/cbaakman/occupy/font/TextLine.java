package net.cbaakman.occupy.font;

import java.util.ArrayList;
import java.util.List;

public class TextLine {

	private List<TextSpaceWithWord> words = new ArrayList<TextSpaceWithWord>();
	private Font font;
	
	public TextLine(Font font) {
		this.font = font;
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (TextSpaceWithWord word : words) {
			s.append(word.toString());
		}
		return s.toString();
	}
	
	public float getHeight() {
		return font.getBoundingBox().getHeight();
	}
	
	public float getWidth() {
		
		int i;
		float w = words.get(0).getWidth();
		
		for (i = 1; i < words.size(); i++) {
			
			w += getSpaceBetween(words.get(i - 1), words.get(i));
			w += words.get(i).getWidth();
		}
		
		return w;
	}

	private float getSpaceBetween(TextSpaceWithWord w1, TextSpaceWithWord w2) {
		return font.getHKern(w1.getLastLetter(), w2.getFirstLetter());
	}

	public List<TextSpaceWithWord> getWords() {
		return words;
	}
}
