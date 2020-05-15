<template>

    <li
        :class="{
                    'breakpoint-indicator': showBreakpointIndicator,
                    'breakpoint-hit-indicator': isBreakpointHit,
                }"
        :data-breakpoint-index="breakpointIndex"
    >
        <!-- TODO:
        add off/on label to the gutter
             div.onClick register breakpoint with li.data-index value
        :data-map-key="currentMapKey"
              :title="hasBreakpoint ? 'Remove breakpoint' : 'Set breakpoint'"
        -->
        <span
              :class="{
                    'breakpointControlOff' : ! hasBreakpoint,
                    'breakpointControlOn' : hasBreakpoint,
              }"
              @click.stop="doToggle()"
        >{{breakpointSwitchStatus}}</span>
        <slot></slot>
    </li>


</template>

<script>
    import debugTestScriptMixin from "../../../mixins/debugTestScript";

    export default {
        data() {
            return {
                isUpdated: false, // Only to nudge Vue reactivity
            }
        },
        methods: {
            doToggle() {
                this.toggleBreakpointIndex(this.indexObj)
                    .then(resultCode => {
                        if (resultCode)
                            this.isUpdated = ! this.isUpdated // Keep this to nudge reactivity
                    })
            },


        },
        computed: {
            indexObj() {
                if (this.isUpdated) {this.isUpdated.valueOf()}
               return this.getBreakpointObj(this.breakpointIndex)
            },
            hasBreakpoint() {
                let retVal = this.$store.getters.hasBreakpoint(this.indexObj)
                return retVal
            },
            showBreakpointIndicator() {
                return this.$store.getters.hasBreakpoint(this.indexObj) && ! this.isBreakpointHit
            },
            isBreakpointHit() {
                return this.$store.getters.isBreakpointHit(this.indexObj)
            },
            breakpointSwitchStatus() {
                if (this.hasBreakpoint) {
                    return "ON"
                } else {
                    return "OFF"
                }
            }
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

    span.breakpointControlOn,
    span.breakpointControlOff {
        position: absolute;
        left: 0px;
        font-size: 8px;
        cursor: pointer;
        color: gray;
        border: #f5f5f5 solid 1px;
    }

    span.breakpointControlOn {
        color: black;
        border : black solid 1px;
    }

    span.breakpointControlOn:hover,
    span.breakpointControlOff:hover {
       text-decoration: underline;
    }

</style>
