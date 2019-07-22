package sds.imaging.rasterizer;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twelvemonkeys.image.ResampleOp;

import sds.imaging.domain.core.Image;
import sds.imaging.rasterizer.exception.RasterizationException;

public class ImageRasterizer implements Rasterizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageRasterizer.class);

    @Override
    public byte[] rasterize(Image image, byte[] data, String fileExtension) {
        byte[] result = null;

        try (InputStream in = new ByteArrayInputStream(data);
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            BufferedImage img = ImageIO.read(in);
            BufferedImage output = scale(img, image);
            if (!ImageIO.write(output, image.getFormat(), bos)) {
                throw new RasterizationException(
                        String.format("Unsupported rasterization format '%s'", image.getFormat()));
             }
            result = bos.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Error converting images: {}", e);
            throw new RasterizationException(e);
        }
        
        return result;
    }
    
    public BufferedImage scale(BufferedImage img, Image image)
    {
        double ratio = 1.0d * img.getWidth() / img.getHeight();

        int newWidth = image.getWidth();
        int newHeight = (int)(image.getHeight() / ratio);
        
        if (ratio < 1) {
            newWidth = (int) (image.getWidth() * ratio);
            newHeight = image.getHeight();
        }

        BufferedImageOp resampler = new ResampleOp(newWidth, 
                newHeight, ResampleOp.FILTER_POINT);
        BufferedImage output = resampler.filter(img, null);

        return output;
    }

    

}
