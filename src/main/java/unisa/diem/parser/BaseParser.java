package unisa.diem.parser;

import org.apache.commons.csv.CSVRecord;

import java.util.List;

/**
 * Abstract class for loaders
 */
public abstract class BaseParser implements Parser {

    protected final String subject; //nome del csv senza est.
    protected final DatasetUtility datasetUtility; //coll al datasetservice
    protected List<CSVRecord> records; //lista di record csv

    /**
     * Initializes the loader by parsing the assigned CSV file and storing its records
     * impl comp astratto del singolo loader,
     * composite
     * @param datasetUtility service for parsing csv files
     * @param subject        name of the csv file to load without the .csv extension
     */
    protected BaseParser(DatasetUtility datasetUtility, String subject) {
        this.subject = subject;
        this.datasetUtility = datasetUtility;
        this.records = datasetUtility.parse(subject);
        if (records == null)
            throw new RuntimeException("Failed to load " + subject);
    }

    /**
     * Creates and stores the FHIR resources from the parsed CSV records
     */
    @Override
    public abstract void parseData();
}
