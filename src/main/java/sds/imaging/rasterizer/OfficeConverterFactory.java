package sds.imaging.rasterizer;

import java.util.Locale;

import sds.officeprocessor.converters.DocToPdf;
import sds.officeprocessor.converters.DocxToPdf;
import sds.officeprocessor.converters.IConvert;
import sds.officeprocessor.converters.OdsToPdf;
import sds.officeprocessor.converters.OdtToPdf;
import sds.officeprocessor.converters.PptToPdf;
import sds.officeprocessor.converters.PptxToPdf;
import sds.officeprocessor.converters.XlsToPdf;
import sds.officeprocessor.converters.XlsxToPdf;

public class OfficeConverterFactory {
    public static IConvert getInstance(String extension) {
        extension = extension.toLowerCase(Locale.getDefault());

        switch (extension) {
        case "doc":
            return new DocToPdf();
        case "docx":
            return new DocxToPdf();
        case "xls":
            return new XlsToPdf();
        case "xlsx":
            return new XlsxToPdf();
        case "ppt":
            return new PptToPdf();
        case "pptx":
            return new PptxToPdf();
        case "ods":
            return new OdsToPdf();
        case "odt":
            return new OdtToPdf();

        default:
            throw new AssertionError(String.format("Unsupported file format: %s", extension));
        }
    }

}
