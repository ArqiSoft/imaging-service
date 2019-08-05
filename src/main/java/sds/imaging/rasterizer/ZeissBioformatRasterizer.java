/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sds.imaging.rasterizer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.out.JPEGWriter;
import loci.formats.services.OMEXMLService;
import org.apache.commons.io.IOUtils;
import sds.imaging.domain.core.Image;

public class ZeissBioformatRasterizer implements Rasterizer {

    private ImageReader reader;

    private JPEGWriter writer;

    private String inputFile;

    private String outputFile;

    @Override
    public byte[] rasterize(Image image, byte[] data, String fileExtension) {

        byte[] resultImageBytes = null;
        try {

            File directory = new File(System.getenv("OSDR_TEMP_FILES_FOLDER"));
            File inputTempFile = File.createTempFile("temp", "." + fileExtension, directory);
            File outputTempFile = File.createTempFile("temp", ".jpeg", directory);
            File outputPngFile = File.createTempFile("temp", ".png", directory);
            try (FileOutputStream out = new FileOutputStream(inputTempFile)) {
                IOUtils.copy(new ByteArrayInputStream(data), out);
            }

            inputFile = inputTempFile.getCanonicalPath();
            outputFile = outputTempFile.getCanonicalPath();

            convert();
            File file = new File(outputFile);
            BufferedImage bi = ImageIO.read(file);

            ImageIO.write(bi, "png", outputPngFile);
            resultImageBytes = Files.readAllBytes(outputPngFile.toPath());
            inputTempFile.delete();
            outputTempFile.delete();
            outputPngFile.delete();
        } catch (IOException ex) {
            Logger.getLogger(ZeissBioformatRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resultImageBytes;
    }

    /**
     * Do the actual work of converting the input file to the output file.
     */
    public void convert() {
        // initialize the files
        boolean initializationSuccess = initialize();

        // if we could not initialize one of the files,
        // then it does not make sense to convert the planes
        if (initializationSuccess) {
            convertPlanes();
        }

        // close the files
        cleanup();
    }

    /**
     * Set up the file reader and writer, ensuring that the input file is
     * associated with the reader and the output file is associated with the
     * writer.
     *
     * @return true if the reader and writer were successfully set up, or false
     * if an error occurred
     */
    private boolean initialize() {
        Exception exception = null;
        try {
            // construct the object that stores OME-XML metadata
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata omexml = service.createOMEXMLMetadata();

            // set up the reader and associate it with the input file
            reader = new ImageReader();
            reader.setMetadataStore(omexml);
            reader.setId(inputFile);

            // set up the writer and associate it with the output file
            writer = new JPEGWriter();
            writer.setMetadataRetrieve(omexml);
            writer.setInterleaved(reader.isInterleaved());
            writer.setId(outputFile);
        } catch (FormatException e) {
            exception = e;
        } catch (IOException e) {
            exception = e;
        } catch (DependencyException e) {
            exception = e;
        } catch (ServiceException e) {
            exception = e;
        }
        if (exception != null) {
            System.err.println("Failed to initialize files.");
            exception.printStackTrace();
        }
        return exception == null;
    }

    /**
     * Save every plane in the input file to the output file.
     */
    private void convertPlanes() {

        for (int series = 0; series < reader.getSeriesCount(); series++) {
            // tell the reader and writer which series to work with
            // in FV1000 OIB/OIF, there are at most two series - one
            // is the actual data, and one is the preview image
            reader.setSeries(series);
            try {
                writer.setSeries(series);
            } catch (FormatException e) {
                System.err.println("Failed to set writer's series #" + series);
                e.printStackTrace();
                break;
            }

            // construct a buffer to hold one image's pixels
            byte[] plane = new byte[FormatTools.getPlaneSize(reader)];

            // convert each image in the current series
            for (int image = 0; image < reader.getImageCount(); image++) {
                try {
                    reader.openBytes(image, plane);
                    writer.saveBytes(image, plane);
                } catch (IOException e) {
                    System.err.println("Failed to convert image #" + image
                            + " in series #" + series);
                    e.printStackTrace();
                } catch (FormatException e) {
                    System.err.println("Failed to convert image #" + image
                            + " in series #" + series);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Close the file reader and writer.
     */
    private void cleanup() {
        try {
            reader.close();
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to cleanup reader and writer.");
            e.printStackTrace();
        }
    }
}
