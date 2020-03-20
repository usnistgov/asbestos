<template>
    <div>
        <div @click.self="select()" v-bind:class="{
                                    pass: result === 'pass' && colorful,
                                    'pass-plain': result === 'pass' && !colorful,
                                    fail: result === 'fail' && colorful,
                                    'fail-plain': result === 'fail' && !colorful,
                                    error: result === 'error' && colorful,
                                    'error-plain': result === 'error' && !colorful,
                                    warning: result === 'warning' && colorful,
                                    'not-run': result === 'not-run'  && colorful
                            }">
            <span class="selectable">Assert:</span> {{ description }}
        </div>
    </div>
</template>

<script>
    import colorizeTestReports from "../../mixins/colorizeTestReports";

    export default {
        data() {
            return {
                open: false,
            }
        },
        methods: {
            select() {
                this.open = !this.open
            }
        },
        computed: {
            result() {
                return this.report.assert.result
            },
            description() {
                const rawDesc = this.script.assert.description
                if (!rawDesc.includes("|"))
                    return rawDesc
                const elements = rawDesc.split("|")
                return elements[0]
            }
        },
        mixins: [ colorizeTestReports ],
        props: [
            'script', 'report'    // action parts
        ],
        name: "EvalActionDetails"
    }
</script>

<style scoped>

</style>
