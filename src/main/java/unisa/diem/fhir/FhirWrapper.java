package unisa.diem.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * Global singleton object holding an instance of the FHIR context.
 */
public class FhirWrapper {

    private static final String baseUrl = "http://localhost:8080/fhir";
    private static FhirWrapper instance;
    private final FhirContext ctx;

    private FhirWrapper() {
        ctx = FhirContext.forR4();
    }

    /**
     * Returns the global instance of the FHIR context.
     * If the instance doesn't exist yet, it is created.
     *
     * @return FHIR context shared instance
     */
    public static FhirContext getContext() {
        if (instance == null) {
            instance = new FhirWrapper();
        }
        return instance.ctx;
    }

    /**
     * Returns a copy of the FHIR client from the global instance of the FHIR context.
     *
     * @return FHIR client
     */
    public static IGenericClient getClient() {
        getContext().getRestfulClientFactory().setSocketTimeout(60 * 1000);
        return getContext().newRestfulGenericClient(baseUrl);
    }

}
