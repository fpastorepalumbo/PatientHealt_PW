package unisa.diem.dicom;

import com.pixelmed.dicom.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handling of DICOM files
 */
public class DicomHandle {

    private static final Logger logger = Logger.getLogger(DicomHandle.class.getName()); // Object for generating log messages
    private final AttributeList attrs; // A tag in the format of the library
    private int rows;
    private int columns;
    private boolean isWord; // Determines whether the pixel data is stored as a word (16-bit) or a byte (8-bit)
    private boolean isRGB; // Determines whether the pixel data is RGB or grayscale
    private short[] shortPixelData; // Stores the data for word pixel data
    private byte[] bytePixelData; // Stores the data for byte pixel data

    public DicomHandle(File file) throws IOException, DicomException {
        attrs = read(file.getAbsolutePath());
        loadEssentialData();
    }

    /**
     * Reads the attributes of a DICOM file, populating the attribute list
     */
    private AttributeList read(String filePath) throws IOException, DicomException {
        File dicomFile = new File(filePath);
        DicomInputStream dicomInputStream;
        if (!dicomFile.exists())
            throw new FileNotFoundException("File not found: " + filePath);
        dicomInputStream = new DicomInputStream(dicomFile);
        AttributeList attributeList = new AttributeList();
        attributeList.read(dicomInputStream);
        dicomInputStream.close();
        return attributeList;
    }

    /**
     * Loads the essential data from the attribute list
     */
    private void loadEssentialData() throws DicomException {
        assert attrs != null;
        rows = attrs.get(TagFromName.Rows).getIntegerValues()[0];
        columns = attrs.get(TagFromName.Columns).getIntegerValues()[0];
        Attribute photometricAttr = attrs.get(TagFromName.PhotometricInterpretation);
        isRGB = photometricAttr.getSingleStringValueOrEmptyString().equals("RGB");
        Attribute pixelDataAttr = attrs.get(TagFromName.PixelData);
        String pixelDataVR = pixelDataAttr.getVRAsString();
        isWord = pixelDataVR.equals("OW");
        if (isWord)
            shortPixelData = pixelDataAttr.getShortValues();
        else
            bytePixelData = pixelDataAttr.getByteValues();
    }

    /**
     * Returns a string representation of the attribute list
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        assert attrs != null;
        attrs.forEach((key, value) -> {
            DicomDictionary dict = AttributeList.getDictionary();
            sb.append(key);
            sb.append(" ");
            sb.append(dict.getNameFromTag(key));
            sb.append(": [");
            sb.append(value.getVRAsString());
            sb.append(", ");
            sb.append(value.getVL());
            sb.append("] ");
            sb.append(value.getSingleStringValueOrEmptyString());
            sb.append("\n");
        });

        return sb.toString();
    }

    /**
     * Returns values for an attribute tag as a String array
     */
    public String[] getStringValues(AttributeTag tag) {
        assert attrs != null;
        try {
            Attribute attr = attrs.get(tag);
            if (attr == null)
                return null;
            return attr.getStringValues();
        } catch (DicomException e) {
            logger.severe("Error reading DICOM attribute " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the first value for an attribute tag as a string
    */
     public String getFirstStringValue(AttributeTag tag) {
        String[] strings = getStringValues(tag);
        if (strings == null)
            return null;
        return strings[0];
    }

    /**
     * Returns values for an attribute tag as an Int array
     */
    public int[] getIntValues(AttributeTag tag) {
        assert attrs != null;
        try {
            return attrs.get(tag).getIntegerValues();
        } catch (DicomException e) {
            logger.severe("Error reading DICOM attribute " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the first value for an attribute tag as an integer
     */
    public int getFirstIntValue(AttributeTag tag) {
        return getIntValues(tag)[0];
    }

    /**
     * Determines if the attribute list contains the attribute tag
     */
    public boolean hasAttr(AttributeTag tag) {
        assert attrs != null;
        return attrs.get(tag) != null;
    }

    /**
     * Creates the image frame as a DicomFrame object
     */
    private DicomFrame getFrame(int index) {
        try {
            int start, end;
            Object pxs;
            if (isRGB) {
                start = index * rows * columns * 3;
                end = start + rows * columns * 3;
            } else {
                start = index * rows * columns;
                end = start + rows * columns;
            }
            if (isWord)
                pxs = Arrays.copyOfRange(shortPixelData, start, end);
            else
                pxs = Arrays.copyOfRange(bytePixelData, start, end);
            return new DicomFrame(columns, rows, isWord, pxs);
        } catch (IOException e) {
            logger.severe("Error reading DICOM attribute list");
            return null;
        }
    }

    /**
     * Returns the image frame as a base64 string for interoperability
     */
    public String getFrameBase64(int index) {
        DicomFrame frame = getFrame(index);
        assert frame != null;
        return frame.toBase64();
    }

    /**
     * Returns a list frames as base64 strings for interoperability
     */
    public List<String> getFramesBase64(int start, int end) {
        List<String> frames = new ArrayList<>();
        for (int i = start; i < end; i++)
            frames.add(getFrameBase64(i));
        return frames;
    }

    /**
     * Returns a list of all the frames as base64strings for interoperability
     */
    public List<String> getAllFramesBase64() {
        int numFrames = getFirstIntValue(TagFromName.NumberOfFrames);
        return getFramesBase64(0, numFrames);
    }

    /**
     * Handling of DICOM frames
     */
    private static class DicomFrame {

        byte[] data;

        /**
         * Creates a DicomFrame object from pixel data, whether the data is stored as a word or a byte
         */
        public DicomFrame(int w, int h, boolean isWord, Object pxs) throws IOException {
            if (!isWord) {
                if (!(pxs instanceof byte[] bytes))
                    throw new IllegalArgumentException("Expected byte[] for byte data, got " + pxs.getClass().getName());
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
                image.getRaster().setDataElements(0, 0, w, h, bytes);
                ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
                ImageIO.write(image, "png", bAOS);
                data = bAOS.toByteArray();
            } else {
                if (!(pxs instanceof short[] shorts))
                    throw new IllegalArgumentException("Expected short[] for word data, got " + pxs.getClass().getName());
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
                image.getRaster().setDataElements(0, 0, w, h, shorts);
                ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
                ImageIO.write(image, "png", bAOS);
                data = bAOS.toByteArray();
            }
        }

        /**
         * Converts the pixel data to a base64 string
         */
        public String toBase64() {
            return Base64.getEncoder().encodeToString(data);
        }
    }
}
