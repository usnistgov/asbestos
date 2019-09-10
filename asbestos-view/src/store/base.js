import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {PROXY} from '../common/http-common'

// TODO add About page and credit <div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/"             title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/"             title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

export const baseStore = {
    state() {
        return {
            session: 'default',
            // environment: 'default',

            testSession: null,
            channelId: null,







            sessions: [],
            environments: [
                'default', 'e1'
            ],


            // these are loaded before the full channel definitions
            //
            // fullChannelId can exist without channel - ChannelView.fetch() will notice this
            // and fetch channel from server
            fullChannelIds: [],  // testSession__channelId
        }
    },
    mutations: {
        setSession(state, theSession) {
            state.session = theSession
        },
        // setEnvironment(state, theEnvironment) {
        //     state.environment = theEnvironment
        // },

        setSessions(state, sessions) {
            state.sessions = sessions
        },
        setChannelId(state, channelId) {
            state.channelId = channelId
        },
        setChannel(state, theChannel) {
            state.channel = theChannel
            if (theChannel === null)
                return
            const fullId = `${theChannel.testSession}__${theChannel.channelId}`
            let channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === fullId
            })
            if (channelIndex === -1)
                state.fullChannelIds.push(fullId)
        },
        installChannel(state, newChannel) {  // adds to end
            const thisChannelId = newChannel.testSession + '__' + newChannel.channelId
            let channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === thisChannelId
            })
            if (channelIndex === -1) {
                state.fullChannelIds.push(thisChannelId)
                console.log(`install new channel - id=${newChannel.channelId}`)
                state.channel = newChannel
            } else {
                console.log(`install replacement channel - id=${newChannel.channelId}`)
                state.channel = newChannel
            }
        },
        deleteChannel(state, theFullChannelId) {
            const channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === theFullChannelId
            })
            if (channelIndex === undefined)
                return
            state.fullChannelIds.splice(channelIndex, 1)
        },
        installChannelIds(state, theFullChannelIds) {
            state.fullChannelIds = theFullChannelIds
        },


    },
    actions: {
        loadSessions({commit}) {
            commit('setSessions', ['default', 'ts1'])
        },
        loadChannelNames({commit}) {  //  same function exists in ChannelNav
            PROXY.get('channel')
                .then(response => {
                    commit('installChannelIds', response.data.sort())
                })
                .catch(function (error) {
                    console.error(error)
                })
        },
    },
    getters: {

    }
}
