<template>
    <div>
        <div class="control-panel-item-title">Environment</div>
        <b-form-select v-model="environment" :options="environments"></b-form-select>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { BFormSelect } from 'bootstrap-vue'
    Vue.component('b-form-select', BFormSelect)

    export default {
        data() {
            return {
                environment: 'default',
                environments: null
            }
        },
        methods: {
            update() {
                let options = []
                this.$store.state.base.environments.forEach(function(ts) {
                    let it = { value: ts, text: ts }
                    options.push(it)
                })
                this.environments = options
            },
        },
        created() {
            this.update()
            this.environment = this.$store.state.base.environment
        },
        watch: {
            '$store.state.base.environments': 'update',

        },
        name: "EnvironmentControlPanel"
    }
</script>

<style scoped>

</style>
