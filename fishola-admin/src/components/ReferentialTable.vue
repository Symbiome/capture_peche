<template>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator'

@Component
export default class ReferentialTable extends Vue {
    @Prop() data?: any[];
    @Prop() columns!: any[];
    selected?:any;
}
</script>

<style lang="less">

@import "../less/main";

</style>
