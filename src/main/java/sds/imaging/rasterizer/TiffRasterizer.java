package sds.imaging.rasterizer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import sds.imaging.domain.core.Image;

public class TiffRasterizer implements Rasterizer {

    private File inputTempFile;

    private File outputTempFile;

    @Override
    public byte[] rasterize(Image image, byte[] data, String fileExtension) {
        byte[] resultImageBytes = null;
        try {
            File directory = new File(System.getenv("OSDR_TEMP_FILES_FOLDER"));
            inputTempFile = File.createTempFile("temp", "." + fileExtension, directory);
            outputTempFile = File.createTempFile("temp", "." + image.getFormat(), directory);
            try (FileOutputStream out = new FileOutputStream(inputTempFile)) {
                IOUtils.copy(new ByteArrayInputStream(data), out);
            }
            convertTiffToPng(inputTempFile);
            resultImageBytes = Files.readAllBytes(outputTempFile.toPath());
            inputTempFile.delete();
            outputTempFile.delete();

            return resultImageBytes;
        } catch (IOException ex) {
            try{
                resultImageBytes = new ImageRasterizer().rasterize(image, data, fileExtension);
            }
            catch(Throwable e){
                Logger.getLogger(TiffRasterizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return resultImageBytes;
    }

    public void convertTiffToPng(File file) throws IOException {
        
        try (InputStream is = new FileInputStream(file)) {
            try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(is)) {
                Iterator<javax.imageio.ImageReader> iterator = ImageIO.getImageReaders(imageInputStream);
                if (iterator == null || !iterator.hasNext()) {
                    throw new RuntimeException("Image file format not supported by ImageIO: " + file.getAbsolutePath());
                }

                javax.imageio.ImageReader reader = iterator.next();
                reader.setInput(imageInputStream);

                int numPage = reader.getNumImages(true);

                //String name = FilenameUtils.getBaseName(outputTempFile.getAbsolutePath());
                final BufferedImage tiff = reader.read(numPage / 2);
                ImageIO.write(tiff, "png", new FileOutputStream(outputTempFile.getAbsolutePath(), false));
            }
        }
    }
}
