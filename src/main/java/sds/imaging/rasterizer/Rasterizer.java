package sds.imaging.rasterizer;

import sds.imaging.domain.core.Image;

public interface Rasterizer {
	/**
	 * Converts data into target type
	 * @param image target image description
         * @param fileExtension input file`s extension
	 * @param data file content
	 * @return converted image
	 */
	byte[] rasterize(Image image, byte[] data, String fileExtension);
}
