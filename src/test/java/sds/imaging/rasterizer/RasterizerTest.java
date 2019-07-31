package sds.imaging.rasterizer;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import sds.imaging.domain.core.Image;

public class RasterizerTest {

	private Image image = new Image(UUID.randomUUID(), 300, 300, "png", "image/png", null);

	@Test
	public void testCifGeneration() throws IOException, URISyntaxException {
		byte[] result = RasterizationFactory.getInstance("cif").rasterize(image, readFile("/data/test.cif"), "cif");

		assertNotNull(result);
	}

	@Test
	public void testMolGeneration() throws IOException, URISyntaxException {
		byte[] result = RasterizationFactory.getInstance("mol").rasterize(image, readFile("/data/test.mol"), "mol");

		assertNotNull(result);
	}

	@Test
	@Ignore
	public void testMol2Generation() throws IOException, URISyntaxException {
		byte[] result = RasterizationFactory.getInstance("mol").rasterize(image, readFile("/data/test.mol2"), "mol2");

		assertNotNull(result);
	}

	@Test
	public void testRxnGeneration() throws IOException, URISyntaxException {
		byte[] result = RasterizationFactory.getInstance("rxn").rasterize(image, readFile("/data/test.rxn"), "rxn");

		assertNotNull(result);
	}

    @Test
    @Ignore
    public void testCdxGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("cdx").rasterize(image, readFile("/data/test.cdx"), "cdx");

        assertNotNull(result);
    }

    @Test
    public void testPdfGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("pdf").rasterize(image, readFile("/data/test.pdf"), "pdf");

        assertNotNull(result);
    }

    @Test
    public void testPdfGeneration2() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("pdf").rasterize(image, readFile("/data/apiext.pdf"), "pdf");

        assertNotNull(result);
    }

    @Test
    public void testGifGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("gif").rasterize(image, readFile("/data/test.gif"), "gif");

        assertNotNull(result);
    }

    @Test
    public void testResizeCircle() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("bmp").rasterize(image, readFile("/data/round.bmp"), "bmp");

        assertNotNull(result);
    }

    @Test
    public void testResizeWidth() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("bmp").rasterize(image, readFile("/data/width.bmp"), "bmp");

        assertNotNull(result);
    }

    @Test
    public void testResizeHeight() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("bmp").rasterize(image, readFile("/data/height.bmp"), "bmp");

        assertNotNull(result);
    }
//    
//    @Test
//    public void testTiffGeneration() throws IOException, URISyntaxException {
//        byte[] result = RasterizationFactory.getInstance("tif").rasterize(image, readFile("/data/test.tif"), "tif");
//
//        assertNotNull(result);
//    }

    @Test
    public void testJpgGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("jpg").rasterize(image, readFile("/data/test.jpg"), "jpg");

        assertNotNull(result);
    }

    @Test
    public void testPngGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("png").rasterize(image, readFile("/data/test.png"), "png");

        assertNotNull(result);
    }

    @Test
    public void testSvgGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("svg").rasterize(image, readFile("/data/test.svg"), "svg");

        assertNotNull(result);
    }

    @Test
    public void testPptxGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("pptx").rasterize(image, readFile("/data/test.pptx"), "pptx");

        assertNotNull(result);
    }

    @Test
    public void testDocxGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("docx").rasterize(image, readFile("/data/test.docx"), "docx");

        assertNotNull(result);
    }

    @Test
    public void testDocGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("doc").rasterize(image, readFile("/data/test.doc"), "doc");

        assertNotNull(result);
    }

    @Test
    public void testXlsxGeneration() throws IOException, URISyntaxException {
        byte[] result = RasterizationFactory.getInstance("xlsx").rasterize(image, readFile("/data/test.xlsx"), "xlsx");

        assertNotNull(result);
    }


	private byte[] readFile(String fileName) throws IOException, URISyntaxException {
		return Files.readAllBytes(
			    Paths.get(this.getClass().getResource(fileName).toURI()));
	}

}
