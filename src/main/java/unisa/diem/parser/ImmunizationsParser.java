package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing of immunizations (vaccinations) data one at a time
 */
public class ImmunizationsParser extends BaseParser {

    ImmunizationsParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "immunizations");
    }

    @Override
    @SneakyThrows
    public void parseData() {
        List<Immunization> buffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + rec.get("ENCOUNTER"));
            Immunization imm = new Immunization();
            imm.setRecorded(datasetUtility.parseDatetime(rec.get("DATE")));
            imm.setPatient(pat);
            imm.setEncounter(enc);
            imm.setVaccineCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://hl7.org/fhir/sid/cvx")
                    .setCode(rec.get("CODE"))
                    .setDisplay(rec.get("DESCRIPTION"))
                )
            );
            buffer.add(imm);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionCreateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d immunizations parsed".formatted(count));
                buffer.clear();
            }
             */
        }

        int count = 0;
        while (count < 30000 && !buffer.isEmpty()) {
            int upperSize = Math.min(100, buffer.size());
            List<Immunization> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d immunizations parsed", upperSize);
            buffer.removeAll(first100);
        }

        buffer.clear();
        datasetUtility.logInfo("immunizations parsed");
    }
}
