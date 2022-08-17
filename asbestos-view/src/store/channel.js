import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export const channelStore = {
    state() {
        return {
            // channel is probably not used
            // channel: null,  // private communication between ChannelNav and ChannelView
            channelTypes: [
                'fhir',
                'mhd'
            ],
            // The mhdVersions array index is linked to pdbAssertions array index.
            // You must update the pdbAssertions array if mhdVersions array was updated.
            // See also assertions.json docBase URLs.
            mhdVersions: [
                'MHDv3.x',
                'MHDv4',
                'MHDv410'
            ],
            pdbAssertions: [
                'Internal',
                'MHDv4_Internal',
                'MHDv410_Internal'
            ]
        }
    }
}
