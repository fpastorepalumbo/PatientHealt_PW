package unisa.diem.loader;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirWrapper;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.List;

public class ImmunizationsLoader extends BaseLoader {

    ImmunizationsLoader(DatasetService datasetService) {
        super(datasetService, "immunizations");
    }

    @Override
    @SneakyThrows
    public void load() {
        int count = 0;
        List<Immunization> buffer = new ArrayList<>();

        for (CSVRecord rec : records) {
            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + rec.get("ENCOUNTER"));

            Immunization imm = new Immunization();

            imm.setRecorded(datasetService.parseDatetime(rec.get("DATE")));
            imm.setPatient(pat);
            imm.setEncounter(enc);
            imm.setVaccineCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://hl7.org/fhir/sid/cvx")
                    .setCode(rec.get("CODE"))
                    .setDisplay(rec.get("DESCRIPTION"))
                )
            );

            count++;
            buffer.add(imm);

            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirWrapper.getContext());
                buffer.forEach(bb::addTransactionCreateEntry);
                FhirWrapper.getClient().transaction().withBundle(bb.getBundle()).execute();

                if (count % 1000 == 0)
                    datasetService.logInfo("Loaded %d immunizations".formatted(count));

                buffer.clear();
            }
        }

        datasetService.logInfo("Loaded ALL immunizations");
    }
}
