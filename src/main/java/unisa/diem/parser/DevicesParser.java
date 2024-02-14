package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import unisa.diem.fhir.FhirSingleton;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parsing of devices (prescriptions of medical devices) data one at a time
 */
public class DevicesParser extends BaseParser {

    public DevicesParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "devices");
    }

    private Map<String, String> splitUDI(String udi) {
        String[] parts = udi.split("\\(([0-9]{2})\\)");
        return Map.of(
            "01", parts[1],
            "11", parts[2],
            "17", parts[3],
            "10", parts[4],
            "21", parts[5]
        );
    }

    @Override
    @SneakyThrows
    public void parseData() {
        int c = 0;
        List<Device> devBuffer = new ArrayList<>();
        List<DeviceRequest> reqBuffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + rec.get("ENCOUNTER"));
            Device dev = new Device();
            dev.setId("DEV-" + (c + 1));
            DeviceRequest dr = new DeviceRequest();
            dev.setPatient(pat);
            Map<String, String> udiParts = splitUDI(rec.get("UDI"));
            dev.addUdiCarrier()
                .setDeviceIdentifier(udiParts.get("01"))
                .setIssuer(udiParts.get("11"))
                .setJurisdiction(udiParts.get("17"))
                .setCarrierAIDC(udiParts.get("10").getBytes())
                .setCarrierHRF(udiParts.get("21"));
            dev.addIdentifier().setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://hl7.org/fhir/identifier-type")
                        .setCode("UDI")
                        .setDisplay("Universal Device Identifier")
                    )
                )
                .setSystem("urn:ietf:rfc:3986")
                .setValue(rec.get("UDI"));
            dev.setType(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(rec.get("CODE"))
                    .setDisplay(rec.get("DESCRIPTION"))
                )
            );
            dr.setSubject(pat);
            dr.setEncounter(enc);
            dr.setCode(new Reference("Device/DEV-" + (c + 1)));
            if (datasetUtility.hasProp(rec, "STOP"))
                dr.setOccurrence(new Period()
                    .setStart(datasetUtility.parseDatetime(rec.get("START")))
                    .setEnd(datasetUtility.parseDatetime(rec.get("STOP")))
                );
            else
                dr.setOccurrence(new Period()
                    .setStart(datasetUtility.parseDatetime(rec.get("START")))
                );
            c++;
            devBuffer.add(dev);
            reqBuffer.add(dr);

            /*
            if (count % 100 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                devBuffer.forEach(bb::addTransactionUpdateEntry);
                reqBuffer.forEach(bb::addTransactionCreateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 1000 == 0)
                    datasetUtility.logInfo("%d devices parsed", count);
                devBuffer.clear();
                reqBuffer.clear();
            }
             */
        }
        int count = 0;
        while (count < 30000 && !devBuffer.isEmpty() && !reqBuffer.isEmpty()){
            int upperSize = Math.min(100, devBuffer.size());
            List<Device> first100 = devBuffer.subList(0, upperSize);
            List<DeviceRequest> first100req = reqBuffer.subList(0, upperSize);
            BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
            first100.forEach(bb::addTransactionCreateEntry);
            first100req.forEach(bb::addTransactionCreateEntry);
            FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
            datasetUtility.logInfo("%d devices parsed", upperSize);
            devBuffer.removeAll(first100);
            reqBuffer.removeAll(first100req);
            count += 100;
        }
        devBuffer.clear();
        reqBuffer.clear();
        datasetUtility.logInfo("Devices parsed");
    }
}
