import axios from 'axios';

const port = '8081'

export const PROXY = axios.create({
    baseURL: `http://localhost:${port}/asbestos/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})

export const LOG = axios.create({
    baseURL: `http://localhost:${port}/asbestos/log/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})

export const ENGINE = axios.create({
    baseURL: `http://localhost:${port}/asbestos/engine/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})
