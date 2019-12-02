<template>
  <div class="my-trips-search">
    <i class="icon-magnifying-glass"></i>
    <input type="text" placeholder="Recherche" name="search_term"/>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

export default {
  props: {
    count: {
      type: String
    }
  },
  name: 'my-trips-search',
  components: {
    // Title,
    // Avatar,
    // FeedbackAnchor,
    // Menu
  }
}

// @Component
// export default class Header extends Vue {
// }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../less/main";

.my-trips-search {

  background-color: @white;

  height: 40px;
  padding-left: 30px;
  border-radius: 30px;
  margin: 20px;
  line-height: 40px;

  .icon-magnifying-glass {
    color: @pelorous;
    margin-right: 10px;
  }

  input {
    border: 0px;
  }

  input::placeholder {
    color: @pale-sky;
    font-style: italic;
    font-weight: normal;
    font-size: 14px;
    // line-height: 19px;
  }
}

</style>
