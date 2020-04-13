<template>
    <div  class="container">
        <div v-for="(name, namei) in Object.keys(attMap)" :key="name + namei">
            <span>
                {{ name }} :
            </span>
            <span v-if="typeof attMap[name] === 'string' || attMap[name] instanceof String">
                 {{ attMap[name] }}
            </span>
            <span v-else-if="Array.isArray(attMap[name])">
                <div v-for="(value, index) in attMap[name]" :key="value + index">
                    <span v-if="typeof value === 'string'" class="container">
                        {{ value }}
                    </span>
                    <log-atts-inter v-else :attMap="value"> </log-atts-inter>
                </div>
            </span>
            <span v-else-if="typeof attMap[name] === 'object' && attMap[name] !== null">
                <log-atts-inter :attMap="attMap[name]"> </log-atts-inter>
            </span>
        </div>
    </div>
</template>

<script>
    export default {
        props: [
            'attMap'
        ],
        name: "LogAttsInter"
    }
</script>

<style scoped>
    .details {
        font-size: smaller;
    }
    .container {
        position: relative;
        left: 15px;
        /*displayOpen: grid;*/
        /*border: 1px solid black;*/
    }
    .container name {
        /*grid-column: 1;*/
    }
    .container value {
        /*grid-column: 2;*/
    }
    .container > * {
        /*background-color: #ddd;*/
    }
    .caption {
        font-weight: bold;
    }

</style>
