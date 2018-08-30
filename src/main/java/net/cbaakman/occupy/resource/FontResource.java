package net.cbaakman.occupy.resource;

import java.util.HashSet;
import java.util.Set;

import org.apache.batik.transcoder.TranscoderException;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.FontStyle;
import net.cbaakman.occupy.load.LoadRecord;

public class FontResource implements Resource<Font> {

	private LoadRecord<FontFactory> asyncFontFactory;
	private FontStyle style;
	
	public FontResource(LoadRecord<FontFactory> asyncFontFactory, FontStyle style) {
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
	public Font init(GL3 gl3) throws InitError, NotReadyError {
		try {
			return asyncFontFactory.get().generateFont(style);
		} catch (TranscoderException e) {
			throw new InitError(e);
		}
	}

	@Override
	public void dispose(GL3 gl3) {
	}
}
