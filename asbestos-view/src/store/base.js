import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {LOG, PROXY} from '../common/http-common'
import {CHANNEL} from '../common/http-common'

export const baseStore = {
    state() {
        return {
            session: 'default',   // name of current
            sessionNames:[],
            sessionConfigs: {},

            channelId: "default",  // current
            channelIds: [],  // for all sessions
            channel: null,   // current configuration matching channelId

            environments: [
                'default',
            ],

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
        setSessionConfig(state, config) {
            state.sessionConfigs[config.name] = config;
        },
        setSessionNames(state, sessions) {
            state.sessionNames = sessions
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
        addChannelIds(state, channelIds) {
            const currentChannelIds = state.channelIds;
            const updatedChannelIds = currentChannelIds.concat(channelIds);
            state.channelIds = updatedChannelIds.sort();
        },
        installChannelURLs(state, urls) {
            state.channelURLs = urls
        },
    },
    actions: {
        // used to initialize FTK
        async initSessionsStore({commit}) {
            let url
            try {
                url = `CHANNEL/sessionNames`
                let data = await CHANNEL.get('sessionNames');
                const sessionNames = data.data;
                console.log(`sessionNames ${sessionNames}`);
                commit('setSessionNames', sessionNames);

                const promises = [];
                sessionNames.forEach(sessionId => {
                    url = `CHANNEL/sessionConfig/${sessionId}`;
                    const promise = CHANNEL.get(`sessionConfig/${sessionId}`);
                    promises.push(promise);
                });
                Promise.all(promises)
                    .then(results => {
                        results.forEach(result => {
                            //console.log(`result is ${result}`);
                            const config = result.data;
                            //console.log(`config is ${config}`);
                            commit('setSessionConfig', config);
                        })
                });

                url = `CHANNEL/channels/all`;
                let result = await CHANNEL.get('channels/all');
                data = result.data;
                console.log(`data is ${data}`)
                let ids = [];
                data.forEach(item => {
                    console.log(`item is ${item}`)
                    ids.push(item.id);
                });
                commit('installChannelIds', ids.sort());


            } catch (error) {
                commit('setError', url + ': ' + error)
                console.error(`${error} for ${url}`)
            }
        },
        // obsolete?
        async selectSession({commit}, sessionId) {
            commit('setSession', sessionId);
            const url = `CHANNEL/sessionConfig/${sessionId}`
            CHANNEL.get(`sessionConfig/${sessionId}`)
                .then(response => {
                    commit('setSessionConfig', response.data)
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(`${error} for ${url}`)
                })
        },
        newSession({commit}, newName) {
            const url = `addSession/${newName}`
            console.log(`${url}`)
            CHANNEL.get(`${url}`)
                .then(response => {
                    commit('setSessions', response.data)
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(`${error} for ${url}`)
                })
        },
        delSession({commit}, name) {
            const url = `delSession/${name}`
            console.log(`${url}`)
            CHANNEL.get(`${url}`)
                .then(response => {
                    commit('setSessions', response.data)
                    if (response.data.length > 0) {
                        const obj = response.data[0];
                        console.log(`new session is ${obj}`)
                        commit('setSession', obj);
                    }
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(`${error} for ${url}`)
                })
        },
        loadChannelNames({commit  /*, state */}) {
            const url = `CHANNEL/channel`
            PROXY.get('channel')
                .then(response => {
                    const fullChannelIds = response.data
                    // const theFullChannelIds = fullChannelIds.filter(id => {
                    //     return id.startsWith(state.session + '__')
                    // })
                    // const ids = theFullChannelIds.map(fullId => {
                    //     return fullId.split('__')[1]
                    // })
                    commit('installChannelIds', fullChannelIds)
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(`${error} for ${url}`)
                })
        },
        loadChannelIds({commit}) {  // for all sessions
            const url = `CHANNEL/channels/all`
            CHANNEL.get('channels/all')
                .then(response => {
                    let ids = []
                    response.data.forEach(item => {
                        ids.push(item.id)
                    })
                    commit('installChannelIds', ids.sort())
                    //commit('installChannelURLs', response.data)
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
                    commit('setProxyBase', response.data.value)
                })
                .catch (e => {
                    commit('setError', url + ': ' + e)
                    console.error('base.loadProxyBase' + ' ' + e)
                })
        }
    },
    getters: {
        getSessionConfig: (state) => {
            return state.sessionConfigs[state.session];
        },
        getProxyBase: (state) => (parms) => {
            if (parms === null)
                return state.proxyBase
            const channelId = parms.channelId
            const sessionId = parms.sessionId
            return `${state.proxyBase}/${sessionId}__${channelId}`
        },
        getFullChannelId: (state) => {
            return `${state.session}__${state.channelId}`
        },
        channelExists: (state) => (theChannelId) => {
            const index = state.channelIds.findIndex(function(channelId) {
                return channelId === theChannelId;
            })
            return index !== -1;
        },
        getEffectiveChannelIds: (state) => {
            if (state.session === 'default')
                return state.channelIds.filter(id => id.startsWith('default__'));
        }
    }
}
