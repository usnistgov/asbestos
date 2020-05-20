<template>   <!-- contained testscript holding conditional -->
    <span>

        <!-- Fixture, Variable, and setup stuff -->
        <div v-if="conditionScript" class="script">
            <div v-if="displayOpen">

                <!-- local fixtures -->
                <div v-for="(fixture, i) in conditionScript.fixture"
                     :key="i">
                    <span class="name" >Fixture: </span>
                    <span class="value">{{ fixture.id }}</span>
                </div>

                <!-- local variables -->
                <div v-for="(variable, i) in conditionScript.variable"
                     :key="'Var' + i">
                    <span class="name" >Variable: </span>
                    <span class="value">{{ variable.name }}</span>
                </div>
            </div>

            <div v-if="conditionScript.setup && conditionReport && conditionReport.setup">
                <!-- don't need yet -->
            </div>
        </div>

        <div v-if="conditionScript">
            <div v-for="(test, testi) in conditionScript.test"
                 :key="'Test' + testi">
                <test-details-contained
                        :script="conditionScript.test[testi]"
                        :report="conditionReport ? conditionReport.test[testi] : null"
                        :description="description(testi)"
                ></test-details-contained>
            </div>
            <!-- add TEARDOWN here -->
        </div>
    </span>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import TestDetailsContained from "./TestDetailsContained";

    export default {
        data() {
            return {
                displayOpen: true,  // start closed
            }
        },
        methods: {
            description(index) {
                return index === 0 ? 'Condition Context' : "Condition"
            },
        },
        computed: {
        },
        created() {
        },
        mounted() {

        },
        watch: {
        },
        mixins: [ errorHandlerMixin ],
        props: [
            'conditionScript', 'conditionReport'
        ],
        components: {
            TestDetailsContained,
        },
        name: "ScriptDetailsContained"
    }
</script>

<style scoped>
.script {
    text-align: left;
}
    .name {
        font-weight: bold;
    }
    .value {

    }

</style>
<style>
</style>
