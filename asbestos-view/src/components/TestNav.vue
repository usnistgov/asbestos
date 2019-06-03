<template>
    <div>
      <div>Head</div>
        <test-part-nav :testId="id" type="fixtures"></test-part-nav>
        <test-part-nav :testId="id" type="variables"></test-part-nav>
        <test-part-nav :testId="id" type="setups"></test-part-nav>
        <test-part-nav :testId="id" type="tests"></test-part-nav>
        <test-part-nav :testId="id" type="teardowns"></test-part-nav>
    </div>
</template>
<script>
    import TestPartNav from "./TestPartNav";

    export default {
        data() {
            return {

            }
        },
        props: ['id'],  // id of test
        components: {TestPartNav},
        methods: {

        }
    }
</script>
<style scoped>

</style>
