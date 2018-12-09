package sds.imaging.rasterizer;

import sds.imaging.domain.core.Image;

public class ReactionRasterizer implements Rasterizer {

	@Override
	public byte[] rasterize(Image image, byte[] data) {
		return IndigoAdapter.rxn2Image(data, image.getFormat(), image.getWidth(), image.getHeight());
	}

}
