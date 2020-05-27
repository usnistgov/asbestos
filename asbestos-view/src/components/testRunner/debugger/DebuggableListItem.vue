<template>

    <li
            :class="{
                    'breakpointHit': isBreakpointHit,
            }"
        :data-breakpoint-index="breakpointIndex"
    >
        <!--
        :data-map-key="currentMapKey"
              {{breakpointSwitchStatus}}
                  -->
        <span
                :class="{
                    'breakpointGutter' : true,
                    'breakpointControlOff' : ! hasBreakpoint && ! isImportHeader,
                    'breakpointControlOn' : hasBreakpoint,
              }"
                :title="hasBreakpoint ? 'Remove breakpoint' : 'Set breakpoint'"
              @mouseover="onBkptSwitchMouseOver"
              @mouseleave="onBkptSwitchMouseLeave"
              @click.stop="doToggle()"
        >
            <template v-if="hasBreakpoint"><span class="stopSignClass">&#x1F6D1;&nbsp;</span></template><!-- Stop sign -->
            <template v-else>&nbsp;&nbsp;</template>
        </span>
<!--        <span v-if="isBreakpointHit" class="breakpointGutterOption evalBtn" @click.stop="doDebugEvalMode($store.state.testRunner.currentTest)">Eval.</span>-->
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
            doFocusHint(event, displayOn) {
                if (event) {
                    let nobj = event.target.parentNode
                    // querySelector: 3=span action-type, 4=span action-description
                    let childTarget = nobj.querySelector('span:nth-child(3)')
                    if (! childTarget) {
                        childTarget = nobj.querySelector('span:nth-child(4)')
                        if (! childTarget) {
                            childTarget = nobj // fallback to parent if child not found
                            // parentNode = the parent span
                        }
                    }
                    if (childTarget) {
                        let className = 'breakpointBorderFocusOn'
                        if (displayOn)
                            childTarget.classList.add(className)
                        else
                            childTarget.classList.remove(className)
                    }  else {
                        console.log('childTarget is not found!')
                    }
                }

            },
            onBkptSwitchMouseOver(event) {
                this.doFocusHint(event, true)
            },
            onBkptSwitchMouseLeave(event) {
                this.doFocusHint(event, false)
            },
        },
        computed: {
            isNested() {
                return this.breakpointIndex.includes('/')
            },
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
            'breakpointIndex', 'isImportHeader',
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

    span.breakpointGutter {
        position: absolute;
        left: 5px;
    }
    span.breakpointGutterOption {
        position: absolute;
        left: 25px;
    }

    span.breakpointControlOn,
    span.breakpointControlOff {
        cursor: pointer;
        border-left: gray solid 1px;
        border-right: gray solid 1px;
        background-color: #f5f5f5;
        horiz-align: center;
        text-align: center;
        width: 14px;
        text-align: left;
    }

    span.breakpointControlOn {
        border-top : none;
        border-bottom: none;
    }

    span.stopSignClass {
        font-size: x-small;
    }

    span.breakpointControlOff:hover,
    span.breakpointControlOn:hover {
        border: red dotted 2px;
    }


</style>
<style>
    .breakpointBorderFocusOn {
        border: red dotted 2px;
    }
    .breakpointHit {
        list-style-type: "\1F449"; /* Index finger pointing right */
        /*background-color: yellow;*/
        /*transition: background-color 500ms ease-in-out;*/
        /* 27A1 = Right arrow */
    }
    .evalBtn {
        cursor: pointer;
        color: blue;
        font-size: x-small;
    }
    .evalBtn:hover {
        text-decoration: underline;
    }
    .breakpointHitBkg {
        background-color: yellow;
    }
    .breakpointFeatureBkg {
        background-color: white;
    }
</style>
