<template>
    <div class="main">
        <div class="nav">
            <nav-panel></nav-panel>
        </div>
        <router-view class="body" name="panel"></router-view>
    </div>
</template>

<script>
    import {newTest, newTestVariable} from "../types/test";
    import NavPanel from "./NavPanel";


    export default {
        data() {
            return {
            }
        },
        components: { NavPanel },
        name: 'ToolBody',
        mounted() {
            this.loadTests()
        },
        methods: {
            loadTests() {
                const testIds = [
                    {
                        id: '1',
                        name: '11937'
                    },
                    {
                        id: '2',
                        name: '20000'
                    },
                    {
                        id: '3',
                        name: '28001'
                    }
                ]
                this.$store.commit('clearTests')
                this.$store.commit('installTestIds', testIds)
                for (var testId of testIds) {
                    let test = newTest()
                    test.id = testId.id
                    test.name = testId.name
                    this.$store.commit('installTest', test)

                    const variable = newTestVariable()
                    variable.name = 'Patient'
                    variable.id = '1'
                     //variable.testId = testId.id
                    this.$store.commit('installTestVariable', { testId: test.id, part: variable })
                }
            }
        }
    }
</script>
<style scoped>
    .main {
        display: grid;
        grid-template-columns: 20% 80%;
        grid-template-areas: 'nav body';
        grid-gap: 0px;
    }

    .nav {
        text-align: left;
        align-content: start;
        grid-area: nav;
        border: 1px solid black;
    }

    .body {
        text-align: left;
        align-content: start;
        grid-area: body;
        border: 1px solid black;
    }

    a:link {
        text-decoration: none;
    }


</style>
