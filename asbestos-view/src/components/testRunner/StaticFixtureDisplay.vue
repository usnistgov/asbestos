<template>
    <div>
        Fixture Display
        <fixture-display
            :report="getFixtureAnalysis()"> </fixture-display>
    </div>
</template>

<script>
    import  {LOG} from '../../common/http-common';
    import FixtureDisplay from "./FixtureDisplay";
    export default {
        methods: {
            async getFixtureAnalysis() {
                const url = `analysis/static/${this.testCollection}/${this.testId}?url=${this.fixturePath()}`
                const result = await LOG.get(url)
                return result.data
            },
        },
        computed: {
            fixturePath() {  // translate separator to /
                return this.path.replace(/,/g, '/')
            }
        },
        components: { FixtureDisplay },
        name: "StaticFixtureDisplay",
        props: [
            'testCollection',
            'testId',
            'path'  // uses . for separator
        ]
    }
</script>

<style scoped>

</style>
