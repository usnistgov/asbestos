<template>
    <div>
        <div class="test-defs-header">
            Test Definitions
        </div>

        <div v-for="testId in $store.state.base.testIds" :key="testId.id">
            <img v-if="openTestIds.includes(testId.id)" src="../assets/arrow-down.png" @click="close(testId.id)">
            <img v-else src="../assets/arrow-right.png" @click="open(testId.id)">
            <router-link class="nav-list-item" v-bind:to="testUrl(testId.id)">
                Test {{testId.name}}
            </router-link>
            <test-nav class="test-nav" :id="testId.id" v-if="openTestIds.includes(testId.id)"></test-nav>
        </div>
    </div>
</template>
<script>
    import TestNav from "./TestNav";

    export default {
        data() {
            return {
                openTestIds: []
            }
        },
        components: { TestNav },
        name: 'nav-panel',
        methods: {
            testUrl(id) {
                return '/test/' + id
            },
            open(id) {
                if (!this.openTestIds.includes(id)) {
                    this.openTestIds.push(id)
                }
            },
            close(id) {
                this.openTestIds = this.openTestIds.filter(e => e !== id)
            }
        }
    }
</script>
<style scoped>
    .test-defs-header {
        font-weight: bold;
    }
    .nav-list-item {
        line-height: 1.5;
    }
    .router-link-active {
        background-color: lightblue;
    }
    .test-nav {
        position: relative;
        left: 15px;
    }
</style>
