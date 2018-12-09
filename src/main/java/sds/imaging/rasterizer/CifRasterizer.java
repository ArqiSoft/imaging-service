package sds.imaging.rasterizer;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jmol.viewer.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sds.imaging.domain.core.Image;
import sds.imaging.rasterizer.exception.RasterizationException;

public class CifRasterizer implements Rasterizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CifRasterizer.class);
    private static Object lock = new Object();

    @Override
    public byte[] rasterize(Image image, byte[] data) {
        Map<String, Object> params = new HashMap<String, Object>();
        Viewer vwr = new Viewer(params);
        int width = image.getWidth();
        int height = image.getHeight();

        vwr.setScreenDimension(width, height);
        
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Generating thumbnail for {} ...", image);
            LOGGER.trace("Source bytes: {}", data.length);
            String str = new String(data, Charset.forName("UTF-8"));
            LOGGER.trace("Source length: {}", str.length());
            LOGGER.trace("Source: \n{}\n====", str);
        }

        byte[] bytes = null;
        
        synchronized(lock) {
            String msg = vwr.loadInline(new String(data, Charset.forName("UTF-8")));
            
            if (vwr.ms.ac == 0) {
                final String errorMessage = "Unsupported input file format";
                throw new RasterizationException(errorMessage);
            }
            
            if (msg != null) {
                throw new RasterizationException(msg);
            }
            
            if (!"png".equals(image.getFormat())) {
                throw new RasterizationException(String.format(
                        "Unsupported output format: '%s'. 'png' is expected", 
                        image.getFormat()));
            }
    
            String[] errors = new String[1];
            bytes = vwr.getImageAsBytes("PNGT", width, height, 2, errors);
            if (errors[0] != null) {
                throw new RasterizationException(errors[0]);
            }
        }

        return bytes;
    }

}
