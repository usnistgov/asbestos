import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {LOG} from '../common/http-common'
import {PROXY} from '../common/http-common'
import {UtilFunctions} from "../common/http-common";

export const logStore = {
    state() {
        return {
            // private to the log viewer
            eventSummaries: [],  // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false, ipAddr: addr }
            // session and channel are based on the channel test control panel, these should not be needed for logStore
            // session: null,
            // channel: null,
            loaded: false,
            analysis: null,
            validationServer: null,
            validationResult: null,
//            eventIdOfAnalysis: null,
        }
    },
    mutations: {
        setValidationResult(state, result) {
            state.validationResult = result
        },
        setValidationServer(state, server) {
            state.validationServer = server
        },
        setAnalysis(state, analysis) {
            state.analysis = analysis
        },
        resetLogLoaded(state) {
            state.loaded = null
        },
        setLogLoaded(state, value) {
            state.loaded = value
        },
        setEventSummaries(state, summaries) {
            // console.debug('In setEventSummaries')
            state.eventSummaries = summaries
            state.loaded = true
        },
        prependEventSummaries(state, summaries) {
            // console.debug('In prependEventSummaries')
            summaries.forEach((e,idx) => {
                state.eventSummaries.splice(0 /* begin index */, 0/* remove=0 or insert */, summaries[idx])
            })
        },
        /*
        setLogSession(state, session) {
            state.session = session
        },
        setLogChannel(state, channel) {
            state.channel = channel
        }
         */
    },
    getters: {
        ipAddresses: state => {
            const holder =  state.eventSummaries.map(x => {
                return x.ipAddr
            })
            return holder.filter((item, index) => {
                return holder.indexOf(item) === index
            })
        }
    },
    actions: {
        async loadSpecificEventSummaries({commit /*, rootState*/}, parms) {
            // const channel = parms.channel
            // const session = parms.session
            // commit('setLogChannel', channel)
            // commit('setLogSession', session)
            if (parms.channel === undefined || parms.channel === null) {
                const errorMsg = 'Channel object is undefined or null in logStore.loadSpecificEventSummaries'
                commit('setError', errorMsg)
                console.error(errorMsg)
                return
            }
            if (parms.testSession=== undefined || parms.testSession === null) {
                const errorMsg = 'Session not set in logStore.loadSpecificEventSummaries'
                commit('setError', errorMsg)
                console.error(errorMsg)
                return
            }
            /*
            if (!rootState.base.channel.channelName === undefined || rootState.base.channel.channelName === null) {
                const errorMsg = 'Channel name not set in logStore.loadSpecificEventSummaries'
                commit('setError', errorMsg)
                console.error(errorMsg)
                return
            }
             */
            const url = UtilFunctions.getLogListUrl(parms, parms.testSession, parms.channel)
            // console.info("loadSpecificEventSummaries: " + url)
            try {
                commit('resetLogLoaded')
                const rawSummaries = await PROXY.post(url, parms.postData)
                if (Array.isArray(rawSummaries.data) && rawSummaries.data.length > 0) {
                    const eventSummaries = rawSummaries.data.sort((a, b) => {
                        if (a.eventName < b.eventName) return 1
                        return -1
                    })
                    if ('prepend' in parms && parms.prepend === true) {
                        commit('prependEventSummaries', eventSummaries)
                    } else {
                        commit('setEventSummaries', eventSummaries)
                    }
                } else {
                    console.error('loadSpecificEventSummaries:rawSummaries is not an array of length > 0.')
                }
            } catch (error) {
                commit('setError', `${error} for LOGLIST/${url}`)
                console.error(`${error} for ${url}`)
            }
        },
        async loadEventSummaries({commit /*, rootState */}, parms) {
            // const channel = parms.channel
            // const session = parms.session
            // commit('setLogChannel', channel)
            // commit('setLogSession', session)
            if (parms.channel === undefined || parms.channel === null) {
                const errorMsg = 'Channel object is undefined or null in logStore.loadEventSummaries'
                commit('setError', errorMsg)
                console.error(errorMsg)
                return
            }
            if (parms.testSession === undefined || parms.testSession === null) {
                const errorMsg = 'Session not set in logStore.loadEventSummaries'
                commit('setError', errorMsg)
                console.error(errorMsg)
                return
            }
            /*
            if (!rootState.base.channel.channelName === undefined || rootState.base.channel.channelName === null) {
                const errorMsg = 'Channel name not set in logStore.loadEventSummaries'
                commit('setError', errorMsg)
                console.error(errorMsg)
                return
            }
             */
            const url = UtilFunctions.getLogListUrl(parms, parms.testSession, parms.channel)
            console.info(url)
            try {
                const methodParams =
                    {
                        params: {
                            summaries: 'true',
                            itemsPerPage : ('itemsPerPage' in parms ? parms.itemsPerPage : -1),
                            pageNum: ('page' in parms ? parms.page : -1),
                            previousPageSize: ('previousPageSize' in parms ? parms.previousPageSize : -1)
                        }
                    }
                commit('resetLogLoaded')
                const rawSummaries = await PROXY.get(url, methodParams)
                // console.log(JSON.stringify(rawSummaries.data))
                // console.log('rawSummaries.data isArray: ' + Array.isArray(rawSummaries.data))
                // console.log('rawSummaries.data typeof: ' + typeof rawSummaries.data)
                if (Array.isArray(rawSummaries.data) && rawSummaries.data.length > 0) {
                    const eventSummaries = rawSummaries.data.sort((a, b) => {
                        if (a.eventName < b.eventName) return 1
                        return -1
                    })
                    commit('setEventSummaries', eventSummaries)
                } else {
                    console.error('loadEventSummaries:rawSummaries is not an array of length > 0.')
                }
            } catch (error) {
                commit('setError', `${error} for LOGLIST/${url}`)
                console.error(`${error} for ${url}`)
            }
        },

        // focusAnchor and validation are never used.  Validation is always done.
        async getLogEventAnalysis({commit}, parms) {
            const channel = parms.channel
            const session = parms.session
            const eventId = parms.eventId
            let focusUrl = parms.url
            //console.log(`focusUrl is ${focusUrl}`)
            let anchor
            if (focusUrl) {
                const focusUrlPoundi = focusUrl.indexOf('#')
                if (focusUrlPoundi !== -1) {
                    anchor = focusUrl.substring(focusUrlPoundi + 1)
                    focusUrl = focusUrl.substring(0, focusUrlPoundi)
                }
            }
            //console.log(`focusUrl is ${focusUrl} anchor is ${anchor}`)
            const requestOrResponse = parms.requestOrResponse

            if (!focusUrl)
                focusUrl = ""
            if (!anchor)
                anchor = ""

            const url = `analysis/event/${session}/${channel}/${eventId}/${requestOrResponse}?validation=true&focusUrl=${focusUrl}&focusAnchor=${anchor}`
            try {
                const result = await LOG.get(url)
                //const data = {analysis: result.data, eventId: eventId}
                commit('setAnalysis', result.data)
                //console.log(`analysis available`)
            } catch (error) {
                commit('setError', `${error} for LOG/${url}`)
                console.error(`${error} for ${url}`)
            }
        },
        async getLogEventAnalysisForObject({commit}, parms) {
            const ignoreBadRefs = parms.ignoreBadRefs
            let resourceUrl = parms.resourceUrl
            const urlObj = new URL(resourceUrl);
            const urlObjParams = urlObj.searchParams;
            console.log(`resourceUrl = ${resourceUrl}`)
            if (resourceUrl)
               resourceUrl = resourceUrl.trim()
            const gzip = parms.gzip
            const eventId = parms.eventId ? parms.eventId : "";
            // Using new URL() to parse parameters, it must have an absolute path so "phony" is added.
            // It is removed below.
            const phony = 'http://localhost';
            let url = new URL(`${phony}/analysis/url`);
            console.log(`getAnalysis ${url}`)
            //?url=${resourceUrl};gzip=${gzip};ignoreBadRefs=${ignoreBadRefs};eventId=${eventId}`);
            //url.searchParams.append(urlObjParams.get("focusUrl"));
            for (let key in urlObjParams.keys()) {
                const value = urlObjParams.get(key);
                console.log(`key=${key} value=${value}`);
                if (value)
                    url.searchParams.append(key, value);
            }
            console.log(`url1 is ${url}`);
            if (gzip)
                url.searchParams.append('gzip', gzip);
            if (eventId)
                url.searchParams.append('eventId', eventId);
            if (ignoreBadRefs)
                url.searchParams.append('ignoreBadRefs', ignoreBadRefs);
            if (resourceUrl)
                url.searchParams.append('url', resourceUrl);
            url = url.toString().split(phony + "/", 2)[1];
            console.log(`final url is ${url}`);
            try {
                const result = await LOG.get(url)
                commit('setAnalysis', result.data)
            } catch (error) {
                commit('setError', `${error}  for LOG/${url}`)
                console.error(`${error} for ${url}`)
            }
        },
        async analyseResource({commit}, resourceString) {
            const url= `analysis/text`
            try {
                const result = await LOG.post(url, {string: resourceString})
                commit('setAnalysis', result.data)
            }   catch (error) {
                commit('setError', `${error} for LOG/${url}`)
                console.error(error)
            }
        },
        async getValidationServer({commit}) {
            const url = `ValidationServer`
            try {
                const result = await LOG.get(url)
                commit('setValidationServer', result.data.value)
            } catch (error) {
                commit('setError', `${error}  for LOG/${url}`)
                console.error(error)
            }
        },
        async getValidation({commit}, parms) {
            const resourceType = parms.resourceType
            const qurl = parms.url
            const url = `Validation/${resourceType}?url=${qurl}`
            try {
                const result = await LOG.get(url)
                commit('setValidationResult', result.data)
            } catch (error) {
                commit('setError', `${error}  for LOG/${url}`)
                console.error(error)
            }
        }
    }
}
