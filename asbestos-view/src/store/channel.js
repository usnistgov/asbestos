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
    getters: {
        isCtIgTcLoaded: state => {
            return state.channelTypeIgTestCollection.length > 0
        },
        getChannelIgTestCollectionArray: (state) => (channelType) => {
            // const channelType = this.$store.state.base.channel.channelType
            const o = state.channelTypeIgTestCollection.find(i => i.channelType === channelType)
            if (o !== undefined && o !== null) {
                return o.igTestCollections
            }
        },
    },
    actions: {
        async loadChannelTypeIgTestCollections({commit}) {
            const url = 'channelTypeIgTestCollection'
            try {
              await ENGINE.get(url)
                    .then(response => {
                        commit('setFtkChannelTypeIgTestCollections', response.data)
                        /*
                        [{"channelType":"fhir","igTestCollections":[{"igName":"MHDv3.x","tcName":"Internal","docBase":"https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_MHD_Rev3-2_TI_2020-08-28.pdf"},{"igName":"MHDv4","tcName":"MHDv4_Internal","docBase":"https://profiles.ihe.net/ITI/MHD/4.0.1"},{"igName":"MHDv410","tcName":"MHDv410_Internal","docBase":"https://profiles.ihe.net/ITI/MHD/4.1.0"}]},{"channelType":"mhd","igTestCollections":[{"igName":"MHDv3.x","tcName":"Internal","docBase":"https://www.ihe.net/uploadedFiles/Documents/ITI/IHE_ITI_Suppl_MHD_Rev3-2_TI_2020-08-28.pdf"},{"igName":"MHDv4","tcName":"MHDv4_Internal","docBase":"https://profiles.ihe.net/ITI/MHD/4.0.1"},{"igName":"MHDv410","tcName":"MHDv410_Internal","docBase":"https://profiles.ihe.net/ITI/MHD/4.1.0"}]}]
                         */
                       // console.info(JSON.stringify(response.data))
                    })
                    .catch(function (error) {
                        commit('setError', url + ': ' + error)
                        // console.debug(`${error} for ${url}`)
                    })

            } catch (error) {
                this.$store.commit('setError', url + ': ' +  error)
            }
        },
    }
}
