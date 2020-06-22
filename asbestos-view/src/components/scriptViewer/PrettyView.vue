<template>
    <div class="soft-boxed">
        <vue-json-pretty :data="data"></vue-json-pretty>
<!--        <div v-if="!data">-->
<!--            <vue-json-pretty :data="null"></vue-json-pretty>-->
<!--        </div>-->
<!--        <div v-else-if="showAction">-->
<!--            <vue-json-pretty :data="data"></vue-json-pretty>-->
<!--        </div>-->
<!--        <div v-else>-->
<!--            <vue-json-pretty :data="data.action"></vue-json-pretty>-->
<!--        </div>-->
    </div>
</template>

<script>
    import VueJsonPretty from "vue-json-pretty";

    export default {
        props: [
          'data',  // script or report "section"
            'showAction'   // ignored
        ],
        components: {
            VueJsonPretty
        },
        name: "PrettyView"
    }
</script>

<style scoped>

</style>
