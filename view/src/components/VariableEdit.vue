<template>
  <div>
  <div class="window">
    <form class="grid-container">

      <div class="grid-status">{{ nameErr }}</div>
      <label for="name" class="grid-name">Name</label>
      <input id="name" class="grid-item" v-on:blur="validate" v-model="current.name">

      <div class="grid-status"></div>
      <label for="defaultValue" class="grid-name">DefaultValue</label>
      <input id="defaultValue" class="grid-item" v-model="current.defaultValue">

      <div class="grid-status"></div>
      <label for="description" class="grid-name">Description</label>
      <input id="description" class="grid-item" v-model="current.description">

      <div></div>
      <div class="grid-status"></div>
      <div class="grid-documentation">Hint help text for default value to enter</div>
      <div></div>
      <label for="hint" class="grid-name">Hint</label>
      <input id="hint" class="grid-item" v-model="current.hint">

      <div></div>
      <div class="grid-status"></div>
      <div class="grid-documentation">Fixture Id of source expression or headerField within this variable.
        This can be a statically defined fixture (at the top of the TestScript) or a dynamically
        set fixture created by responseId of the action.operation element</div>
      <div></div>
      <label for="sourceId" class="grid-name">SourceId</label>
      <input id="sourceId" class="grid-item space-after" v-on:blur="validate" v-model="current.sourceId">

      <div class="grid-status yellow-background">{{ expressionErr }}</div>
      <div class="grid-documentation yellow-background bold-font">One of...</div>
      <div class="grid-documentation yellow-background"></div>
      <div class="grid-documentation yellow-background">The FHIRPath expression against the fixture body. If headerField is defined,
        then the variable will be evaluated against the headers that sourceId is pointing to.
        If expression or path is defined, then the variable will be evaluated against the
        fixture body that sourceId is pointing to. It is an error to define any combination of
        expression, headerField and path.</div>
      <div></div>
      <label for="expression" class="grid-name">Expression</label>
      <input id="expression" class="grid-item yellow-background" v-on:blur="validate" v-model="current.expression">



      <div></div>
      <div class="grid-status yellow-background"></div>
      <div class="grid-documentation yellow-background">HTTP header field name for source. If headerField is defined,
        then the variable will be evaluated against the headers that sourceId is pointing to.
        If path is defined, then the variable will be evaluated against the fixture body that
        sourceId is pointing to. It is an error to define both headerField and path.</div>
      <div></div>
      <label for="headerField" class="grid-name">HeaderField</label>
      <input id="headerField" class="grid-item yellow-background" v-on:blur="validate" v-model="current.headerField">

      <div></div>
      <div class="grid-status yellow-background"></div>
      <div class="grid-documentation yellow-background">XPath or JSONPath against the fixture body. If headerField is defined,
        then the variable will be evaluated against the headers that sourceId is pointing to.
        If expression or path is defined, then the variable will be evaluated against the fixture
        body that sourceId is pointing to. It is an error to define any combination of expression,
        headerField and path.</div>
      <div></div>
      <label for="path" class="grid-name">Path</label>
      <input id="path" class="grid-item yellow-background" v-on:blur="validate" v-model="current.path">

    </form>
  </div>
  </div>
</template>

<script>
import { mapMutations } from 'vuex'
import {store} from "../store"

const cloneDeep = require('clone-deep')

export default {
  data () {
    return {
      current: cloneDeep(this.variable),
      nameErr: null,
      expressionErr: null,
      validates: true,
    }
  },
  store: store,
  props: ['variable'],
  created: function () {
    this.validate()
  },
  watch: {
    variable: function (newVal) {
      this.current = cloneDeep(newVal)
      this.validate()
    }
  },
  mounted() {
  },
  computed: {
  },
  methods: {
    ...mapMutations({
      addVariable: 'addTestVariable',
      delVariable: 'delTestVariable'
    }),
    clearErrors () {
      // if (!this.currentId) {
      //   return
      // }
      // console.log('current is ' + this.currentIndex)
      // let variable = this.$store.state.testEditor.variables[this.currentIndex]
      this.nameErr = null
      this.expressionErr = null
      this.validates = true
    },
    validate () {
      // if (!this.currentId) {
      //   return
      // }
      this.clearErrors()
      // let variable = this.$store.state.testEditor.variables[this.currentIndex]
      if (!this.current.name) {
        this.nameErr = 'Name required'
        this.current.validates = false
      }
      if (this.current.name && this.current.name.startsWith('#')) {
        this.nameErr = 'Enter real name'
        this.validates = false
      }
      let count = 0
      if (this.current.expression) {
        count++
      }
      if (this.current.headerField) {
        count++
      }
      if (this.current.path) {
        count++
      }
      if (count > 1) {
        this.expressionErr = 'Only one of expression, headerField, or path may be defined'
      } else if (count === 0) {
        this.expressionErr = 'One of expression, headerField, or path must be defined'
      } else {
        this.expressionErr = null
      }
      if (this.expressionErr) { this.validates = false }
    },
  },
  name: 'VariableEdit'
}

</script>

<style scoped>
.window {
  display: grid;
  grid-template-columns: auto auto;
  margin: 5px;
}
.selectors-group {
}
.selector {
  width: 100px;
  background-color:  white;
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
.grid-status {
  background-color: rgba(255, 255, 255, 0.8);
  color: red;
  grid-column: 2;
}
.grid-documentation {
  font-size: 0.8em;
  background-color: rgba(255, 255, 255, 0.8);
  grid-column: 2;
}
.bold-font {
  font-weight: bold;
}
.haserror {
  color: red;
}
.space-after {
  margin-bottom: 15px;
}
.yellow-background {
  background-color: palegoldenrod;
}
</style>
