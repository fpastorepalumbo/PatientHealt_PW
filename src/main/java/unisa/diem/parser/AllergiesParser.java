package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class AllergiesParser extends BaseParser {

    AllergiesParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "allergies");
    }
//carico tutte le allergie, non singola risorsa,
    @Override
    @SneakyThrows
    public void parseData() {
        int count = 0;
        List<AllergyIntolerance> buffer = new ArrayList<>();

        for (CSVRecord rec : records) {
            AllergyIntolerance alin = new AllergyIntolerance();
            alin.setOnset(DateTimeType.parseV3(rec.get("START"))); //data inizio allergia

            if (datasetUtility.hasProp(rec, "STOP"))
                alin.setLastOccurrence(datasetUtility.parseDate(rec.get("STOP")));

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
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext()); //crea il gruppo da caricare
                buffer.forEach(bb::addTransactionCreateEntry); //inserisco tutte le allergie nel gruppo / FA CREATE
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute(); //fa la trans, col bundle che ho appena creato
            //serie di op consid come op singola... scrivo sul server
                if (count % 1000 == 0)
                    datasetUtility.logInfo("Loaded %d allergies", count);

                buffer.clear();
            }
        }

        datasetUtility.logInfo("Loaded ALL allergies");
    }
}
