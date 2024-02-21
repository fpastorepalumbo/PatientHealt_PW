package unisa.diem.dicom;

import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.TagFromName;
import unisa.diem.fhir.FhirWrapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Patient;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Service bean with method for working with DICOM files.
 */
public class DicomService {

    /**
     * Returns the filename part of a DICOM file.
     * In the Coherent dataset a dicom filename is in the format:
     *
     * <pre>
     * {PATIENT_NAME}_{PATIENT_SURNAME}_{PATIENT_UID}_{DICOM_StudyInstanceUID}.dcm
     * </pre>
     * <p>
     * This method returns the first three parts of the filename. The last is
     * embedded inside the DICOM metadata.
     * This is useful to restrict the number of DICOM files to be read.
     *
     * @param img ImagingStudy resource
     * @return filename part
     */
    private String getFilenamePart(ImagingStudy img) {
        Patient patient = FhirWrapper.getClient().read().resource(Patient.class)
                .withId(img.getSubject().getReference()).execute();

        String givenName = patient.getNameFirstRep().getGivenAsSingleString(),
                surname = patient.getNameFirstRep().getFamily(),
                patientId = patient.getIdElement().getIdPart();

        return givenName + "_" + surname + "_" + patientId;
    }

    /**
     * Returns the DICOM file object from the Coherent dataset.
     * Reads all files whose name contains the given filename part and returns the
     * one whose StudyInstanceUID matches.
     *
     * @param filename filename part
     * @return DICOM file object
     */
    public File getFileObject(String filename) {
        Path root = Path.of("src/main/resources/coherent-11-07-2022/dicom");

        try (var stream = Files.walk(root)) {
            List<Path> paths = stream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString()
                            .contains(filename))
                    .toList();

            if (paths.isEmpty())
                return null;

            for (Path path : paths) {
                DicomHandle dh = new DicomHandle(path.toFile());
                String studyUid = dh.getString(TagFromName.StudyInstanceUID);
                if (path.toString().contains(studyUid))
                    return path.toFile();
            }

            return null;
        } catch (IOException | DicomException e) {
            return null;
        }
    }

    /**
     * Returns the DICOM Handle object from the Coherent dataset.
     * Reads all files whose name contains the given filename part and returns the
     * one whose StudyInstanceUID matches.
     *
     * @param filename filename part
     * @return DICOM file object
     */
    public DicomHandle getDicomFile(String filename) {
        Path root = Path.of("src/main/resources/coherent-11-07-2022/dicom");

        try (var stream = Files.walk(root)) {
            List<Path> paths = stream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString()
                            .contains(filename))
                    .toList();

            if (paths.isEmpty())
                return null;

            for (Path path : paths) {
                DicomHandle dh = new DicomHandle(path.toFile());
                String studyUid = dh.getString(TagFromName.StudyInstanceUID);
                if (path.toString().contains(studyUid))
                    return dh;
            }

            return null;
        } catch (IOException | DicomException e) {
            return null;
        }
    }

    /**
     * Returns the DICOM file corresponding to the given ImagingStudy resource.
     *
     * @param img ImagingStudy resource
     * @return DICOM Handle object
     */
    public DicomHandle getDicomFile(ImagingStudy img) {
        return getDicomFile(getFilenamePart(img));
    }

    /**
     * Get the DICOM file corresponding to the given ImagingStudy resource and
     * encodes it in Base64.
     *
     * @param img ImagingStudy resource
     * @return Base64 encoded string of the DICOM corresponding DICOM file
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
