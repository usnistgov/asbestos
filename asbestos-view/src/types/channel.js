export function newChannel () {
    return {
        channelName: 'new',   // simple id - no testSession__ prefix
        environment: 'default',
        testSession: 'default',
        actorType: 'fhir',
        channelType: 'fhir',
        fhirBase: '',
        xdsSiteName: '',
        writeLocked: false,
        logMhdCapabilityStatementRequest: false
    }
}
