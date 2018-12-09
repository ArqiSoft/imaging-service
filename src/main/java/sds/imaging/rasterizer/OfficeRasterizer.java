package sds.imaging.rasterizer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sds.imaging.domain.core.Image;
import sds.imaging.rasterizer.exception.RasterizationException;
import sds.officeprocessor.converters.IConvert;

public class OfficeRasterizer implements Rasterizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfficeRasterizer.class);

    private IConvert converter;
    private Rasterizer rasterizer = new PdfRasterizer();

    public OfficeRasterizer(IConvert instance) {
        this.converter = instance;
    }

    @Override
    public byte[] rasterize(Image image, byte[] data) {
        byte[] result = null;
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                InputStream pdf = converter.Convert(byteArrayInputStream)) {
            result = rasterizer.rasterize(image, IOUtils.toByteArray(pdf));
        } catch (NullPointerException | IOException e) {
            LOGGER.error("Error converting document to image: {}", e);
            throw new RasterizationException(e);
        }
        
        return result;
    }

}
