<template>

    <li
        :class="{
                    'breakpoint-indicator': showBreakpointIndicator,
                    'breakpoint-hit-indicator': isBreakpointHit,
                }"
        :data-map-key="currentMapKey"
        :data-breakpoint-index="breakpointIndex"
    >
        <!-- TODO:
        add off/on label to the gutter
             div.onClick register breakpoint with li.data-index value
        -->
        <span style="position: absolute; left: 1px; font-size: 8px; color: gray">OFF</span>
        <slot></slot>
    </li>


</template>

<script>
    import debugTestScriptMixin from "../../../mixins/debugTestScript";

    export default {
        data() {
            return {
            }
        },
        methods: {
        },

        computed: {
            showBreakpointIndicator() {
                let obj = this.getBreakpointObj(this.breakpointIndex)
                return this.$store.getters.hasBreakpoint(obj) && ! this.isBreakpointHit
            },
            isBreakpointHit() {
                let obj = this.getBreakpointObj(this.breakpointIndex)
                return this.$store.getters.isBreakpointHit(obj)
            },
        },
        props: [
            'breakpointIndex',
        ],
        mixins: [
            debugTestScriptMixin,
        ],
        components: {
        },
        name: "DebuggableListItem"
    }
</script>

<style scoped>

</style>
