<template>
    <div>
        <div class="fixtures-head">Fixtures</div>
        <div v-for="(fixture, fixturei) in fixtures"
             :key="fixture + fixturei">
            <div v-if="fixture.autocreate !== undefined" class="fixture-box top-border">
                <div class="fixture-name">autocreate</div>
                <div class="fixture-value">{{fixture.autocreate}}</div>
            </div>
            <div v-if="fixture.autodelete !== undefined" class="fixture-box top-border">
                <div class="fixture-name">autodelete</div>
                <div class="fixture-value">{{fixture.autodelete}}</div>
            </div>
            <div v-if="fixture.resource" class="fixture-box top-border">
                <div class="fixture-name">resource.reference</div>
                <div class="fixture-value">{{fixture.resource.reference}}</div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: "FixtureScript",
        props: [
            'fixtures'
        ]
    }
</script>

<style scoped>

</style>