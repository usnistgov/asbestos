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
            <template v-if="hasBreakpoint"><span class="breakpointIndicatorClass">&#x1F6D1;&nbsp;</span></template><!-- Stop sign -->
            <template v-else>&nbsp;&nbsp;</template>
        </span>
        <!-- Initially, when this component is created, either hide or display the gutter option span label. -->
        <span v-show="hasGutterOptions"
                  :class="{'breakpointGutterOption' : true,
                    'breakpointOptionHidden' : isOptionInitiallyHidden,
                   }"
                  title="Additional breakpoints exist in details" :data-breakpoint-index="breakpointIndex">{{getGutterOptionDisplayString()}}</span>
        <slot></slot>
    </li>


</template>

<script>
    import debugTestScriptMixin from "../../../mixins/debugTestScript";

    export default {
        data() {
            return {
                isUpdated: false, // Only to nudge Vue reactivity
                isOptionInitiallyHidden: false,
            }
        },
        created() {
            /* assuming initially the testscript-test-level display is collapsed, hide the label if no breakpoints exist in test-details */
           this.isOptionInitiallyHidden = (this.getBreakpointsInDetails(this.indexObj) === 0)
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
            getGutterOptionDisplayString() {
                let ct = this.getBreakpointsInDetails(this.indexObj)
                if (ct > 0) {
                    return '+' + ct + '.'
                } else {
                    return ''
                }
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
            isBreakpointHit() {
                return this.$store.getters.isBreakpointHit(this.indexObj)
            },
        },
        // watch: {
        // },
        props: [
            'breakpointIndex', 'isImportHeader', 'hasGutterOptions',
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
<style>

    span.breakpointGutter {
        position: absolute;
        left: 5px;
    }
    span.breakpointGutterOption {
        font-size: 8px;
        position: absolute;
        left: 25px;
    }

    span.breakpointControlOn,
    span.breakpointControlOff {
        cursor: pointer;
        border: gray dotted 1px;
        background-color: #f5f5f5;
        horiz-align: center;
        text-align: center;
        width: 14px;
        text-align: left;
    }
    span.breakpointControlOn {
        /*border-top : none;*/
        /*border-bottom: none;*/
    }
    span.breakpointIndicatorClass {
        font-size: x-small;
    }
    span.breakpointControlOff:hover,
    span.breakpointControlOn:hover {
        border: red dotted 2px;
    }
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
    .breakpointOptionHidden {
        visibility: hidden;
    }
    /*span.breakpointGutter:first-of-type {*/
    /*    border-top: gray solid 1px;*/
    /*    border-bottom: gray solid 1px;*/
    /*}*/

</style>
