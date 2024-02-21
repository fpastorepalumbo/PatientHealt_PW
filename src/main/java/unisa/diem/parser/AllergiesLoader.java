package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirWrapper;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class AllergiesLoader extends BaseLoader {

    AllergiesLoader(DatasetService datasetService) {
        super(datasetService, "allergies");
    }

    @Override
    @SneakyThrows
    public void load() {
        int count = 0;
        List<AllergyIntolerance> buffer = new ArrayList<>();

        for (CSVRecord rec : records) {
            AllergyIntolerance alin = new AllergyIntolerance();
            alin.setOnset(DateTimeType.parseV3(rec.get("START")));

            if (datasetService.hasProp(rec, "STOP"))
                alin.setLastOccurrence(datasetService.parseDate(rec.get("STOP")));

            alin.setPatient(new Reference("Patient/" + rec.get("PATIENT")));
            alin.setEncounter(new Reference("Encounter/" + rec.get("ENCOUNTER")));
            alin.setCode(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://snomed.info/sct")
                            .setCode(rec.get("CODE"))
                            .setDisplay(rec.get("DESCRIPTION"))
                    )
            );

            count++;
            buffer.add(alin);

            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirWrapper.getContext());
                buffer.forEach(bb::addTransactionCreateEntry);
                FhirWrapper.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetService.logInfo("Loaded %d allergies", count);
                buffer.clear();
            }
        }
        // datasetService.logInfo("Loaded ALL allergies");
    }
}
