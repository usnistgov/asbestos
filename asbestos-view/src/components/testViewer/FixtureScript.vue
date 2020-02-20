<template>
    <div>
        <div class="fixtures-head">
            Fixtures
            <span v-if="editMode">
                <button @click="doAdd()">Add</button>
            </span>
        </div>
        <div v-for="(fixture, fixturei) in fixtures"
             :key="fixture + fixturei">

            <div class="top-border" v-bind:class=" { 'edit-view': fixture.id === editTarget }">
                <div v-if="fixture.id" class="fixture-box">

<!--                    <div v-if="editMode && fixture.id === editTarget" class="edit-mode fixture-box">-->
<!--                        <div class="fixture-name bold">Editing</div>-->
<!--                        <div class="fixture-value">-->
<!--                            <button @click="save()">Accept</button>-->
<!--                            <button @click="cancelEdit()">Cancel</button>-->
<!--                        </div>-->
<!--                    </div>-->


                    <div class="fixture-name">id</div>
                    <div class="fixture-value bold">
                        {{fixture.id}}
                        <span class="red">{{undefFixtureLabel(fixture.id)}}</span>
                        <span v-if="editMode">
                        <button @click="startEdit(fixture.id)">Edit</button>
                        <button @click="startDelete(fixture.id)">Delete</button>
                        <span v-if="confirmDelete">
                            <button @click="doDelete()">Confirm</button>
                            <button @click="cancelDelete()">Cancel</button>
                        </span>
                    </span>
                    </div>
                </div>
                <div v-if="fixture.autocreate !== undefined" class="fixture-box">
                    <div class="fixture-name">autocreate</div>
                    <div class="fixture-value">
                        <span v-if="editMode">
                            <input type="checkbox" id="check + fixturei" v-model="fixture.autocreate">
                        </span>
                        <span v-else>{{fixture.autocreate}}</span>
                    </div>
                </div>
                <div v-if="fixture.autodelete !== undefined" class="fixture-box">
                    <div class="fixture-name">autodelete</div>
                    <div class="fixture-value">
                        <span v-if="editMode">
                            <input type="checkbox" v-model="fixture.autodelete">
                        </span>
                        <span v-else>{{fixture.autodelete}}</span>
                    </div>
                </div>
                <div v-if="fixture.resource" class="fixture-box">
                    <div class="fixture-name">resource.reference</div>
                    <div class="fixture-value">{{fixture.resource.reference}}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                editMode: false,

                deleteTarget: null,
                confirmDelete: false,
                deleteConfirmed: false,

                editTarget: null,
                deleted: null,
            }
        },
        methods: {
            /////////////////////////////////////////////////////////////////////////////
            startEdit(id) {
                if (id !== null && this.editTarget !== null) {
                    if (id === this.editTarget) {
                        this.editTarget = null
                        return
                    }
                    this.editTarget = id
                    return
                }
                if (this.editTarget)
                    this.editTarget = null
                else
                    this.editTarget = id
            },
            cancelEdit() {
                this.editTarget = null
            },
            save() {
                console.log(`save ${this.editTarget}`)
                this.editTarget = null
            },
            /////////////////////////////////////////////////////////////////////////////
            doAdd() {

            },
            /////////////////////////////////////////////////////////////////////////////
            startDelete(id) {
                if (this.deleteTarget)
                    return
                this.deleteTarget = id
                this.confirmDelete = true
            },
            doDelete() {
                this.confirmDelete = false
                console.log(`delete ${this.deleteTarget}`)
                let deleteIndex = -1
                this.fixtures.forEach(function(x, index) {
                    console.log(`index is ${index}`)
                    if (x.id === this.deleteTarget)
                        deleteIndex = index
                } )
                this.deleted = deleteIndex
                if (deleteIndex !== -1 )
                    this.fixtures.splice(deleteIndex, 1)
                this.deleteTarget = null
            },
            cancelDelete() {
                this.confirmDelete = false
                this.deleteTarget = null
            },
            //////////////////////////////////////////////////////////////////////////////
            undefFixtureLabel(fixtureName) {
                return this.unusedFixtures.indexOf(fixtureName) === -1 ? "" : "Unused"
            },
        },
        watch: {
            edit: function(newVal) {
                if (this.editMode !== newVal)
                    this.editMode = newVal
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
