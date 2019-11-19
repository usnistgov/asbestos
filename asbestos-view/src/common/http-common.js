import Vue from 'vue';
import axios from 'axios';
import ServiceProperties from "../plugins/serviceProperties";

Vue.use(ServiceProperties)

const fhirToolkitBase = Vue.prototype.$fhirToolkitBase

export const PROXY = axios.create({
    baseURL: `${fhirToolkitBase}/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})

export const LOG = axios.create({
    baseURL: `${fhirToolkitBase}/log/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})

export const ENGINE = axios.create({
    baseURL: `${fhirToolkitBase}/engine/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})

export const CHANNEL = axios.create({
    baseURL: `${fhirToolkitBase}/channel/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})
