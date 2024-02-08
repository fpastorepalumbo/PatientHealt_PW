package unisa.diem.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * Global singleton object holding an instance of the FHIR context.
 */
public class FhirSingleton {

    private static final String serverUrl = "http://localhost:8080/fhir"; // ip docker server
    private static FhirSingleton instance; // Fhir instance
    private final FhirContext context; // FHIR resource

    /**
     * Class constructor, creating an instance configured for the R4 version of FHIR
     */
    private FhirSingleton() {
        context = FhirContext.forR4();
    }

    /**
     * Returns the global instance of the FHIR context, creating it if it doesn't exist
     */
    public static FhirContext getContext() {
        if (instance == null)
            instance = new FhirSingleton();
        return instance.context;
    }

    /**
     * Creates a new RESTful client from the instance of the FHIR context.
     */
    public static IGenericClient getClient() {
        getContext().getRestfulClientFactory().setSocketTimeout(60 * 1000);
        return getContext().newRestfulGenericClient(serverUrl);
    }
}
