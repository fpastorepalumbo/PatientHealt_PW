package unisa.diem.parser;

import org.apache.commons.csv.CSVRecord;

import java.util.List;

/**
 * Abstract class for loaders
 */
public abstract class BaseParser implements Parser {

    protected final String subject; // CSV file name, without extension
    protected final DatasetUtility datasetUtility; // link at the dataset utility
    protected List<CSVRecord> records; // list of records from the CSV file

    /**
     * Initializes the parser by parsing the assigned CSV file and storing its records
     * Composite abstract class
     */
    protected BaseParser(DatasetUtility datasetUtility, String subject) {
        this.subject = subject;
        this.datasetUtility = datasetUtility;
        this.records = datasetUtility.parse(subject);
        if (records == null)
            throw new RuntimeException("Parsing failure for: " + subject);
    }

    /**
     * Abstract method to parse the data
     */
    @Override
    public abstract void parseData();
}
