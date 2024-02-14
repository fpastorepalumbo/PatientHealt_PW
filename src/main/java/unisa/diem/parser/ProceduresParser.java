package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import org.hl7.fhir.r4.model.*;
import unisa.diem.fhir.FhirSingleton;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing of procedures (medical procedures, interventions, operations) data one at a time
 */
public class ProceduresParser extends BaseParser {

    ProceduresParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "procedures");
    }

    @Override
    public void parseData() {
        List<Procedure> buffer = new ArrayList<>();
        for (CSVRecord record : records) {
            Reference pat = new Reference("Patient/" + record.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + record.get("ENCOUNTER"));
            Procedure proc = new Procedure();
            proc.getPerformedDateTimeType().setValueAsString(record.get("DATE"));
            proc.setSubject(pat);
            proc.setEncounter(enc);
            proc.setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(record.get("CODE"))
                    .setDisplay(record.get("DESCRIPTION"))
                )
            );
            proc.addReasonCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(record.get("REASONCODE"))
                    .setDisplay(record.get("REASONDESCRIPTION"))
                )
            );
            buffer.add(proc);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionCreateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d procedures parsed", count);
                buffer.clear();
            }
            */
        }
        int count = 0;
        while (count < 30000 && !buffer.isEmpty()){
            int upperSize = Math.min(100, buffer.size());
            List<Procedure> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d procedures parsed", upperSize);
            buffer.removeAll(first100);
            count += 100;
        }

        buffer.clear();
        datasetUtility.logInfo("Procedures parsed");
    }
}
