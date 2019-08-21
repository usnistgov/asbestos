import axios from 'axios';

export const PROXY = axios.create({
    baseURL: `http://localhost:8081/proxy/`,
    headers: {
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, PATCH, DELETE'
    },
    params: {
        crossdomain: true,
    }
})