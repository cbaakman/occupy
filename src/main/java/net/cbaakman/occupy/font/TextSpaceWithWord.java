package net.cbaakman.occupy.font;

/**
 * Basically, a word with preceding spaces.
 */
public class TextSpaceWithWord {
	
	private Font font;
	private String letters;
	
	public TextSpaceWithWord(Font font, String letters) {
		this.font = font;
		this.letters = letters;
	}
	
	@Override
	public String toString() {
		return letters;
	}
	
	public char getFirstLetter() {
		return letters.charAt(0);
	}
	public char getLastLetter() {
		return letters.charAt(letters.length() - 1);
	}
	
	public float getWidth() {
		int i;
		float w = 0.0f;
		for (i = 0; i < letters.length(); i++) {
			char c = letters.charAt(i);
			
			if (i > 0) {
				char prevc = letters.charAt(i - 1);
				
				w += font.getHKern(prevc, c);
			}
			
			w += font.getHorizAdvX(c);
		}
		return w;
	}
}
