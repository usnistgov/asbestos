<template>
    <div v-if="action && context">
        <div class="selectable" @click.stop="toggleOpen()">
            <span v-if="open">
                <img src="../../assets/arrow-down.png">
            </span>
            <span v-else>
                <img src="../../assets/arrow-right.png"/>
            </span>
            <span class="selectable">Context</span>
            <div v-if="open">
                <vue-markdown>{{filtered}}</vue-markdown>
            </div>
        </div>
    </div>
</template>

<script>
    import VueMarkdown from 'vue-markdown';

    export default {
        data() {
            return {
                open: false,
            }
        },
        computed: {
            filtered() {
                if (this.context === null)
                    return null;
                const start = this.context.indexOf('### Fixtures');
                if (start === -1)
                    return this.context;
                return this.context.substr(start);
            },
            context() {
                if (this.action) {
                    if (this.action.operation) {
                        const extensions = this.action.operation.extension;
                        if (extensions) {
                            for (let i = 0; i < extensions.length; i++) {
                                const extension = extensions[i];
                                if (extension.url === 'urn:action-context') {
                                    return extension.valueString;
                                }
                            }
                        }
                    } else if (this.action.assert) {
                        const extensions = this.action.assert.extension;
                        if (extensions) {
                            for (let i = 0; i < extensions.length; i++) {
                                const extension = extensions[i];
                                if (extension.url === 'urn:action-context') {
                                    return extension.valueString;
                                }
                            }
                        }
                    }
                }
                return null;
            }
        },
        methods: {
            toggleOpen() {
                this.open = !this.open;
            }
        },
        props: [
            'action'
        ],
        components: {
            VueMarkdown,
        },
        name: "ActionContext"
    }
</script>

<style scoped>

</style>
