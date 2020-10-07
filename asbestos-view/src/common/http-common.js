import axios from 'axios';

export var PROXY = null
export var ENGINE = null
export var LOG = null
export var VALIDATE = null
export var CHANNEL = null
export var FHIRTOOLKITBASEURL = null
export var HTTPSFHIRTOOLKITBASEURL = null
export var toolkitBaseToUse = null
export var PROJECTVERSION = null
export var ASBTS_USERPROPS =  {
    signedIn : false,
    bauser : "", /* Basic authentication username */
    bapw : "" /* Basic authentication password */
};

export const UtilFunctions = {
    getChannelBase :function(channel) {
        return FHIRTOOLKITBASEURL + "/proxy/" + channel.testSession + "__" + channel.channelName
    },
    getHttpsChannelBase :function(channel) {
        return HTTPSFHIRTOOLKITBASEURL + "/proxy/" + channel.testSession + "__" + channel.channelName
    },
    getTestEngineBase: function() {
        return toolkitBaseToUse + "/engine"
    },
    getProxyBase: function() {
        return toolkitBaseToUse + "/proxy"
    },
    isHttpsMode: function() {
        return toolkitBaseToUse === HTTPSFHIRTOOLKITBASEURL
    },
    getWssBase: function() {
        return HTTPSFHIRTOOLKITBASEURL.replace("https","wss")
    },
}

export async function getServiceProperties() {
    if (process.env.NODE_ENV === 'production') {
        return await axios
            .get('/serviceProperties.json')
    } else {
        return {
            data : {
                httpsFhirToolkitBase : process.env.VUE_APP_HTTPS_FHIR_TOOLKIT_BASE,
                fhirToolkitBase : process.env.VUE_APP_FHIR_TOOLKIT_BASE,
                projectVersion : "Development"
            }
        };
    }
}


export async function initServiceProperties() {
    if (FHIRTOOLKITBASEURL === null) {
        try {
            await getServiceProperties().then(response => {
                    PROJECTVERSION = `v${response.data.projectVersion}`
                    FHIRTOOLKITBASEURL = response.data.fhirToolkitBase
                    HTTPSFHIRTOOLKITBASEURL = response.data.httpsFhirToolkitBase

                    if (window.location.protocol === 'https:') {
                        toolkitBaseToUse = HTTPSFHIRTOOLKITBASEURL
                    } else {
                        toolkitBaseToUse = FHIRTOOLKITBASEURL
                    }

                    PROXY = axios.create({
                        baseURL: toolkitBaseToUse + '/',
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                    ENGINE = axios.create({
                        baseURL: toolkitBaseToUse + '/engine/',
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                    LOG = axios.create({
                        baseURL: `${toolkitBaseToUse}/log/`,
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                VALIDATE = axios.create({
                    baseURL: `${toolkitBaseToUse}/validate/`,
                    headers: {
                        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                    },
                    params: {
                        crossdomain: true,
                    }
                })
                    CHANNEL = axios.create({
                        baseURL: `${toolkitBaseToUse}/channel/`,
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                console.log(`initServiceProperties done`)
                console.log(`FHIR Toolkit base is ${toolkitBaseToUse}`)
                }
            )
        } catch (e) {
            console.log('initServiceProperties Error:' + e);
            return false
        }
    }
    return true
}

