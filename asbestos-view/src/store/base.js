import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {LOG, PROXY} from '../common/http-common'
import {CHANNEL} from '../common/http-common'

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
            channelURLs: [], // for this session { id:  ... , url: ... , site: ....}
            errors: [],

            proxyBase: null,
        }
    },
    mutations: {
        setProxyBase(state, value) {
            state.proxyBase = value
        },
        setError(state, error) {
            state.errors.push(error)
        },
        clearError(state) {
            state.errors = []
        },
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
            } else {
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
            state.channelIds = channelIds.sort()
        },
        installChannelURLs(state, urls) {
            state.channelURLs = urls
        },
    },
    actions: {
        loadSessions({commit}) {
            const url = `CHANNEL/sessionNames`
            CHANNEL.get('sessionNames')
                .then(response => {
                    commit('setSessions', response.data)
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(error)
                })
//            commit('setSessions', ['default'])
        },
        loadChannelNames({commit, state}) {
            const url = `CHANNEL/channel`
            PROXY.get('channel')
                .then(response => {
                    const fullChannelIds = response.data
                    const theFullChannelIds = fullChannelIds.filter(id => {
                        return id.startsWith(state.session + '__')
                    })
                    const ids = theFullChannelIds.map(fullId => {
                        return fullId.split('__')[1]
                    })
                    commit('installChannelIds', ids)
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(error)
                })
        },
        loadChannelNamesAndURLs({commit}) {
            const url = `CHANNEL/channels/all`
            CHANNEL.get('channels/all')
                .then(response => {
                    let ids = []
                    response.data.forEach(item => {
                        ids.push(item.id)
                    })
                    commit('installChannelIds', ids)
                    commit('installChannelURLs', response.data)
                })
                .catch(e => {
                    commit('setError', url + ': ' + e)
                })
        },
        loadChannel({commit}, fullId) {
            const url = `CHANNEL/${fullId}`
            return CHANNEL.get(fullId)
                .then(response => {
                    commit('installChannel', response.data)
                    return response.data
                })
                .catch(e => {
                    commit('setError', url + ': ' + e)
                    console.error('channel/' + fullId + ' ' + e)
                })
        },
        loadProxyBase({commit, state}) {
            if (state.proxyBase)
                return
            const url = `ProxyBase`
            LOG.get(url)
                .then(response => {
                    commit('setProxyBase', response.data)
                })
                .catch (e => {
                    commit('setError', url + ': ' + e)
                    console.error('ProxyBase' + ' ' + e)
                })
        }
    },
    getters: {

    }
}
