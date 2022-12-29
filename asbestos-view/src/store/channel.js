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

            ftkChannelCapabilities: [],

            doReloadObjArray: function(state, tcObjs) {
                if (state.ftkChannelCapabilities.length > 0) {
                    state.ftkChannelCapabilities.splice(0, state.ftkChannelCapabilities.length)
                }
                for (let r of tcObjs) {
                    state.ftkChannelCapabilities.push(r)
                }
            },


        }
    },
    mutations: {
        setFtkChannelCapabilities(state, chObjArray) {
            state.doReloadObjArray(state,  chObjArray)
        },
    },
    actions: {
        async loadChannelCapabilities({commit, state}) {
            const url = 'collections'
            try {
                ENGINE.get(url)
                    .then(response => {
                        commit('testCollectionsLoaded')  // startup heartbeat for test engine
                        commit('setClientTestCollectionObjs', response.data.filter(e => !e.hidden && !e.server).sort(state.sortTestCollection))
                        commit('setServerTestCollectionObjs', response.data.filter(e => !e.hidden && e.server).sort(state.sortTestCollection))
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
