export function newChannel () {
    return {
        channelId: 'new',   // simple id - no testSession__ prefix
        environment: 'default',
        testSession: 'default',
        actorType: 'fhir',
        channelType: 'passthrough',
        fhirBase: '',
        xdsSiteName: ''
    }
}
