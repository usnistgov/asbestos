<template>
    <div class="soft-boxed">
        <div v-if="data && data.operation && data.operation.modifierExtension">
            <div v-for="(mext, mext_i) in data.operation.modifierExtension"
                 :key="'mext'+mext_i">
                <div v-for="(extension, extension_i) in mext.modifierExtension"
                     :key="'PrettyExt'+extension_i">
                    <div v-if="extension.url === 'component'">
                        Call {{extension.valueString}}
                    </div>
                </div>
            </div>
        </div>


        <vue-json-pretty
                :data="data"
                :deep="depth"
                @click="click"
                :selectable-type="'single'"
        ></vue-json-pretty>
    </div>
</template>

<script>
    import VueJsonPretty from "vue-json-pretty";

    export default {
        data() {
            return {
                expanded: false,
            }
        },
        computed: {
            depth() {
                return this.deepView || this.expanded ? 10 : 2;
            }
        },
        methods: {
            click(path, data) {
                console.log(`click: path=${path}  data=${data}`)
            }
        },
        props: [
          'data',  // script or report //  operation or assert
            'deepView',
        ],
        components: {
            VueJsonPretty
        },
        name: "PrettyView"
    }
</script>

<style scoped>

</style>
