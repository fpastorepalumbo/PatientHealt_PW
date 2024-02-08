package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class ObservationsParser extends BaseParser {

    /**
     * Parsing of observations (e.g. blood pressure, glucose, weight, height, sexual orientation, etc.) data one at a time
     */
    ObservationsParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "observations");
    }

    @Override
    public void parseData() {
        int count = 0;
        List<Observation> buffer = new ArrayList<>();
        for (CSVRecord record : records) {
            Reference pat = new Reference("Patient/" + record.get("PATIENT"));
            Reference enc = null;
            if (datasetUtility.hasProp(record, "ENCOUNTER"))
                enc = new Reference("Encounter/" + record.get("ENCOUNTER"));
            Observation obs = new Observation();
            obs.getEffectiveDateTimeType().setValueAsString(record.get("DATE"));
            obs.setSubject(pat);
            if (enc != null)
                obs.setEncounter(enc);
            obs.setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem(record.get("CODE").equals("29303009")
                        ? "http://snomed.info/sct" : "http://loinc.org"
                    )
                    .setCode(record.get("CODE"))
                    .setDisplay(record.get("DESCRIPTION"))
                )
            );
            String unitStr = record.get("UNITS");
            if (unitStr.equals("{nominal}")) {
                obs.setValue(new StringType(record.get("VALUE")));
            } else if (unitStr.equals("{count}")) {
                float value = Float.parseFloat(record.get("VALUE"));
                obs.setValue(new IntegerType((int) value));
            } else if (record.get("TYPE").equals("numeric")) {
                Quantity q = new Quantity();
                q.setValue(Double.parseDouble(record.get("VALUE")));
                q.setUnit(unitStr);
                obs.setValue(q);
            } else {
                obs.setValue(new StringType(record.get("VALUE")));
            }
            count++;
            buffer.add(obs);
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionCreateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d observations parsed", count);
                buffer.clear();
            }
        }
        datasetUtility.logInfo("Observations parsed");
    }
}
