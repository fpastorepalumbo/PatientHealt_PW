package unisa.diem.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.*;

/**
 * Class that provides access services to FHIR resources defined in FhirSingleton class
 */
public class FhirService {

    public IGenericClient getClient() {
        return FhirSingleton.getClient();
    }

    /**
     * Returns a new resource from the ID written in the CSV file
     */
    public Resource getResource(Class<? extends Resource> type, String id) {
        return FhirSingleton.getClient().read().resource(type).withId(id).execute();
    }

    /**
     * Returns the Claim related to the Encounter
     */
    public Claim getClaim(Encounter enc) {
        return getClient().search().forResource(Claim.class)
                .where(Claim.ENCOUNTER.hasId("Encounter/" + enc.getIdElement().getIdPart()))
                .returnBundle(Bundle.class)
                .execute().getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(Claim.class::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the Claim related to the MedicationRequest
     */
    public Claim getClaim(MedicationRequest medReq) {
        return getClient().search().forResource(Claim.class)
                .where(Claim.ENCOUNTER.hasId(medReq.getEncounter().getReference()))
                .returnBundle(Bundle.class)
                .execute().getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(Claim.class::cast)
                .toList().get(1);
    }

    /**
     * Get the ExplanationOfBenefit related to the given Encounter
     */
    public ExplanationOfBenefit getEOB(Encounter enc) {
        return getClient().search().forResource(ExplanationOfBenefit.class)
                .where(ExplanationOfBenefit.CLAIM.hasId("Claim/" + getClaim(enc).getIdElement().getIdPart()))
                .returnBundle(Bundle.class)
                .execute().getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(ExplanationOfBenefit.class::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the ExplanationOfBenefit related to the MedicationRequest.
     */
    public ExplanationOfBenefit getEOB(MedicationRequest medReq) {
        return getClient().search().forResource(ExplanationOfBenefit.class)
                .where(ExplanationOfBenefit.CLAIM.hasId("Claim/" + getClaim(medReq).getIdElement().getIdPart()))
                .returnBundle(Bundle.class)
                .execute().getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(ExplanationOfBenefit.class::cast)
                .findFirst()
                .orElse(null);
    }
}
