<template>
    <div>
        <div class="window">
            <form class="grid-container">
                <label class="grid-name">Id</label>
                <div class="grid-item">{{ $route.params.channelId }}</div>

                <label class="grid-name">Test Session</label>


            </form>
        </div>
    </div>
</template>

<script>
    import {store} from "../store"
    export default {
        data () {
            return {
                current: null
            }
        },
        store: store,
        name: "ChannelView"
    }
</script>

<style scoped>
    .window {
        display: grid;
        grid-template-columns: auto auto;
        margin: 5px;
    }
    .grid-container {
        display: grid;
        grid-template-columns: 15ch auto;
        grid-template-rows: auto;

    }
    .grid-name {
        font-weight: bold;
        background-color: rgba(255, 255, 255, 0.8);
        grid-column: 1;
    }
    .grid-item {
        background-color: rgba(255, 255, 255, 0.8);
        grid-column: 2;
    }
</style>
