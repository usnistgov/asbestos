<template>
    <span>
<!--        <div v-if="script" >-->
<!--            Conditional:-->
<!--            <span v-if="script.description">-->
<!--                {{ script.description }}-->
<!--            </span>-->
<!--        </div>-->

        <!-- Fixture, Variable, and setup stuff -->
        <div v-if="script" class="script">
            <div v-if="displayDetail">
                <div v-for="(fixture, i) in script.fixture"
                     :key="i">
                    <span class="name" >Fixture: </span>
                    <span class="value">{{ fixture.id }}</span>
                </div>
                <div v-for="(variable, i) in script.variable"
                     :key="'Var' + i">
                    <span class="name" >Variable: </span>
                    <span class="value">{{ variable.name }}</span>
                </div>
            </div>

            <div v-if="script.setup && report && report.setup">
                <!-- don't need yet -->
            </div>
        </div>

        <div>
            <div v-for="(test, testi) in script.test"
                 :key="'Test' + testi">
                <test-details-contained
                        :script="script.test[testi]"
                        :report="report ? (report.test ? report.test[testi] : null) : null"
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
                displayDetail: false,  // display fixture, variable, setup stuff
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
            'script', 'report'  // TestScript and TestReport
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
    /*
    .test-margins {
        margin-left: 20px;
        margin-right: 20px;
    }
    .action-margins {
        margin-left: 50px;
        margin-right: 50px;
    }
    .conditional-margins {
        margin-left: 40px;
        margin-right: 40px;
    }
    .script-description-margins {
        margin-left: 30px;
        margin-right: 30px;
    }

     */

</style>
