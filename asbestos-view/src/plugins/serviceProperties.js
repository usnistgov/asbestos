import axios from 'axios';

// Service.properties.js

const ServiceProperties = {
    install (Vue) {

        // Add Vue instance methods by attaching them to Vue.prototype.
        if (process.env.NODE_ENV === 'production') {
            /* Needs to be tested */
            axios
                .get('/serviceProperties.json')
                .then(response => (Vue.prototype.$fhirToolkitBase = response.data.fhirToolkitBase))
                .catch(error => console.log(error))
        } else if (process.env.NODE_ENV === 'development') {
            Vue.prototype.$fhirToolkitBase = process.env.VUE_APP_FHIR_TOOLKIT_BASE
            console.log( process.env.VUE_APP_FHIR_TOOLKIT_BASE)
            console.log( Vue.prototype.$fhirToolkitBase)
        } else {
            console.log('Error: unrecognized process.env.NODE_ENV = ' + process.env.NODE_ENV)
        }
    }
}

export default ServiceProperties;