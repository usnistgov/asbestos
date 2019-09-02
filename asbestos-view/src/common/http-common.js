import axios from 'axios';

export const PROXY = axios.create({
    baseURL: `http://localhost:8081/asbestos/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})

export const LOG = axios.create({
    baseURL: `http://localhost:8081/asbestos/log/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})

export const ENGINE = axios.create({
    baseURL: `http://localhost:8081/asbestos/engine/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})
