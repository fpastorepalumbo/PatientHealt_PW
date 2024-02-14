package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing of practitioners (medical personnel including clinic secretaries) data one at a time
 */
public class PractitionerParser extends BaseParser {

    PractitionerParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "providers");
    }

    @Override
    @SneakyThrows
    public void parseData() {
        List<Practitioner> buffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Reference org = new Reference("Organization/" + rec.get("ORGANIZATION"));
            Practitioner provider = new Practitioner();
            provider.setId(rec.get("Id"));
            provider.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("ANON")
                        .setDisplay("Anonymous ID")
                    )
                )
                .setSystem("urn:ietf:rfc:3986")
                .setValue(rec.get("Id"));
            provider.addName()
                .setText(rec.get("NAME"));
            provider.addAddress()
                .addLine(rec.get("ADDRESS"))
                .setCity(rec.get("CITY"))
                .setState(rec.get("STATE"))
                .setPostalCode(rec.get("ZIP"));
            provider.setGender(
                rec.get("GENDER").equals("M") ? AdministrativeGender.MALE : AdministrativeGender.FEMALE
            );
            provider.addQualification()
                .setIssuer(org)
                .setCode(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://snomed.info/sct")
                        .setDisplay(rec.get("SPECIALITY"))
                    )
                );
            provider.addExtension()
                .setUrl("http://hl7.org/fhir/StructureDefinition/geolocation")
                .setValue(new Address()
                    .addExtension()
                    .setUrl("latitude")
                    .setValue(new DecimalType(rec.get("LAT")))
                    .addExtension()
                    .setUrl("longitude")
                    .setValue(new DecimalType(rec.get("LON")))
                );
            buffer.add(provider);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionUpdateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d practitioners parsed".formatted(count));

                buffer.clear();
            }
             */
        }
        int count = 0;
        while (count < 30000 && !buffer.isEmpty()) {
            int upperSize = Math.min(100, buffer.size());
            List<Practitioner> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d practitioners parsed", upperSize);
            buffer.removeAll(first100);
            count += 100;
        }

        buffer.clear();
        datasetUtility.logInfo("Practitioners parsed");
    }
}
