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
 * Object for handling DICOM files and their metadata
 */
public class DicomHandle {

    private static final Logger logger = Logger.getLogger(DicomHandle.class.getName());
    private final AttributeList attrs; //tag nel formato della libreria
    private int numRows;
    private int numCols;
    private boolean isWord; //byte rappr
    private boolean isRGB;
    private short[] shortPixelData;
    private byte[] bytePixelData;

    public DicomHandle(File file) throws IOException, DicomException {
        attrs = read(file.getAbsolutePath());
        loadEssentialData();
    }

    /**
     * Reads a DICOM file and returns its attribute list
     * legge tutti gli attributi e va a popolare la attribute list
     * @param filePath path to the DICOM file
     * @return attribute list
     * @throws IOException    if the file is not found
     * @throws DicomException if the file is not a valid DICOM file
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
     * Loads essential and frequently-used data from the attribute list
     *
     * @throws DicomException if the attribute list is not valid
     */
    private void loadEssentialData() throws DicomException {
        assert attrs != null;

        numRows = attrs.get(TagFromName.Rows).getIntegerValues()[0];
        numCols = attrs.get(TagFromName.Columns).getIntegerValues()[0];

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
     * Returns a string representation of the attribute list in the form
     * <pre>KEY NAME: [VR, VL] STRING_VALUE</pre>
     * If a string representation for a value cannot be provided (i.e. binary values), the value field is left blank
     *
     * @return string representation of the attribute list
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
     * Returns values for a given attribute tag as a string array
     * If the attribute is not present, or if it is not a string attribute, returns null
     *
     * @param tag attribute tag
     * @return string array of values
     */
    public String[] getStrings(AttributeTag tag) {
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
     * Returns the first value for a given attribute tag as a string
     *
     * @param tag attribute tag
     * @return string value
     */
    public String getString(AttributeTag tag) {
        String[] strings = getStrings(tag);
        if (strings == null)
            return null;
        return strings[0];
    }

    /**
     * Returns values for a given attribute tag as an integer array
     * If the attribute is not present, or if it is not an integer attribute, returns null
     *
     * @param tag attribute tag
     * @return integer array of values
     */
    public int[] getInts(AttributeTag tag) {
        assert attrs != null;
        try {
            return attrs.get(tag).getIntegerValues();
        } catch (DicomException e) {
            logger.severe("Error reading DICOM attribute " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the first value for a given attribute tag as an integer
     *
     * @param tag attribute tag
     * @return integer value
     */
    public int getInt(AttributeTag tag) {
        return getInts(tag)[0];
    }

    /**
     * Tells if the attribute list contains a given attribute tag
     *
     * @param tag attribute tag
     * @return true if the attribute list contains the given tag, false otherwise
     */
    public boolean hasAttr(AttributeTag tag) {
        assert attrs != null;
        return attrs.get(tag) != null;
    }

    /**
     * Returns the frame at the given index as a DicomFrame object
     *
     * @param index frame index
     * @return DicomFrame object
     */
    private DicomFrame getFrame(int index) {
        try {
            int start, end;
            Object pxs;

            if (isRGB) {
                start = index * numRows * numCols * 3;
                end = start + numRows * numCols * 3;
            } else {
                start = index * numRows * numCols;
                end = start + numRows * numCols;
            }

            if (isWord)
                pxs = Arrays.copyOfRange(shortPixelData, start, end);
            else
                pxs = Arrays.copyOfRange(bytePixelData, start, end);

            return new DicomFrame(numCols, numRows, isWord, pxs);
        } catch (IOException e) {
            logger.severe("Error reading DICOM attribute list");
            return null;
        }
    }

    /**
     * Returns the frame at the given index as a base64-encoded string
     *
     * @param index frame index
     * @return base64-encoded string
     */
    public String serveFrame(int index) {
        DicomFrame frame = getFrame(index);
        assert frame != null;
        return frame.toBase64();
    }

    /**
     * Returns a list of base64-encoded strings for the frames in the given range
     *
     * @param start start index
     * @param end   end index
     * @return list of base64-encoded strings
     */
    public List<String> serveFrames(int start, int end) {
        List<String> frames = new ArrayList<>();
        for (int i = start; i < end; i++) {
            frames.add(serveFrame(i));
        }
        return frames;
    }

    /**
     * Returns a list of base64-encoded strings for all the frames
     *
     * @return list of base64-encoded strings
     */
    public List<String> serveAllFrames() {
        int numFrames = getInt(TagFromName.NumberOfFrames);
        return serveFrames(0, numFrames);
    }

    /**
     * Object for handling DICOM frames
     */
    private static class DicomFrame {
        byte[] data;

        /**
         * Creates a DicomFrame object from either a byte array or a short array of PixelData
         * The array's element size is determined by the value representation (VR) of the PixelData attribute
         *
         * @param w      frame width
         * @param h      frame height
         * @param isWord true if the frame is a word frame, false if it is a byte frame
         * @param pxs    byte array
         * @throws IOException if the byte array cannot be converted to a BufferedImage
         */
        public DicomFrame(int w, int h, boolean isWord, Object pxs) throws IOException {

            if (!isWord) {
                if (!(pxs instanceof byte[] bytes))
                    throw new IllegalArgumentException("Expected byte[] for byte data, got " + pxs.getClass().getName());

                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
                image.getRaster().setDataElements(0, 0, w, h, bytes);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                data = baos.toByteArray();
            } else {
                if (!(pxs instanceof short[] shorts))
                    throw new IllegalArgumentException("Expected short[] for word data, got " + pxs.getClass().getName());

                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_GRAY);
                image.getRaster().setDataElements(0, 0, w, h, shorts);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                data = baos.toByteArray();
            }
        }

        /**
         * Returns the frame as a base64-encoded string
         *
         * @return base64-encoded string
         */
        public String toBase64() {
            return Base64.getEncoder().encodeToString(data);
        }
    }
}
