<template>
    <span @mouseover="onStatusMouseOver" @mouseleave="onStatusMouseLeave" @click.stop="onStatusClick" :title="debugTitle">
             <test-status
                          :status-on-right="false"
                          :report="report"
             > </test-status>
    </span>
</template>

<script>
    import TestStatus from './TestStatus'

    export default {
        data() {
            return {
                displayBreakpointTitle: false,
            }
        },
        methods: {
            onStatusMouseOver() {
                this.$emit('onStatusMouseOver')
            },
            onStatusMouseLeave() {
                this.titleText = ''
                this.$emit('onStatusMouseLeave')
            },
            onStatusClick() {
                this.$emit('onStatusClick')
            },
        },

        computed: {
        },
        props: [
            'statusOnRight', 'report', 'debugTitle'
        ],
        components: {
            TestStatus
        },
        name: "TestStatusEventWrapper"
    }
</script>

<style scoped>

</style>
