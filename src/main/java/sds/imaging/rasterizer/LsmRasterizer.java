/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sds.imaging.rasterizer;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.gui.ImageWindow;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.ImageReader;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.Calibration;

import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

import org.imagearchive.lsm.reader.info.CZLSMInfo;
import org.imagearchive.lsm.reader.info.ChannelNamesAndColors;
import org.imagearchive.lsm.reader.info.ImageDirectory;
import org.imagearchive.lsm.reader.info.LSMFileInfo;
import sds.imaging.domain.core.Image;

public class LsmRasterizer implements Rasterizer {

    private final char micro = '\u00b5';
    private boolean show = true;

    private final String micrometer = micro + "m";

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

            ImagePlus res = open(inputTempFile.getAbsolutePath());
            FileSaver resSaver = new FileSaver(res);
            resSaver.saveAsBmp(outputTempFile.getAbsolutePath());

            resultImageBytes = Files.readAllBytes(outputTempFile.toPath());
            inputTempFile.delete();
            outputTempFile.delete();

            return resultImageBytes;
        } catch (IOException ex) {
            Logger.getLogger(LsmRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LsmRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(LsmRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(LsmRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(LsmRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(LsmRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(LsmRasterizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resultImageBytes;
    }

    public ImagePlus open(final String path) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        show = false;
        return open(path, true);
    }

    public ImagePlus open(final String arg, final boolean verbose) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final OpenDialog od = new OpenDialog("Open LSM image... ", arg);
        final String name = od.getFileName();
        if (name == null) {
            return null;
        }
        File file = new File(od.getDirectory(), name);
        ImagePlus imp = open(file.getParent(), file.getName(), true, false);
        if (show && !arg.equals("noshow") & imp != null) {
            imp.setPosition(1, 1, 1);
            imp.show();
            imp.updateAndDraw();

            final Class toolbox
                    = Class.forName("org.imagearchive.lsm.toolbox.gui.ImageFocusListener");
            final Constructor toolboxCon = toolbox.getConstructor(new Class[]{});
            Object o = toolboxCon.newInstance(new Object[]{});
            WindowFocusListener listener = (WindowFocusListener) o;
            final Method toolboxMet
                    = o.getClass().getMethod("windowGainedFocus",
                            new Class[]{WindowEvent.class});
            if (listener != null) {
                final ImageWindow win = imp.getWindow();
                if (win != null) {
                    win.addWindowFocusListener(listener);
                }
            }
            if (toolboxMet != null) {
                toolboxMet.invoke(o, new Object[]{null});
            }

        }
        return imp;
    }

    public ImagePlus open(final String directory, final String filename,
            final boolean verbose, final boolean thumb) throws FileNotFoundException, IOException {
        ImagePlus imp = null;
        RandomAccessFile file;
        LSMFileInfo lsm;
        file = new RandomAccessFile(new File(directory, filename), "r");
        final RandomAccessStream stream = new RandomAccessStream(file);
        lsm = new LSMFileInfo();
        lsm.fileName = filename;
        lsm.directory = directory;
        if (isLSMfile(stream)) {
            ImageDirectory imDir = readImageDirectoy(stream, 8, thumb);
            lsm.imageDirectories.add(imDir);
            while (imDir.OFFSET_NEXT_DIRECTORY != 0) {
                imDir = readImageDirectoy(stream, imDir.OFFSET_NEXT_DIRECTORY, thumb);
                lsm.imageDirectories.add(imDir);
            }
            imp = open(stream, lsm, verbose, thumb);
            stream.close();
        } else {
            IJ.error("Not a valid lsm file");
        }

        return imp;
    }

    public boolean isLSMfile(final RandomAccessStream stream) throws IOException {
        boolean identifier = false;

        stream.seek(2);
        long id = swap(stream.readShort());
        if (id == 42) {
            identifier = true;
        }

        return identifier;
    }

    private long getTagCount(final RandomAccessStream stream, final long position) throws IOException {
        stream.seek((int) position);
        long tags = swap(stream.readShort());
        return tags;
    }

    private ImageDirectory readImageDirectoy(final RandomAccessStream stream,
            final long startPosition, final boolean thumb) throws IOException {
        final ImageDirectory imDir = new ImageDirectory();
        final long tags = getTagCount(stream, startPosition);
        byte[] tag;

        final int MASK = 0x00ff;
        final long MASK2 = 0x000000ff;
        long currentTagPosition = 0;
        // needed because sometimes offset do not fit in
        // the imDir structure and are placed elsewhere
        long stripOffset = 0, stripByteOffset = 0;

        for (int i = 0; i < tags; i++) {
            currentTagPosition = startPosition + 2 + i * 12;
            tag = readTag(stream, (int) currentTagPosition);
            int tagtype = ((tag[1] & MASK) << 8) | ((tag[0] & MASK));

            switch (tagtype) {
                case 254:
                    imDir.TIF_NEWSUBFILETYPE
                            = ((tag[11] & MASK2) << 24) | ((tag[10] & MASK2) << 16)
                            | ((tag[9] & MASK2) << 8) | (tag[8] & MASK2);
                    break;
                case 256:
                    imDir.TIF_IMAGEWIDTH
                            = ((tag[11] & MASK2) << 24) | ((tag[10] & MASK2) << 16)
                            | ((tag[9] & MASK2) << 8) | (tag[8] & MASK2);
                    break;
                case 257:
                    imDir.TIF_IMAGELENGTH
                            = ((tag[11] & MASK2) << 24) | ((tag[10] & MASK2) << 16)
                            | ((tag[9] & MASK2) << 8) | (tag[8] & MASK2);
                    break;
                case 258:
                    imDir.TIF_BITSPERSAMPLE_LENGTH
                            = ((tag[7] & MASK2) << 24) | ((tag[6] & MASK2) << 16)
                            | ((tag[5] & MASK2) << 8) | (tag[4] & MASK2);
                    imDir.TIF_BITSPERSAMPLE_CHANNEL[0] = ((tag[8] & MASK2));
                    imDir.TIF_BITSPERSAMPLE_CHANNEL[1] = ((tag[9] & MASK2));
                    imDir.TIF_BITSPERSAMPLE_CHANNEL[2] = ((tag[10] & MASK2));
                    break;
                case 259:
                    imDir.TIF_COMPRESSION = ((tag[8] & MASK2));
                    break;
                case 262:
                    imDir.TIF_PHOTOMETRICINTERPRETATION = ((tag[8] & MASK2));
                    break;
                case 273:
                    imDir.TIF_STRIPOFFSETS_LENGTH
                            = ((tag[7] & MASK2) << 24) | ((tag[6] & MASK2) << 16)
                            | ((tag[5] & MASK2) << 8) | (tag[4] & MASK2);
                    stripOffset
                            = ((tag[11] & MASK2) << 24) | ((tag[10] & MASK2) << 16)
                            | ((tag[9] & MASK2) << 8) | (tag[8] & MASK2);
                    break;
                case 277:
                    imDir.TIF_SAMPLESPERPIXEL = ((tag[8] & MASK2));
                    break;
                case 279:
                    imDir.TIF_STRIPBYTECOUNTS_LENGTH
                            = ((tag[7] & MASK2) << 24) | ((tag[6] & MASK2) << 16)
                            | ((tag[5] & MASK2) << 8) | (tag[4] & MASK2);
                    stripByteOffset
                            = ((tag[11] & MASK2) << 24) | ((tag[10] & MASK2) << 16)
                            | ((tag[9] & MASK2) << 8) | (tag[8] & MASK2);
                    break;
                case 317:
                    imDir.TIF_PREDICTOR = ((tag[8] & MASK2));
                    break;
                case 34412:
                    imDir.TIF_CZ_LSMINFO_OFFSET
                            = ((tag[11] & MASK2) << 24) | ((tag[10] & MASK2) << 16)
                            | ((tag[9] & MASK2) << 8) | (tag[8] & MASK2);
                    break;
                default:
                    break;
            }
        }
        imDir.TIF_STRIPOFFSETS = new long[(int) imDir.TIF_STRIPOFFSETS_LENGTH];
        if (imDir.TIF_STRIPOFFSETS_LENGTH == 1) {
            imDir.TIF_STRIPOFFSETS[0]
                    = stripOffset;
        } else {
            imDir.TIF_STRIPOFFSETS
                    = getIntTable(stream, stripOffset, (int) imDir.TIF_STRIPOFFSETS_LENGTH);
        }
        imDir.TIF_STRIPBYTECOUNTS
                = new long[(int) imDir.TIF_STRIPBYTECOUNTS_LENGTH];
        if (imDir.TIF_STRIPBYTECOUNTS_LENGTH == 1) {
            imDir.TIF_STRIPBYTECOUNTS[0]
                    = stripByteOffset;
        } else {
            imDir.TIF_STRIPBYTECOUNTS
                    = getIntTable(stream, stripByteOffset,
                            (int) imDir.TIF_STRIPBYTECOUNTS_LENGTH);
        }

        stream.seek((int) (currentTagPosition + 12));
        final int offset_next_directory = swap(stream.readInt());
        imDir.OFFSET_NEXT_DIRECTORY = offset_next_directory;

        if (imDir.TIF_CZ_LSMINFO_OFFSET != 0) {
            imDir.TIF_CZ_LSMINFO
                    = getCZ_LSMINFO(stream, imDir.TIF_CZ_LSMINFO_OFFSET, thumb);
        }
        return imDir;
    }

    private byte[] readTag(final RandomAccessStream stream, final int position) throws IOException {
        final byte[] tag = new byte[12];
        stream.seek(position);
        stream.readFully(tag);
        return tag;
    }

    private long[] getIntTable(final RandomAccessStream stream,
            final long position, final int count) throws IOException {
        final long[] offsets = new long[count];

        stream.seek((int) position);
        for (int i = 0; i < count; i++) {
            offsets[i] = swap(stream.readInt());
        }

        return offsets;

    }

    private CZLSMInfo getCZ_LSMINFO(final RandomAccessStream stream,
            final long position, final boolean thumb) throws IOException {
        final CZLSMInfo cz = new CZLSMInfo();

        if (position == 0) {
            return cz;
        }
        stream.seek((int) position + 8);
        cz.DimensionX = swap(stream.readInt());
        cz.DimensionY = swap(stream.readInt());
        cz.DimensionZ = swap(stream.readInt());

        // number of channels
        cz.DimensionChannels = swap(stream.readInt());
        // Timestack size
        cz.DimensionTime = swap(stream.readInt());

        cz.IntensityDataType = swap(stream.readInt());

        cz.ThumbnailX = swap(stream.readInt());
        cz.ThumbnailY = swap(stream.readInt());
        cz.VoxelSizeX = swap(stream.readDouble());
        cz.VoxelSizeY = swap(stream.readDouble());
        cz.VoxelSizeZ = swap(stream.readDouble());

        stream.seek((int) position + 88);
        cz.ScanType = swap(stream.readShort());
        stream.seek((int) position + 108);
        cz.OffsetChannelColors = swap(stream.readInt());
        stream.seek((int) position + 120);
        cz.OffsetChannelDataTypes = swap(stream.readInt());
        stream.seek((int) position + 264);
        cz.DimensionP = swap(stream.readInt());
        if (cz.DimensionP < 1) {
            cz.DimensionP = 1;
        }
        // not used so far...
        cz.DimensionM = swap(stream.readInt());
        if (cz.DimensionM < 1) {
            cz.DimensionM = 1;
        }

        if (cz.OffsetChannelDataTypes != 0) {
            cz.OffsetChannelDataTypesValues
                    = getOffsetChannelDataTypesValues(stream, cz.OffsetChannelDataTypes,
                            cz.DimensionChannels);
        }
        if (cz.OffsetChannelColors != 0) {
            final ChannelNamesAndColors channelNamesAndColors
                    = getChannelNamesAndColors(stream, cz.OffsetChannelColors,
                            cz.DimensionChannels);
            cz.channelNamesAndColors = channelNamesAndColors;
        }

        return cz;
    }

    private int[] getOffsetChannelDataTypesValues(
            final RandomAccessStream stream, final long position,
            final long channelCount) throws IOException {
        final int[] OffsetChannelDataTypesValues = new int[(int) channelCount];

        stream.seek((int) position);

        for (int i = 0; i < channelCount; i++) {
            OffsetChannelDataTypesValues[i] = swap(stream.readInt());
        }

        return OffsetChannelDataTypesValues;
    }

    private ChannelNamesAndColors getChannelNamesAndColors(
            final RandomAccessStream stream, final long position,
            final long channelCount) throws IOException {
        final ChannelNamesAndColors channelNamesAndColors
                = new ChannelNamesAndColors();

        stream.seek((int) position);
        channelNamesAndColors.BlockSize = swap(stream.readInt());
        channelNamesAndColors.NumberColors = swap(stream.readInt());
        channelNamesAndColors.NumberNames = swap(stream.readInt());
        channelNamesAndColors.ColorsOffset = swap(stream.readInt());
        channelNamesAndColors.NamesOffset = swap(stream.readInt());
        channelNamesAndColors.Mono = swap(stream.readInt());
        // reserved 4 words
        stream.seek((int) channelNamesAndColors.NamesOffset + (int) position);
        channelNamesAndColors.ChannelNames = new String[(int) channelCount];
        // long Namesize = channelNamesAndColors.BlockSize-
        // channelNamesAndColors.NamesOffset;
        for (int j = 0; j < channelCount; j++) {
            final long size = swap(stream.readInt());
            channelNamesAndColors.ChannelNames[j]
                    = readSizedNULLASCII(stream, size);
        }
        stream.seek((int) channelNamesAndColors.ColorsOffset + (int) position);
        channelNamesAndColors.Colors
                = new int[(int) (channelNamesAndColors.NumberColors)];

        for (int j = 0; j < (int) (channelNamesAndColors.NumberColors); j++) {
            channelNamesAndColors.Colors[j] = swap(stream.readInt());
        }

        return channelNamesAndColors;
    }

    public String readSizedNULLASCII(final RandomAccessStream stream,
            final long s) throws IOException {
        int offset = 0;
        String tempstr = "";
        int in;
        char ch;
        boolean addchar = true;

        while (offset < s) {
            in = stream.read();
            if (in == -1) {
                break;
            }
            ch = (char) in;
            if (addchar == true) {
                final String achar = new Character(ch).toString();
                if (ch != 0x00) {
                    tempstr += achar;
                } else {
                    addchar = false;
                }
            }
            offset++;
        }

        return tempstr;
    }

    /*
	 * apply_colors, applies color gradient; function taken out from Lut_Panel
	 * plugin
     */
    private void applyColors(final ImagePlus imp, final int channel,
            final Color[] gc, final int i) {
        final FileInfo fi = new FileInfo();
        final int size = 256;
        fi.reds = new byte[size];
        fi.greens = new byte[size];
        fi.blues = new byte[size];
        fi.lutSize = size;
        float nColorsfl;
        final float interval = size;
        float iR = gc[0].getRed();
        float iG = gc[0].getGreen();
        float iB = gc[0].getBlue();
        float idR = gc[1].getRed() - gc[0].getRed();
        float idG = gc[1].getGreen() - gc[0].getGreen();
        float idB = gc[1].getBlue() - gc[0].getBlue();
        idR = (idR / interval);
        idG = (idG / interval);
        idB = (idB / interval);
        int a;
        for (a = (int) (interval * 0); a < (int) (interval * (0) + interval); a++, iR
                += idR, iG += idG, iB += idB) {
            fi.reds[a] = (byte) (iR);
            fi.greens[a] = (byte) (iG);
            fi.blues[a] = (byte) (iB);
        }
        final int b = (int) (interval * 0 + interval) - 1;
        fi.reds[b] = (byte) (gc[1].getRed());
        fi.greens[b] = (byte) (gc[1].getGreen());
        fi.blues[b] = (byte) (gc[1].getBlue());
        nColorsfl = size;
        if (nColorsfl > 0) {
            if (nColorsfl < size) {
                interpolate(size, fi.reds, fi.greens, fi.blues,
                        (int) nColorsfl);
            }
            showLut(imp, channel, fi, true);
        }
    }

    /*
	 * interpolate, modified from the ImageJ method by Wayne Rasband.
     */
    private void interpolate(final int size, final byte[] reds,
            final byte[] greens, final byte[] blues, final int nColors) {
        final byte[] r = new byte[nColors];
        final byte[] g = new byte[nColors];
        final byte[] b = new byte[nColors];
        System.arraycopy(reds, 0, r, 0, nColors);
        System.arraycopy(greens, 0, g, 0, nColors);
        System.arraycopy(blues, 0, b, 0, nColors);
        final double scale = nColors / (float) size;
        int i1, i2;
        double fraction;
        for (int i = 0; i < size; i++) {
            i1 = (int) (i * scale);
            i2 = i1 + 1;
            if (i2 == nColors) {
                i2 = nColors - 1;
            }
            fraction = i * scale - i1;
            reds[i]
                    = (byte) ((1.0 - fraction) * (r[i1] & 255) + fraction * (r[i2] & 255));
            greens[i]
                    = (byte) ((1.0 - fraction) * (g[i1] & 255) + fraction * (g[i2] & 255));
            blues[i]
                    = (byte) ((1.0 - fraction) * (b[i1] & 255) + fraction * (b[i2] & 255));
        }
    }

    /*
	 * showLut, applies the new Lut on the actual image
     */
    private void showLut(final ImagePlus imp, final int channel,
            final FileInfo fi, final boolean showImage) {
        if (imp != null) {
            if (imp.getType() == ImagePlus.COLOR_RGB) {
                IJ
                        .error("Color tables cannot be assiged to RGB Images.");
            } else {
                IndexColorModel cm = new IndexColorModel(8, 256, fi.reds, fi.greens, fi.blues);
                imp.setPosition(channel + 1, imp.getSlice(), imp.getFrame());
                if (imp.isComposite()) {
                    ((CompositeImage) imp).setChannelColorModel(cm);
                    ((CompositeImage) imp).updateChannelAndDraw();
                } else {
                    imp.getProcessor().setColorModel(cm);
                    imp.updateAndDraw();
                }
            }
        }
    }

    public ImagePlus open(final RandomAccessStream stream,
            final LSMFileInfo lsmFi, final boolean verbose, final boolean thumb) throws IOException {
        final ImageDirectory firstImDir
                = (ImageDirectory) lsmFi.imageDirectories.get(0);
        if (firstImDir == null) {
            if (verbose) {
                IJ.error("LSM ImageDir null.");
            }
            return null;
        } // should not be if it is a true LSM file

        final CZLSMInfo cz = (CZLSMInfo) firstImDir.TIF_CZ_LSMINFO;
        if (cz == null) {
            if (verbose) {
                IJ.error("LSM ImageDir null.");
            }
            return null;
        } // should not be, first Directory should have a CZ...

        ImagePlus imps = null;
        switch (cz.ScanType) {
            case 0:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            case 1:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            case 2:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            case 3:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            case 4:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            case 5:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            case 6:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            case 10:
                imps = readStack(stream, lsmFi, cz, thumb);
                return imps;
            default:
                if (verbose) {
                    IJ.error("Unsupported LSM scantype: " + cz.ScanType);
                }
                break;
        }
        return imps;
    }

    private ImagePlus readStack(final RandomAccessStream stream,
            final LSMFileInfo lsmFi, final CZLSMInfo cz, final boolean thumb) throws IOException {
        ImageDirectory firstImDir = (ImageDirectory) lsmFi.imageDirectories.get(0);
        lsmFi.url = "";
        lsmFi.fileFormat = FileInfo.TIFF;
        lsmFi.pixelDepth = cz.VoxelSizeZ * 1000000;
        lsmFi.pixelHeight = cz.VoxelSizeY * 1000000;
        lsmFi.pixelWidth = cz.VoxelSizeX * 1000000;
        lsmFi.unit = micrometer;
        lsmFi.valueUnit = micrometer;
        lsmFi.nImages = 1;
        lsmFi.intelByteOrder = true;

        ImageStack st;
        int datatype = (int) cz.IntensityDataType;
        if (datatype == 0) {
            datatype = cz.OffsetChannelDataTypesValues[0];
        }
        switch (datatype) {
            case 1:
                lsmFi.fileType = FileInfo.GRAY8;
                break;
            case 2:
                lsmFi.fileType = FileInfo.GRAY16_UNSIGNED;
                break;
            case 3:
                lsmFi.fileType = FileInfo.GRAY16_UNSIGNED;
                break;
            case 5:
                lsmFi.fileType = FileInfo.GRAY32_FLOAT;
                break;
            default:
                lsmFi.fileType = FileInfo.GRAY8;
                break;
        }

        ColorModel cm;

        if (lsmFi.fileType == FileInfo.COLOR8 && lsmFi.lutSize > 0) {
            cm = new IndexColorModel(8, lsmFi.lutSize, lsmFi.reds, lsmFi.greens, lsmFi.blues);
        } else {
            cm = LookUpTable.createGrayscaleColorModel(lsmFi.whiteIsZero);
        }

        if (!thumb) {
            st = new ImageStack((int) firstImDir.TIF_IMAGEWIDTH, (int) firstImDir.TIF_IMAGELENGTH, cm);
        } else {
            st = new ImageStack((int) cz.ThumbnailX, (int) cz.ThumbnailY, cm);
        }

        ImageReader reader;
        int flength = 0;
        lsmFi.stripOffsets = new int[1];
        lsmFi.stripLengths = new int[1];
        for (int imageCounter = 0; imageCounter < lsmFi.imageDirectories.size(); imageCounter++) {
            final ImageDirectory imDir
                    = (ImageDirectory) lsmFi.imageDirectories.get(imageCounter);
            for (int i = 0; i < imDir.TIF_STRIPBYTECOUNTS.length; i++) {
                if (imDir.TIF_COMPRESSION == 5) {
                    lsmFi.compression = FileInfo.LZW;
                    flength
                            = (int) new File(lsmFi.directory
                                    + System.getProperty("file.separator") + lsmFi.fileName).length();
                    if (imDir.TIF_PREDICTOR == 2) {
                        lsmFi.compression
                                = FileInfo.LZW_WITH_DIFFERENCING;
                    }
                } else {
                    lsmFi.compression = 0;
                }
            }

            if (!thumb && imDir.TIF_NEWSUBFILETYPE == 0) {
                lsmFi.width = (int) imDir.TIF_IMAGEWIDTH;
                lsmFi.height = (int) imDir.TIF_IMAGELENGTH;
                Object pixels;
                for (int channelCount = 0; channelCount < (int) (cz.DimensionChannels); channelCount++) {
                    datatype = (int) cz.IntensityDataType;
                    if (datatype == 0) {
                        datatype
                                = cz.OffsetChannelDataTypesValues[channelCount];
                    }
                    switch (datatype) {
                        case 1:
                            lsmFi.fileType = FileInfo.GRAY8;
                            break;
                        case 2:
                            lsmFi.fileType = FileInfo.GRAY16_UNSIGNED;
                            break;
                        case 3:
                            lsmFi.fileType = FileInfo.GRAY16_UNSIGNED;
                            break;
                        case 5:
                            lsmFi.fileType = FileInfo.GRAY32_FLOAT;
                            break;
                        default:
                            lsmFi.fileType = FileInfo.GRAY8;
                            break;
                    }
                    lsmFi.stripLengths[0] = (int) imDir.TIF_STRIPBYTECOUNTS[channelCount];
                    lsmFi.stripOffsets[0] = (int) imDir.TIF_STRIPOFFSETS[channelCount];
                    reader = new ImageReader(lsmFi);
                    if (channelCount < imDir.TIF_STRIPOFFSETS_LENGTH) {

                        if (lsmFi.stripLengths[0] + lsmFi.stripOffsets[0] > flength) {
                            lsmFi.stripLengths[0] = flength - lsmFi.stripOffsets[0];
                        }
                        // System.err.println("ImageCounter:"+imageCounter+
                        // lsmFi.stripLengths[0]+" "+lsmFi.stripOffsets[0]);

                        stream.seek(lsmFi.stripOffsets[0]);

                        pixels = reader.readPixels(stream);
                        st.addSlice("", pixels);
                    }
                }
            } else if (thumb && imDir.TIF_NEWSUBFILETYPE == 1) { // ONLY IF
                // THUMBS
                lsmFi.width = (int) imDir.TIF_IMAGEWIDTH;
                lsmFi.height = (int) imDir.TIF_IMAGELENGTH;

                reader = new ImageReader(lsmFi);
                Object pixels;
                int channels = 1; // only read the first channel for the thumbs.
                // speed!
                for (int channelCount = 0; channelCount < channels; channelCount++) {
                    lsmFi.stripLengths[0] = (int) imDir.TIF_STRIPBYTECOUNTS[channelCount];
                    lsmFi.stripOffsets[0] = (int) imDir.TIF_STRIPOFFSETS[channelCount];
                    if (channelCount < imDir.TIF_STRIPOFFSETS_LENGTH) {

                        stream.seek(lsmFi.stripOffsets[0]);

                        pixels = reader.readPixels(stream);
                        st.addSlice("", pixels);
                    }
                }
                imageCounter = lsmFi.imageDirectories.size(); // break out of
                // for loop,
                // speed
            }
        }
        IJ.showProgress(1.0);
        ImagePlus imp = new ImagePlus(lsmFi.fileName, st);
        // this is a hack, cast Positions as Timepoints
        imp.setDimensions((int) cz.DimensionChannels, (int) cz.DimensionZ,
                (int) (cz.DimensionTime * cz.DimensionP));
        if (cz.DimensionChannels >= 2
                && (imp.getStackSize() % cz.DimensionChannels) == 0) {
            imp = new CompositeImage(imp, CompositeImage.COLOR);
        }
        imp.setFileInfo(lsmFi);
        final Calibration cal = new Calibration();
        cal.setUnit(lsmFi.unit);
        cal.pixelDepth = lsmFi.pixelDepth;
        cal.pixelHeight = lsmFi.pixelHeight;
        cal.pixelWidth = lsmFi.pixelWidth;
        imp.setCalibration(cal);
        final Color[] color = new Color[2];
        color[0] = new Color(0, 0, 0);
        for (int channel = 0; channel < (int) cz.DimensionChannels; channel++) {
            final int r = cz.channelNamesAndColors.Colors[channel] & 255;
            final int g = (cz.channelNamesAndColors.Colors[channel] >> 8) & 255;
            final int b = (cz.channelNamesAndColors.Colors[channel] >> 16) & 255;
            color[1] = new Color(r, g, b);
            if (r == 0 && g == 0 && b == 0) {
                color[1] = Color.white;
            }
            applyColors(imp, channel, color, 2);
        }
        if (imp.getOriginalFileInfo().fileType == FileInfo.GRAY16_UNSIGNED) {
            final double min = imp.getProcessor().getMin();
            final double max = imp.getProcessor().getMax();
            imp.getProcessor().setMinAndMax(min, max);
        }

        int stackPosition = 1;
        for (int i = 1; i <= cz.DimensionTime; i++) {
            for (int j = 1; j <= cz.DimensionZ; j++) {
                for (int k = 1; k <= cz.DimensionChannels; k++) {
                    // imp.setPosition(k, j, i);
                    // int stackPosition = imp.getCurrentSlice();
                    if (stackPosition <= imp.getStackSize()) {
                        final String label = cz.channelNamesAndColors.ChannelNames[k - 1];
                        st.setSliceLabel(label, stackPosition++);
                    }
                }
            }
        }

        setInfo(imp, lsmFi);
        return imp;
    }

    public ImagePlus setInfo(final ImagePlus imp, final LSMFileInfo lsm) {
        final ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
        if (imDir == null) {
            return null;
        }
        final CZLSMInfo cz = (CZLSMInfo) imDir.TIF_CZ_LSMINFO;

        final String stacksize = IJ.d2s(cz.DimensionZ, 0);
        final String width = IJ.d2s(lsm.width, 0);
        final String height = IJ.d2s(lsm.height, 0);
        final String channels = IJ.d2s(cz.DimensionChannels, 0);
        String scantype;
        switch (cz.ScanType) {
            case 0:
                scantype = "Normal X-Y-Z scan";
                break;
            case 1:
                scantype = "Z scan";
                break;
            case 2:
                scantype = "Line scan";
                break;
            case 3:
                scantype = "Time series X-Y";
                break;
            case 4:
                scantype = "Time series X-Y";
                break;
            case 5:
                scantype = "Time series - Means of ROIs";
                break;
            case 6:
                scantype = "Time series - X-Y-Z";
                break;
            case 10:
                scantype = "Point mode";
                break;
            default:
                scantype = "UNKNOWN !";
                break;
        }

        final String voxelsize_x
                = IJ.d2s(cz.VoxelSizeX * 1000000, 2) + " " + micrometer;
        final String voxelsize_y
                = IJ.d2s(cz.VoxelSizeY * 1000000, 2) + " " + micrometer;
        final String voxelsize_z
                = IJ.d2s(cz.VoxelSizeZ * 1000000, 2) + " " + micrometer;
        final String timestacksize = IJ.d2s(cz.DimensionTime, 0);
        final String positionssize = IJ.d2s(cz.DimensionP, 0);
        final String mosaicsize = IJ.d2s(cz.DimensionM, 0);

        final String plane_width
                = IJ.d2s(cz.DimensionX * cz.VoxelSizeX, 2) + " " + micrometer;
        final String plane_height
                = IJ.d2s(cz.DimensionY * cz.VoxelSizeY, 2) + " " + micrometer;
        final String volume_depth
                = IJ.d2s(cz.DimensionZ * cz.VoxelSizeZ, 2) + " " + micrometer;

        String infos = "Filename: " + lsm.fileName + "\n";
        infos += "Width: " + width + "\n";
        infos += "Height: " + height + "\n";
        infos += "Channels: " + channels + "\n";
        infos += "Z_size:" + stacksize + "\n";
        infos += "T_size: " + timestacksize + "\n";
        infos += "P_size: " + positionssize + "\n";
        infos += "M_size: " + mosaicsize + "\n";
        infos += "Scan_type: " + scantype + "\n";
        infos += "Voxel_size_X: " + voxelsize_x + "\n";
        infos += "Voxel_size_Y: " + voxelsize_y + "\n";
        infos += "Voxel_size_Z: " + voxelsize_z + "\n";
        infos += "Plane_width: " + plane_width + "\n";
        infos += "Plane_height: " + plane_height + "\n";
        infos += "Plane_depth: " + volume_depth + "\n";
        imp.setProperty("Info", infos);
        return imp;
    }

    /**
     * ***************************************************************************
     * ****************
     */
    private short swap(final short x) {
        return (short) ((x << 8) | ((x >> 8) & 0xff));
    }

    private char swap(final char x) {
        return (char) ((x << 8) | ((x >> 8) & 0xff));
    }

    private int swap(final int x) {
        return (swap((short) x) << 16) | (swap((short) (x >> 16)) & 0xffff);
    }

    private long swap(final long x) {
        return ((long) swap((int) (x)) << 32)
                | (swap((int) (x >> 32)) & 0xffffffffL);
    }

    private float swap(final float x) {
        return Float.intBitsToFloat(swap(Float.floatToIntBits(x)));
    }

    private double swap(final double x) {
        return Double.longBitsToDouble(swap(Double.doubleToLongBits(x)));
    }

}
