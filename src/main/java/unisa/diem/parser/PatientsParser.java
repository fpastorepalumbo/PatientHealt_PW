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
 * Parsing of patients data one at a time
 */
public class PatientsParser extends BaseParser {

    PatientsParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "patients");
    }

    @Override
    @SneakyThrows
    public void parseData() {
        List<Patient> buffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Patient patient = new Patient();
            patient.setId(rec.get("Id"));
            patient.addExtension()
                .setUrl("http://hl7.org/fhir/StructureDefinition/patient-birthPlace")
                .setValue(new Address().setText(rec.get("BIRTHPLACE")));
            if (datasetUtility.hasProp(rec, "MAIDEN"))
                patient.addExtension()
                    .setUrl("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName")
                    .setValue(new StringType(rec.get("MAIDEN")));
            patient.addAddress()
                .addLine(rec.get("ADDRESS"))
                .setCity(rec.get("CITY"))
                .setState(rec.get("STATE"))
                .setPostalCode(rec.get("ZIP"))
                .setDistrict(rec.get("COUNTY"));
            patient.addExtension()
                .setUrl("http://hl7.org/fhir/StructureDefinition/geolocation")
                .setValue(new Address()
                    .addExtension()
                    .setUrl("latitude")
                    .setValue(new DecimalType(rec.get("LAT")))
                    .addExtension()
                    .setUrl("longitude")
                    .setValue(new DecimalType(rec.get("LON")))
                );
            HumanName name = new HumanName();
            name.addGiven(rec.get("FIRST"));
            name.setFamily(rec.get("LAST"));
            if (datasetUtility.hasProp(rec, "PREFIX"))
                name.addPrefix(rec.get("PREFIX"));
            if (datasetUtility.hasProp(rec, "SUFFIX"))
                name.addSuffix(rec.get("SUFFIX"));
            patient.addName(name);
            patient.setBirthDate(datasetUtility.parseDate(rec.get("BIRTHDATE")));
            if (datasetUtility.hasProp(rec, "DEATHDATE"))
                patient.setDeceased(new DateTimeType(datasetUtility.parseDate(rec.get("DEATHDATE"))));
            else
                patient.setDeceased(new BooleanType(false));
            patient.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("ANON")
                        .setDisplay("Anonymous ID")
                    )
                )
                .setSystem("urn:ietf:rfc:3986")
                .setValue(rec.get("Id"));
            patient.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("SS")
                        .setDisplay("Social Security Number")
                    )
                )
                .setSystem("http://hl7.org/fhir/sid/us-ssn")
                .setValue(rec.get("SSN"));
            patient.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("PPN")
                        .setDisplay("Passport Number")
                    )
                )
                .setSystem("http://standardhealthrecord.org/fhir/StructureDefinition/passportNumber")
                .setValue(rec.get("PASSPORT"));
            patient.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("DL")
                        .setDisplay("Driver's License Number")
                    )
                )
                .setSystem("urn:oid:2.16.840.1.113883.4.3.25")
                .setValue(rec.get("DRIVERS"));
            patient.setGender(
                rec.get("GENDER").equals("M") ? AdministrativeGender.MALE : AdministrativeGender.FEMALE
            );
            if (datasetUtility.hasProp(rec, "MARITAL"))
                patient.setMaritalStatus(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                        .setCode(rec.get("MARITAL"))
                    )
                );
            buffer.add(patient);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionUpdateEntry); //UPDATE
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d patients parsed", count);
                buffer.clear();
            }
             */
        }
        while (buffer.isEmpty()) {
            int upperSize = Math.min(100, buffer.size());
            List<Patient> first100 = buffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d patients parsed", upperSize);
            buffer.removeAll(first100);
        }

        buffer.clear();
        datasetUtility.logInfo("Patients parsed");
    }
}
