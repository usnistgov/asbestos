<template>
    <div>
        <!--   history navigation   -->
        <div v-if="history.length > 0" class="solid-boxed">
            <div class="nav-buttons">
                <div v-if="moreToTheLeft()" class="tooltip left-arrow-position">
                    <img id="left-button" class="selectable" src="../../assets/left-arrow.png" @click="left()"/>
                    <span class="tooltiptext">Previous</span>
                </div>
            </div>
            <div class="details">History</div>
            <div class="vdivider"></div>

        </div>

        <!--  BASE OBJECT     -->
        <div>
            <span class="caption">Focus Object:</span>
            <div class="vdivider"></div>
            <div class="grid-container">
                <span v-if="report.base">
                    <div class="grid-item">
                        <span v-bind:class="objectDisplayClass(report.base)"
                              @click="selectedResourceIndex = -1">
                            {{ report.base.name }}
                        </span>
                    </div>
                </span>
            </div>
        </div>

        <!--  RELATED     -->
        <div class="vdivider"></div>
        <span class="caption">Related: </span>
        <span>(referenced by Focus Object)</span>
        <div class="vdivider"></div>
        <div class="grid-container">
                    <span v-for="(resource, resourcei) in report.objects"
                          :key="resource + resourcei">
                        <div class="grid-item">
                            <span v-bind:class="objectDisplayClass(resource)"
                                  @click="selectedResourceIndex = resourcei">
                                {{ resource.name }} ({{ resource.relation }})
                                <span class="tooltip">
                                    <img id="focus" class="selectable" src="../../assets/focus.png" @click.stop="loadAnalysisFromEventContext(report.objects[resourcei].url, report.objects[resourcei].eventContext, true)">
                                    <span class="tooltiptext">Focus</span>
                                </span>
                            </span>

                        </div>
                    </span>
        </div>
        <div class="vdivider"></div>

        <!--  SELECTED      -->
        <div v-if="selectedResourceIndex === null"></div>

        <!--  BASE OBJECT DETAILS -->
        <div v-else-if="selectedResourceIndex === -1 && report.base">
            <log-object-display :report="report.base"> </log-object-display>
        </div>

        <!--  RELATED OBJECT DETAILS -->
        <div v-else-if="selectedResourceIndex > -1">
            <log-object-display :report="report.objects[selectedResourceIndex]"> </log-object-display>
        </div>

    </div>
</template>

<script>
    import LogObjectDisplay from "../logViewer/LogObjectDisplay"

    export default {
        data() {
            return {
                history: [],   // {url: report.base.url, eventId: eventId } - history[0] is never removed - it is the base object
            }
        },
        methods: {
            moreToTheLeft() {
                return this.history.length > 1
            },
            left() {  // make previous object the focus
                if (this.moreToTheLeft()) {
                    this.pop()
                }
            },
            peek() {
                if (this.history.length === 0)
                    return null
                return this.history[this.history.length - 1]
            },
            pop() {
                if (this.history.length === 0)
                    return null
                return this.history.pop()
            },
            objectDisplayClass: function (resource) {
                const defined = ['DocumentManifest', 'DocumentReference', 'Patient', 'Binary']
                return {
                    manifest: resource.name === 'DocumentManifest',
                    ref: resource.name === 'DocumentReference',
                    patient: resource.name === 'Patient',
                    binary: resource.name === 'Binary',
                    other: defined.indexOf(resource.name) < 0
                }
            },
        },
        computed: {

        },
        components: { LogObjectDisplay },
        name: "FixtureDisplay",
        props: [
            'report' // this is analysis report, not TestReport
        ]
    }
</script>

<style scoped>

</style>
