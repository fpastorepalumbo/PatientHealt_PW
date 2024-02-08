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
     * Resolves a FHIR resource by reading l'id del CSV,    the resource specified by the provided type and id
     */
    public Resource getResource(Class<? extends Resource> type, String id) {
        return FhirSingleton.getClient().read().resource(type).withId(id).execute();
    }

    /**
     * Get the Claim related to the given Encounter(interazione tra paziene e medico).
     *  realz 1 a 1
     */
    public Claim getClaim(Encounter res) {
        return getClient().search().forResource(Claim.class)
            .where(Claim.ENCOUNTER.hasId("Encounter/" + res.getIdElement().getIdPart()))
            .returnBundle(Bundle.class)
            .execute().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .map(Claim.class::cast)
            .findFirst().orElse(null);
    }

    /**
     * Get the Claim related to the given MedicationRequest (prescrizione medicinale)
     */
    public Claim getClaim(MedicationRequest res) {
        return getClient().search().forResource(Claim.class)
            .where(Claim.ENCOUNTER.hasId(res.getEncounter().getReference()))
            .returnBundle(Bundle.class)
            .execute().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .map(Claim.class::cast)
            .toList().get(1);
    }

    /**
     * Get the ExplanationOfBenefit(copertura assicurativa) related to the given Encounter.
     *
     * @param res Encounter resource
     * @return ExplanationOfBenefit resource
     */
    public ExplanationOfBenefit getEOB(Encounter res) {
        return getClient().search().forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.CLAIM.hasId("Claim/" + getClaim(res).getIdElement().getIdPart()))
            .returnBundle(Bundle.class)
            .execute().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .map(ExplanationOfBenefit.class::cast)
            .findFirst().orElse(null);
    }

    /**
     * Get the ExplanationOfBenefit related to the given MedicationRequest.
     *
     * @param res MedicationRequest resource
     * @return ExplanationOfBenefit resource
     */
    public ExplanationOfBenefit getEOB(MedicationRequest res) {
        return getClient().search().forResource(ExplanationOfBenefit.class)
            .where(ExplanationOfBenefit.CLAIM.hasId("Claim/" + getClaim(res).getIdElement().getIdPart()))
            .returnBundle(Bundle.class)
            .execute().getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .map(ExplanationOfBenefit.class::cast)
            .findFirst().orElse(null);
    }
}
