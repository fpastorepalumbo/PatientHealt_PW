package unisa.diem.parser;

import ca.uhn.fhir.util.BundleBuilder;
import com.pixelmed.dicom.TagFromName;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Reference;
import unisa.diem.dicom.DicomHandle;
import unisa.diem.fhir.FhirSingleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsing of imaging studies (e.g. X-rays, MRIs, etc.) data one at a time
 */
public class ImagingStudiesParser extends BaseParser {

    private Map<String, String> patientIndex = new HashMap<>();


    ImagingStudiesParser(DatasetUtility datasetUtility) {
        super(datasetUtility, "imaging_studies");
        patientIndex = makeIndex();
    }

    /**
     * Creates a map of patient IDs to their first and last names
     */
    private Map<String, String> makeIndex() {
        Map<String, String> index = new HashMap<>();
        for (CSVRecord rec : datasetUtility.parse("patients"))
            index.put(rec.get("Id"), rec.get("FIRST") + "_" + rec.get("LAST"));
        return index;
    }

    @SneakyThrows
    @Override
    public void parseData() {
        int count = 0;
        List<ImagingStudy> buffer = new ArrayList<>();
        for (CSVRecord rec : records) {
            Reference pat = new Reference("Patient/" + rec.get("PATIENT"));
            Reference enc = new Reference("Encounter/" + rec.get("ENCOUNTER"));
            String filename = patientIndex.get(rec.get("PATIENT")) + "_" + rec.get("PATIENT");
            DicomHandle dicom = datasetUtility.getDicomService().getDicomFile(filename);
            if (dicom == null)
                continue;
            ImagingStudy is = new ImagingStudy();
            is.setId(rec.get("Id"));
            is.addIdentifier()
                .setType(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                        .setCode("ANON")
                        .setDisplay("Anonymous ID")
                    )
                )
                .setSystem("urn:ietf:rfc:3986")
                .setValue(rec.get("Id"));
            is.setStarted(datasetUtility.parseDate(rec.get("DATE")));
            is.setSubject(pat);
            is.setEncounter(enc);
            is.addModality(new Coding()
                .setSystem("http://dicom.nema.org/resources/ontology/DCM")
                .setCode(rec.get("MODALITY_CODE"))
                .setDisplay(rec.get("MODALITY_DESCRIPTION"))
            );
            ImagingStudy.ImagingStudySeriesComponent series = is.addSeries();
            if (dicom.hasAttr(TagFromName.SeriesDate))
                series.setStarted(datasetUtility.parseDate(dicom.getFirstStringValue(TagFromName.SeriesDate)));
            series.setUid(dicom.getFirstStringValue(TagFromName.SeriesInstanceUID));
            series.setNumber(dicom.getFirstIntValue(TagFromName.SeriesNumber));
            series.setDescription(dicom.getFirstStringValue(TagFromName.SeriesDescription));
            series.setBodySite(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode(rec.get("BODYSITE_CODE"))
                .setDisplay(rec.get("BODYSITE_DESCRIPTION"))
            );
            series.setLaterality(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(dicom.getFirstStringValue(TagFromName.Laterality))
                )
                .setNumberOfInstances(
                    dicom.hasAttr(TagFromName.NumberOfSeriesRelatedInstances) ?
                        dicom.getFirstIntValue(TagFromName.NumberOfSeriesRelatedInstances) : 1
                )
                .setModality(new Coding()
                    .setSystem("http://dicom.nema.org/resources/ontology/DCM")
                    .setCode(rec.get("MODALITY_CODE"))
                    .setDisplay(rec.get("MODALITY_DESCRIPTION"))
                )
                .setBodySite(new Coding()
                    .setSystem("http://snomed.info/sct")
                    .setCode(rec.get("BODYSITE_CODE"))
                    .setDisplay(rec.get("BODYSITE_DESCRIPTION"))
                )
                .addInstance()
                .setUid(dicom.getFirstStringValue(TagFromName.SOPInstanceUID))
                .setNumber(dicom.getFirstIntValue(TagFromName.InstanceNumber))
                .setSopClass(new Coding()
                    .setSystem("http://dicom.nema.org/resources/ontology/DCM")
                    .setCode(rec.get("SOP_CODE"))
                    .setDisplay(rec.get("SOP_DESCRIPTION"))
                );
            count++;
            buffer.add(is);
            if (count % 10 == 0 || count == records.size()) {
                BundleBuilder bb = new BundleBuilder(FhirSingleton.getContext());
                buffer.forEach(bb::addTransactionUpdateEntry);
                FhirSingleton.getClient().transaction().withBundle(bb.getBundle()).execute();
                if (count % 100 == 0)
                    datasetUtility.logInfo("%d imaging studies parsed", count);
                buffer.clear();
            }
        }
        datasetUtility.logInfo("Imaging studies parsed");
    }
}
