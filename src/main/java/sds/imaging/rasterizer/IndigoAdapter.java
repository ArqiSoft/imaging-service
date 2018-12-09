package sds.imaging.rasterizer;

import java.util.Locale;

import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoObject;
import com.epam.indigo.IndigoRenderer;

public class IndigoAdapter {

	public static byte[] mol2Image(byte[] data, String format, int width, int height) {
        Indigo indigo = new Indigo();
		return renderedImage(indigo.loadMolecule(data), indigo, format, width, height);
	}

	public static byte[] rxn2Image(byte[] data, String format, int width, int height) {
        Indigo indigo = new Indigo();
		return renderedImage(indigo.loadReaction(data), indigo, format, width, height);
	}
	
	private static byte[] renderedImage(IndigoObject obj, Indigo indigo, String format, int width, int height) {
        indigo.setOption("ignore-stereochemistry-errors", true);
        indigo.setOption("ignore-noncritical-query-features", true);	    

        IndigoRenderer renderer = new IndigoRenderer(indigo);
		indigo.setOption("render-stereo-style", "ext");
		indigo.setOption("render-margins", 5, 5);
		indigo.setOption("render-coloring", true);
		indigo.setOption("render-relative-thickness", "1.5");
		indigo.setOption("render-image-size", width, height);
		indigo.setOption("render-output-format", format.toLowerCase(Locale.getDefault()));
		
		return renderer.renderToBuffer(obj);
	}

}
