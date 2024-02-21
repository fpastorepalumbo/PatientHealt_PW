package unisa.diem.parser;

import unisa.diem.dicom.DicomService;
import lombok.Getter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service bean with utility methods for dealing with CSV records.
 */
public class DatasetService {

    private final DateFormat onlyDateFmt = new SimpleDateFormat("yyyy-MM-dd");
    private final DateFormat datetimeFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final Logger logger = Logger.getLogger("DatasetLoader");

    @Getter
    private DicomService dicomService;

    public DatasetService() {
        dicomService = new DicomService();
    }

    /**
     * Logs a message with the INFO level
     *
     * @param fmt  format string
     * @param args format arguments
     */
    public void logInfo(String fmt, Object... args) {
        logger.setLevel(Level.INFO);
        logger.info(String.format(fmt, args));
    }

    /**
     * Logs a message with the SEVERE level
     *
     * @param fmt  format string
     * @param args format arguments
     */
    public void logSevere(String fmt, Object... args) {
        logger.severe(String.format(fmt, args));
    }

    /**
     * Returns the value of the given property from the given record.
     * The condition for existance of a property are
     * <ul>
     * <li>the property is mapped in the CSV file</li>
     * <li>the property is not null</li>
     * <li>the property is not an empty string</li>
     * <li>the property is not the string "false"</li>
     * </ul>
     *
     * @param record CSV record
     * @param prop   property name
     * @return true if the property exists, false otherwise
     */
    public boolean hasProp(CSVRecord record, String prop) {
        return (record.isMapped(prop) &&
                record.get(prop) != null &&
                !record.get(prop).isEmpty() &&
                !record.get(prop).equals("false"));
    }

    /**
     * Parse the CSV file with the given name and return its records.
     *
     * @param subject name of the CSV file without the .csv extension
     * @return list of CSV records
     */
    public List<CSVRecord> parse(String subject) {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try {
            String basePath = "src/main/resources/coherent-11-07-2022";
            String filePath = basePath + "/csv/" + subject + ".csv";
            return csvFormat.parse(new FileReader(filePath)).getRecords();
        } catch (IOException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    /**
     * Parse a date string in the format yyyy-MM-dd and return a Date object.
     *
     * @param date date string
     * @return date object
     * @throws ParseException if the date string is not in the expected format
     */
    public Date parseDate(String date) throws ParseException {
        return onlyDateFmt.parse(date);
    }

    /**
     * Parse a datetime string in the format yyyy-MM-dd'T'HH:mm:ss'Z' and return a
     * Date object.
     *
     * @param datetime datetime string
     * @return date object
     * @throws ParseException if the date string is not in the expected format
     */
    public Date parseDatetime(String datetime) throws ParseException {
        return datetimeFmt.parse(datetime);
    }

    /**
     * Checks for the presence of the file ds_loaded_ok in the current directory,
     * and if it doesn't exist, loads the dataset and creates it
     *
     * @throws IOException if file errors arise
     */
    public void loadDataset() throws IOException {
        // Path path = Paths.get("ds_loaded_ok");
        // if (!Files.exists(path)) {
            new DatasetLoader(this).load();
            // Files.createFile(path);
        // }
    }
}
