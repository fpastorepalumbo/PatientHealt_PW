package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirWrapper;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicationRequestsLoader extends BaseLoader {
    private final Iterable<CSVRecord> encRecords;
    private final Map<String, CSVRecord> encIndex;

    MedicationRequestsLoader(DatasetService datasetService) {
        super(datasetService, "medications");

        encRecords = datasetService.parse("encounters");
        if (records == null || encRecords == null) {
           // datasetService.logSevere("Failed to load medications");
        }
        encIndex = makeIndex();
    }

    private Map<String, CSVRecord> makeIndex() {
        Map<String, CSVRecord> index = new HashMap<>();
        for (CSVRecord rec : encRecords)
            index.put(rec.get("Id"), rec);
        return index;
    }

    @Override
    @SneakyThrows
    public void load() {
        int count = 0;
        List<MedicationRequest> buffer = new ArrayList<>();
        List<Claim> clmBuffer = new ArrayList<>();
        List<ExplanationOfBenefit> eobBuffer = new ArrayList<>();

        for (CSVRecord rec : records) {

            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + rec.get("ENCOUNTER"));

            MedicationRequest mst = new MedicationRequest();
            mst.setId("MR-" + (count + 1));
            mst.setSubject(pat);
            mst.setEncounter(enc);
            mst.setMedication(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(rec.get("CODE"))
                    .setDisplay(rec.get("DESCRIPTION"))
                )
            );

            MedicationRequest.MedicationRequestDispenseRequestComponent dispense =
                new MedicationRequest.MedicationRequestDispenseRequestComponent();
            if (datasetService.hasProp(rec, "STOP")) {
                dispense.setValidityPeriod(new Period()
                    .setStart(datasetService.parseDatetime(rec.get("START")))
                    .setEnd(datasetService.parseDatetime(rec.get("STOP")))
                );
                mst.setStatus(MedicationRequest.MedicationRequestStatus.COMPLETED);
            } else {
                dispense.setValidityPeriod(new Period()
                    .setStart(datasetService.parseDatetime(rec.get("START")))
                );
                mst.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
            }
            mst.setDispenseRequest(dispense);

            // claim
            Claim claim = new Claim();
            claim.setId("CM-" + (count + 1));
            claim.setPatient(pat);
            claim.setPrescription(new Reference("MedicationRequest/MR-" + (count + 1)));
            claim.setTotal(new Money()
                .setValue(Double.parseDouble(rec.get("TOTALCOST")))
                .setCurrency("USD")
            );

            claim.addItem()
                .addEncounter(enc)
                .setQuantity(new Quantity()
                    .setValue(Integer.parseInt(rec.get("DISPENSES")))
                    .setUnit("#")
                    .setSystem("http://unitsofmeasure.org")
                    .setCode("#")
                )
                .setUnitPrice(new Money()
                    .setValue(Double.parseDouble(rec.get("BASE_COST")))
                    .setCurrency("USD")
                )
                .setNet(new Money()
                    .setValue(Double.parseDouble(rec.get("TOTALCOST")))
                    .setCurrency("USD")
                );

            // eob
            String payId = encIndex.get(rec.get("ENCOUNTER")).get("PAYER");
            Reference pay = new Reference("Organization/" + payId);

            ExplanationOfBenefit eob = new ExplanationOfBenefit();
            eob.setId("EM-" + (count + 1));
            eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
            eob.setOutcome(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
            eob.setClaim(new Reference("Claim/CM-" + (count + 1)));
            eob.setPatient(pat);
            eob.setInsurer(pay);
            eob.getPayee().setParty(pat);
            eob.addItem()
                .addEncounter(enc)
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
            buffer.add(mst);
            clmBuffer.add(claim);
            eobBuffer.add(eob);

            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirWrapper.getContext());
                buffer.forEach(bb::addTransactionUpdateEntry);
                clmBuffer.forEach(bb::addTransactionUpdateEntry);
                eobBuffer.forEach(bb::addTransactionUpdateEntry);
                FhirWrapper.getClient().transaction().withBundle(bb.getBundle()).execute();

                //if (count % 1000 == 0)
                //    datasetService.logInfo("Loaded %d medication statements", count);

                buffer.clear();
                clmBuffer.clear();
                eobBuffer.clear();
            }
        }

       // datasetService.logInfo("Loaded ALL medication statements");
    }
}
