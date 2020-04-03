/*
module.exports = {
    chainWebpack: config => config.optimization.minimize(false),
    filenameHashing: false
}
*/
/*
module.exports = {
    devServer: {
        disableHostCheck: true
    }
}
 */

const fs = require('fs')

var devServer1 = null

console.log("VUE_APP_PROTOCOL_TO_USE is: " + process.env.VUE_APP_PROTOCOL_TO_USE)

if (process.env.VUE_APP_PROTOCOL_TO_USE === 'https') {
    devServer1 =  {
        open: process.platform === 'darwin',
            host: 'fhirtoolkit.test',
            port: 8082,
            https: {
                key: fs.readFileSync('../asbestos-assembly/bundled-tomcat-9.0.26/Toolkits/FhirToolkit/fhirtoolkitui-certificate/private_key.pem'),
                cert: fs.readFileSync('../asbestos-assembly/bundled-tomcat-9.0.26/Toolkits/FhirToolkit/fhirtoolkitui-certificate/certificate.pem')
            },
            hotOnly: false,
    }
} else {
    devServer1 =  {
        open: process.platform === 'darwin',
        port: 8082,
        hotOnly: false,
    }
}

module.exports = {
    devServer: devServer1 ? devServer1 : {}
}
