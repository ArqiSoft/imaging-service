/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sds.imaging.rasterizer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.FormatTools;
import loci.formats.meta.IMetadata;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;
import org.apache.commons.io.IOUtils;
import sds.imaging.domain.core.Image;

/**
 *
 * @author Aleksander Sidorenko
 */
public class NikonRasterizer implements Rasterizer {

    /**
     * The file format reader.
     */
    private ImageReader reader;

    /**
     * The file format writer.
     */
    private OMETiffWriter writer;

    /**
     * The file to be read.
     */
    private String inputFile;

    /**
     * The file to be written.
     */
    private String outputFile;

    /**
     * The tile width to be used.
     */
    private int tileSizeX;

    /**
     * The tile height to be used.
     */
    private int tileSizeY;

    @Override
    public byte[] rasterize(Image image, byte[] data, String fileExtension) {
        
        byte[] resultImageBytes = null;
        try {
            File directory = new File(System.getenv("OSDR_TEMP_FILES_FOLDER"));
            File inputTempFile = File.createTempFile("temp", "." + fileExtension, directory);
            File outputTempFile = File.createTempFile("temp", "." + image.getFormat(), directory);
            try (FileOutputStream out = new FileOutputStream(inputTempFile)) {
                IOUtils.copy(new ByteArrayInputStream(data), out);
            }

            initialize(inputTempFile.getCanonicalPath(), outputTempFile.getCanonicalPath(), image.getWidth(), image.getHeight());

            readWriteTiles();

            cleanup();
                
            resultImageBytes = Files.readAllBytes(outputTempFile.toPath());
            inputTempFile.delete();
            outputTempFile.delete();
            
        } catch (DependencyException ex) {
            Logger.getLogger(NikonRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FormatException ex) {
            Logger.getLogger(NikonRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NikonRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            Logger.getLogger(NikonRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resultImageBytes;
    }

    private void initialize(String inputFile, String outputFile, int tileSizeX, int tileSizeY) throws DependencyException, FormatException, IOException, ServiceException {
        // construct the object that stores OME-XML metadata
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.tileSizeX = tileSizeX;
        this.tileSizeY = tileSizeY;
        
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata omexml = service.createOMEXMLMetadata();

        reader = new ImageReader();
        reader.setMetadataStore(omexml);
        reader.setId(inputFile);

        writer = new OMETiffWriter();
        writer.setMetadataRetrieve(omexml);
        writer.setInterleaved(reader.isInterleaved());

        this.tileSizeX = writer.setTileSizeX(tileSizeX);
        this.tileSizeY = writer.setTileSizeY(tileSizeY);

        writer.setId(outputFile);
    }

    public void readWriteTiles() throws FormatException, DependencyException, ServiceException, IOException {
        byte[] buf = new byte[FormatTools.getPlaneSize(reader)];

        for (int series = 0; series < reader.getSeriesCount(); series++) {
            reader.setSeries(series);
            writer.setSeries(series);

            // convert each image in the current series
            for (int image = 0; image < reader.getImageCount(); image++) {
                // Read tiles from the input file and write them to the output OME-Tiff
                // The OME-Tiff Writer will automatically write the images in a tiled format
                buf = reader.openBytes(image);
                writer.saveBytes(image, buf);
            }
        }
    }

    private void cleanup() {
        try {
            reader.close();
        } catch (IOException e) {
            System.err.println("Failed to close reader.");
            e.printStackTrace();
        }
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to close writer.");
            e.printStackTrace();
        }
    }
}
