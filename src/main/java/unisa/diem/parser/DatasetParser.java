package unisa.diem.parser;

/**
 * Main loader class
 */
public class DatasetParser implements Parser {
    private final Parser[] parsers;
// carica tutti i parser per poi mandarli sul server
    /**
     * Register all unitary loaders
     *
     * @param datasetUtility copy of the dataset service
     */
    public DatasetParser(DatasetUtility datasetUtility) {
        this.parsers = new Parser[]{
            new PatientsParser(datasetUtility),
            new OrganizationsParser(datasetUtility),
            new PractitionerParser(datasetUtility),
            new PayersParser(datasetUtility),
            new EncountersParser(datasetUtility),
            new ConditionsParser(datasetUtility),
            new ObservationsParser(datasetUtility),
            new AllergiesParser(datasetUtility),
            new CarePlansParser(datasetUtility),
            new ImmunizationsParser(datasetUtility),
            new MedicationRequestsParser(datasetUtility),
            new DevicesParser(datasetUtility),
            new ImagingStudiesParser(datasetUtility),
            new ProceduresParser(datasetUtility)
        };
    }

    /**
     * Invokes, in order, the load method of each unitary loader
     */
    public void parseData() {
        for (Parser parser : parsers)
            parser.parseData();
    }
}
