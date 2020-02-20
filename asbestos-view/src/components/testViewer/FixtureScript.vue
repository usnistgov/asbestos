<template>
    <div>
        <div class="fixtures-head">
            Fixtures
            <span v-if="editing">
                <button @click="doAdd()">Add</button>
            </span>
        </div>
        <div v-for="(fixture, fixturei) in fixtures"
             :key="fixture + fixturei">
            <div v-if="fixture.id" class="fixture-box top-border">
                <div class="fixture-name">id</div>
                <div class="fixture-value bold">
                    {{fixture.id}}
                    <span class="red">{{undefFixtureLabel(fixture.id)}}</span>
                    <span v-if="editing">
                        <button @click="doEdit(fixture.id)">Edit</button>
                        <button @click="doDelete(fixture.id)">Delete</button>
                    </span>
                </div>
            </div>
            <div v-if="fixture.autocreate !== undefined" class="fixture-box">
                <div class="fixture-name">autocreate</div>
                <div class="fixture-value">{{fixture.autocreate}}</div>
            </div>
            <div v-if="fixture.autodelete !== undefined" class="fixture-box">
                <div class="fixture-name">autodelete</div>
                <div class="fixture-value">{{fixture.autodelete}}</div>
            </div>
            <div v-if="fixture.resource" class="fixture-box">
                <div class="fixture-name">resource.reference</div>
                <div class="fixture-value">{{fixture.resource.reference}}</div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                editing: false,
            }
        },
        methods: {
            doAdd() {

            },
            doDelete(id) {
                console.log(`delete ${id}`)
            },
            doEdit(id) {
                console.log(`edit ${id}`)
            },
            undefFixtureLabel(fixtureName) {
                return this.unusedFixtures.indexOf(fixtureName) === -1 ? "" : "Unused"
            },
        },
        watch: {
            edit: function(newVal) {
                if (this.editing !== newVal)
                    this.editing = newVal
            }
        },
        name: "FixtureScript",
        props: [
            'fixtures', 'unusedFixtures', 'edit'
        ]
    }
</script>

<style scoped>

</style>
