package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirWrapper;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;

import java.util.ArrayList;
import java.util.List;

public class ProvidersLoader extends BaseLoader {

    ProvidersLoader(DatasetService datasetService) {
        super(datasetService, "providers");
    }

    @Override
    @SneakyThrows
    public void load() {
        List<Practitioner> buffer = new ArrayList<>();
        int count = 0;
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

            count++;
            buffer.add(provider);

            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirWrapper.getContext());
                buffer.forEach(bb::addTransactionUpdateEntry);
                FhirWrapper.getClient().transaction().withBundle(bb.getBundle()).execute();

                if (count % 1000 == 0)
                    datasetService.logInfo("Loaded %d practitioners".formatted(count));

                buffer.clear();
            }
        }
        datasetService.logInfo("Loaded ALL practitioners");
    }
}
