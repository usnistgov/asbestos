import axios from 'axios';

export var PROXY = null
export var ENGINE = null
export var LOG = null
export var CHANNEL = null
export var FHIRTOOLKITBASEURL = null
export var PROJECTVERSION = null

export const UtilFunctions = {
    getChannelBase :function(channel) {
        return FHIRTOOLKITBASEURL + "/proxy/" + channel.testSession + "__" + channel.channelId
    }
}

export async function getServiceProperties() {
    if (process.env.NODE_ENV === 'production') {
        return await axios
            .get('/serviceProperties.json')
    } else {
        return {
            data : {
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
                    const constFhirToolkitBaseUrl = response.data.fhirToolkitBase

                    FHIRTOOLKITBASEURL = constFhirToolkitBaseUrl
                    console.log('fhirToolkitBaseUrl is: ' + constFhirToolkitBaseUrl)

                    PROXY = axios.create({
                        baseURL: constFhirToolkitBaseUrl + '/',
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                    ENGINE = axios.create({
                        baseURL: constFhirToolkitBaseUrl + '/engine/',
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                    LOG = axios.create({
                        baseURL: `${constFhirToolkitBaseUrl}/log/`,
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                    CHANNEL = axios.create({
                        baseURL: `${constFhirToolkitBaseUrl}/channel/`,
                        headers: {
                            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
                        },
                        params: {
                            crossdomain: true,
                        }
                    })
                }
            )
        } catch (e) {
            console.log('initServiceProperties Error:' + e);
            return false
        }
    }
    return true
}

