import Vue from 'vue'
import Vuex from 'vuex'
import {ENGINE} from "../common/http-common";

Vue.use(Vuex)

export const channelStore = {
    state() {
        return {
            /*
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
*/

            channelTypeIgTestCollection: [],

            doReloadObjArray: function(state, tcObjs) {
                if (state.channelTypeIgTestCollection.length > 0) {
                    state.channelTypeIgTestCollection.splice(0, state.channelTypeIgTestCollection.length)
                }
                for (let r of tcObjs) {
                    state.channelTypeIgTestCollection.push(r)
                }
            },


        }
    },
    mutations: {
        setFtkChannelTypeIgTestCollections(state, chObjArray) {
            state.doReloadObjArray(state,  chObjArray)
        },
    },
    actions: {
        async loadChannelTypeIgTestCollections({commit}) {
            const url = 'channelTypeIgTestCollection'
            try {
                ENGINE.get(url)
                    .then(response => {
                        commit('setFtkChannelTypeIgTestCollections', response.data)
                        console.info(JSON.stringify(response.data))
                    })
                    .catch(function (error) {
                        commit('setError', url + ': ' + error)
                        console.error(`${error} for ${url}`)
                    })

            } catch (error) {
                this.$store.commit('setError', url + ': ' +  error)
            }
        },
    }
}
