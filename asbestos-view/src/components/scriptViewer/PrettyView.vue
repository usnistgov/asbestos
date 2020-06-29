<template>
    <div class="soft-boxed">
        <div v-if="data && data.operation && data.operation.modifierExtension">
            <div v-for="(mext, mext_i) in data.operation.modifierExtension"
                 :key="'mext'+mext_i">
                <div v-for="(extension, extension_i) in mext.modifierExtension"
                     :key="'PrettyExt'+extension_i">
                    <div v-if="extension.url === 'component'">
                        <div v-if="moduleId" class="pointer-cursor underline">
                            <a v-bind:href="moduleRef">Call {{extension.valueString}}</a>
                        </div>
                        <div v-else>
                            Call {{extension.valueString}}
                        </div>
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
            moduleRef() {
                return '#' + this.moduleId;
            },
            depth() {
                return this.deepView || this.expanded ? 10 : 2;
            },
            moduleId() {
                if (this.report) {
                    const extensions = this.report.modifierExtension;
                    if (extensions) {
                        for (let i = 0; i < extensions.length; i++) {
                            const extension = extensions[i];
                            if (extension.url === 'urn:moduleId')
                                return extension.valueString;
                        }
                    }
                }
                return null;
            },
        },
        methods: {
            click(path, data) {
                console.log(`click: path=${path}  data=${data}`)
            }
        },
        props: [
          'data',  // script or report //  operation or assert
            'deepView',
            'report'  // corresponding report (operation or assert)
                    // when data is script (only used when script has calls to modules)
        ],
        components: {
            VueJsonPretty
        },
        name: "PrettyView"
    }
</script>

<style scoped>

</style>
