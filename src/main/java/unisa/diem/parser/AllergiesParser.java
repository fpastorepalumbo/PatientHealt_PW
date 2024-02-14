package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing of allergies data one at a time
 */
public class AllergiesParser extends BaseParser {

    AllergiesParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "allergies");
    }

    @Override
    @SneakyThrows
    public void parseData() {
        List<AllergyIntolerance> buffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            AllergyIntolerance alin = new AllergyIntolerance();
            alin.setOnset(DateTimeType.parseV3(rec.get("START"))); // date of allergy discovery
            if (datasetUtility.hasProp(rec, "STOP"))
                alin.setLastOccurrence(datasetUtility.parseDate(rec.get("STOP"))); // date of allergy resolution
            alin.setPatient(new Reference("Patient/" + rec.get("PATIENT")));
            alin.setEncounter(new Reference("Encounter/" + rec.get("ENCOUNTER")));
            alin.setCode(new CodeableConcept().addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(rec.get("CODE"))
                    .setDisplay(rec.get("DESCRIPTION"))
                )
            );
            buffer.add(alin);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext()); //  creating the bundle to send to the server
                buffer.forEach(bb::addTransactionCreateEntry); // adding the allergies to the bundle
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute(); // sending the bundle to the server with a transaction
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d allergies parsed", count); // logging the progress
                buffer.clear();
            }
            */
        }

        int count = 0;
        while (count < 30000 && !buffer.isEmpty()) {
            int upperSize = Math.min(100, buffer.size());
            List<AllergyIntolerance> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d allergies parsed", upperSize);
            buffer.removeAll(first100);
            count+=100;
        }
        buffer.clear();
        datasetUtility.logInfo("Allergies parsed");
    }
}
