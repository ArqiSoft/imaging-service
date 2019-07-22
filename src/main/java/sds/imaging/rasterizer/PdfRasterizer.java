package sds.imaging.rasterizer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sds.imaging.domain.core.Image;
import sds.imaging.rasterizer.exception.RasterizationException;

public class PdfRasterizer implements Rasterizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfRasterizer.class);
    
    public static final float DPI_FACTOR = 8.25f;
    public static final Set<String> SUPPORTED_FORMATS;
    
    static {
        final String[] EXTENSION_VALUES = new String[] { "jpg", "jpeg", "png", "bmp", "gif"};
        HashSet<String> set = new HashSet<String>();
        Collections.addAll(set, EXTENSION_VALUES);
        SUPPORTED_FORMATS = set;
    }

    @Override
    public byte[] rasterize(Image image, byte[] data, String fileExtension) {
        String destFormat = image.getFormat().toLowerCase(Locale.getDefault());
        if (!SUPPORTED_FORMATS.contains(destFormat)) {
            throw new RasterizationException(
                    String.format("Unsupported rasterization format '%s'. Supported formats: %s", 
                            image.getFormat(), SUPPORTED_FORMATS));
        }
        
        return generateImageFromPDF(data, image.getFormat(), image.getHeight(), image.getWidth());
    }

    private byte[] generateImageFromPDF(byte[] data, String extension, int height, int width) {
        byte[] result = null;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(data));
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (document.getNumberOfPages() > 0) {
                PDPage pdpage = (PDPage) document.getDocumentCatalog().getAllPages().get(0);
                int dpi = (int) (width / DPI_FACTOR);
                BufferedImage bim = pdpage.convertToImage(BufferedImage.TYPE_4BYTE_ABGR, dpi);
                if (!ImageIO.write(bim, extension, bos)) {
                    throw new RasterizationException(
                            String.format("Unsupported rasterization format '%s'", extension));
                }
                result = bos.toByteArray();
            }
        } catch (IOException e) {
            LOGGER.error("Error converting pdf to image: {}", e);
            throw new RasterizationException(e);
        }

        return result;
    }
}
