<template>
    <div class="soft-boxed">
<!--        <input type="checkbox" name="expander" id="expander" v-model="expanded">-->
<!--        <label for="expander">Expand</label>-->
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
          'data',  // script or report "section"
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
