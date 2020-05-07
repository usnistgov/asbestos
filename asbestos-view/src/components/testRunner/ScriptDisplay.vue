<template>
    <div class="container">
        <div class="script-header">
            Script
        </div>
        <div class="report-header">
            Report
        </div>
        <div class="script">
            <vue-json-pretty :data="script"></vue-json-pretty>
        </div>
        <div class="report">
            <vue-json-pretty :data="filteredReport"></vue-json-pretty>
        </div>
    </div>
</template>

<script>
    import VueJsonPretty from 'vue-json-pretty'
    export default {
        methods: {

        },
        computed: {
            filteredReport() {
//                return this.report
                if (!this.report) return null;
                const copy = JSON.parse(JSON.stringify(this.report))
                if (copy.operation && copy.operation.extension) {
                    const extension = copy.operation.extension;
                    for (let i=0; i<extension.length; i++) {
                        if (extension[i].url  === 'urn:action-context')
                            extension[i].url = 'Removed by UI';
                    }
                }
            //     if (copy.operation &&
            //         copy.operation.extension &&
            //         copy.operation.extension[0] &&
            //         copy.operation.extension[0].url === 'urn:action-context'
            // )
            //         copy.operation.extension[0] = 'Removed by UI';
                return copy
            }
        },
        props: [
            // parts representing a single action
            'script', 'report',
        ],
        components: {
            VueJsonPretty
        },
        name: "ScriptDisplay"
    }
</script>

<style scoped>
    .container {
        display: grid;
        grid-template-columns: 50% 50%;
    }
    .script-header {
        grid-column: 1;
        grid-row: 1;
        font-weight: bold;
    }
    .report-header {
        grid-column: 2;
        grid-row: 1;
        font-weight: bold;
    }
    .script {
        grid-column: 1;
        grid-row: 2;
    }
    .report {
        grid-column: 2;
        grid-row: 2;
    }

</style>
