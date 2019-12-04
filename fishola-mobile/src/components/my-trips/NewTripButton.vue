<template>
  <div class="new-trip">
    <div>
      <button v-if="count == 0" v-on:click="newTrip"><i class="icon-fishing"/> Commencer</button>
      <button v-if="count != 0" v-on:click="newTrip"><i class="icon-plus"/> Nouveau</button>
    </div>
  </div>
</template>

<script lang="ts">
import Trip from '@/pojos/Trip';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class NexTripButton extends Vue {
  @Prop() count!: Number;
  @Prop() trips!: Trip[];

  mounted() {
  }

  newTrip () {
    let tripsBacklog = [];
    tripsBacklog.push(new Trip('0', 'Sortie du mardi 3 décembre', 'Lac du Bourget', 'Mardi 3 décembre'));
    tripsBacklog.push(new Trip('1', 'Pêche d\'anniversaire', 'Lac Léman', 'Jeudi 21 novembre'));
    tripsBacklog.push(new Trip('2', 'Avec Miguel', 'Lac d\'Annecy', 'Samedi 15 octobre'));
    tripsBacklog.push(new Trip('3', 'Sortie du lundi 2 septembre', 'Lac d\'Aiguebelette', 'Lundi 2 septembre'));
    tripsBacklog.push(new Trip('4', 'Entre filles', 'Lac du Bourget', 'Lundi 28 août'));
    tripsBacklog.push(new Trip('5', 'Sortie du mardi 3 décembre', 'Lac du Bourget', 'Mardi 3 décembre'));
    tripsBacklog.push(new Trip('6', 'Pêche d\'anniversaire', 'Lac Léman', 'Jeudi 21 novembre'));
    tripsBacklog.push(new Trip('7', 'Avec Miguel', 'Lac d\'Annecy', 'Samedi 15 octobre'));
    tripsBacklog.push(new Trip('8', 'Sortie du lundi 2 septembre', 'Lac d\'Aiguebelette', 'Lundi 2 septembre'));
    tripsBacklog.push(new Trip('9', 'Entre filles', 'Lac du Bourget', 'Lundi 28 août'));

    tripsBacklog[0].canBeModified = true;
    tripsBacklog[0].duration = '2h02min';
    tripsBacklog[1].duration = '1h22min';
    tripsBacklog[2].duration = '3h17min';
    tripsBacklog[3].duration = '2h58min';
    tripsBacklog[4].duration = '2h49min';
    tripsBacklog[5].duration = '2h02min';
    tripsBacklog[6].duration = '1h22min';
    tripsBacklog[7].duration = '3h17min';
    tripsBacklog[8].duration = '2h58min';
    tripsBacklog[9].duration = '2h49min';

    this.trips.push(tripsBacklog[this.trips.length % tripsBacklog.length]);
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

@import "../../less/main";

.new-trip {

  height: 40px;
  width: 100%;
  position: absolute;
  bottom: 80px;

  display: flex;
  justify-content: center;

  div {
    height: 40px;
  }

  button {
    height: 100%;
    width: 100%;
    border-radius: 50px;

    font-style: normal;
    font-weight: bold;
    font-size: 18px;
    line-height: 25px;

    color: @white;
    background-color: @terra-cotta;

    border: 0px;
    padding-left: 20px;
    padding-right: 20px;

    i {
      margin-right: 5px;
      font-size: 20px;
    }
  }
}

</style>
