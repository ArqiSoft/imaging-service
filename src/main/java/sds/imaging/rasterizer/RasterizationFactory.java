package sds.imaging.rasterizer;

import java.util.Locale;

public class RasterizationFactory {

	public static Rasterizer getInstance(String extension) {
            extension = extension.toLowerCase(Locale.getDefault());

            switch (extension) {
            case "cif":
                return new CifRasterizer();

            case "jpg":
            case "jpeg":
            case "png":
            case "bmp":
            case "gif":
            case "tif":
            case "tiff":
            case "svg":
            case "ico":
                return new ImageRasterizer();

            case "rxn":
                return new ReactionRasterizer();

            case "mol":
            case "cdx":
                return new StructureRasterizer();

            case "pdf":
                return new PdfRasterizer();

            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
            case "ods":
            case "odt":
                return new OfficeRasterizer(OfficeConverterFactory.getInstance(extension));

            default:
                throw new AssertionError(String.format("Unsupported file format: %s", extension));
            }
	}
	
	public static String getMimeType(String extension) {
            if (extension == null || extension.isEmpty()) {
                throw new IllegalArgumentException("extension");
            }

            switch (extension) {
                case "bmp": return "image/bmp";
                case "emf": return "image/emf";
                case "gif": return "image/gif";
                case "ico": return "image/x-icon";
                case "icon": return "image/vnd.microsoft.icon";
                case "jpeg": return "image/jpeg";
                case "jpg": return "image/jpeg";
                case "png": return "image/png";
                case "tiff": return "image/tiff";
                case "wmf": return "image/wmf";
                case "svg": return "image/svg+xml";

                default: return "application/octet-stream";
            }
	}

}
