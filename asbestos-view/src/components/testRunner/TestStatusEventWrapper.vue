<template>
    <span>
    <span
            v-bind:class="{'breakpoint-gutter-inactive': breakpointGutter}"
            @mouseover="onStatusMouseOver"
            @mouseleave="onStatusMouseLeave"
            @click.stop="onStatusClick"
            :title="debugTitle">&nbsp;
    </span>
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
                breakpointGutter: false,
            }
        },
        methods: {
            onStatusMouseOver() {
                this.breakpointGutter = true
                this.$emit('onStatusMouseOver')
            },
            onStatusMouseLeave() {
                this.breakpointGutter = false
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
