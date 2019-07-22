package sds.imaging.rasterizer;

import sds.imaging.domain.core.Image;

public class StructureRasterizer implements Rasterizer {

	@Override
	public byte[] rasterize(Image image, byte[] data, String fileExtension) {
		return IndigoAdapter.mol2Image(data, image.getFormat(), image.getWidth(), image.getHeight());
	}

}
