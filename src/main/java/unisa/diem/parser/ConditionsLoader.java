package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirWrapper;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class ConditionsLoader extends BaseLoader {

    ConditionsLoader(DatasetService datasetService) {
        super(datasetService, "conditions");
    }

    @Override
    public void load() {
        int count = 0;
        List<Condition> buffer = new ArrayList<>();

        for (CSVRecord rec : records) {
            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + rec.get("ENCOUNTER"));

            Condition cond = new Condition();

            cond.setOnset(DateTimeType.parseV3(rec.get("START")));

            if (datasetService.hasProp(rec, "STOP"))
                cond.setAbatement(DateTimeType.parseV3(rec.get("STOP")));

            cond.setSubject(pat);
            cond.setEncounter(enc);

            cond.setCode(new CodeableConcept()
                    .addCoding(new Coding()
                            .setSystem("http://snomed.info/sct")
                            .setCode(rec.get("CODE"))
                            .setDisplay(rec.get("DESCRIPTION"))
                    )
            );

            count++;
            buffer.add(cond);

            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirWrapper.getContext());
                buffer.forEach(bb::addTransactionCreateEntry);
                FhirWrapper.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetService.logInfo("Loaded %d conditions".formatted(count));
                buffer.clear();
            }
        }
        datasetService.logInfo("Loaded ALL conditions");
    }
}
