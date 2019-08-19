export function newChannel () {
    return {
        channelId: 'new',
        environment: 'default',
        testSession: 'default',
        actorType: 'fhir',
        channelType: 'passthrough',
        fhirBase: '',
        xdsSiteName: ''
    }
}
