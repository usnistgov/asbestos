<template>
    <div>
        <div v-for="(action, actioni) in actions"
             :key="action + actioni">
            <div v-if="action.operation" class="operation-box">
                <div class="operation-name">Operation</div>
                <div class="operation-value">{{operationTypeDisplay(action.operation.type)}}</div>

                <div v-if="action.operation.resource" class="spanner operation-box">
                    <div class="name">resource</div>
                    <div class="value">{{action.operation.resource}}</div>
                </div>

                <div v-if="action.operation.label" class="spanner operation-box">
                    <div class="name">label</div>
                    <div class="value">{{action.operation.label}}</div>
                </div>

                <div v-if="action.operation.description" class="spanner operation-box">
                    <div class="name">description</div>
                    <div class="value">{{action.operation.description}}</div>
                </div>

                <div v-if="action.operation.accept" class="spanner operation-box">
                    <div class="name">accept</div>
                    <div class="value">{{action.operation.accept}}</div>
                </div>

                <div v-if="action.operation.contentType" class="spanner operation-box">
                    <div class="name">contentType</div>
                    <div class="value">{{action.operation.contentType}}</div>
                </div>

                <div v-if="action.operation.destination" class="spanner operation-box">
                    <div class="name">destination</div>
                    <div class="value">{{action.operation.destination}}</div>
                </div>

                <div v-if="action.operation.encodeRequestUrl" class="spanner operation-box">
                    <div class="name">encodeRequestUrl</div>
                    <div class="value">{{action.operation.encodeRequestUrl}}</div>
                </div>

                <div v-if="action.operation.method" class="spanner operation-box">
                    <div class="name">method</div>
                    <div class="value">{{action.operation.method}}</div>
                </div>

                <div v-if="action.operation.origin" class="spanner operation-box">
                    <div class="name">origin</div>
                    <div class="value">{{action.operation.origin}}</div>
                </div>

                <div v-if="action.operation.params" class="spanner operation-box">
                    <div class="name">params</div>
                    <div class="value">{{action.operation.params}}</div>
                </div>

                <div v-for="(requestHeader, requestHeaderi) in action.operation.requestHeader"
                     :key="requestHeader + requestHeaderi">
                    <div v-if="requestHeader.field" class="spanner operation-box">
                        <div class="name">requestHeader.field</div>
                        <div class="value">{{requestHeader.field}}</div>
                    </div>

                    <div v-if="requestHeader.value" class="spanner operation-box">
                        <div class="name">requestHeader.value</div>
                        <div class="value">{{requestHeader.value}}</div>
                    </div>
                </div>

                <div v-if="action.operation.requestId" class="spanner operation-box">
                    <div class="name">requestId</div>
                    <div class="value">{{action.operation.requestId}}</div>
                </div>

                <div v-if="action.operation.responseId" class="spanner operation-box">
                    <div class="name">responseId</div>
                    <div class="value">{{action.operation.responseId}}</div>
                </div>

                <div v-if="action.operation.sourceId" class="spanner operation-box">
                    <div class="name">sourceId</div>
                    <div class="value">{{action.operation.sourceId}}</div>
                </div>

                <div v-if="action.operation.targetId" class="spanner operation-box">
                    <div class="name">targetId</div>
                    <div class="value">{{action.operation.targetId}}</div>
                </div>

                <div v-if="action.operation.url" class="spanner operation-box">
                    <div class="name">url</div>
                    <div class="value">{{action.operation.url}}</div>
                </div>
            </div>
            <div v-else class="operation-box">
                <div class="assert-name">Assert</div>
                <div class="assert-value"></div>

                <div v-if="action.assert.label" class="spanner operation-box">
                    <div class="name">label</div>
                    <div class="value">{{action.assert.label}}</div>
                </div>

                <div v-if="action.assert.description" class="spanner operation-box">
                    <div class="name">description</div>
                    <div class="value">{{action.assert.description}}</div>
                </div>

                <div v-if="action.assert.direction" class="spanner operation-box">
                    <div class="name">direction</div>
                    <div class="value">{{action.assert.direction}}</div>
                </div>

                <div v-if="action.assert.compareToSourceId" class="spanner operation-box">
                    <div class="name">compareToSourceId</div>
                    <div class="value">{{action.assert.compareToSourceId}}</div>
                </div>

                <div v-if="action.assert.compareToSourceExpression" class="spanner operation-box">
                    <div class="name">compareToSourceExpression</div>
                    <div class="value">{{action.assert.compareToSourceExpression}}</div>
                </div>

                <div v-if="action.assert.compareToSourcePath" class="spanner operation-box">
                    <div class="name">compareToSourcePath</div>
                    <div class="value">{{action.assert.compareToSourcePath}}</div>
                </div>

                <div v-if="action.assert.contentType" class="spanner operation-box">
                    <div class="name">contentType</div>
                    <div class="value">{{action.assert.contentType}}</div>
                </div>

                <div v-if="action.assert.expression" class="spanner operation-box">
                    <div class="name">expression</div>
                    <div class="value">{{action.assert.expression}}</div>
                </div>

                <div v-if="action.assert.headerField" class="spanner operation-box">
                    <div class="name">headerField</div>
                    <div class="value">{{action.assert.headerField}}</div>
                </div>

                <div v-if="action.assert.minimumId" class="spanner operation-box">
                    <div class="name">minimumId</div>
                    <div class="value">{{action.assert.minimumId}}</div>
                </div>

                <div v-if="action.assert.navigationLinks !== undefined" class="spanner operation-box">
                    <div class="name">navigationLinks</div>
                    <div class="value">{{action.assert.navigationLinks}}</div>
                </div>

                <div v-if="action.assert.operator" class="spanner operation-box">
                    <div class="name">operator</div>
                    <div class="value">{{action.assert.operator}}</div>
                </div>

                <div v-if="action.assert.path" class="spanner operation-box">
                    <div class="name">path</div>
                    <div class="value">{{action.assert.path}}</div>
                </div>

                <div v-if="action.assert.requestMethod" class="spanner operation-box">
                    <div class="name">requestMethod</div>
                    <div class="value">{{action.assert.requestMethod}}</div>
                </div>

                <div v-if="action.assert.requestURL" class="spanner operation-box">
                    <div class="name">requestURL</div>
                    <div class="value">{{action.assert.requestURL}}</div>
                </div>

                <div v-if="action.assert.resource" class="spanner operation-box">
                    <div class="name">resource</div>
                    <div class="value">{{action.assert.resource}}</div>
                </div>

                <div v-if="action.assert.response" class="spanner operation-box">
                    <div class="name">response</div>
                    <div class="value">{{action.assert.response}}</div>
                </div>

                <div v-if="action.assert.responseCode" class="spanner operation-box">
                    <div class="name">responseCode</div>
                    <div class="value">{{action.assert.responseCode}}</div>
                </div>

                <div v-if="action.assert.sourceId" class="spanner operation-box">
                    <div class="name">sourceId</div>
                    <div class="value">{{action.assert.sourceId}}</div>
                </div>

                <div v-if="action.assert.validateProfileId" class="spanner operation-box">
                    <div class="name">validateProfileId</div>
                    <div class="value">{{action.assert.validateProfileId}}</div>
                </div>

                <div v-if="action.assert.value" class="spanner operation-box">
                    <div class="name">value</div>
                    <div class="value">{{action.assert.value}}</div>
                </div>

                <div v-if="action.assert.warningOnly !== undefined" class="spanner operation-box">
                    <div class="name">warningOnly</div>
                    <div class="value">{{action.assert.warningOnly}}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        methods: {
            operationTypeDisplay(type) {
                const code = type.display ? type.display : type.code
                // const system = type.system ? type.system : null
                // if (system)
                //     return code + " (" + system + ")"
                return code
            }

        },
        name: "ActionScript",
        props: [
            'actions'
        ]
    }
</script>

<style scoped>

</style>