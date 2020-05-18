<template>

    <li
        :data-breakpoint-index="breakpointIndex"
    >
        <!-- TODO:
        add off/on label to the gutter
             div.onClick register breakpoint with li.data-index value
        :data-map-key="currentMapKey"
              {{breakpointSwitchStatus}}
            :class="{
                    'breakpoint-indicator': showBreakpointIndicator,
                    'breakpoint-hit-indicator': isBreakpointHit,
                }"
                  -->
        <span
                :class="{
                    'breakpointControlOff' : ! hasBreakpoint,
                    'breakpointControlOn' : hasBreakpoint,
              }"
                :title="hasBreakpoint ? 'Remove breakpoint' : 'Set breakpoint'"
              @mouseover="onBkptSwitchMouseOver"
              @mouseleave="onBkptSwitchMouseLeave"
              @click.stop="doToggle()"
        >
            <template v-if="hasBreakpoint">
               &#x1F6D1;
            </template>
            <template v-else>
                &nbsp;
            </template>
        </span>
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
            onBkptSwitchMouseOver(event) {
                if (event) {
                    let nobj = event.target.parentNode.querySelector('span:nth-child(3)')
                       if (nobj) {
                           nobj.classList.add('bkptBorderFocusOn')
                       }  else {
                           console.log('nobj is null!')
                       }
                }
            },
            onBkptSwitchMouseLeave(event) {
                if (event) {
                    let nobj = event.target.parentNode.querySelector('span:nth-child(3)')
                    if (nobj) {
                        nobj.classList.remove('bkptBorderFocusOn')
                    }
                }
            }
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
                    return  "OFF"
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
        cursor: pointer;
        /*border: gray dotted 1px;*/
        border: gray solid 1px;
        horiz-align: center;
        text-align: center;
    }

    span.breakpointControlOn {
        border : none;
        font-size: smaller;
    }

    span.breakpointControlOff:hover {
        border: red dotted 1px;
    }


</style>
<style>
    .bkptBorderFocusOn {
        border: red dotted 2px;
    }
</style>
