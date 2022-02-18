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
            mhdVersions: [
                'MHDv3.x',
                'MHDv4'
            ],
            pdbAssertions: [
                'Internal',
                'MHDv4_Internal'
            ]
        }
    }
}
