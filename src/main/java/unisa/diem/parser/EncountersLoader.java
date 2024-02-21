package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirWrapper;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class EncountersLoader extends BaseLoader {

    EncountersLoader(DatasetService datasetService) {
        super(datasetService, "encounters");
    }

    @Override
    @SneakyThrows
    public void load() {
        int count = 0;
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
            encounter.addParticipant()
                    .setIndividual(pro);
            encounter.setServiceProvider(org);

            encounter.addType()
                    .addCoding(new Coding()
                            .setSystem("http://snomed.info/sct")
                            .setCode(rec.get("CODE"))
                            .setDisplay(rec.get("DESCRIPTION"))
                    );

            encounter.addReasonCode()
                    .addCoding(new Coding()
                            .setSystem("http://snomed.info/sct")
                            .setCode(rec.get("REASONCODE"))
                            .setDisplay(rec.get("REASONDESCRIPTION"))
                    );

            encounter.setPeriod(new Period()
                    .setStart(datasetService.parseDatetime(rec.get("START")))
            );

            if (datasetService.hasProp(rec, "STOP"))
                encounter.getPeriod().setEnd(datasetService.parseDatetime(rec.get("STOP")));

            claim.setId("CE-" + (count + 1));
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

            eob.setId("EE-" + (count + 1));
            eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
            eob.setOutcome(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
            eob.setClaim(new Reference("Claim/CE-" + (count + 1)));
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

            count++;
            encBuffer.add(encounter);
            claimBuffer.add(claim);
            eobBuffer.add(eob);

            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirWrapper.getContext());
                encBuffer.forEach(bb::addTransactionUpdateEntry);
                claimBuffer.forEach(bb::addTransactionUpdateEntry);
                eobBuffer.forEach(bb::addTransactionUpdateEntry);
                FhirWrapper.getClient().transaction().withBundle(bb.getBundle()).execute();

                if (count % 1000 == 0)
                    datasetService.logInfo("Loaded %d encounters", count);

                encBuffer.clear();
                claimBuffer.clear();
                eobBuffer.clear();
            }
        }
        datasetService.logInfo("loaded ALL encounters");
    }
}
