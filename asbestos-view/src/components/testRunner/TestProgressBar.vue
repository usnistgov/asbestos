<template>
    <div style="width:10px;height:13px;margin-right:2px;border: 1px solid black;">

        <div v-if="allPassed" style="background-color:lightgreen;height:100%"></div>
        <div v-else-if="allFailed" style="background-color:#D75A4A;height:100%"></div>

        <template v-if="!allPassed && !allFailed">
            <div :style="`background-color:white;height:${adjustedPct.notRunPct*100}%`"></div>
            <div :style="`background-color:lightgreen;height:${adjustedPct.passPct*100}%`"></div>
            <div :style="`background-color:#D75A4A;height:${adjustedPct.failPct*100}%;`"></div>
        </template>

    </div>
</template>
<script>

    export default {
        props: {
            passCount: {
                type: Number,
                required: true
            },
            failCount: {
                type: Number,
                required: true
            },
            notRunCount: {
                type: Number,
                required: true
            },
            totalCount: {
                type: Number,
                required: true
            }
        },
        computed: {
            allPassed() {
                return this.passCount === this.totalCount
            },
            allFailed() {
                return this.failCount === this.totalCount
            },
            adjustedPct() {
                const boostVal = .15
                let adjVal = 0
                let pctObj = {passPct: 0, failPct: 0, notRunPct: 0}
                pctObj.passPct = this.passCount / this.totalCount
                pctObj.failPct = this.failCount / this.totalCount
                pctObj.notRunPct = this.notRunCount / this.totalCount
                for (const propName in pctObj) {
                    if (pctObj[propName] > 0 && pctObj[propName] < boostVal) {
                        adjVal += boostVal - pctObj[propName]
                        pctObj[propName] = boostVal
                    }
                }
                let majorityPct = 0
                let majorityPropName = null
                for (const propName in pctObj) {
                    if (pctObj[propName] > majorityPct) {
                        majorityPct = pctObj[propName]
                        majorityPropName = propName
                    }
                }
                if (majorityPct > 0 && majorityPropName !==null) {
                    pctObj[majorityPropName] -= adjVal
                }

                return pctObj
            }
        }
    }

</script>

<style>

</style>