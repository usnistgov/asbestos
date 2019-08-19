<template>
    <div>
        <router-link to="/session/default/channel">Channels</router-link>
        <div class="divider"/>
        <router-link to="/test">Tests</router-link>
        <div class="divider"/>
        <div v-if="$route.params.sessionId" class="right">Test Session: {{ $route.params.sessionId }}</div>
        <router-view></router-view>
    </div>
</template>

<script>
    export default {
        name: 'TopLayout',
        components: {
        }
    }
</script>
<style scoped>
    .divider{
        width:5px;
        height:auto;
        display:inline-block;
    }
    .right {
        display:inline-block;
        float: right;
    }
</style>
