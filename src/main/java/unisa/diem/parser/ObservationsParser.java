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
        List<Observation> bufferECG = new ArrayList<>();
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
            String value = record.get("VALUE");
            if (value.length() > 10 && !value.contains(" "))
                bufferECG.add(obs);
            else
                buffer.add(obs);
        }

        while (!bufferECG.isEmpty()) {
            int upperECGSize = Math.min(100, bufferECG.size());
            List<Observation> first100 = bufferECG.subList(0, upperECGSize);
            BundleBuilder bbECG = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bbECG::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bbECG.getBundle()).execute();
            datasetUtility.logInfo("%d ECG observations parsed", upperECGSize);
            bufferECG.removeAll(first100);
        }

        int c = 0;
        while (c < 30000 && !buffer.isEmpty()){
            int upperSize = Math.min(100, buffer.size());
            List<Observation> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d observations parsed", upperSize);
            buffer.removeAll(first100);
            c += 100;
        }

        buffer.clear();
        datasetUtility.logInfo("Observations parsed");
    }
}
