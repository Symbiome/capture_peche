<template>
  <div class="forgotten-password hiddenWhenKeyboardShows">
    <a v-on:click="expandCollapse">Mot de passe oublié ? </a>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
/**
* ForgottenPassword component, which, when clicked:
* - hides all the css classes given in props
* - alows user to enter an email and get forgotten password reset mail
*/
@Component
export default class ForgottenPassword extends Vue {

  // Already typed email (typically to reuse email already typed in a login input)
  @Prop() alreadTypedEmail?:string;
  // Css selector describing elements to hide when component gets clicked
  // Example value: ".class1,.class2,.class3"
  @Prop() tohideSelector?:string;

  // Indicates if components is in its collapsed (just forgotten password text) or expanded (with inputs & buttons) form
  private collapsed: boolean = true;

  /*
  * Expand or collapsed the component according to its current state
  */
  expandCollapse() {
    this.hideRevealElements();
    this.collapsed = !this.collapsed;
    this.$root.$emit('toaster-warning', 'Work in progress');
  }

  /**
  * Hides or reveal all elements matching the toHideSelector prop
  */
  hideRevealElements() {
    if (this.tohideSelector != null) {
      let toHides = document.querySelectorAll(this.tohideSelector);
      toHides.forEach( toHide => {
        if (toHide.classList.contains("hidden")) {
          toHide.classList.remove("hidden");
        } else {
          toHide.classList.add("hidden");
        }
      });
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">

  @import "../../less/main";

</style>
