<template>
    <div>
        <div v-if="script.action">
            <div v-for="(action, actioni) in script.action" class="action-margins"
                 :key="'Action' + actioni">

            </div>
        </div>
    </div>
</template>

<script>
    //import ActionDetails from './ActionDetails'
    export default {
        props: [
            'script',   // moduleScript.test
            'report',   // moduleReport.test
        ],
        components: {
      //      ActionDetails
        },
        name: "ImportTestDetails"
    }
</script>

<style scoped>

</style>
