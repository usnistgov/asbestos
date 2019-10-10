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
            channelId: "default",

            sessions: [],
            environments: [
                'default',
            ],

            channelIds: [],  // for this session
            error: null,
        }
    },
    mutations: {
        setError(state, error) {
            state.error = error
        },
        setSession(state, theSession) {
//            console.log(`setSession = ${theSession}`)
            state.session = theSession
        },
        // setEnvironment(state, theEnvironment) {
        //     state.environment = theEnvironment
        // },

        setSessions(state, sessions) {
//            console.log(`setSessions = ${sessions}`)
            state.sessions = sessions
        },
        setChannelId(state, channelId) {
            state.channelId = channelId
        },
        setChannel(state, theChannel) {
//            console.log(`mutatation setChannel ${theChannel}`)
            state.channel = theChannel
            if (theChannel === null)
                return
            let channelIndex = state.channelIds.findIndex( function(channelId) {
                return channelId === theChannel.channelId
            })
            if (channelIndex === -1)
                state.channelIds.push(theChannel.channelId)
        },
        installChannel(state, newChannel) {  // adds to end
            let channelIndex = state.channelIds.findIndex( function(channelId) {
                return channelId === newChannel.channelId
            })
            if (channelIndex === -1) {
                state.channelIds.push(newChannel.channelId)
//                console.log(`mutation install new channel - id=${newChannel.channelId}`
            } else {
                console.log(`mutation install replacement channel - id=${newChannel.channelId}`)
//                state.channel = newChannel
            }
            state.channel = newChannel
        },
        deleteChannel(state, theChannelId) {
            const channelIndex = state.channelIds.findIndex( function(channelId) {
                return channelId === theChannelId
            })
            if (channelIndex === undefined)
                return
            state.channelIds.splice(channelIndex, 1)
        },
        installChannelIds(state, channelIds) {
//            console.log(`mutation installChannelIds ${channelIds}`)
            state.channelIds = channelIds.sort()
        },


    },
    actions: {
        loadSessions({commit}) {
            commit('setSessions', ['default'])
        },
        loadChannelNames({commit, state}) {  //  same function exists in ChannelNav
            PROXY.get('channel')
                .then(response => {
                    const fullChannelIds = response.data
                    const theFullChannelIds = fullChannelIds.filter(id => {
                        return id.startsWith(state.session + '__')
                    })
                    const ids = theFullChannelIds.map(fullId => {
                        return fullId.split('__')[1]
                    })
//                    console.log(`action loadChannelNames ${ids}`)
                    commit('installChannelIds', ids)
                })
                .catch(function (error) {
                    console.error(error)
                })
        },
    },
    getters: {

    }
}
