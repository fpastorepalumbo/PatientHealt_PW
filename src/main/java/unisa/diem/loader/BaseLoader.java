package unisa.diem.loader;

import org.apache.commons.csv.CSVRecord;

import java.util.List;

/**
 * Abstract class for loaders
 */
public abstract class BaseLoader implements Loader {

    protected final String subject;
    protected final DatasetService datasetService;
    protected List<CSVRecord> records;

    /**
     * Initializes the loader by parsing the assigned CSV file and storing its records
     *
     * @param datasetService service for parsing csv files
     * @param subject        name of the csv file to load without the .csv extension
     */
    protected BaseLoader(DatasetService datasetService, String subject) {
        this.subject = subject;
        this.datasetService = datasetService;
        this.records = datasetService.parse(subject);
        if (records == null)
            throw new RuntimeException("Failed to load " + subject);
    }

    /**
     * Creates and stores the FHIR resources from the parsed CSV records
     */
    @Override
    public abstract void load();
}
