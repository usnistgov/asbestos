import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {LOG, PROXY} from '../common/http-common'
import {CHANNEL} from '../common/http-common'
import {STARTUPSESSION} from "../common/http-common";
import {newChannel} from '@/types/channel'

function setDataStateObj(destObj, srcObj) {
    for (let propKey in destObj) {
        destObj[propKey] = srcObj[propKey]
    }
}


export const baseStore = {
    state() {
        return {
            session: 'default',   // name of current test session, which could be different from the channel test session
            sessionNames:[],
            sessionConfigs: {},
            /**
             * channelName should be used only for creating new channel names,
             * copying a channel within the current test session,
             * or to represent the name of the channel to be selected when switching test sessions
             */
            channelName: 'default',  // current
            channelIds: [],  // for all sessions
            /**
             * channel object should be used for FTK testing purposes since it contains the complete test session and channel name information
             */
            channel: newChannel(),   // current configuration matching channelId
            channelIsNew: false, // newly created means not saved to server yet

            environments: [
                'default',
            ],
            errors: [],
            proxyBase: null,
            ftkInitialized: false,
            ftkChannelLoaded: false,
        }
    },
    mutations: {
        ftkInitComplete(state, value) {
          state.ftkInitialized = value
        },
        ftkChannelLoaded(state, value) {
            state.ftkChannelLoaded = value
        },
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
            Vue.set(state.sessionConfigs, config.name, config)
            // state.sessionConfigs[config.name] = config;
        },
        setSessionNames(state, sessions) {
            state.sessionNames = sessions
        },
        setChannelName(state, channelName) {
            state.channelName = channelName
        },
        setChannel(state, theChannel) {
            if (theChannel !== undefined && theChannel !== null) {
                console.log('set channel: ' + theChannel.channelName)

                setDataStateObj(state.channel, theChannel)
            } else {
                setDataStateObj(state.channel, newChannel())
            }
            if (theChannel === undefined || theChannel === null)
                return

            const targetChannelId = `${theChannel.testSession}__${theChannel.channelName}`;
            let channelIndex = state.channelIds.findIndex( function(channelId) {
                return channelId === targetChannelId;
            })
            if (channelIndex === -1)
                state.channelIds.push(targetChannelId)
            state.channelIsNew = false;
        },
        setChannelIsNew(state, val) {
            state.channelIsNew = val;
        },
        installChannel(state, theChannel) {
            if (theChannel !== undefined && theChannel !== null) {
                // console.log('installed : ' + theChannel.channelName)
                setDataStateObj(state.channel, theChannel)
            } else {
                setDataStateObj(state.channel, newChannel())
            }
            /*
            if (!newChannel) {
                // state.channel = newChannel;
                console.log('store channel is undef? ' + (newChannel === undefined || newChannel === null))
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
             */
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
        // Generally used to initialize FTK by the default Home Page
        // requestedSessionId: only used when accessing FTK directly through a URL that has a specific test session
        async initSessionsStore({commit}, requestedSessionId) {
            let url
            try {
                url = `rw/testSession`
                console.log(url);
                let data = await PROXY.get(url);
                const sessionNames = data.data;
                // console.log(`sessionNames ${sessionNames}`);
                commit('setSessionNames', sessionNames);

                const aggregatePromises = []
                const promises = [];
                sessionNames.forEach(sessionId => {
                    url = `rw/testSession/${sessionId}`
                    // console.log(url);
                    const promise = PROXY.get(url);
                    promises.push(promise);
                });
                aggregatePromises.push(
                    Promise.all(promises)
                    .then(results => {
                        results.forEach(result => {
                            //console.log(`result is ${result}`);
                            const config = result.data;
                            //console.log(`config is ${config}`);
                            commit('setSessionConfig', config);
                        })
                }));

                // url = `LOG/startupSession`;
                // data = await LOG.get('startupSession');
                // let startupSession = data.data;
                const startupSession = (requestedSessionId !== undefined && requestedSessionId !== null && requestedSessionId !== '') ? requestedSessionId : STARTUPSESSION
                console.log('Setting startup session to ' + startupSession)
                commit('setSession', startupSession);

                url = `CHANNEL/channels/all`;
                console.log(url);
                aggregatePromises.push(CHANNEL.get('channels/all')
                    .then(result => {
                    data = result.data;
                    let ids = [];
                    data.forEach(item => {
                        ids.push(item.id);
                    });
                    commit('installChannelIds', ids.sort());
                }))

                Promise.all(aggregatePromises)
                    .then(() => {
                       commit('ftkInitComplete',true)
                    })

            } catch (error) {
                commit('setError', url + ': ' + error)
                console.error(`${error} for ${url}`)
            }
        },
        async selectSession({state, commit /*,getters, dispatch*/}, sessionId) {
            if (sessionId === state.session) return;
            const url = `rw/testSession/${sessionId}`
            return PROXY.get(url)
                .then(response => {
                    commit('setSession', sessionId);
                    commit('setSessionConfig', response.data)
                    // const chIds = getters.getEffectiveChannelIds
                    // if (chIds !== undefined && chIds !== null && chIds[0] !== undefined) {
                    //     console.log('Trying to load the first channel in effective session channel list: ' + chIds[0])
                    //     dispatch('loadChannel', chIds[0])
                    // }
                    // commit('installChannel', chIds[0]);
                    // commit('setChannelName', null);

                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(`${error} for ${url}`)
                })
        },
        loadChannelNames({commit  /*, state */}) {
            const url = `CHANNEL/channel`
            PROXY.get('rw/channel')
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
        loadChannel({commit}, paramObj) {
            const fullId = paramObj.channelId
            const raiseFtkCommit = paramObj.raiseFtkCommit
            if (this.channel !== undefined) {
                if (fullId === this.channel.testSession + '__' + this.channel.channelName) {
                    console.log('Returning a cached copy of channel.')
                    return this.channel
                }
            }
            commit('installChannel', null);
            console.log(`loadChannel ${fullId}`)
            const parts = fullId.split('__', 2);
            if (parts.length !== 2 || parts[0] === null || parts[1] === null || parts[1] === 'null')
                return;
            const url = `CHANNEL/${fullId}`
            return CHANNEL.get(fullId)
                .then(response => {
                    commit('installChannel', response.data)
                    commit('setChannelName', parts[1]);
                    if (raiseFtkCommit) {
                        commit('ftkChannelLoaded', true);
                    }
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
        getChannel: (state) => {
           return state.channel
        },
        getChannelName: (state,getters) => {
            return getters.getChannel.channelName
        },
        getChannelTestSession: (state,getters) => {
           return getters.getChannel.testSession
        },
        getSessionConfig: (state) => {
            return state.sessionConfigs[state.session];
        },
        getProxyBase: (state) => (parms) => {
            if (parms === null)
                return state.proxyBase
            const channelName = parms.channelName
            const sessionId = parms.sessionId
            return `${state.proxyBase}/${sessionId}__${channelName}`
        },
        getChannelId: (state) => {
            return `${state.channel.testSession}__${state.channel.channelName}`
        },
        channelExists: (state) => (theChannelId) => {
            const index = state.channelIds.findIndex(function(channelId) {
                return channelId === theChannelId;
            })
            return index !== -1;
        },
        /**
         * This method only returns local channel excluding Includes
         */
        getChannelIdsForSession: (state) => (session) => {
            return state.channelIds.filter(id => id.startsWith(`${session}__`));
        },
        getChannelIdsForCurrentSession: (state,getters) => {
            return state.channelIds.filter(id => {
                try {
                    const parts = id.split('__')
                    const chSessionPart = parts[0]
                    const includesSession = [state.session].concat(getters.getSessionIncludes(state.session))
                    // console.log(`looking for ${includesSession}`)
                    return includesSession.includes(chSessionPart)
                } catch (e) {
                   console.log(e)
                   return false
                }
            })
        },
        getChannelNamesForCurrentSession: (state, getters) => {
            return getters.getChannelIdsForCurrentSession.map(function (channelId) {
                const parts = channelId.split('__');
                // Hide the session name if channel is already part of the same session
                if (state.session === parts[0]) {
                    return parts[1];
                } else {
                    // Full Id
                    return channelId
                }
            })
        },
        getSessionIncludes: (state) => (session) => {
              const config = state.sessionConfigs[session];
              if (config === undefined || config === null)
                  return [];
              return config.includes;
        },
        /*
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
         */
    }
}
