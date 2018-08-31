package net.cbaakman.occupy.load;

import java.util.HashSet;
import java.util.Set;

import org.apache.batik.transcoder.TranscoderException;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.FontStyle;

public class FontLoadable implements Loadable<Font> {

	private LoadRecord<FontFactory> asyncFontFactory;
	private FontStyle style;
	
	public FontLoadable(LoadRecord<FontFactory> asyncFontFactory, FontStyle style) {
		this.asyncFontFactory = asyncFontFactory;
		this.style = style;
	}
	
	@Override
	public Set<LoadRecord<?>> getDependencies() {
		Set<LoadRecord<?>> set = new HashSet<LoadRecord<?>>();
		set.add(asyncFontFactory);
		return set;
	}
	
	@Override
	public String toString() {
		return String.format("font:%s-with-%s", asyncFontFactory.toString(), style.toString());
	}

	@Override
	public Font load() throws InitError, NotLoadedError {
		try {
			return asyncFontFactory.get().generateFont(style);
		} catch (TranscoderException e) {
			throw new InitError(e);
		}
	}
}
