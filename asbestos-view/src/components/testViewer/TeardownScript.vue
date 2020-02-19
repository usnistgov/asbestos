<template>
    <div>
        <div class="teardown-head">Teardown</div>
        <div v-if="teardown">
            <div v -if="teardown.action" class="teardown-box top-border">
                <action-script :actions="teardown.action"> </action-script>
            </div>
        </div>
    </div>
</template>

<script>
    import ActionScript from "./ActionScript";
    export default {
        name: "TeardownScript",
        props: [
            'teardown'
        ],
        components: {
            ActionScript
        }
    }
</script>

<style scoped>

</style>