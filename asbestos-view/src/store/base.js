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

            channelName: 'default',  // current
            channelIds: [],  // for all sessions
            channel: null,   // current configuration matching channelId
            channelIsNew: false, // newly created means not saved to server yet

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
        setChannelName(state, channelName) {
            state.channelName = channelName
        },
        setChannel(state, theChannel) {
            state.channel = theChannel
            if (theChannel === null)
                return
            const targetChannelId = `${theChannel.testSession}__${theChannel.channelName}`;
            let channelIndex = state.channelIds.findIndex( function(channelId) {
                return channelId === targetChannelId;
            })
            if (channelIndex === -1)
                state.channelIds.push(targetChannelId)
            state.channelIsNew = false;
        },
        setChannelIsNew(state) {
            state.channelIsNew = true;
        },
        installChannel(state, newChannel) {
            if (!newChannel) {
                state.channel = newChannel;
                return;
            }
            // const targetChannelId = `${newChannel.testSession}__${newChannel.channelName}`;
            // let channelIndex = state.channelIds.findIndex( function(channelId) {
            //     return channelId === targetChannelId;
            // })
            // if (channelIndex === -1) {
            //     state.channelIds.push(targetChannelId)
            // } else {
                state.channel = newChannel
            // }
            state.channel = newChannel
            state.channelIsNew = false;
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
                console.log(url);
                let data = await CHANNEL.get('sessionNames');
                const sessionNames = data.data;
                console.log(`sessionNames ${sessionNames}`);
                commit('setSessionNames', sessionNames);

                const promises = [];
                sessionNames.forEach(sessionId => {
                    url = `CHANNEL/sessionConfig/${sessionId}`;
                    console.log(url);
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
                console.log(url);
                let result = await CHANNEL.get('channels/all');
                data = result.data;
                let ids = [];
                data.forEach(item => {
                    ids.push(item.id);
                });
                commit('installChannelIds', ids.sort());


            } catch (error) {
                commit('setError', url + ': ' + error)
                console.error(`${error} for ${url}`)
            }
        },
        async selectSession({state, commit}, sessionId) {
            if (sessionId === state.session) return;
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
                    commit('setSessionNames', response.data)
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
                    commit('setSessionNames', response.data)
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
                })
                .catch(e => {
                    commit('setError', url + ': ' + e)
                })
        },
        loadChannel({commit}, fullId) {
            commit('installChannel', null);
            console.log(`loadChannel ${fullId}`)
            const parts = fullId.split('__', 2);
            if (parts.length !== 2 || parts[0] === null || parts[1] === null || parts[1] === 'null')
                return;
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
        getChannelId: (state) => {
            return `${state.session}__${state.channelName}`
        },
        channelExists: (state) => (theChannelId) => {
            const index = state.channelIds.findIndex(function(channelId) {
                return channelId === theChannelId;
            })
            return index !== -1;
        },
        getChannelIdsForSession: (state) => (session) => {
            return state.channelIds.filter(id => id.startsWith(`${session}__`));
        },
        getChannelIdsForCurrentSession: (state) => {
            return state.channelIds.filter(id => id.startsWith(`${state.session}__`));
        },
        getChannelNamesForCurrentSession: (state, getters) => {
            return getters.getChannelIdsForCurrentSession.map(function (channelId) {
                const parts = channelId.split('__');
                return parts[1];
            })
        },
        getSessionIncludes: (state) => (session) => {
              const config = state.sessionConfigs[session];
              if (!config)
                  return [];
              return config.includes;
        },
        getEffectiveChannelIds: (state, getters) => {
            let session = state.session;
            let ids = getters.getChannelIdsForSession(session);
            // let includedSessions = getters.getSessionIncludes(session);
            // while ( Array.isArray(includedSessions) && includedSessions.length > 0) {
            //     session = includedSessions.pop();
            //     let channelIds = getters.getChannelIdsForSession(session);
            //     ids = ids.concat(channelIds);
            //     getters.getSessionIncludes(session).forEach(inc => includedSessions.push(inc));
            // }
            return ids;
        }
    }
}
