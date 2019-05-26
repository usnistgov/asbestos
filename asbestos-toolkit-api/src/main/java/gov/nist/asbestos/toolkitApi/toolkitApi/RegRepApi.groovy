package gov.nist.asbestos.toolkitApi.toolkitApi

import gov.nist.asbestos.simapi.toolkit.configDatatypes.server.SimulatorActorType
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.SimConfig
import gov.nist.asbestos.simapi.toolkit.toolkitServicesCommon.SimId
import groovy.transform.TypeChecked

@TypeChecked
class RegRepApi {
    String toolkitPort = '8888'
    String toolkitUrl = "http://localhost:${toolkitPort}/xdstools2"
    String testSession = 'default'
    String regrepId = 'rr'
    BasicSimParameters regrepParms
    String environmentName = 'default'
    private SimulatorBuilder builder = null
    private EngineSpi engine = null

    SimConfig regrepSimConfig = null

    RegRepApi() {
        buildRegrepParms()
    }

    RegRepApi(String toolkitUrl) {
        super()
        this.toolkitUrl = toolkitUrl
    }

    private buildRegrepParms() {
        regrepParms = new BasicSimParameters()
        regrepParms.id = regrepId
        regrepParms.user = environmentName
        regrepParms.actorType = SimulatorActorType.REPOSITORY_REGISTRY
        regrepParms.environmentName = environmentName
    }

    RegRepApi withTestSession(String testSessoin) {
        this.testSession = testSessoin
        buildRegrepParms()
        return this
    }

    RegRepApi withRegrepSimId(String id) {
        this.regrepId = id
        buildRegrepParms()
        return this
    }

    RegRepApi withEnvironment(String name) {
        this.environmentName = name
        buildRegrepParms()
        return this
    }

    RegRepApi createRegRepIfNeeded() {
        initEngine()
        SimId regrepSimId = engine.getSimId(regrepParms)
        if (!engine.exists(regrepSimId)) {
            regrepSimConfig = engine.create(regrepParms)
        }
        this
    }

    RegRepApi deleteRegrep() {
        initEngine()
        engine.deleteIfExists(regrepId, environmentName)
        this
    }

    private initEngine() {
        if (!builder)
            builder = new SimulatorBuilder(toolkitUrl)
        if (!engine)
            engine = builder.engine
    }

}
