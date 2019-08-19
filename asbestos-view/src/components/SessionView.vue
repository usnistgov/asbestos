<template>
    <div class="session_view">
        <router-view name="nav" class="nav"/>
        <router-view name="detail" class="detail"/>
    </div>
</template>

<script>
    export default {
        name: "SessionView"
    }
</script>

<style scoped>
    h2 {
        grid-area: header;
    }
.session_view {
    display: grid;
    grid-template-columns: auto 1fr;
    grid-template-rows: auto;
    grid-template-areas:
            "header header"
            "nav detail"
        ;
}
    .nav {
        grid-area: nav;
        border: 1px dotted;
        margin-right: .75rem;
        padding: .3rem;
    }
    .detail {
        grid-area: detail;
    }
</style>
