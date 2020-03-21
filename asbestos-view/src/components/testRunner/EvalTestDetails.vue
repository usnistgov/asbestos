<template>
    <div>
        <span v-if="isPass">
            <img src="../../assets/checked.png" class="align-left">
        </span>
        <span v-else-if="isFail">
            <img src="../../assets/error.png" class="align-left">
        </span>
        <span v-else-if="isError">
            <img src="../../assets/yellow-error.png" class="align-left">
        </span>
        <span v-else>
            <img src="../../assets/blank-circle.png" class="align-left">
        </span>

        <span>{{script.description}}</span>

        <!-- actions will be asserts only-->
        <div v-for="(action, actioni) in script.action" class="assert-part"
             :key="'Action' + actioni">

            <eval-action-details
                    :script="action"
                    :report="report.action[actioni]"
            > </eval-action-details>

        </div>
    </div>
</template>

<script>
    import EvalActionDetails from "./EvalActionDetails";

    export default {
        computed: {
            isPass() {
                let pass = true;
                this.report.action.forEach(action => {
                    if (action.assert.result === 'fail' || action.assert.result === 'error')
                        pass = false;
                });
                return pass;
            },
            isFail() {
                let fail = false
                this.report.action.forEach(action => {
                    if (action.assert.result === 'fail')
                        fail = true
                })
                return fail
            },
            isError() {
                let error = false
                this.report.action.forEach(action => {
                    if (action.assert.reault === 'error')
                        error = true
                })
                return error
            },
        },
        props: [
            'script', 'report'
        ],
        components: {
            EvalActionDetails
        },
        name: "EvalTestDetails"
    }
</script>

<style scoped>
    .assert-part {
        margin-left: 20px;
        margin-right: 20px;
        /*cursor: pointer;*/
        /*text-decoration: underline;*/
    }
</style>
