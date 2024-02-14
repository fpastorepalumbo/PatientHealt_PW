package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing of organizations (hospitals, clinics, private practices) data one at a time
 */
public class OrganizationsParser extends BaseParser {

    OrganizationsParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "organizations");
    }

    @Override
    @SneakyThrows
    public void parseData() {
        List<Organization> buffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Organization org = new Organization();
            org.setId(rec.get("Id"));
            org.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("ANON")
                        .setDisplay("Anonymous ID")
                    )
                )
                .setSystem("urn:ietf:rfc:3986")
                .setValue(rec.get("Id"));
            org.setName(rec.get("NAME"));
            org.addAddress()
                .setCity(rec.get("CITY"))
                .setState(rec.get("STATE"))
                .setPostalCode(rec.get("ZIP"));
            org.addExtension()
                .setUrl("http://hl7.org/fhir/StructureDefinition/geolocation")
                .setValue(new Address()
                    .addExtension()
                    .setUrl("latitude")
                    .setValue(new DecimalType(rec.get("LAT")))
                    .addExtension()
                    .setUrl("longitude")
                    .setValue(new DecimalType(rec.get("LON")))
                );
            org.addTelecom()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue(rec.get("PHONE"));
            buffer.add(org);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionUpdateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d organizations parsed".formatted(count));
                buffer.clear();
            }
             */
        }
        int count = 0;
        while (count < 30000 && !buffer.isEmpty()) {
            int upperSize = Math.min(100, buffer.size());
            List<Organization> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d organizations parsed", upperSize);
            buffer.removeAll(first100);
            count += 100;
        }

        buffer.clear();
        datasetUtility.logInfo("Organizations parsed");
    }
}
