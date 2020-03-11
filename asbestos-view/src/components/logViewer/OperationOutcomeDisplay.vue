<template>
    <span>
        <div class="divider"></div>
        <div class="divider"></div>
        <span class="has-cursor details">
            <span @click.stop="open = !open">Details</span>
            <span v-if="open"><img src="../../assets/arrow-down.png" @click.stop="open = !open"></span>
            <span v-else><img src="../../assets/arrow-right.png"  @click.stop="open = !open"></span>
        </span>

        <div v-if="open">
            <div v-if="headerMessage">
                <span class="divider"></span>
                <span>
                    {{headerMessage}}
                </span>
            </div>
            <div v-for="(order, orderi) in orderLabels" :key="order + orderi">
                <div v-for="(issue, issuei) in oo.issue" :key="issue + issuei">
                    <div v-if="issue.severity.myStringValue === order">
                        <div class="divider"></div>
                        <div class="divider"></div>
                        <span v-if="issue.severity" class="bold">{{issue.severity.myStringValue}}: </span>
                        <br />
                        <div class="divider"></div>
                        <div class="divider"></div>
                        <div class="divider"></div>
                        <div class="divider"></div>
                        Resource
                        <span v-if="issue.location">{{issue.location[0].myStringValue}}: </span>
                        <br />
                        <div class="divider"></div>
                        <div class="divider"></div>
                        <div class="divider"></div>
                        <div class="divider"></div>
                        <span v-if="issue.diagnostics">
                            {{profile(issue.diagnostics.myStringValue)}}
                            <br />
                            <div class="divider"></div>
                            <div class="divider"></div>
                            <div class="divider"></div>
                            <div class="divider"></div>
                            {{message(issue.diagnostics.myStringValue)}}
                        </span>
                    </div>
                </div>
            </div>
         </div>
    </span>
</template>

<script>
    export default {
        data() {
            return {
                open: false,
                orderLabels: ['error', 'warning', 'information'],
            }
        },
        methods: {
            profile(diagnostic) {
                const parts = diagnostic.split(",")
                return (parts.length > 1) ? parts[0] : "Profile"
            },
            message(diagnostic) {
                const i = diagnostic.indexOf(",")
                if (i === -1)
                    return diagnostic
                return diagnostic.substring(i+1)
            },
        },
        name: "OperationOutcomeDisplay",
        props: [
            'oo', 'headerMessage',
        ],
    }
</script>

<style scoped>
    .details {
        font-size: smaller;
    }
</style>
