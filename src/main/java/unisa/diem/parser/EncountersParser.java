package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing of encounters (generic interactions between doctor and patient) data one at a time
 */
public class EncountersParser extends BaseParser {

    EncountersParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "encounters");
    }

    @Override
    @SneakyThrows
    public void parseData() {
        int c = 0;
        List<Encounter> encBuffer = new ArrayList<>();
        List<Claim> claimBuffer = new ArrayList<>();
        List<ExplanationOfBenefit> eobBuffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference pro = new Reference("Practitioner/" + rec.get("PROVIDER"));
            Reference org = new Reference("Organization/" + rec.get("ORGANIZATION"));
            Reference pay = new Reference("Organization/" + rec.get("PAYER"));
            Encounter encounter = new Encounter();
            Claim claim = new Claim();
            ExplanationOfBenefit eob = new ExplanationOfBenefit();
            encounter.setId(rec.get("Id"));
            encounter.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("ANON")
                        .setDisplay("Anonymous ID")
                    )
                )
                .setSystem("urn:ietf:rfc:3986")
                .setValue(rec.get("Id"));
            encounter.setSubject(pat);
            encounter.addParticipant().setIndividual(pro);
            encounter.setServiceProvider(org);
            encounter.addType()
                .addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(rec.get("CODE"))
                    .setDisplay(rec.get("DESCRIPTION"))
                );
            encounter.addReasonCode().addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(rec.get("REASONCODE"))
                    .setDisplay(rec.get("REASONDESCRIPTION"))
                );
            encounter.setPeriod(new Period()
                .setStart(datasetUtility.parseDatetime(rec.get("START")))
            );
            if (datasetUtility.hasProp(rec, "STOP"))
                encounter.getPeriod().setEnd(datasetUtility.parseDatetime(rec.get("STOP")));
            claim.setId("CE-" + (c + 1));
            claim.addItem()
                .addEncounter(new Reference("Encounter/" + rec.get("Id")))
                .setQuantity(new Quantity()
                    .setValue(1)
                    .setUnit("#")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("#")
                )
                .setUnitPrice(new Money()
                    .setValue(Double.parseDouble(rec.get("BASE_ENCOUNTER_COST")))
                    .setCurrency("USD")
                )
                .setNet(new Money()
                    .setValue(Double.parseDouble(rec.get("TOTAL_CLAIM_COST")))
                    .setCurrency("USD")
                );
            claim.setPatient(pat);
            claim.setTotal(new Money()
                .setValue(Double.parseDouble(rec.get("BASE_ENCOUNTER_COST")))
                .setCurrency("USD")
            );
            eob.setId("EE-" + (c + 1));
            eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
            eob.setOutcome(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
            eob.setClaim(new Reference("Claim/CE-" + (c + 1)));
            eob.setPatient(pat);
            eob.setInsurer(pay);
            eob.getPayee().setParty(pat);
            eob.addItem()
                .addEncounter(new Reference("Encounter/" + rec.get("Id")))
                .addAdjudication()
                .setCategory(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/adjudication")
                        .setCode("benefit")
                    )
                )
                .setAmount(new Money()
                    .setValue(Double.parseDouble(rec.get("PAYER_COVERAGE")))
                    .setCurrency("USD")
                );
            c++;
            encBuffer.add(encounter);
            claimBuffer.add(claim);
            eobBuffer.add(eob);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                encBuffer.forEach(bb::addTransactionUpdateEntry);
                claimBuffer.forEach(bb::addTransactionUpdateEntry);
                eobBuffer.forEach(bb::addTransactionUpdateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d encounters parsed", count);
                encBuffer.clear();
                claimBuffer.clear();
                eobBuffer.clear();
            }
             */
        }

        while (!encBuffer.isEmpty() && !claimBuffer.isEmpty() && !eobBuffer.isEmpty()){
            int upperSize = Math.min(100, encBuffer.size());
            List<Encounter> first100 = encBuffer.subList(0, upperSize);
            List<Claim> first100C = claimBuffer.subList(0, upperSize);
            List<ExplanationOfBenefit> first100E = eobBuffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            first100C.forEach(bb::addTransactionCreateEntry);
            first100E.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d encounters parsed", upperSize);
            encBuffer.removeAll(first100);
            claimBuffer.removeAll(first100C);
            eobBuffer.removeAll(first100E);
        }

        encBuffer.clear();
        claimBuffer.clear();
        eobBuffer.clear();
        datasetUtility.logInfo("Encounters parsed");
    }
}
