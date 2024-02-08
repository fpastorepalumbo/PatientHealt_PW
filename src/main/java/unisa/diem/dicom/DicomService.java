package unisa.diem.dicom;

import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.TagFromName;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Patient;
import unisa.diem.fhir.FhirSingleton;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Class that provides access services to DICOM files in the Coherent dataset
 */
public class DicomService {

    /**
     * Returns the filename part of the DICOM file corresponding to the given ImagingStudy resource
     */
    private String getFilenamePart(ImagingStudy img) {
        Patient patient = FhirSingleton.getClient().read().resource(Patient.class)
                .withId(img.getSubject().getReference()).execute();
        String givenName = patient.getNameFirstRep().getGivenAsSingleString(),
                surname = patient.getNameFirstRep().getFamily(),
                patientId = patient.getIdElement().getIdPart();
        return givenName + "_" + surname + "_" + patientId;
    }

    /**
     * Returns the DICOM file corresponding to the given ImagingStudy resource
     */
    public File getFileObject(String filename) {
        Path root = Path.of("src/main/resources/coherent-11-07-2022/dicom");
        try (var stream = Files.walk(root)) {
            List<Path> paths = stream.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().contains(filename))
                    .toList();
            if (paths.isEmpty())
                return null;
            for (Path path : paths) {
                DicomHandle dh = new DicomHandle(path.toFile());
                String studyUid = dh.getFirstStringValue(TagFromName.StudyInstanceUID);
                if (path.toString().contains(studyUid))
                    return path.toFile();
            }
            return null;
        } catch (IOException | DicomException e) {
            return null;
        }
    }

    /**
     * Creates the hande of the DICOM file corresponding to the given ImagingStudy resource
     */
    public DicomHandle getDicomFile(String filename) {
        Path root = Path.of("src/main/resources/coherent-11-07-2022/dicom");
        try (var stream = Files.walk(root)) {
            List<Path> paths = stream.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().contains(filename))
                    .toList();
            if (paths.isEmpty())
                return null;
            for (Path path : paths) {
                DicomHandle dh = new DicomHandle(path.toFile());
                String studyUid = dh.getFirstStringValue(TagFromName.StudyInstanceUID);
                if (path.toString().contains(studyUid))
                    return dh;
            }
            return null;
        } catch (IOException | DicomException e) {
            return null;
        }
    }

    /**
     * Returns the DICOM file corresponding to the given ImagingStudy resource (CDA)
     */
    public DicomHandle getDicomFile(ImagingStudy img) {
        return getDicomFile(getFilenamePart(img));
    }

    /**
     * Returns the DICOM file corresponding to the given ImagingStudy resource encoding it in Base64 (CDA)
     */
    public String getBase64Binary(ImagingStudy img) {
        try {
            File file = getFileObject(getFilenamePart(img));
            byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
            return new String(encoded, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            return null;
        }
    }
}
