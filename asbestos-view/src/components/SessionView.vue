<template>
    <div>
        <h2>Session {{ $route.params.sessionId }}</h2>
        <router-view/>
        <router-view name="a"/>
    </div>
</template>

<script>
    export default {
        name: "SessionView"
    }
</script>

<style scoped>

</style>
