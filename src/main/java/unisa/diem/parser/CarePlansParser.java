package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class CarePlansParser extends BaseParser {

    /**
     * Parsing of care-plans (therapy programs) data one at a time
     */
    CarePlansParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "careplans");
    }
    @Override
    @SneakyThrows
    public void parseData() {
        List<CarePlan> buffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + rec.get("ENCOUNTER"));
            CarePlan cp = new CarePlan();
            cp.setId(rec.get("Id"));
            Period period = new Period();
            period.setStart(datasetUtility.parseDate(rec.get("START")));
            if (datasetUtility.hasProp(rec, "STOP"))
                period.setEnd(datasetUtility.parseDate(rec.get("STOP")));
            cp.setPeriod(period);
            cp.setSubject(pat);
            cp.setEncounter(enc);
            cp.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(rec.get("CODE"))
                    .setDisplay(rec.get("DESCRIPTION"))
                )
            );
            cp.addActivity()
                .getDetail()
                .addReasonCode(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://snomed.info/sct")
                        .setCode(rec.get("REASONCODE"))
                        .setDisplay(rec.get("REASONDESCRIPTION"))
                    )
                );
            cp.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("ANON")
                        .setDisplay("Anonymous ID")
                    )
                )
                .setValue(rec.get("Id"))
                .setSystem("urn:ietf:rfc:3986");
            buffer.add(cp);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionUpdateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d careplans parsed", count);
                buffer.clear();
            }
             */
        }
        int count = 0;
        while (count < 30000 && !buffer.isEmpty()){
            int upperSize = Math.min(100, buffer.size());
            List<CarePlan> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d careplans parsed", upperSize);
            buffer.removeAll(first100);
            count += 100;
        }

        buffer.clear();
        datasetUtility.logInfo("Careplans parsed");
    }
}
