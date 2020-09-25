import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {LOG} from '../common/http-common'

export const logStore = {
    state() {
        return {
            // private to the log viewer
            eventSummaries: [],  // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false, ipAddr: addr }
            session: null,
            channel: null,
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
            state.loaded = false
        },
        setEventSummaries(state, summaries) {
            state.eventSummaries = summaries
            state.loaded = true
        },
        setLogSession(state, session) {
            state.session = session
        },
        setLogChannel(state, channel) {
            state.channel = channel
        }
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
        async loadEventSummaries({commit, rootState}, parms) {
            const channel = parms.channel
            const session = parms.session
            commit('setLogChannel', channel)
            commit('setLogSession', session)
            if (!rootState.base.session) {
                commit('setError', 'Session not set in logStore.loadEventSummaries')
                console.error('Session not set for logStore.loadEventSummaries')
                return
            }
            if (!rootState.base.channelId) {
                commit('setError', 'Channel not set in logStore.loadEventSummaries')
                console.error('Channel not set in logStore.loadEventSummaries')
                return
            }
            const url = `${rootState.base.session}/${rootState.base.channelId}`
            try {
                const rawSummaries = await LOG.get(url, {
                    params: {
                        summaries: 'true'
                    }
                })
                const eventSummaries = rawSummaries.data.sort((a, b) => {
                    if (a.eventName < b.eventName) return 1
                    return -1
                })
                commit('setEventSummaries', eventSummaries)
            } catch (error) {
                commit('setError', `${error} for LOG/${url}`)
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
